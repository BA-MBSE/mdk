package gov.nasa.jpl.mbee.mdk.mms.sync.local;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.uml2.ext.jmi.UML2MetamodelConstants;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.*;
import com.nomagic.uml2.impl.PropertyNames;
import com.nomagic.uml2.transaction.TransactionCommitListener;
import gov.nasa.jpl.mbee.mdk.api.incubating.convert.Converters;
import gov.nasa.jpl.mbee.mdk.mms.sync.status.SyncStatusConfigurator;
import gov.nasa.jpl.mbee.mdk.util.Changelog;
import gov.nasa.jpl.mbee.mdk.util.MDUtils;
import gov.nasa.jpl.mbee.mdk.options.MDKOptionsGroup;

import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class responds to commits done in the document.
 *
 * @author igomes
 */
public class LocalSyncTransactionCommitListener implements TransactionCommitListener {
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static final List<String> IGNORED_PROPERTY_CHANGE_EVENT_NAMES = Arrays.asList(
            PropertyNames.PACKAGED_ELEMENT,
            UML2MetamodelConstants.ID,
            PropertyNames.NESTED_CLASSIFIER
    );

    private Project project;

    /**
     * Allow listener to be disabled during imports.
     */
    private final AtomicBoolean disabled = new AtomicBoolean();
    private final Changelog<String, Element> inMemoryLocalChangelog = new Changelog<>();

    {
        if (MDUtils.isDeveloperMode()) {
            inMemoryLocalChangelog.setShouldLogChanges(true);
        }
    }

    public LocalSyncTransactionCommitListener(Project project) {
        this.project = project;
    }

    public boolean isDisabled() {
        synchronized (this.disabled) {
            return (disabled.get() || !MDKOptionsGroup.getMDKOptions().isChangeListenerEnabled());
        }
    }

    public void setDisabled(boolean disabled) {
        synchronized (this.disabled) {
            this.disabled.set(disabled);
        }
    }

    public Changelog<String, Element> getInMemoryLocalChangelog() {
        return inMemoryLocalChangelog;
    }

    @Override
    public Runnable transactionCommited(Collection<PropertyChangeEvent> events) {
        if (isDisabled()) {
            return null;
        }
        return new TransactionCommitHandler(events, project);
    }

    private class TransactionCommitHandler implements Runnable {
        private final Collection<PropertyChangeEvent> events;
        private final Project project;

        TransactionCommitHandler(final Collection<PropertyChangeEvent> events, Project project) {
            this.events = events;
            this.project = project;
        }

        @Override
        public void run() {
            try {
                CountDownLatch doneSignal = new CountDownLatch(events.size());
                new Thread(() -> {
                    try {
                        doneSignal.await();
                    } catch (InterruptedException ignored) {
                    }
                    SyncStatusConfigurator.getSyncStatusAction().update();
                }, "Sync Status Update").start();
                for (PropertyChangeEvent event : events) {
                    EXECUTOR_SERVICE.execute(() -> {
                        Object source = event.getSource();
                        if (!(source instanceof Element) || ProjectUtilities.isElementInAttachedProject((Element) source)) {
                            doneSignal.countDown();
                            return;
                        }
                        Element sourceElement = (Element) source;
                        String changedPropertyName = event.getPropertyName();
                        if (changedPropertyName == null || changedPropertyName.startsWith("_") || IGNORED_PROPERTY_CHANGE_EVENT_NAMES.contains(changedPropertyName)) {
                            doneSignal.countDown();
                            return;
                        }
                        if ((event.getNewValue() == null && event.getOldValue() == null) || (event.getNewValue() != null && event.getNewValue().equals(event.getOldValue()))) {
                            doneSignal.countDown();
                            return;
                        }

                        if (!changedPropertyName.equals(UML2MetamodelConstants.INSTANCE_DELETED)) {
                            Element root = sourceElement;
                            while (root.getOwner() != null) {
                                root = root.getOwner();
                            }
                            if (!root.equals(project.getModel())) {
                                doneSignal.countDown();
                                return;
                            }
                        }

                        // START PRE-PROCESSING
                        Comment comment;
                        if (changedPropertyName.equals(PropertyNames.BODY) && sourceElement instanceof Comment && (comment = (Comment) sourceElement).getAnnotatedElement().size() == 1 && comment.getAnnotatedElement().iterator().next() == comment.getOwner()) {
                            sourceElement = sourceElement.getOwner();
                        }
                        else if (changedPropertyName.equals(PropertyNames.VALUE) && sourceElement instanceof ValueSpecification ||
                                changedPropertyName.equals(PropertyNames.BODY) && sourceElement instanceof OpaqueExpression ||
                                changedPropertyName.equals(PropertyNames.OPERAND) && sourceElement instanceof Expression) {
                            // Need to find the actual element that needs to be sent (most likely a Property or Slot that's the closest owner of this element)
                            do {
                                sourceElement = sourceElement.getOwner();
                            }
                            while (sourceElement instanceof ValueSpecification);
                            // There may be multiple ValueSpecification changes so go up the chain of owners until we find the actual owner that should be submitted
                        }
                        // END PRE-PROCESSING

                        if (Converters.getElementToJsonConverter().apply(sourceElement, project) == null) {
                            doneSignal.countDown();
                            return;
                        }
                        String sysmlId = Converters.getElementToIdConverter().apply(sourceElement);
                        if (sysmlId == null) {
                            doneSignal.countDown();
                            return;
                        }

                        Changelog.ChangeType changeType = Changelog.ChangeType.UPDATED;
                        switch (changedPropertyName) {
                            case UML2MetamodelConstants.INSTANCE_DELETED:
                                changeType = Changelog.ChangeType.DELETED;
                                break;
                            case UML2MetamodelConstants.INSTANCE_CREATED:
                                changeType = Changelog.ChangeType.CREATED;
                                break;
                        }
                        inMemoryLocalChangelog.addChange(sysmlId, sourceElement, changeType);
                        doneSignal.countDown();
                    });
                }
            } catch (Exception e) {
                Application.getInstance().getGUILog().log("[ERROR] LocalSyncTransactionCommitListener had an unexpected error: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
    }
}
