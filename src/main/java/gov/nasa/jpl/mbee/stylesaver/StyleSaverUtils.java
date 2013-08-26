package gov.nasa.jpl.mbee.stylesaver;

import java.util.Collection;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.teamwork.application.TeamworkUtils;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

/**
 * Utility functions for the Styler should be put here.
 * 
 * @author Benjamin Inada, JPL/Caltech
 */
public class StyleSaverUtils {
	/**
	 * Returns the correct View or derived stereotype necessary for saving styles in this project.
	 * 
	 * @param proj	the project to look up.
	 * @return		the working stereotype for this project or null if one does not exist.
	 */
	public static Stereotype getWorkingStereotype(Project proj) {
		Stereotype workingStereotype = null;
		String[] possStereotypes = { "view", "DGView", "View" };
		int index = 0;
		
		while((workingStereotype == null) && (index < possStereotypes.length)) {			
			workingStereotype = StereotypesHelper.getStereotype(proj, possStereotypes[index], "Document Profile 3");
			
			index++;
		}
		
		if(workingStereotype == null) {
			return null;
		}

		// get all the owned elements of the stereotype
		Collection<Element> ownedElems = workingStereotype.getOwnedElement();
		boolean stylePropertyFound = false;
		
		if(ownedElems == null) {
			/* 
			 * NOTE: may be a good idea to implement usage of a "style block" associated
			 * with this diagram if the style property is not found.
			 * 
			 * We will just return null and exit the program for now.
			 */
			
			return null;
		}
	
		// search for the style property
		for(Element elem : ownedElems) {
			if(elem.getHumanName().equals("Property style")) {
				stylePropertyFound = true;
			}
		}

		if(!stylePropertyFound) {
			// see block comment above
			return null;
		}
	
		return workingStereotype;
	}
	
	/**
	 * Checks if the diagram has the actual working stereotype or derived.
	 * Also checks if the working stereotype has a style tag associated with it.
	 * 
	 * @param diag				the diagram to check
	 * @param workingStereotype the stereotype to check
	 * @return 					true if the diagram is stereotyped property, false otherwise
	 */
	public static boolean isGoodStereotype(DiagramPresentationElement diag, Stereotype workingStereotype) {
		boolean hasStereotype = StereotypesHelper.hasStereotypeOrDerived(diag.getElement(), workingStereotype);
		boolean hasSlot = StereotypesHelper.getPropertyByName(workingStereotype, "style") != null;
		
		return hasStereotype && hasSlot;
	}
	
	/**
	 * Checks if the diagram is a locked Teamwork project.
	 * 
	 * @param project	the project that contains the diagram.
	 * @param diagram	the diagram to check
	 * @return			true if the diagram is locked, false otherwise
	 */
	public static boolean isDiagramLocked(Project project, Element diagram) {
		// get all the locked elements in the project
		Collection<Element> lockedElems;
		try {
			lockedElems = TeamworkUtils.getLockedElement(project, null);
		} catch(NullPointerException e) {
			// looks like this is just a local project
			return true;
		}
		
		// try to find the diagram in the collection of locked project elements
		for(Element elem : lockedElems) {
			if(elem.getID().equals(diagram.getID())) {
				return true;
			}
		}
		
		return false;
	}
}