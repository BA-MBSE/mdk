package gov.nasa.jpl.mbee.actions.ems.sync;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import gov.nasa.jpl.mbee.actions.LockAction;
import gov.nasa.jpl.mbee.actions.systemsreasoner.SRAction;
import gov.nasa.jpl.mbee.ems.sync.delta.SyncElement;
import gov.nasa.jpl.mbee.ems.sync.delta.SyncElements;
import gov.nasa.jpl.mbee.ems.sync.jms.JMSMessageListener;
import gov.nasa.jpl.mbee.ems.sync.jms.JMSSyncProjectEventListenerAdapter;
import gov.nasa.jpl.mbee.ems.sync.local.LocalSyncProjectEventListenerAdapter;
import gov.nasa.jpl.mbee.ems.sync.local.LocalSyncTransactionCommitListener;
import gov.nasa.jpl.mbee.lib.Changelog;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationRule;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationRuleViolation;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationSuite;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ViolationSeverity;
import org.json.simple.JSONObject;

import javax.annotation.CheckForNull;
import java.awt.event.ActionEvent;
import java.util.*;

/**
 * Created by igomes on 12/5/16.
 */
public class DetailedSyncStatusAction extends SRAction {
    public DetailedSyncStatusAction() {
        super(DetailedSyncStatusAction.class.getSimpleName());
    }

    private static final ValidationSuite validationSuite = new ValidationSuite("Sync Status");

    private static final ValidationRule
            locallyCreatedValidationRule = new ValidationRule("Locally Created", "The model shall not have any locally created elements that are unsynced.", ViolationSeverity.INFO),
            locallyUpdatedValidationRule = new ValidationRule("Locally Updated", "The model shall not have any locally updated elements that are unsynced.", ViolationSeverity.INFO),
            locallyDeletedValidationRule = new ValidationRule("Locally Deleted", "The model shall not have any locally deleted elements that are unsynced.", ViolationSeverity.INFO),
            mmsCreatedValidationRule = new ValidationRule("MMS Created", "The model shall not have any MMS created elements that are unsynced.", ViolationSeverity.INFO),
            mmsUpdatedValidationRule = new ValidationRule("MMS Updated", "The model shall not have any MMS updated elements that are unsynced.", ViolationSeverity.INFO),
            mmsDeletedValidationRule = new ValidationRule("MMS Deleted", "The model shall not have any MMS deleted elements that are unsynced.", ViolationSeverity.INFO);

    private static final Map<SyncElement.Type, Map<Changelog.ChangeType, ValidationRule>> validationRuleMap = new HashMap<>(SyncElement.Type.values().length);

    static {
        validationSuite.addValidationRule(locallyCreatedValidationRule);
        validationSuite.addValidationRule(locallyUpdatedValidationRule);
        validationSuite.addValidationRule(locallyDeletedValidationRule);
        validationSuite.addValidationRule(mmsCreatedValidationRule);
        validationSuite.addValidationRule(mmsUpdatedValidationRule);
        validationSuite.addValidationRule(mmsDeletedValidationRule);

        Map<Changelog.ChangeType, ValidationRule> localValidationRuleMap = new HashMap<>(SyncElement.Type.values().length);
        localValidationRuleMap.put(Changelog.ChangeType.CREATED, locallyCreatedValidationRule);
        localValidationRuleMap.put(Changelog.ChangeType.UPDATED, locallyUpdatedValidationRule);
        localValidationRuleMap.put(Changelog.ChangeType.DELETED, locallyDeletedValidationRule);
        validationRuleMap.put(SyncElement.Type.LOCAL, localValidationRuleMap);

        Map<Changelog.ChangeType, ValidationRule> mmsValidationRuleMap = new HashMap<>(SyncElement.Type.values().length);
        mmsValidationRuleMap.put(Changelog.ChangeType.CREATED, mmsCreatedValidationRule);
        mmsValidationRuleMap.put(Changelog.ChangeType.UPDATED, mmsUpdatedValidationRule);
        mmsValidationRuleMap.put(Changelog.ChangeType.DELETED, mmsDeletedValidationRule);
        validationRuleMap.put(SyncElement.Type.MMS, mmsValidationRuleMap);
    }

