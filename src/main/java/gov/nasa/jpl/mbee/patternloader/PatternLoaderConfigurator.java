package gov.nasa.jpl.mbee.patternloader;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.ConfiguratorWithPriority;
import com.nomagic.magicdraw.actions.DiagramContextAMConfigurator;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.PresentationElement;

/**
 * A class for configurating the Pattern Loader right-click menu option.
 * 
 * @author Benjamin Inada, JPL/Caltech
 */
public class PatternLoaderConfigurator implements DiagramContextAMConfigurator {
    /**
     * Configures the element right-click menu option.
     * 
     * @param manager
     *            the actions manager to add the category to.
     * @param diagram
     *            the diagram to configurate.
     * @param selected
     *            the selected elements.
     * @param requester
     *            the element that was right-clicked
     */
    @Override
    public void configure(ActionsManager manager, DiagramPresentationElement diagram,
            PresentationElement[] selected, PresentationElement requester) {
        // check if the category was already added to the manager
        if (manager.getActionFor("PatternLoader") != null) {
            return;
        }

        // check that the requester is good for either an element or diagram
        if (PatternLoaderUtils.isGoodElementRequester(requester)) {
            ActionsCategory category = new ActionsCategory("Pattern Loader", "Pattern Loader");
            category.addAction(new PatternLoader("PatternLoader", "Pattern Loader", 0, null, requester));

            manager.addCategory(1, category);
        } else if ((requester == null) && (PatternLoaderUtils.isGoodElementRequester(diagram))) {
            ActionsCategory category = new ActionsCategory("Pattern Loader", "Pattern Loader");
            category.addAction(new PatternLoader("PatternLoader", "Pattern Loader", 0, null, requester));

            manager.addCategory(1, category);
        }
    }

    /**
     * Gets the priority of the configurator.
     * 
     * @return the priority.
     */
    @Override
    public int getPriority() {
        return ConfiguratorWithPriority.MEDIUM_PRIORITY;
    }
}
