package gov.nasa.jpl.mbee.generator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.json.simple.JSONArray;

import gov.nasa.jpl.mbee.ems.validation.ImageValidator;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mbee.model.DocBookOutputVisitor;
import gov.nasa.jpl.mbee.model.Document;
import gov.nasa.jpl.mbee.viewedit.DBAlfrescoVisitor;
import gov.nasa.jpl.mbee.viewedit.PresentationElement;
import gov.nasa.jpl.mbee.viewedit.PresentationElement.PEType;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.DBBook;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationSuite;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdinformationflows.InformationFlow;
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies.Dependency;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ElementValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Expression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralString;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Relationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.TypedElement;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.impl.ElementsFactory;

public class ViewPresentationGenerator {
	private Classifier paraC = Utils.getOpaqueParaClassifier();
	private Classifier tableC = Utils.getOpaqueTableClassifier();
	private Classifier listC = Utils.getOpaqueListClassifier();
	private Classifier imageC = Utils.getOpaqueImageClassifier();
	private Classifier sectionC = Utils.getSectionClassifier();
	private Property generatedFromView = Utils.getGeneratedFromViewProperty();
	private Property generatedFromElement = Utils.getGeneratedFromElementProperty();
	private Stereotype presentsS = Utils.getPresentsStereotype();
	private ElementsFactory ef = Application.getInstance().getProject().getElementsFactory();

	private boolean recurse;
	private Element view;

	public ViewPresentationGenerator(Element view, boolean recursive) {
		this.view = view;
		this.recurse = recursive;
	}

	public void generate() {
		DocumentValidator dv = new DocumentValidator(view);
		dv.validateDocument();
		if (dv.isFatal()) {
			dv.printErrors(false);
			return;
		}
		// first run a local generation of the view model to get the current
		// model view structure
		DocumentGenerator dg = new DocumentGenerator(view, dv, null);
		Document dge = dg.parseDocument(true, recurse, false);
		(new PostProcessor()).process(dge);

		DocBookOutputVisitor visitor = new DocBookOutputVisitor(true);
		DBAlfrescoVisitor visitor2 = new DBAlfrescoVisitor(recurse, true);
		dge.accept(visitor);
		DBBook book = visitor.getBook();
		if (book == null)
			return;
		book.accept(visitor2);
		Map<Element, List<PresentationElement>> view2pe = visitor2.getView2Pe();
		Map<Element, List<PresentationElement>> view2unused = visitor2.getView2Unused();
		Map<Element, JSONArray> view2elements = visitor2.getView2Elements();
		if (!visitor2.getNotEditable().isEmpty()) {
			// also check for the View Instances editability
			Application.getInstance().getGUILog().log("There are instances or view constraints/views that are not editable. Instance generation aborted.");
			return;
		}

		SessionManager.getInstance().createSession("view instance gen");
		try {
			viewInstanceBuilder(view2pe, view2unused);
			for (Element v : view2elements.keySet()) {
				JSONArray es = view2elements.get(v);
				StereotypesHelper.setStereotypePropertyValue(v, Utils.getViewClassStereotype(), "elements", es.toJSONString());
			}
			SessionManager.getInstance().closeSession();
		} catch (Exception e) {
			Utils.printException(e);
			SessionManager.getInstance().cancelSession();
		}

		ImageValidator iv = new ImageValidator(visitor2.getImages());
		// this checks images generated from the local generation against what's
		// on the web based on checksum
		iv.validate();
		ValidationSuite imageSuite = iv.getSuite();
		List<ValidationSuite> vss = new ArrayList<ValidationSuite>();
		vss.add(imageSuite);
		Utils.displayValidationWindow(vss, "Images Validation");
	}

	private void viewInstanceBuilder(Map<Element, List<PresentationElement>> view2pe, Map<Element, List<PresentationElement>> view2unused) {
		// first pass through all the views and presentation elements to handle them
		for (Element v : view2pe.keySet()) {
			handleViewOrSection(v, null, view2pe.get(v));
		}
		// then, pass through all the unused PresentationElements and move their particular
		// InstanceSpecification to the unused InstSpec package
		for (List<PresentationElement> presElems : view2unused.values()) {
	        	for (PresentationElement presentationElement : presElems) {
	        		InstanceSpecification is = presentationElement.getInstance();
	        		is.setOwner(createUnusedInstancesPackage());
	        	}
	    }
	}