    @Override
    public void actionPerformed(@CheckForNull ActionEvent actionEvent) {
        super.actionPerformed(actionEvent);

        for (ValidationRule validationRule : validationSuite.getValidationRules()) {
            validationRule.getViolations().clear();
        }
        Project project = Application.getInstance().getProject();
        for (SyncElement.Type syncElementType : SyncElement.Type.values()) {
            Collection<SyncElement> syncElements = SyncElements.getAllOfType(project, syncElementType);
            for (SyncElement syncElement : syncElements) {
                Changelog<String, Void> changelog = SyncElements.buildChangelog(syncElement);
                for (Changelog.ChangeType changeType : Changelog.ChangeType.values()) {
                    for (String key : changelog.get(changeType).keySet()) {
                        String comment = "[" + syncElementType.name() + "] [" + changeType.name() + "]";
                        BaseElement baseElement = project.getElementByID(key);
                        Element element = null;
                        if (baseElement instanceof Element) {
                            element = (Element) baseElement;
                        }
                        else {
                            comment += " " + key;
                        }
                        validationRuleMap.get(syncElementType).get(changeType).addViolation(new ValidationRuleViolation(element, comment));
                    }
                }
            }
        }
        for (Changelog.ChangeType changeType : Changelog.ChangeType.values()) {
            LocalSyncTransactionCommitListener localSyncTransactionCommitListener = LocalSyncProjectEventListenerAdapter.getProjectMapping(project).getLocalSyncTransactionCommitListener();
            if (localSyncTransactionCommitListener != null) {
                ValidationRule validationRule = validationRuleMap.get(SyncElement.Type.LOCAL).get(changeType);
                for (Map.Entry<String, Element> entry : localSyncTransactionCommitListener.getInMemoryLocalChangelog().get(changeType).entrySet()) {
                    Element element = entry.getValue();
                    validationRule.addViolation(element, "[" + SyncElement.Type.LOCAL.name() + "] [" + changeType.name() + "]" + (element != null && !project.isDisposed(element) ? "" : " " + entry.getKey()));
                }
            }
            JMSMessageListener jmsMessageListener = JMSSyncProjectEventListenerAdapter.getProjectMapping(project).getJmsMessageListener();
            if (jmsMessageListener != null) {
                ValidationRule validationRule = validationRuleMap.get(SyncElement.Type.MMS).get(changeType);
                for (Map.Entry<String, JSONObject> entry : jmsMessageListener.getInMemoryJMSChangelog().get(changeType).entrySet()) {
                    BaseElement baseElement = project.getElementByID(entry.getKey());
                    validationRule.addViolation(new ValidationRuleViolation(baseElement instanceof Element ? (Element) baseElement : null, "[" + SyncElement.Type.MMS.name() + "] [" + changeType.name() + "]" + (baseElement instanceof Element ? "" : " " + entry.getKey())));
                }
            }
        }

        if (project.isRemote()) {
            for (ValidationRule validationRule : validationSuite.getValidationRules()) {
                for (ValidationRuleViolation validationRuleViolation : validationRule.getViolations()) {
                    // TODO Add CopyActions/diff/etc. @donbot
                    validationRuleViolation.addAction(new LockAction(validationRuleViolation.getElement(), false));
                    validationRuleViolation.addAction(new LockAction(validationRuleViolation.getElement(), true));
                }
            }
        }

        if (validationSuite.hasErrors()) {
            Utils.displayValidationWindow(validationSuite, "Sync Status");
        }
        else {
            Application.getInstance().getGUILog().log("[INFO] No unsynced elements detected.");
        }
    }
}
