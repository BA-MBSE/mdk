package gov.nasa.jpl.mbee;

import java.util.ArrayList;

import gov.nasa.jpl.mbee.actions.systemsreasoner.CopyAction;
import gov.nasa.jpl.mbee.actions.systemsreasoner.CreateInstanceAction;
import gov.nasa.jpl.mbee.actions.systemsreasoner.DespecializeAction;
<<<<<<< HEAD
import gov.nasa.jpl.mbee.actions.systemsreasoner.SpecializeAction;
=======
import gov.nasa.jpl.mbee.actions.systemsreasoner.SRAction;
>>>>>>> b748eb4... reasons systemer wip
import gov.nasa.jpl.mbee.actions.systemsreasoner.ValidateAction;
import gov.nasa.jpl.mbee.actions.systemsreasoner.SpecializeAction;

import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.ActionsGroups;
import com.nomagic.magicdraw.actions.BrowserContextAMConfigurator;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.actions.MDActionsCategory;
import com.nomagic.magicdraw.ui.browser.Node;
import com.nomagic.magicdraw.ui.browser.Tree;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

public class SRConfigurator implements BrowserContextAMConfigurator {
<<<<<<< HEAD

=======
	
	private SRAction validateAction;
	private SRAction despecAction;
	private SRAction specAction;
	private SRAction copyAction;
	private SRAction instAction;
	
	
>>>>>>> b748eb4... reasons systemer wip
    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return 0; //medium
    }

    @Override
    public void configure(ActionsManager manager, Tree tree) {
        ActionsCategory category = (ActionsCategory)manager.getActionFor("SRMain");
        if (category == null) {
            category = new MDActionsCategory("SRMain", "Reasons Systemer", null, ActionsGroups.APPLICATION_RELATED);
            category.setNested(true);
            //manager.addCategory(0, category);
        }
<<<<<<< HEAD
        
        if (target instanceof Class) {
        	Class clazz = (Class) target;
        	SpecializeAction specAction = new SpecializeAction(clazz);
        	DespecializeAction despecAction = new DespecializeAction(clazz);
        	ValidateAction valAction = new ValidateAction(clazz);
        	CopyAction copyAction = new CopyAction(clazz);
        	CreateInstanceAction instAction = new CreateInstanceAction(clazz);
        	
        	if (clazz.getGeneralization().isEmpty()) {
        		specAction.disable();
        		despecAction.disable();
=======
    	
    	if (tree.getSelectedNodes().length > 1) {
    		category = handleMultipleNodes(category, tree.getSelectedNodes());
    	} else {
    		category = handleSingleNode(category, tree.getSelectedNode());
    	}
    	
        category.setUseActionForDisable(true);
        if (category.isEmpty()) {
        	final MDAction mda = new MDAction(null, null, null, "null");
        	mda.updateState();
        	mda.setEnabled(false);
        	category.addAction(mda);
        }
    }
    
    public ActionsCategory handleMultipleNodes(ActionsCategory category, Node[] nodes) {
    	ArrayList<Classifier> classes = new ArrayList<Classifier>();
    	
    	for (Node node: nodes) {
	    	if (node != null) {
		    	Object o = node.getUserObject();
		    	if(o instanceof Classifier) {
		    		classes.add((Classifier) o);
		    	}
	    	}
    	}

		validateAction = new ValidateAction(classes);
		category.addAction(validateAction);
		
		specAction = new SpecializeAction(classes);
		category.addAction(specAction);
		
		despecAction = new DespecializeAction(classes);
		category.addAction(despecAction);
		
    	return category;
    }
    
    public ActionsCategory handleSingleNode(ActionsCategory category, Node node) {
    	
//    	ActionsStateUpdater.updateActionsState();
    	
    	category.addAction(validateAction);
    	category.addAction(despecAction);
    	category.addAction(specAction);
    	category.addAction(copyAction);
    	category.addAction(instAction);
    	
    	if (node == null)
    		return category;
    	Object o = node.getUserObject();
    	if(!(o instanceof Element))
    		return category;
    	Element target = (Element) o;
        
<<<<<<< HEAD
    	// First check target instanceof
    	
=======
        // remove later Ivan
        copyAction = new CopyAction(target);
        
    	// check target instanceof
>>>>>>> 0e3f721... Created copy action to test export/import utility
        if (target instanceof Activity) {
        	Activity active = (Activity) target;
        	copyAction = new CopyAction(active);
        }
        if (target instanceof Class) {
        	Class clazz = (Class) target;
        	validateAction = new ValidateAction(clazz);
        	despecAction = new DespecializeAction(clazz);
        	specAction = new SpecializeAction(clazz);
        	copyAction = new CopyAction(clazz);
        	instAction = new CreateInstanceAction(clazz);
        	
        	if (clazz.getGeneralization().isEmpty()) {
        		String noGenError = "No Generalizations";
        		//validateAction.disable(noGenError);
        		despecAction.disable(noGenError);
        	}
        }
        
        if (target instanceof Classifier) {
        	Classifier clazzifier = (Classifier) target;
        	copyAction = new CopyAction(clazzifier);
        }
        
        // Clear out the category of unused actions
        for (NMAction s: category.getActions()) {
        	if (s == null)
        		category.removeAction(s);
        }
        
        // Disable all if not editable target, add error message
        if (!(target.isEditable())) {
        	for (NMAction s: category.getActions()) {
        		SRAction sra = (SRAction) s;
        		sra.disable("Not Editable");
>>>>>>> 9733057... Systems Reasoner now supports multiple selected elements
        	}
        	category.addAction(specAction);
        	category.addAction(despecAction);
        	category.addAction(valAction);
        	category.addAction(copyAction);
        	category.addAction(instAction);
        }
<<<<<<< HEAD
        category.setUseActionForDisable(true);
        if (category.isEmpty()) {
        	// copy of a hacked thing from DocGenConfigurator line 183-ish
        	final MDAction mda = new MDAction(null, null, null, "null");
        	mda.updateState();
        	mda.setEnabled(false);
        	category.addAction(mda);
        }
=======
        return category;
>>>>>>> 9733057... Systems Reasoner now supports multiple selected elements
    }
}
