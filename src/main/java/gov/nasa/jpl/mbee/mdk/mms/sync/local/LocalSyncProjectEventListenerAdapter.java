package gov.nasa.jpl.mbee.mdk.mms.sync.local;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectEventListenerAdapter;
import com.nomagic.magicdraw.uml.transaction.MDTransactionManager;
import gov.nasa.jpl.mbee.mdk.api.incubating.convert.Converters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by igomes on 6/28/16.
 */
public class LocalSyncProjectEventListenerAdapter extends ProjectEventListenerAdapter {
    private static final Map<Project, LocalSyncProjectMapping> projectMappings = new ConcurrentHashMap<>();

    @Override
    public void projectCreated(Project project) {
        projectOpened(project);
    }

    // Cannot rely on this being called when MagicDraw programmatically reloads a project, which makes the old Project stale.
    // Mitigating by moving all logic to mapping constructor, but this leaves gaps where events may not be captured.
    @Override
    public void projectOpened(Project project) {
        closeLocalCommitListener(project);
        getProjectMapping(project);
    }

    @Override
    public void projectClosed(Project project) {
        closeLocalCommitListener(project);
        projectMappings.remove(project);
    }

    @Override
    public void projectReplaced(Project oldProject, Project newProject) {
        projectClosed(oldProject);
        projectOpened(newProject);
    }

    @Override
    public void projectSaved(Project project, boolean savedInServer) {
        LocalSyncProjectMapping localSyncProjectMapping = getProjectMapping(project);
        LocalSyncTransactionCommitListener listener = localSyncProjectMapping.getLocalSyncTransactionCommitListener();
        if (listener == null) {
            projectOpened(project);
            listener = LocalSyncProjectEventListenerAdapter.getProjectMapping(project).getLocalSyncTransactionCommitListener();
        }
        listener.getInMemoryLocalChangelog().clear();
    }

    private static void closeLocalCommitListener(Project project) {
        LocalSyncProjectMapping localSyncProjectMapping = projectMappings.get(project);
        if (localSyncProjectMapping != null && localSyncProjectMapping.getLocalSyncTransactionCommitListener() != null) {
            project.getRepository().getTransactionManager().removeTransactionCommitListener(localSyncProjectMapping.getLocalSyncTransactionCommitListener());
        }
    }

    public static LocalSyncProjectMapping getProjectMapping(Project project) {
        LocalSyncProjectMapping localSyncProjectMapping = projectMappings.get(project);
        if (localSyncProjectMapping == null) {
            projectMappings.put(project, localSyncProjectMapping = new LocalSyncProjectMapping(project));
            if (project.isRemote()) {
                ((MDTransactionManager) project.getRepository().getTransactionManager()).addTransactionCommitListenerIncludingUndoAndRedo(localSyncProjectMapping.getLocalSyncTransactionCommitListener());
            }
        }
        return localSyncProjectMapping;
    }

    public static class LocalSyncProjectMapping {
        private LocalSyncTransactionCommitListener localSyncTransactionCommitListener;

        public LocalSyncProjectMapping (Project project) {
            localSyncTransactionCommitListener = new LocalSyncTransactionCommitListener(project);
        }

        public LocalSyncTransactionCommitListener getLocalSyncTransactionCommitListener() {
            return localSyncTransactionCommitListener;
        }

    }
}
