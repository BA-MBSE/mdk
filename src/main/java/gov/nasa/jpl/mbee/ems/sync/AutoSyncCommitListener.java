package gov.nasa.jpl.mbee.ems.sync;

import gov.nasa.jpl.mbee.DocGen3Profile;
import gov.nasa.jpl.mbee.ems.ExportUtility;
import gov.nasa.jpl.mbee.lib.Utils;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.uml2.ext.jmi.UML2MetamodelConstants;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DirectedRelationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Expression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Generalization;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.OpaqueExpression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.impl.PropertyNames;
import com.nomagic.uml2.transaction.TransactionCommitListener;
import com.nomagic.uml2.transaction.TransactionManager;

/**
 * This class responds to commits done in the document.
 * 
 * @author jsalcedo
 * 
 */
public class AutoSyncCommitListener implements TransactionCommitListener {
	/**
	 * Allow listener to be disabled during imports.
	 */
	private boolean disabled = false;
	private TransactionManager tm;

	public AutoSyncCommitListener() {

	}

	/**
	 * Adapter to call handleChangeEvent() from the TransactionCommitListener
	 * interface.
	 */
	private class TransactionCommitHandler implements Runnable {
		private final Collection<PropertyChangeEvent> events;
		private Map<String, JSONObject> elements = new HashMap<String, JSONObject>();
		private Set<String> deletes = new HashSet<String>();
		TransactionCommitHandler(final Collection<PropertyChangeEvent> events) {
			this.events = events;
		}

		@Override
		public void run() {
			// If the plugin has been disabled,
			// simply return without processing
			// the events.
			//
			if (disabled)
				return;

			for (PropertyChangeEvent event : events) {

				String strTmp = "NULL";
				if (event != null) {
					strTmp = event.toString();
				}

				// Get the object (e.g. Element) that
				// contains the change.
				//
				Object source = event.getSource();
				if (source instanceof Element) {

					String changedPropertyName = event.getPropertyName();
					if (changedPropertyName == null) {
						// If the property name is null, this indicates
						// multiple properties were changed, so
						// simply continue.
						//
						continue;
					}
					else {
						if (event.getNewValue() == null && event.getOldValue() == null)
							continue;
						if ((event.getNewValue() == null && event.getOldValue() != null)
								|| (event.getNewValue() != null && event.getOldValue() == null)
								|| (!event.getNewValue().equals(event.getOldValue())))
							handleChangedProperty((Element) source, changedPropertyName, event.getNewValue(),
									event.getOldValue());
					}
				}
			}
			if (!elements.isEmpty())
				sendChanges();
		}

		@SuppressWarnings("unchecked")
        private void sendChanges() {
			JSONObject toSend = new JSONObject();
			JSONArray eles = new JSONArray();
			eles.addAll(elements.values());
			toSend.put("elements", eles);
			String url = ExportUtility.getPostElementsUrl();
			if (url != null)
				ExportUtility.send(url, toSend.toJSONString(), null, false);
			String deleteUrl = ExportUtility.getUrlWithWorkspace();
			for (String id: deletes) {
			    String durl = deleteUrl + "/elements/" + id;
			    ExportUtility.delete(durl);
			}
		}

		@SuppressWarnings("unchecked")
        private JSONObject getElementObject(Element e) {
		    JSONObject elementOb = null;
		    String elementID = ExportUtility.getElementID(e);
		    if (elements.containsKey(elementID)) {
                elementOb = elements.get(elementID);
            }
            else {
                elementOb = new JSONObject();
                elementOb.put("sysmlid", elementID);
                elements.put(elementID, elementOb);
            }
		    return elementOb;
		}
		
