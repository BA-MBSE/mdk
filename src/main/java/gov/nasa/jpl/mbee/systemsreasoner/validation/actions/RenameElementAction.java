package gov.nasa.jpl.mbee.systemsreasoner.validation.actions;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;

public class RenameElementAction extends MDAction {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private NamedElement source, target;

	public RenameElementAction(final NamedElement source, final NamedElement target, final String title) {
		super(title, title, null, null);
		this.source = source;
		this.target = target;
	}
	
	@Override
	public void actionPerformed(java.awt.event.ActionEvent e) {
		SessionManager.getInstance().createSession("rename element");
		target.setName(source.getName());
		SessionManager.getInstance().closeSession();
	}
}
