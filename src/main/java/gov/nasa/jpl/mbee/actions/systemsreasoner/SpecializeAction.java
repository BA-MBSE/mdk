package gov.nasa.jpl.mbee.actions.systemsreasoner;

<<<<<<< HEAD
=======
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mbee.lib.Utils2;

import java.awt.Frame;
>>>>>>> b748eb4... reasons systemer wip
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import com.nomagic.magicdraw.ui.dialogs.SelectElementInfo;
import com.nomagic.magicdraw.ui.dialogs.SelectElementTypes;
import com.nomagic.magicdraw.ui.dialogs.selection.ElementSelectionDlg;
import com.nomagic.magicdraw.ui.dialogs.selection.ElementSelectionDlgFactory;
import com.nomagic.magicdraw.ui.dialogs.selection.SelectionMode;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;

public class SpecializeAction extends SRAction {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final String actionid = "Specialize";
<<<<<<< HEAD
	public Element element;
	
<<<<<<< HEAD
	public SpecializeAction(Element element) {
        super(actionid, actionid, null, ActionsGroups.APPLICATION_RELATED);
        this.element = element;
=======
	public SpecializeAction(Class clazz) {
<<<<<<< HEAD
        super(actionid);
        this.clazz = clazz;
>>>>>>> 0f4609b... made a super class to handle the enabling and disabling
    }
=======
        super(actionid, clazz);
		this.clazz = clazz;
	}
>>>>>>> e5c673c... Altered where element attached to Actions is stored, now in the superclass

	@Override
    public void actionPerformed(ActionEvent e) {
		if (!(element instanceof Class)) {
			return;
		}
		
		final Class clazz = (Class) element;
		//clazz.
=======
	public List<Classifier> classifiers;
	
	public SpecializeAction(Classifier classifier) {
        this(Utils2.newList(classifier));
	}
	
	public SpecializeAction(List<Classifier> classifiers) {
		super(actionid);
		this.classifiers = classifiers;
	}
	
	@Override
    public void actionPerformed(ActionEvent e) {
		final List<java.lang.Class<?>> types = new ArrayList<java.lang.Class<?>>();
		types.add(com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class.class);
		
		final Frame dialogParent = MDDialogParentProvider.getProvider().getDialogParent();
		final ElementSelectionDlg dlg = ElementSelectionDlgFactory.create(dialogParent);
>>>>>>> b748eb4... reasons systemer wip
		
		final SelectElementTypes set = new SelectElementTypes(types, types, null, null);
		final SelectElementInfo sei = new SelectElementInfo(true, false, Application.getInstance().getProject().getModel().getOwner(), true);
		ElementSelectionDlgFactory.initMultiple(dlg, set, sei, new ArrayList<Object>());
		dlg.setSelectionMode(SelectionMode.MULTIPLE_MODE);
		if (dlg != null) {
			dlg.setVisible(true);
			if (dlg.isOkClicked() && dlg.getSelectedElements() != null && !dlg.getSelectedElements().isEmpty()) {
				SessionManager.getInstance().createSession("creating generalizations");
				for (final BaseElement be : dlg.getSelectedElements()) {
					if (be instanceof Classifier) {
						for (final Classifier specific : classifiers) {
							Utils.createGeneralization((Classifier) be, specific);
						}
					}
				}
				SessionManager.getInstance().closeSession();
				new ValidateAction(classifiers).actionPerformed(null);
			}
		}
	}
}
