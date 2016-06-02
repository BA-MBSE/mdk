package gov.nasa.jpl.mbee.systemsreasoner.validation.actions;

import gov.nasa.jpl.mbee.systemsreasoner.validation.GenericRuleViolationAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.nomagic.magicdraw.copypaste.CopyPasting;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.Activity;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Namespace;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.RedefinableElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.TypedElement;
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdbasicbehaviors.Behavior;

public class RedefineActionAction extends GenericRuleViolationAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_NAME = "Redefine Action";

	private Classifier clazz;
	private RedefinableElement re;
	private boolean createSpecializedType;
	private String name;

	public RedefineActionAction(final Classifier clazz, final RedefinableElement re) {
		this(clazz, re, false, DEFAULT_NAME);
	}

	public RedefineActionAction(final Classifier clazz, final RedefinableElement re, final boolean createSpecializedType, final String name) {
		super(name);
		this.clazz = clazz;
		this.re = re;
		this.createSpecializedType = createSpecializedType;
		this.name = name;
	}

	public static RedefinableElement redefineAttribute(final Classifier clazz, final RedefinableElement re, final boolean createSpecializedType) {
		return redefineAttribute(clazz, re, createSpecializedType, new ArrayList<Property>());
	}

	public static RedefinableElement redefineAttribute(final Classifier clazz, final RedefinableElement re, final boolean createSpecializedType, final List<Property> traveled) {
		if (re.isLeaf()) {
			Application.getInstance().getGUILog().log(re.getQualifiedName() + " is a leaf. Cannot redefine further.");
		}

		if (!clazz.isEditable()) {
			Application.getInstance().getGUILog().log(clazz.getQualifiedName() + " is not editable. Skipping redefinition.");
			return null;
		}

		if (clazz instanceof Activity) {
			Activity act = (Activity) clazz;
			RedefinableElement redefinedElement = null;
			for (final Behavior p : act.getOwnedBehavior()) {
				if (p instanceof RedefinableElement && ((RedefinableElement) p).getRedefinedElement().contains(re)) {
					redefinedElement = (RedefinableElement) p;
					break;
				}
			}
			if (redefinedElement == null) {
				redefinedElement = (RedefinableElement) CopyPasting.copyPasteElement(re, act, false);
				if (redefinedElement instanceof Namespace) {
					Collection<?> emptyCollection = new ArrayList<String>();
					((Namespace) redefinedElement).getOwnedMember().retainAll(emptyCollection); 
				}
				redefinedElement.getRedefinedElement().add((RedefinableElement) re);
				if (createSpecializedType && redefinedElement instanceof Property && redefinedElement instanceof TypedElement && ((TypedElement) redefinedElement).getType() != null) {
					CreateSpecializedTypeAction.createSpecializedType((Property) redefinedElement, clazz, true, traveled);
				}
				return redefinedElement;
			} else {
				Application.getInstance().getGUILog().log(re.getQualifiedName() + " has already been redefined in " + clazz.getQualifiedName() + ".");
				return null;
			}
		} else {
			Application.getInstance().getGUILog().log(clazz.getQualifiedName() + " is not an Activity.");
			return null;
		}

	}

	@Override
	public void run() {
		redefineAttribute(clazz, re, createSpecializedType);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSessionName() {
		return "redefine action";
	}
}