		@SuppressWarnings("unchecked")
		private void handleChangedProperty(Element sourceElement, String propertyName, Object newValue, Object oldValue) {
			JSONObject elementOb = null;
			String elementID = null;
			ArrayList<String> moveKeywords = new ArrayList<String>();
			// Create a list of the 'owning' property names.
			//
			moveKeywords.add(PropertyNames.OWNING_ASSOCIATION);
			moveKeywords.add(PropertyNames.OWNING_CONSTRAINT);
			moveKeywords.add(PropertyNames.OWNING_ELEMENT);
			moveKeywords.add(PropertyNames.OWNING_EXPRESSION);
			moveKeywords.add(PropertyNames.OWNING_INSTANCE);
			moveKeywords.add(PropertyNames.OWNING_INSTANCE_SPEC);
			moveKeywords.add(PropertyNames.OWNING_LOWER);
			moveKeywords.add(PropertyNames.OWNING_PACKAGE);
			moveKeywords.add(PropertyNames.OWNING_PARAMETER);
			moveKeywords.add(PropertyNames.OWNING_PROPERTY);
			moveKeywords.add(PropertyNames.OWNING_SIGNAL);
			moveKeywords.add(PropertyNames.OWNING_SLOT);
			moveKeywords.add(PropertyNames.OWNING_STATE);
			moveKeywords.add(PropertyNames.OWNING_TEMPLATE_PARAMETER);
			moveKeywords.add(PropertyNames.OWNING_TRANSITION);
			moveKeywords.add(PropertyNames.OWNING_UPPER);
			moveKeywords.add(PropertyNames._U_M_L_CLASS);
			moveKeywords.add(PropertyNames.OWNER);

			//
			// Examine property name to determine how to
			// process the change.
			//
			if (propertyName.equals(PropertyNames.NAME)) {
				elementOb = getElementObject(sourceElement);
				ExportUtility.fillName(sourceElement, elementOb);
			}
			else if (sourceElement instanceof Comment && 
			        ExportUtility.isElementDocumentation((Comment) sourceElement) && 
			        propertyName.equals(PropertyNames.BODY)) { // doc changed
				Element actual = sourceElement.getOwner();
				elementOb = getElementObject(actual);
				ExportUtility.fillDoc(actual, elementOb);
			}
			else if ((sourceElement instanceof ValueSpecification) && (propertyName.equals(PropertyNames.VALUE)) ||
			        (sourceElement instanceof OpaqueExpression) && (propertyName.equals(PropertyNames.BODY)) ||
			        (sourceElement instanceof Expression) && (propertyName.equals(PropertyNames.OPERAND))) {
				//
				// Need to find the actual element that needs to be sent (most
				// likely a Property or Slot that's the closest owner of this
				// value spec).
				Element actual = sourceElement.getOwner();

				// There may multiple ValueSpecification changes
				// so go up the chain of owners until we find
				// the actual owner (Element) that has the changes.
				//
				while (actual instanceof ValueSpecification)
					actual = actual.getOwner();
				
				elementOb = getElementObject(actual);
				if (actual instanceof Slot || actual instanceof Property) {
				    JSONObject specialization = ExportUtility.fillPropertySpecialization(actual, null, false);
				    elementOb.put("specialization", specialization);
				} if (actual instanceof Constraint) {
				    JSONObject specialization = ExportUtility.fillConstraintSpecialization((Constraint)actual, null);
				    elementOb.put("specialization", specialization);
				}
			}
			// Check if this is a Property or Slot. Need these next two if
			// statement
			// to handle the case where a value is being deleted.
			//
			else if ((sourceElement instanceof Property) && (propertyName.equals(PropertyNames.DEFAULT_VALUE) || propertyName.equals(PropertyNames.TYPE))) {
				JSONObject specialization = ExportUtility.fillPropertySpecialization(sourceElement, null, false);
				elementOb = getElementObject(sourceElement);
                elementOb.put("specialization", specialization);
			}
			else if ((sourceElement instanceof Slot) && propertyName.equals(PropertyNames.VALUE)) {
			    elementOb = getElementObject(sourceElement);
			    JSONObject specialization = ExportUtility.fillPropertySpecialization(sourceElement, null, false);
                elementOb.put("specialization", specialization);
			}
			else if ((sourceElement instanceof Constraint) && propertyName.equals(PropertyNames.SPECIFICATION)) {
			    elementOb = getElementObject(sourceElement);
                JSONObject specialization = ExportUtility.fillConstraintSpecialization((Constraint)sourceElement, null);
                elementOb.put("specialization", specialization);
			}
			else if (propertyName.equals(UML2MetamodelConstants.INSTANCE_CREATED)
					&& ExportUtility.shouldAdd(sourceElement)) {
				elementOb = getElementObject(sourceElement);
				ExportUtility.fillElement(sourceElement, elementOb, null, null);
			}
			else if (propertyName.equals(UML2MetamodelConstants.INSTANCE_DELETED)
			        && ExportUtility.shouldAdd(sourceElement)) {
				elementID = ExportUtility.getElementID(sourceElement);

				// JJS TO DO: implement the actual delete when there is the
				// call to make the a delete in the server.
				//
				if (elements.containsKey(elementID))
					elements.remove(elementID);
				deletes.add(elementID);
			}
			else if (sourceElement instanceof DirectedRelationship && 
			        (propertyName.equals(PropertyNames.SUPPLIER) || propertyName.equals(PropertyNames.CLIENT))) {
				// This event represents a move of a relationship from
				// one element (A) to another element (B). Process only
				// the events associated with the element B.
				//
				if ((newValue != null) && (oldValue == null)) {
					JSONObject specialization = ExportUtility.fillDirectedRelationshipSpecialization((DirectedRelationship)sourceElement, null);
					elementOb = getElementObject(sourceElement);
					elementOb.put("specialization", specialization);
				}
			}
			else if ((sourceElement instanceof Generalization)
					&& ((propertyName.equals(PropertyNames.SPECIFIC)) || (propertyName.equals(PropertyNames.GENERAL)))) {
				if ((newValue != null) && (oldValue == null)) {
					JSONObject specialization = ExportUtility.fillDirectedRelationshipSpecialization((DirectedRelationship)sourceElement, null);
					elementOb = getElementObject(sourceElement);
					elementOb.put("specialization", specialization);
				}
			}
			else if ((moveKeywords.contains(propertyName)) && ExportUtility.shouldAdd(sourceElement)) {
				// This code handle moving an element (not a relationship)
				// from one class to another.
				elementOb = getElementObject(sourceElement);
				ExportUtility.fillOwner(sourceElement, elementOb);
			}
		}
	}

	public void disable() {
		disabled = true;
	}

	public void enable() {
		disabled = false;
	}

	public TransactionManager getTm() {
		return tm;
	}

	public void setTm(TransactionManager tm) {
		this.tm = tm;
	}

	@Override
	public Runnable transactionCommited(Collection<PropertyChangeEvent> events) {
		return new TransactionCommitHandler(events);
	}
}
