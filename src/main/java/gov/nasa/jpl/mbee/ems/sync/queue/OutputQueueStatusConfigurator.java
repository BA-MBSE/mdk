package gov.nasa.jpl.mbee.ems.sync.queue;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import gov.nasa.jpl.mbee.DocGenPlugin;

public class OutputQueueStatusConfigurator implements AMConfigurator {

    private static final OutputQueueStatusAction outputQueueStatusAction = new OutputQueueStatusAction();

    public static OutputQueueStatusAction getOutputQueueStatusAction() {
        return outputQueueStatusAction;
    }

    @Override
    public int getPriority() {
        return AMConfigurator.MEDIUM_PRIORITY;
    }

    @Override
    public void configure(ActionsManager actionsManager) {
        DocGenPlugin.MAIN_TOOLBAR_ACTIONS_MANAGER = actionsManager;
        ActionsCategory category = actionsManager.getCategory(DocGenPlugin.MAIN_TOOLBAR_CATEGORY_NAME);
        if (category == null) {
            category = new ActionsCategory(DocGenPlugin.MAIN_TOOLBAR_CATEGORY_NAME, DocGenPlugin.MAIN_TOOLBAR_CATEGORY_NAME);
            ActionsCategory lastActionsCategory = actionsManager.getLastActionsCategory();
            if (lastActionsCategory != null) {
                actionsManager.addCategory(lastActionsCategory, category, true);
            }
            else {
                actionsManager.addCategory(category);
            }
        }
        category.addAction(outputQueueStatusAction);
    }
}