	private void handleViewOrSection(Element view, InstanceSpecification section, List<PresentationElement> pes) {
		// first clear out any instancespec exp in the element already
		Package owner = getFolder(view);
		List<InstanceValue> list = new ArrayList<InstanceValue>();
		for (PresentationElement pe : pes) {
			if (pe.isManual()) {
				InstanceValue iv = ef.createInstanceValueInstance();
				iv.setInstance(pe.getInstance());
				list.add(iv);
				continue;
			}
			InstanceSpecification is = pe.getInstance();
			// check here to make sure that the owner is correct
			if (is == null) {
				is = ef.createInstanceSpecificationInstance();
				
				is.setName(pe.getName());
				if (pe.getType() == PEType.PARA)
					is.getClassifier().add(paraC);
				else if (pe.getType() == PEType.TABLE)
					is.getClassifier().add(tableC);
				else if (pe.getType() == PEType.LIST)
					is.getClassifier().add(listC);
				else if (pe.getType() == PEType.IMAGE)
					is.getClassifier().add(imageC);
				else if (pe.getType() == PEType.SECTION)
					is.getClassifier().add(sectionC);
				Slot s = ef.createSlotInstance();
				s.setOwner(is);
				s.setDefiningFeature(generatedFromView);
				ElementValue ev = ef.createElementValueInstance();
				ev.setElement(view);
				s.getValue().add(ev);
				if (pe.getType() == PEType.SECTION && pe.getLoopElement() != null) {
					Slot ss = ef.createSlotInstance();
					ss.setOwner(is);
					ss.setDefiningFeature(generatedFromElement);
					ElementValue ev2 = ef.createElementValueInstance();
					ev2.setElement(pe.getLoopElement());
					ss.getValue().add(ev2);
				}
			}
			is.setOwner(owner);
			if (pe.getNewspec() != null) {
				LiteralString ls = ef.createLiteralStringInstance();
				ls.setOwner(is);
				ls.setValue(pe.getNewspec().toJSONString());
				is.setSpecification(ls);
			}
			is.setName(pe.getName());
			if (is.getName().equals(null)) {
				is.setName("<>");
			}
			InstanceValue iv = ef.createInstanceValueInstance();
			iv.setInstance(is);
			list.add(iv);
			if (pe.getType() == PEType.SECTION) {
				handleViewOrSection(view, is, pe.getChildren());
			}
		}
		if (section != null) {
			Expression ex = ef.createExpressionInstance();
			ex.setOwner(section);
			ex.getOperand().addAll(list);
			section.setSpecification(ex);
		} else {
			Constraint c = getViewConstraint(view);
			Expression ex = ef.createExpressionInstance();
			ex.setOwner(c);
			ex.getOperand().addAll(list);
			c.setSpecification(ex);
		}
	}

	private Constraint getViewConstraint(Element view) {
		Constraint c = Utils.getViewConstraint(view);
		if (c != null)
			return c;
		c = ef.createConstraintInstance();
		c.setOwner(view);
		c.getConstrainedElement().add(view);
		return c;
	}

	private Package getFolder(Element view) {
		// find the root document
		Package viewInst = createViewInstancesPackage(); // this is the trunk (find/build package)

		List<Element> results = Utils.collectDirectedRelatedElementsByRelationshipStereotype(view, presentsS, 1, false, 1);
		if (!results.isEmpty() && results.get(0) instanceof Package) {
			setPackageHierarchy(view, viewInst, (Package) results.get(0));
			return (Package) results.get(0);
		}
		
		Package viewTarget = createViewTargetPackage(view); // this is the leaf (find/build package)
		setPackageHierarchy(view, viewInst, viewTarget); // this decides how to join them together

		// build dependencies here
		Dependency d = ef.createDependencyInstance();

		d.setOwner(viewTarget);
		ModelHelper.setSupplierElement(d, viewTarget);
		ModelHelper.setClientElement(d, view);
		StereotypesHelper.addStereotype(d, presentsS);
		return viewTarget;
	}
	
	private void setPackageHierarchy(Element view, Package trunk, Package leaf) {
		// evaluate place in stack
		// get the first parent section of the view 
		Element parent = null;
		for (Relationship r : view.get_relationshipOfRelatedElement()) {
			if (r instanceof Association) {
				// Associations have owned ends, which tell you which element
				// owns which other element
				Association asso = (Association) r;
				for (Property prop : asso.getOwnedEnd()) {
					// for every incoming association (arrow pointing at this element)
					// there are properties with the name of the source of the association
					// for every outgoing association (arrow pointing away from this element)
					// there are properties with the name of this element
					if (!prop.getType().getName().equals(((NamedElement) view).getName())) {
						// this is usually always true
						parent = (Element) prop.getType();
					}
				}
			}
		}
		// from parent, try to grab dependency to a package in View Instances
		Package parentPack = getViewTargetPackage(parent);
		if (parentPack != null) {
			leaf.setOwner(parentPack);
		} else {
			leaf.setOwner(trunk);
		}
	}

	private Package createViewInstancesPackage() {
		// this has to place this thing in the Data package of the model		
		Package rootPackage = Utils.getRootElement();
		// grab the rootPack id here too
		for (NamedElement child : rootPackage.getMember()) {
			// fix this to have better checking than just string
			if (child instanceof Package && child.getName().equals("View Instances")) {
				return (Package) child;
			}
		}
		// projid_view_instances set that id to that one
		Package viewInst = ef.createPackageInstance();
		viewInst.setName("View Instances");
		viewInst.setOwner(rootPackage);
		return viewInst;
	}
	
	private Package createUnusedInstancesPackage() {
		Package rootPackage = createViewInstancesPackage();
		for (NamedElement child : rootPackage.getMember()) {
			// fix this to have better checking than just string
			if (child instanceof Package && child.getName().equals("Unused Instances")) {
				return (Package) child;
			}
		}
		Package viewInst = ef.createPackageInstance();
		viewInst.setName("Unused Instances");
		viewInst.setOwner(rootPackage);
		return viewInst;
	}
	
	private Package createViewTargetPackage(Element elem) {
		// need to check the dependency things better than grabbing all of them
		Package viewTarg = getViewTargetPackage(elem);
		if (viewTarg == null) {
			Package newPack = ef.createPackageInstance();
			newPack.setName(((NamedElement)elem).getName());
			// need to set the dependency as well probably
			return newPack;
		}
		return viewTarg;
	}
	
	private Package getViewTargetPackage(Element elem) {
		if (elem == null) {
			return null;
		}
		for (Relationship r: elem.get_relationshipOfRelatedElement()) {
			if (r instanceof Dependency) {
				Dependency dep = (Dependency) r;
				for (Element target: dep.getTarget()) {
					if (target instanceof Package)
						return (Package) target;
				}
			}
		}
		return null;
	}
	
}
