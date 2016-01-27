package gov.nasa.jpl.mbee.systemsreasoner.validation;

import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.AddInheritanceToAssociationAction;
import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.AspectRemedyAction;
import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.CreateSlotsAction;
import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.OpenSpecificationAction;
import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.RedefineAttributeAction;
import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.RenameElementAction;
import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.RetypeElementAction;
import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.SelectInContainmentTreeAction;
import gov.nasa.jpl.mbee.systemsreasoner.validation.actions.SetRedefinitionAction;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationRule;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationRuleViolation;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationSuite;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ViolationSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.CallBehaviorAction;
import com.nomagic.uml2.ext.magicdraw.classes.mddependencies.Dependency;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Classifier;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Generalization;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.RedefinableElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.TypedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdbasicbehaviors.Behavior;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

public class SRValidationSuite extends ValidationSuite implements Runnable {

	private static final String NAME = "SR Validate";
	private List<Element> elements;

	private static final ValidationRule generalMissingRule = new ValidationRule("Missing General", "General is missing in generalization", ViolationSeverity.ERROR),
			generalNotClassRule = new ValidationRule("General Not Class", "General is not of type class", ViolationSeverity.ERROR), attributeMissingRule = new ValidationRule(
					"Missing Owned Attribute", "Owned attribute is missing", ViolationSeverity.ERROR), aspectMissingRule = new ValidationRule("Missing Defined Aspect",
					"An aspect is defined but not realized", ViolationSeverity.ERROR), nameRule = new ValidationRule("Naming Inconsistency", "Names are inconsistent",
					ViolationSeverity.WARNING),
			attributeTypeRule = new ValidationRule("Attribute Type Inconsistency", "Attribute types are inconsistent", ViolationSeverity.WARNING),
			generalSpecificNameRule = new ValidationRule("General Specific Name Inconsistency", "General and specific names are inconsistent", ViolationSeverity.INFO),
			// orphanAttributeRule = new ValidationRule("Potential Orphan", "First degree attribute is never redefined", ViolationSeverity.WARNING);
			instanceClassifierExistenceRule = new ValidationRule("Instance Classifier Unspecified", "Instance classifier is not specified", ViolationSeverity.ERROR),
			missingSlotsRule = new ValidationRule("Missing Slot(s) Detected", "Missing slot(s) detected", ViolationSeverity.ERROR), associationInheritanceRule = new ValidationRule(
					"Association inheritance missing.", "The association of the specialized element does not inherit from its general counterpart.", ViolationSeverity.ERROR);

	{
		this.addValidationRule(generalMissingRule);
		this.addValidationRule(generalNotClassRule);
		this.addValidationRule(attributeMissingRule);
		this.addValidationRule(aspectMissingRule);
		this.addValidationRule(nameRule);
		this.addValidationRule(attributeTypeRule);
		// this.addValidationRule(orphanAttributeRule);
		this.addValidationRule(generalSpecificNameRule);
		this.addValidationRule(instanceClassifierExistenceRule);
		this.addValidationRule(missingSlotsRule);
		this.addValidationRule(associationInheritanceRule);
	}

	public SRValidationSuite(final Element element) {
	    super(NAME);
	    this.elements = new ArrayList<Element>();
	    this.elements.add(element);
	}

	public SRValidationSuite(final List<Element> elements) {
		super(NAME);
		this.elements = elements;
	}

	@Override
	public void run() {
		for (final ValidationRule vr : this.getValidationRules()) {
			vr.getViolations().clear();
		}

		final ListIterator<Element> iterator = elements.listIterator();
		while (iterator.hasNext()) {
			final Element element = iterator.next();

			if (element instanceof Classifier) {
				final Classifier classifier = (Classifier) element;
				// traverse the hierarchy down
				for (final Generalization generalization : classifier.get_generalizationOfGeneral()) {
					if (!elements.contains(generalization.getSpecific())) {
						iterator.add(generalization.getSpecific());
						iterator.previous();
					}
				}
				for (final InstanceSpecification instance : classifier.get_instanceSpecificationOfClassifier()) {
					if (!elements.contains(instance)) {
						iterator.add(instance);
						iterator.previous();
					}
				}
				for (final Classifier general : classifier.getGeneral()) {
					// if (!classifier.getName().equals(general.getName())) {
					// final ValidationRuleViolation v = new ValidationRuleViolation(classifier, generalSpecificNameRule.getDescription() + ": [GENERAL] " + general.getName()
					// + " - [SPECIFIC] " + classifier.getName());
					// v.addAction(new RenameElementAction(general, classifier, "Update Specific"));
					// v.addAction(new RenameElementAction(classifier, general, "Update General"));
					// generalSpecificNameRule.addViolation(v);
					// }

					// Inheritance on Associations Rule
					assocRule : for (Element child : classifier.getOwnedElement()) {
						if (child instanceof Property) {
							Type partType = ((Property) child).getType();
							for (Element superChild : general.getOwnedElement()) {
								if (superChild instanceof Property) {
									Type superPartType = ((Property) superChild).getType();
									final ValidationRuleViolation v = new ValidationRuleViolation(classifier, associationInheritanceRule.getDescription() + ": [GENERAL] "
											+ general.getName() + " - [SPECIFIC] " + classifier.getName());
									if (partType == null || superPartType == null) {
										// the following will always add a rule violation, without thinking at all
										// this is future to handle null types as if they were other types
										// name check will still catch the differences, likely
										//
										// if (partType == superPartType) {
										//	v.addAction(new AddInheritanceToAssociationAction(((Property) child).getAssociation(), ((Property) superChild).getAssociation()));
										//	associationInheritanceRule.addViolation(v);
										// }
									} else {
										if (partType.equals(superPartType)) {
											// when the partType and the superPartType are equivalent, we check for the Inheritance Association and fix if its broke
											if (hasInheritanceFromTo(((Property) child).getAssociation(), ((Property) superChild).getAssociation())) {
												break assocRule;
											} else {
												v.addAction(new AddInheritanceToAssociationAction(((Property) child).getAssociation(), ((Property) superChild).getAssociation()));
												associationInheritanceRule.addViolation(v);
											}
										} else if (partType instanceof Classifier) {
											// if they are not equivalent, we have to do more work
											if (((Classifier) partType).getGeneral().contains(superPartType)) {
												if (hasInheritanceFromTo(((Property) child).getAssociation(), ((Property) superChild).getAssociation())) {
													break assocRule;
												} else {
													v.addAction(new AddInheritanceToAssociationAction(((Property) child).getAssociation(), ((Property) superChild).getAssociation()));
													associationInheritanceRule.addViolation(v);
												}
											}
										}
									}
								}
							}
						}
					}
				}

				for (final NamedElement ne : classifier.getInheritedMember()) {
					if (ne instanceof RedefinableElement && !((RedefinableElement) ne).isLeaf()) {
						final RedefinableElement redefEl = (RedefinableElement) ne;
						RedefinableElement redefingEl = null;

						for (Element p : classifier.getOwnedElement()) {
							if (p instanceof RedefinableElement) {
								if (doesEventuallyRedefine((RedefinableElement) p, (RedefinableElement) redefEl)) {
									// if (p instanceof RedefinableElement && ((RedefinableElement) p).getRedefinedElement().contains(redefinableElement)) {
									redefingEl = (RedefinableElement) p;
									break;
								}
							}
						}
						if (redefingEl == null) {
							boolean redefinedInContext = false;
							for (final NamedElement ne2 : classifier.getInheritedMember()) {
								if (ne2 instanceof RedefinableElement && ne2 instanceof RedefinableElement
										&& doesEventuallyRedefine((RedefinableElement) ne2, (RedefinableElement) redefEl)) {
									redefinedInContext = true;
									break;
								}
							}
							if (!redefinedInContext) {
								final ValidationRuleViolation v = new ValidationRuleViolation(classifier, (redefEl instanceof TypedElement
										&& ((TypedElement) redefEl).getType() != null ? "[TYPED] " : "")
										+ attributeMissingRule.getDescription() + ": " + redefEl.getQualifiedName()); // this is the attribute hook
								for (final Property p : classifier.getAttribute()) {
									if (p.getName().equals(redefEl.getName()) && !p.hasRedefinedElement()) {
										v.addAction(new SetRedefinitionAction(p, redefEl, "Redefine by Name Collision"));
									}
								}
								v.addAction(new RedefineAttributeAction(classifier, redefEl));
								if (redefEl instanceof TypedElement && ((TypedElement) redefEl).getType() != null) {
									// intentionally showing this option even if the type isn't specializable so the user doesn't have to go through
									// grouping them separately to validate. It will just ignore and log if a type isn't specializable.
									v.addAction(new RedefineAttributeAction(classifier, redefEl, true, "Redefine Attribute & Specialize Types Recursively"));
								}
								attributeMissingRule.addViolation(v);
							}
						} else {
							if ((redefingEl.getName() == null && redefEl.getName() != null) || (redefingEl.getName() != null && !redefingEl.getName().equals(redefEl.getName()))) {
								final ValidationRuleViolation v = new ValidationRuleViolation(redefingEl, nameRule.getDescription() + ": [GENERAL] " + redefEl.getName()
										+ " - [SPECIFIC] " + redefingEl.getName());
								v.addAction(new RenameElementAction(redefEl, redefingEl, "Update Specific"));
								v.addAction(new RenameElementAction(redefingEl, redefEl, "Update General"));
								nameRule.addViolation(v);
							}
							if (redefingEl instanceof TypedElement && redefEl instanceof TypedElement) {
								final TypedElement redefingTypdEl = (TypedElement) redefingEl;
								final TypedElement redefableTypdEl = (TypedElement) redefEl;

								if ((redefingTypdEl.getType() == null && redefableTypdEl.getType() != null)
										|| (redefingTypdEl.getType() != null && redefingTypdEl.getType() instanceof Classifier && redefableTypdEl.getType() instanceof Classifier && !doesEventuallyGeneralizeTo(
												(Classifier) redefingTypdEl.getType(), (Classifier) redefableTypdEl.getType()))) {
									if (redefingTypdEl.getType() instanceof Classifier && redefableTypdEl.getType() instanceof Classifier
											&& ((Classifier) redefingTypdEl.getType()).getGeneral().contains(redefableTypdEl.getType())) {
										iterator.add(((Classifier) redefingTypdEl.getType()));
										iterator.previous();
									} else {
										final ValidationRuleViolation v = new ValidationRuleViolation(redefingTypdEl, attributeTypeRule.getDescription() + ": [GENERAL] "
												+ (redefableTypdEl.getType() != null ? redefableTypdEl.getType().getQualifiedName() : "null") + " - [SPECIFIC] "
												+ (redefingTypdEl.getType() != null ? redefingTypdEl.getType().getQualifiedName() : "null"));
										v.addAction(new RetypeElementAction(redefableTypdEl, redefingTypdEl, "Update Specific"));
										v.addAction(new RetypeElementAction(redefingTypdEl, redefableTypdEl, "Update General"));
										attributeTypeRule.addViolation(v);
									}
								}
							}
						}

					}
				}
				/**
				 * Check for aspect structures:
				 */
				for (Dependency d : classifier.getClientDependency()) {
					boolean aspectFound = false;
					Classifier aspect = null;
					Stereotype s = StereotypesHelper.getAppliedStereotypeByString(d, "aspect");
					if (s != null) {
						for (Element el : d.getTarget()) {
							if (el instanceof Classifier) {
								aspect = (Classifier) el;
								for (Element q : classifier.getOwnedElement()) {
									if (q instanceof Property) {
										Type t = ((TypedElement) q).getType();
										if (t instanceof Classifier) {
											if (((Classifier) t).getGeneral().contains(el)) {
												aspectFound = true;

											}
										}
									} else if (q instanceof CallBehaviorAction) {
										Behavior b = ((CallBehaviorAction) q).getBehavior();
										if (b.getGeneral().contains(el)) {
											aspectFound = true;
										}
									}
								}
							}
						}
						if (!aspectFound) {
							if (aspect != null) {
								final ValidationRuleViolation v = new ValidationRuleViolation(classifier, aspectMissingRule.getDescription() + ": [CLASS WITH ASPECT] "
										+ classifier.getName() + " - [ASPECT] " + aspect.getName());
								v.addAction(new AspectRemedyAction(classifier, aspect));
								aspectMissingRule.addViolation(v);
							}
						}
					}
				}

			} else if (element instanceof InstanceSpecification) {
				final InstanceSpecification instance = (InstanceSpecification) element;

				for (final Slot slot : instance.getSlot()) {
					for (final ValueSpecification vs : slot.getValue()) {
						final InstanceSpecification i;
						if (vs instanceof InstanceValue && (i = ((InstanceValue) vs).getInstance()) != null && !elements.contains(i)) {
							iterator.add(i);
							iterator.previous();
						}
					}
				}

				if (!instance.hasClassifier()) {
					final ValidationRuleViolation v = new ValidationRuleViolation(instance, instanceClassifierExistenceRule.getDescription() + ": " + instance.getQualifiedName());
					v.addAction(new OpenSpecificationAction(instance));
					v.addAction(new SelectInContainmentTreeAction(instance));
					instanceClassifierExistenceRule.addViolation(v);
					continue;
				}

				// boolean needsReslotting = false;
				final List<Property> missingProperties = new ArrayList<Property>();
				for (final Classifier classifier : instance.getClassifier()) {
					for (final Property property : CreateSlotsAction.collectSlottableProperties(classifier)) {
						boolean isDefined = false;
						for (final Slot slot : instance.getSlot()) {
							if (slot.getDefiningFeature().equals(property)) {
								isDefined = true;
								break;
							}
						}
						if (!isDefined) {
							missingProperties.add(property);
						}
					}
				}
				if (!missingProperties.isEmpty()) {
					String suffix = "";
					if (instance.hasSlot()) {
						suffix += ": ";
						for (int i = 0; i < missingProperties.size(); i++) {
							final Property property = missingProperties.get(i);
							suffix += property.getName() != null && !property.getName().isEmpty() ? property.getName() : "<>";
							if (i != missingProperties.size() - 1) {
								suffix += ", ";
							}
						}
					}
					final ValidationRuleViolation v = new ValidationRuleViolation(instance, (!instance.hasSlot()
							? missingSlotsRule.getDescription().replaceFirst("Missing", "No")
							: missingSlotsRule.getDescription()) + suffix);
					v.addAction(IndeterminateProgressMonitorProxy.doubleWrap(new CreateSlotsAction(instance, false, false, false, "Create Missing Slots"), "Systems Reasoner"));
					v.addAction(IndeterminateProgressMonitorProxy.doubleWrap(new CreateSlotsAction(instance, false, false, true, "Recreate Slots"), "Systems Reasoner"));
					v.addAction(IndeterminateProgressMonitorProxy.doubleWrap(new CreateSlotsAction(instance, false, true, true, "Delete Child Instances & Recreate Slots"),
							"Systems Reasoner"));

					v.addAction(IndeterminateProgressMonitorProxy.doubleWrap(new CreateSlotsAction(instance, true, false, false, "[R] Create Missing Slots"), "Systems Reasoner"));
					v.addAction(IndeterminateProgressMonitorProxy.doubleWrap(new CreateSlotsAction(instance, true, false, true, "[R] Recreate Slots"), "Systems Reasoner"));
					v.addAction(IndeterminateProgressMonitorProxy.doubleWrap(new CreateSlotsAction(instance, true, true, true, "[R] Delete Child Instances & Recreate Slots"),
							"Systems Reasoner"));
					missingSlotsRule.addViolation(v);
				}
			}
		}
	}
	private boolean hasInheritanceFromTo(Association association, Association superAssociation) {
		if (ModelHelper.getGeneralClassifiersRecursivelly(association).contains(superAssociation)) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean doesEventuallyRedefine(final RedefinableElement source, final RedefinableElement target) {
		if (source.getRedefinedElement().contains(target)) {
			return true;
		}
		for (final RedefinableElement p : source.getRedefinedElement()) {
			if (doesEventuallyRedefine(p, target)) {
				return true;
			}
		}
		return false;
	}

	public static boolean doesEventuallyGeneralizeTo(final Classifier source, final Classifier target) {
		if (source.getGeneral().contains(target)) {
			return true;
		}
		if (source.equals(target)) {
			return true;
		}
		for (final Classifier classifier : source.getGeneral()) {
			if (doesEventuallyGeneralizeTo(classifier, target)) {
				return true;
			}
		}
		return false;
	}

}
