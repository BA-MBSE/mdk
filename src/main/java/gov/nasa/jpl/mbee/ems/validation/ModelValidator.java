/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.mbee.ems.validation;

import gov.nasa.jpl.mbee.ems.ExportUtility;
import gov.nasa.jpl.mbee.ems.ImportUtility;
import gov.nasa.jpl.mbee.ems.validation.actions.CompareText;
import gov.nasa.jpl.mbee.ems.validation.actions.CreateMagicDrawElement;
import gov.nasa.jpl.mbee.ems.validation.actions.DeleteAlfrescoElement;
import gov.nasa.jpl.mbee.ems.validation.actions.DeleteMagicDrawElement;
import gov.nasa.jpl.mbee.ems.validation.actions.ElementDetail;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportAssociation;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportComment;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportConnector;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportConstraint;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportDoc;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportElement;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportName;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportOwner;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportPropertyType;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportRel;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportSite;
import gov.nasa.jpl.mbee.ems.validation.actions.ExportValue;
import gov.nasa.jpl.mbee.ems.validation.actions.FixModelOwner;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportAssociation;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportComment;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportConnector;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportConstraint;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportDoc;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportName;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportPropertyType;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportRel;
import gov.nasa.jpl.mbee.ems.validation.actions.ImportValue;
import gov.nasa.jpl.mbee.ems.validation.actions.InitializeProjectModel;
import gov.nasa.jpl.mbee.lib.Debug;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationRule;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationRuleViolation;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationSuite;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ViolationSeverity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.task.ProgressStatus;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.DirectedRelationship;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ElementValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Expression;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceValue;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralBoolean;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralInteger;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralReal;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralString;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.LiteralUnlimitedNatural;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Package;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Slot;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ValueSpecification;
import com.nomagic.uml2.ext.magicdraw.compositestructures.mdinternalstructures.Connector;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Extension;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.ProfileApplication;

public class ModelValidator {

    private ValidationSuite suite = new ValidationSuite("Model Sync");
    private ValidationRule nameDiff = new ValidationRule("Mismatched Name", "name is different", ViolationSeverity.ERROR);
    private ValidationRule docDiff = new ValidationRule("Mismatched Doc", "documentation is different", ViolationSeverity.ERROR);
    private ValidationRule valueDiff = new ValidationRule("Mismatched Value", "property/slot value is different", ViolationSeverity.ERROR);
    private ValidationRule propertyTypeDiff = new ValidationRule("Mismatched Property Type", "property type is different", ViolationSeverity.ERROR);
    private ValidationRule ownership = new ValidationRule("Moved", "Wrong containment", ViolationSeverity.ERROR);
    private ValidationRule exist = new ValidationRule("Exist", "Doesn't Exist or Moved", ViolationSeverity.WARNING);
    private ValidationRule relDiff = new ValidationRule("Relationship", "Relationship source or target", ViolationSeverity.ERROR);
    private ValidationRule commentDiff = new ValidationRule("Comment", "Comment different", ViolationSeverity.ERROR);
    private ValidationRule projectExist = new ValidationRule("Project Exist", "Project doesn't exist", ViolationSeverity.ERROR);
    private ValidationRule baselineTag = new ValidationRule("Baseline Tag Set", "Baseline Tag isn't set", ViolationSeverity.WARNING);
    private ValidationRule metaclassDiff = new ValidationRule("No longer a document", "no longer a document", ViolationSeverity.WARNING);
    private ValidationRule connectorDiff = new ValidationRule("Connector", "role/property paths are different", ViolationSeverity.ERROR);
    private ValidationRule constraintDiff = new ValidationRule("Constraint", "constraint spec is different", ViolationSeverity.ERROR);
    private ValidationRule associationDiff = new ValidationRule("Association", "association roles are different", ViolationSeverity.ERROR);
    private ValidationRule siteDiff = new ValidationRule("Site", "site existence", ViolationSeverity.ERROR);
    private Project prj;
    private Element start;
    private JSONObject result;       
	private boolean checkExist;
    private Set<Element> elementSet;
        
    public ModelValidator(Element start, JSONObject result, boolean checkExist, Set<Element> elementSet) {
        //result is from web, elementSet is from model
        this.start = start;
        suite.addValidationRule(nameDiff);
        suite.addValidationRule(docDiff);
        suite.addValidationRule(valueDiff);
        suite.addValidationRule(ownership);
        suite.addValidationRule(exist);
        suite.addValidationRule(relDiff);
        suite.addValidationRule(commentDiff);
        suite.addValidationRule(projectExist);
        suite.addValidationRule(baselineTag);
        suite.addValidationRule(metaclassDiff);
        suite.addValidationRule(propertyTypeDiff);
        suite.addValidationRule(connectorDiff);
        suite.addValidationRule(constraintDiff);
        suite.addValidationRule(associationDiff);
        suite.addValidationRule(siteDiff);
        this.checkExist = checkExist;
        this.result = result;
        prj = Application.getInstance().getProject();
        this.elementSet = elementSet;
    }
    
    public boolean checkProject() {
        if (ExportUtility.baselineNotSet)
            baselineTag.addViolation(new ValidationRuleViolation(Project.getProject(start).getModel(), "The baseline tag isn't set, baseline check wasn't done."));
        String projectUrl = ExportUtility.getUrlForProject();
        if (projectUrl == null)
            return false;
        String response = ExportUtility.get(projectUrl, false);
        if (response == null) {
            ValidationRuleViolation v = new ValidationRuleViolation(Project.getProject(start).getModel(), "This project doesn't exist on the web yet, or the site has been moved");
            v.addAction(new InitializeProjectModel(false));
            projectExist.addViolation(v);
            return false;
        }
        if (ProjectUtilities.isElementInAttachedProject(start)){
            Utils.showPopupMessage("You should not validate or export elements not from this project! Open the right project and do it from there");
            return false;
        }
        String url = ExportUtility.getUrlWithWorkspace();
        if (url == null)
            return false;
        String id = start.getID();
        if (start == Application.getInstance().getProject().getModel())
            id = Application.getInstance().getProject().getPrimaryProject().getProjectID();
        id = id.replace(".", "%2E");
        url += "/elements/" + id + "?recurse=true";
        GUILog log = Application.getInstance().getGUILog();
        log.log("[INFO] Getting elements from server...");
        response = ExportUtility.get(url, false);
        log.log("[INFO] Finished getting elements");
        if (response == null) {
            response = "{\"elements\": []}";
        }
        result = (JSONObject)JSONValue.parse(response);
        ResultHolder.lastResults = result;
        return true;
    }
    
    @SuppressWarnings("unchecked")
    public void validate(boolean fillContainment, ProgressStatus ps) {
        JSONArray elements = (JSONArray)result.get("elements");
        if (elements == null)
            return;
        Map<String, JSONObject> elementsKeyed = new HashMap<String, JSONObject>();
        if (fillContainment) {
            elementSet = new HashSet<Element>();
            getAllMissing(start, elementSet, elementsKeyed);
            validateModel(elementsKeyed, elementSet, ps);
        } else {
            validateModel(elementsKeyed, elementSet, ps);
        }
        result.put("elementsKeyed", elementsKeyed);
    }
    
    private void updateElementsKeyed(JSONObject result, Map<String, JSONObject> elementsKeyed) {
        if (result == null)
            return;
        JSONArray elements = (JSONArray)result.get("elements");
        if (elements == null)
            return;
        for (JSONObject elementInfo: (List<JSONObject>)elements) {
            String elementId = (String)elementInfo.get("sysmlid");
            if (elementId == null)
                continue;
            if (elementId.contains("-slot-")) {
                Element e = ExportUtility.getElementFromID(elementId);
                if (e != null)
                    elementId = e.getID();
                else
                    continue; //??
            }
            elementsKeyed.put(elementId, elementInfo);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void validateModel(Map<String, JSONObject> elementsKeyed, Set<Element> all, ProgressStatus ps) {
        //Set<Element> all = new HashSet<Element>();
        Set<String> checked = new HashSet<String>();

        //MDEV #673: update to handle specialization elements.
        //
        updateElementsKeyed(result, elementsKeyed);
        // elementsKeyed.keySet() refers to all MagicDraw element IDs on Alfresco
        // all refers to MagicDraw view element and owned elements
        // 1st loop: MagicDraw elements get compared with Alfresco elements 
        Set<Element> missing = new HashSet<Element>();
        for (Element e: all) {
            if (ps != null && ps.isCancel())
                break;
            if (!elementsKeyed.containsKey(e.getID())) {
            	// MagicDraw element is not on Alfresco
                if (checkExist && ExportUtility.shouldAdd(e)) {
                    missing.add(e);
                } else
                    continue;
            }
        }
        JSONObject missingResult = getManyAlfrescoElements(missing);
        updateElementsKeyed(missingResult, elementsKeyed);
        for (Element e: all) {
            if (ps != null && ps.isCancel())
                break;
            if (!elementsKeyed.containsKey(e.getID())) {
                ValidationRuleViolation v = new ValidationRuleViolation(e, "[EXIST] This doesn't exist on alfresco or it may be moved");
                v.addAction(new DeleteMagicDrawElement(e));
                v.addAction(new ExportElement(e));
                exist.addViolation(v);
                continue;
            }
            JSONObject elementInfo = (JSONObject)elementsKeyed.get(e.getID());
            checkElement(e, elementInfo);
            checked.add(e.getID());
        }
        
        Set<String> elementsKeyedIds = new HashSet<String>(elementsKeyed.keySet());
        elementsKeyedIds.removeAll(checked);
        
        // 2nd loop: unchecked Alfresco elements with sysml ID are now processed 
        for (String elementsKeyedId: elementsKeyedIds) {
            // MagicDraw element that has not been compared to Alfresco
            Element e = ExportUtility.getElementFromID(elementsKeyedId);
            if (e == null){
                if (elementsKeyedId.startsWith("PROJECT"))
                    continue;
                // Alfresco sysml element is not in MagicDraw 
                JSONObject jSONobject = (JSONObject)elementsKeyed.get(elementsKeyedId);
                String type = null;
                if (jSONobject.containsKey("specialization")) {
                    type = (String)((JSONObject)jSONobject.get("specialization")).get("type");
                }
                if (type != null && type.equals("Project"))
                    continue;
                if (type == null)
                    type = "Element";
                if (ImportUtility.VALUESPECS.contains(type))
                    continue;
                ValidationRuleViolation v = new ValidationRuleViolation(e, "[EXIST on Alfresco] " + type + " '" + elementsKeyedId + "' exists on Alfresco but not in Magicdraw");
                v.addAction(new ElementDetail(jSONobject));
                v.addAction(new CreateMagicDrawElement(jSONobject, elementsKeyed));
                v.addAction(new DeleteAlfrescoElement(elementsKeyedId, elementsKeyed));
                exist.addViolation(v);
            }  
            else {
            	checkElement(e, elementsKeyed.get(elementsKeyedId));
            }
            
        }
    }
    
    private void getAllMissing(Element current, Set<Element> missing, Map<String, JSONObject> elementsKeyed) {
        if (ProjectUtilities.isElementInAttachedProject(current))
            return;
        if (!ExportUtility.shouldAdd(current))
            return;
        if (!elementsKeyed.containsKey(current.getID()))
            if (!(current instanceof Model && ((Model)current).getName().equals("Data")))
                missing.add(current);
        for (Element e: current.getOwnedElement()) {
            getAllMissing(e, missing, elementsKeyed);            
        }
    }
    
    
    private void checkElement(Element e, JSONObject elementInfo) {
        String elementDoc = ModelHelper.getComment(e);
        String elementDocClean = ExportUtility.cleanHtml(elementDoc);
        String elementName = null;
        Boolean editable = (Boolean)elementInfo.get("editable");
        String webDoc = (String)elementInfo.get("documentation");
        if (webDoc != null) {
            webDoc = ExportUtility.cleanHtml(webDoc);
            elementInfo.put("documentation", webDoc);
        }
        if (e instanceof NamedElement) {
            elementName = ((NamedElement)e).getName();
        }
        String webName = ExportUtility.unescapeHtml((String)elementInfo.get("name"));
        if (elementName != null && !elementName.equals(webName)) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[NAME] model: " + elementName + ", web: " + webName);
            v.addAction(new ImportName((NamedElement)e, webName, result));
            if (editable)
                v.addAction(new ExportName((NamedElement)e));
            nameDiff.addViolation(v);
        }
        if (elementDoc != null && !(webDoc == null && elementDoc.equals("")) && !elementDocClean.equals(webDoc)) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[DOC] model: " + truncate(elementDocClean) + ", web: " + truncate((String)elementInfo.get("documentation")));
            v.addAction(new CompareText(e, webDoc, elementDocClean, result));
            v.addAction(new ImportDoc(e, webDoc, result));
            if (editable)
                v.addAction(new ExportDoc(e));
            docDiff.addViolation(v);
        }
        if (e instanceof Property) {
            ValidationRuleViolation v = valueDiff((Property)e, elementInfo);
            if (v != null)
                valueDiff.addViolation(v);
            ValidationRuleViolation v2 = propertyTypeDiff((Property)e, elementInfo);
            if (v2 != null)
                propertyTypeDiff.addViolation(v2);
        } else if (e instanceof Slot) {
            ValidationRuleViolation v = valueDiff((Slot)e, elementInfo);
            if (v != null)
                valueDiff.addViolation(v);
        } else if (e instanceof Comment) {
            //ValidationRuleViolation v = commentDiff((Comment)e, elementInfo);
            //if (v != null)
             //   valueDiff.addViolation(v);
        } else if (e instanceof DirectedRelationship) {
        	ValidationRuleViolation v = relationshipDiff((DirectedRelationship)e, elementInfo);
        	if (v != null) {
                relDiff.addViolation(v);
            }
        } else if (e instanceof Connector) {
            ValidationRuleViolation v = connectorDiff((Connector)e, elementInfo);
            if (v != null)
                connectorDiff.addViolation(v);
        } else if (e instanceof Constraint) {
            ValidationRuleViolation v = constraintDiff((Constraint)e, elementInfo);
            if (v != null)
                constraintDiff.addViolation(v);
        } else if (e instanceof Association) {
            ValidationRuleViolation v = associationDiff((Association)e, elementInfo);
            if (v != null)
                associationDiff.addViolation(v);
        } else if (e instanceof Package) {
            ValidationRuleViolation v = siteDiff((Package)e, elementInfo);
            if (v != null)
                siteDiff.addViolation(v);
        }
        ValidationRuleViolation v = ownerDiff(e, elementInfo);
        if (v != null)
            ownership.addViolation(v);
    }
    
    private ValidationRuleViolation ownerDiff(Element e, JSONObject elementInfo) {
        Boolean editable = (Boolean)elementInfo.get("editable");
        if ( e.getOwner() != null ) {
            String ownerID = e.getOwner().getID();
            String webOwnerID = (String)elementInfo.get("owner");
            if (webOwnerID == null || webOwnerID.startsWith("PROJECT")) {
                if (webOwnerID == null)
                    webOwnerID = Application.getInstance().getProject().getModel().getID();
                else {
                    if (webOwnerID.equals(Application.getInstance().getProject().getPrimaryProject().getProjectID()))
                        webOwnerID = Application.getInstance().getProject().getModel().getID();
                    else
                        webOwnerID = null;
                }
                    
            }
            if (!ownerID.equals(webOwnerID)) {
                Element owner = null;
                if (webOwnerID != null)
                    owner = (Element)prj.getElementByID(webOwnerID);
                ValidationRuleViolation v = new ValidationRuleViolation(e, "[OWNER] model: " + e.getOwner().getHumanName() + ", web: " + (owner == null ? "null" : owner.getHumanName()));
                //v.addAction(new FixModelOwner(e, owner, result)); //disable owner import for now since nothing can change the owner on the web
                if (editable)
                    v.addAction(new ExportOwner(e));
                return v;
            }
        }
        return null;
    }
    
    private ValidationRuleViolation propertyTypeDiff(Property e, JSONObject info) {
        Boolean editable = (Boolean)info.get("editable");
        JSONObject specialization = (JSONObject)info.get("specialization");
        Type modelType = e.getType();
        String modelTypeId = null;
        if (modelType != null)
            modelTypeId = modelType.getID();
        String webTypeId = null;
        if (specialization != null)
            webTypeId = (String)specialization.get("propertyType");
        Element webTypeElement = null;
        if (webTypeId != null)
            webTypeElement = ExportUtility.getElementFromID(webTypeId);
        if ((modelTypeId != null && !modelTypeId.equals(webTypeId)) || (webTypeId != null && !webTypeId.equals(modelTypeId))) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[PTYPE] model: " + (modelType == null ? "null" : modelType.getName()) + ", web: " + (webTypeElement == null ? "null" : webTypeElement.getHumanName()));
            v.addAction(new ImportPropertyType(e, (Type)webTypeElement, result));
            if (editable)
                v.addAction(new ExportPropertyType(e));
            return v;
        }
        return null;
    }
    
    private ValidationRuleViolation siteDiff(Package e, JSONObject elementInfo) {
        JSONObject model = ExportUtility.fillPackage(e, null);
        Boolean serverSite = (Boolean)((JSONObject)elementInfo.get("specialization")).get("isSite");
        boolean serversite = false;
        if (serverSite != null && serverSite)
            serversite = true;
        boolean modelsite = (Boolean)model.get("isSite");
        if (!serversite && modelsite || serversite && !modelsite) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[SITE] model: " + modelsite + ", web: " + serversite);
            v.addAction(new ExportSite(e));
            return v;
        }
        return null;
    }
    
    private ValidationRuleViolation relationshipDiff(DirectedRelationship e, JSONObject elementInfo) {
        JSONObject specialization = (JSONObject)elementInfo.get("specialization");
        Boolean editable = (Boolean)elementInfo.get("editable");
        String websourceId = (String)specialization.get("source");
        Element websource = null;
        String webtargetId = (String)specialization.get("target");
        Element webtarget = null;
        Element localsource = ModelHelper.getClientElement(e);
        Element localtarget = ModelHelper.getSupplierElement(e);
        if (websourceId != null)
            websource = (Element)prj.getElementByID(websourceId);
        if (webtargetId != null)
            webtarget = (Element)prj.getElementByID(webtargetId);
        if (websource != null && webtarget != null && localsource != null && localtarget != null && (websource != localsource || webtarget != localtarget)) {
            String msg = "[REL] ";
            if (websource != localsource)
                msg += "model source: " + localsource.getHumanName() + ", web source: " + websource == null ? "null" : websource.getHumanName() + " ";
            if (webtarget != localtarget)
                msg += "model target: " + localtarget.getHumanName() + ", web target: " + webtarget == null ? "null" : webtarget.getHumanName();
            ValidationRuleViolation v = new ValidationRuleViolation(e, msg);
            v.addAction(new ImportRel(e, result));
            if (editable)
                v.addAction(new ExportRel(e));
            return v;
        }
        return null;
    }
    
    private ValidationRuleViolation valueDiff(Property e, JSONObject info) {
    	//MDEV #673
    	//
        Boolean editable = (Boolean)info.get("editable");
    	JSONObject specialization = (JSONObject)info.get("specialization");
    	String valueTypes;
    	JSONObject firstObject = null;
    	
        ValueSpecification vs = e.getDefaultValue();
        JSONArray value = (JSONArray)specialization.get("value");
        if ((value == null) || (value.isEmpty()))
        	valueTypes = null;
        else {
        	//retrieve the type of the first element
        	//in the value array.
        	//
        	firstObject = (JSONObject)value.get(0);
        	valueTypes = (String)firstObject.get("type");
        }
        
        if ((vs == null || (vs instanceof ElementValue && ((ElementValue)vs).getElement() == null) || 
                (vs instanceof InstanceValue && ((InstanceValue)vs).getInstance() == null))
                && (valueTypes == null))
            return null;
        if ((vs != null || (vs instanceof ElementValue && ((ElementValue)vs).getElement() != null) || 
                (vs instanceof InstanceValue && ((InstanceValue)vs).getInstance() != null))
                && (valueTypes == null)) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[VALUE] model: not null, web: null");
            v.addAction(new ImportValue(e, null, null, result));
            if (editable)
                v.addAction(new ExportValue(e));
            return v;
        }
        if ((vs == null || (vs instanceof ElementValue && ((ElementValue)vs).getElement() == null) || 
                (vs instanceof InstanceValue && ((InstanceValue)vs).getInstance() == null)) 
                && value != null && value.size() > 0 && valueTypes != null) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[VALUE] model: null, web: " + truncate(value.toString()));
            v.addAction(new ImportValue(e, value, PropertyValueType.valueOf(valueTypes), result));
            if (editable)
                v.addAction(new ExportValue(e));
            return v;
        }
        PropertyValueType valueType = PropertyValueType.valueOf((String)firstObject.get("type"));
        Map<String, Object> results = valueSpecDiff(vs, firstObject);
        String message = (String)results.get("message");
        boolean stringMatch = (Boolean)results.get("stringMatch");
        String webString = (String)results.get("webString");
        String modelString = (String)results.get("modelString");
        if (!message.equals("")) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, message);
            if (stringMatch)
                v.addAction(new CompareText(e, webString, modelString, result));
            v.addAction(new ImportValue(e, value, valueType, result));
            if (editable)
                v.addAction(new ExportValue(e));
            return v;
        }
        return null;
    }
    
    private ValidationRuleViolation valueDiff(Slot e, JSONObject info) {
        Boolean editable = (Boolean)info.get("editable");
        Debug.outln( "valueDiff(Slot:" + Utils.slotValueToString( e )
                     + ", JSONObjec info=" + info );
        JSONObject specialization = (JSONObject)info.get("specialization");
        String valueTypes;
        JSONObject firstObject = null;
        
        List<ValueSpecification> vss = e.getValue();
        JSONArray value = (JSONArray)specialization.get("value");
        if ((value == null) || (value.isEmpty()))
            valueTypes = null;
        else {
            firstObject = (JSONObject)value.get(0);
            valueTypes = (String)firstObject.get("type");
        }
        Debug.outln("JSONArray value = " + value);
        boolean nullElementValues = areNullElementValues(vss);
        if ((vss == null || vss.isEmpty() || nullElementValues) && (valueTypes == null)) {
            Debug.outln("returning null: vs=" + vss + ", valueTypes=" + valueTypes + ", value=" + value);
            return null;
        }
        if (vss != null && vss.size() > 0 && !nullElementValues && (valueTypes == null)) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[VALUE] model: not null, web: null");
            v.addAction(new ImportValue(e, null, null, result));
            if (editable)
                v.addAction(new ExportValue(e));
            Debug.outln("1) returning ValidationRuleViolation: " + v );
            return v;
        }
        if ((vss == null || vss.isEmpty() || nullElementValues) && value != null && value.size() > 0 && valueTypes != null) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[VALUE] model: null, web: " + truncate(value.toString()));
            v.addAction(new ImportValue(e, value, PropertyValueType.valueOf(valueTypes), result));
            if (editable)
                v.addAction(new ExportValue(e));
            Debug.outln("2) returning ValidationRuleViolation: " + v );
            return v;
        }
        if ((vss.size() != value.size())) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[VALUE] model and web values don't match");
            v.addAction(new ImportValue(e, value, PropertyValueType.valueOf(valueTypes), result));
            if (editable)
                v.addAction(new ExportValue(e));
            Debug.outln("3) returning ValidationRuleViolation: " + v );
            return v;
        }

        PropertyValueType valueType = PropertyValueType.valueOf(valueTypes);
        String message = "";
        String badMessage = "[VALUE] model: " + truncate(RepresentationTextCreator.getRepresentedText(e)) + ", web: " + truncate(value.toString());
        String modelString = null;
        String webString = null;
        boolean stringMatch = false;
        Map<String, Object> results = null;
        for (int i = 0; i < vss.size(); i++) {
            results = valueSpecDiff(vss.get(i), (JSONObject)value.get(i));
            message = (String)results.get("message");
            stringMatch = (Boolean)results.get("stringMatch");
            webString = (String)results.get("webString");
            modelString = (String)results.get("modelString");
            if (!message.equals("")) {
                ValidationRuleViolation v = new ValidationRuleViolation(e, message);
                if (stringMatch)
                    v.addAction(new CompareText(e, webString, modelString, result));
                v.addAction(new ImportValue(e, value, valueType, result));
                if (editable)
                    v.addAction(new ExportValue(e));
                return v;
            }
        }        
        return null;
    }
    
    private ValidationRuleViolation connectorDiff(Connector e, JSONObject info) {
        JSONObject webspec = (JSONObject)info.get("specialization");
        Boolean editable = (Boolean)info.get("editable");
        JSONArray webSourcePropPath = (JSONArray)webspec.get("sourcePath");
        JSONArray webTargetPropPath = (JSONArray)webspec.get("targetPath");
        String webtype = (String)webspec.get("connectorType");
        JSONObject modelspec = ExportUtility.fillConnectorSpecialization(e, null);
        JSONArray modelSourcePropPath = (JSONArray)modelspec.get("sourcePath");
        JSONArray modelTargetPropPath = (JSONArray)modelspec.get("targetPath");
        String modeltype = (String)modelspec.get("connectorType");
        if (!modelSourcePropPath.equals(webSourcePropPath) || !modelTargetPropPath.equals(webTargetPropPath) || 
                (modeltype != null && !modeltype.equals(webtype) || webtype != null && !webtype.equals(modeltype))) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[CONNECTOR] connector roles/paths/types doesn't match");
            if (editable)
                v.addAction(new ExportConnector(e));
            v.addAction(new ImportConnector(e, webspec, result));
            return v;
        }
        return null;
    }
    
    private ValidationRuleViolation constraintDiff(Constraint e, JSONObject info) {
        Boolean editable = (Boolean)info.get("editable");
        JSONObject spec = (JSONObject)info.get("specialization");
        JSONObject value = (JSONObject)spec.get("specification");
        JSONObject modelspec = ExportUtility.fillConstraintSpecialization(e, null);
        JSONObject modelvalue = (JSONObject)modelspec.get("specification");
        /*Map<String, Object> results = valueSpecDiff(e.getSpecification(), value);
        String message = (String)results.get("message");
        boolean stringMatch = (Boolean)results.get("stringMatch");
        String webString = (String)results.get("webString");
        String modelString = (String)results.get("modelString");*/
        //if (!message.equals("")) {
        if (modelvalue != null && !modelvalue.equals(value) || value != null && !value.equals(modelvalue)) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[CONSTRAINT] specifications don't match");
            //if (stringMatch)
              //  v.addAction(new CompareText(e, webString, modelString, result));
            v.addAction(new ImportConstraint(e, spec, result));
            if (editable)
                v.addAction(new ExportConstraint(e));
            return v;
        }
        return null;
    }
    
    private ValidationRuleViolation associationDiff(Association e, JSONObject info) {
        Boolean editable = (Boolean)info.get("editable");
        JSONObject webspec = (JSONObject)info.get("specialization");
        JSONObject modelspec = ExportUtility.fillAssociationSpecialization(e, null);
        String modelSource = (String)modelspec.get("source");
        String modelTarget = (String)modelspec.get("target");
        String modelSourceAggr = (String)modelspec.get("sourceAggregation");
        String modelTargetAggr = (String)modelspec.get("targetAggregation");
        JSONArray modelOwned = (JSONArray)modelspec.get("owned");
        String webSource = (String)webspec.get("source");
        String webTarget = (String)webspec.get("target");
        String webSourceAggr = (String)webspec.get("sourceAggregation");
        String webTargetAggr = (String)webspec.get("targetAggregation");
        JSONArray webOwned = (JSONArray)webspec.get("ownedEnd");
        if (!modelSource.equals(webSource) || !modelTarget.equals(webTarget) || 
                !modelSourceAggr.equals(webSourceAggr) || !modelTargetAggr.equals(webTargetAggr) ||
                !modelOwned.equals(webOwned)) {
            ValidationRuleViolation v = new ValidationRuleViolation(e, "[ASSOC] Association roles/aggregation/navigability are different");
            if (editable)
                v.addAction(new ExportAssociation(e));
            v.addAction(new ImportAssociation(e, webspec, result));
            return v;
        }
        return null;
    }
    
    private Map<String, Object> valueSpecDiff(ValueSpecification vs, JSONObject firstObject) {
        Map<String, Object> result = new HashMap<String, Object>();
        PropertyValueType valueType = PropertyValueType.valueOf((String)firstObject.get("type"));
        String message = "";
        String typeMismatchMessage = "[VALUE] value spec types don't match";
        String modelString = null;
        String webString = null;
        boolean stringMatch = false;
        if (valueType == PropertyValueType.LiteralString) {
            if (vs instanceof LiteralString) {
                modelString = ExportUtility.cleanHtml(((LiteralString)vs).getValue());
                webString = ExportUtility.cleanHtml((String)firstObject.get("string"));
                firstObject.put("string", webString);
                if (!modelString.equals(webString)) {
                    stringMatch = true;
                    message = "[VALUE] model: " + truncate(modelString) + ", web: " + truncate(webString);
                }
            } else {
                message = typeMismatchMessage;
            }
        } else if (valueType == PropertyValueType.LiteralBoolean) {
            if (vs instanceof LiteralBoolean) {
                if ((Boolean)firstObject.get("boolean") != ((LiteralBoolean)vs).isValue()) {
                    message = "[VALUE] model: " + ((LiteralBoolean)vs).isValue() + ", web: " + firstObject.get("boolean");
                }
            } else {
                message = typeMismatchMessage;
            }
        } else if (valueType == PropertyValueType.LiteralInteger) {
            if (vs instanceof LiteralInteger) {
                if (((LiteralInteger)vs).getValue() != (Long)firstObject.get("integer")) {
                    message = "[VALUE] model: " + ((LiteralInteger)vs).getValue() + ", web: " + firstObject.get("integer");
                }
            } else {
                message = typeMismatchMessage;
            }
        } else if (valueType == PropertyValueType.LiteralUnlimitedNatural) {
            if (vs instanceof LiteralUnlimitedNatural) {
                if (((LiteralUnlimitedNatural)vs).getValue() != (Long)firstObject.get("naturalValue")) {
                    message = "[VALUE] model: " + ((LiteralUnlimitedNatural)vs).getValue() + ", web: " + firstObject.get("naturalValue");
                }
            } else {
                message = typeMismatchMessage;
            }
        } else if (valueType == PropertyValueType.LiteralReal) {
            if (vs instanceof LiteralReal) {
                Double webValue = null;
                if (firstObject.get("double") instanceof Long)
                    webValue = Double.parseDouble(((Long)firstObject.get("double")).toString());
                else
                    webValue = (Double)firstObject.get("double");
                if (((LiteralReal)vs).getValue() != webValue) {
                    message = "[VALUE] model: " + ((LiteralReal)vs).getValue() + ", web: " + firstObject.get("double");
                }
            } else {
                message = typeMismatchMessage;
            }
        } else if (valueType == PropertyValueType.ElementValue) {
            if (vs instanceof ElementValue) {
                if (((ElementValue)vs).getElement() == null || !ExportUtility.getElementID(((ElementValue)vs).getElement()).equals(firstObject.get("element"))) {
                    message = "[VALUE] model: " + ((ElementValue)vs).getElement().getHumanName() + ", web: " + firstObject.get("element");
                }
            } else {
                message = typeMismatchMessage;
            }
        } else if (valueType == PropertyValueType.InstanceValue) {
            if (vs instanceof InstanceValue) {
                if (((InstanceValue)vs).getInstance() == null || !ExportUtility.getElementID(((InstanceValue)vs).getInstance()).equals(firstObject.get("instance"))) {
                    message = "[VALUE] model: " + ((InstanceValue)vs).getInstance().getHumanName() + ", web: " + firstObject.get("instance");
                }
            } else {
                message = typeMismatchMessage;
            }
        } else if (valueType == PropertyValueType.Expression) {
            if (vs instanceof Expression) {
                JSONObject model = ExportUtility.fillValueSpecification(vs, null);
                if (!model.equals(firstObject))
                    message = "[VALUE] expressions don't match";
            } else {
                message = typeMismatchMessage;
            }
        } else { //type of value in model and alfresco don't match or unknown type
            
        }   
        result.put("message", message);
        result.put("webString", webString);
        result.put("modelString", modelString);
        result.put("stringMatch", stringMatch);
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private ValidationRuleViolation commentDiff(Comment e, JSONObject elementInfo) {
        String modelBodyClean = ExportUtility.cleanHtml(((Comment)e).getBody());
        String webBody = (String)elementInfo.get("body");
        if (webBody == null)
            webBody = "";
        else
            webBody = ExportUtility.cleanHtml(webBody);
        ValidationRuleViolation v = null;
        if (!modelBodyClean.equals(webBody)) {
            v = new ValidationRuleViolation(e, "[Comment] model: " + truncate(modelBodyClean) + ", web: " + truncate(webBody));
            v.addAction(new ImportComment(e, webBody, result));
            v.addAction(new ExportComment(e));
        }
        Set<String> modelAnnotated = new HashSet<String>();
        for (Element el: e.getAnnotatedElement()) {
            modelAnnotated.add(el.getID());
        }
        JSONArray web = (JSONArray)elementInfo.get("annotatedElements");
        if (web != null) {
            Set<String> webs = new HashSet<String>(web);
            if (!webs.containsAll(modelAnnotated) || !modelAnnotated.containsAll(webs)) {
                if (v == null) {
                    v = new ValidationRuleViolation(e, "[Comment] The anchored elements are different");
                    v.addAction(new ImportComment(e, webBody, result));
                    v.addAction(new ExportComment(e));
                }
            }
        }
        return v;
    }
    
    public void showWindow() {
        List<ValidationSuite> vss = new ArrayList<ValidationSuite>();
        vss.add(suite);
        Utils.displayValidationWindow(vss, "Model Web Difference Validation");
    }
    
    public ValidationSuite getSuite() {
        return suite;
    }
    
    private boolean areNullElementValues(List<ValueSpecification> vs) {
        for (ValueSpecification v: vs) {
            if (!(v instanceof ElementValue || v instanceof InstanceValue) || 
                    (v instanceof ElementValue && ((ElementValue)v).getElement() != null) || 
                    (v instanceof InstanceValue && ((InstanceValue)v).getInstance() != null))
                return false;
        }
        return true;
    }
    
    private static String truncate(String s) {
        if (s == null)
            return null;
        if (s.length() > 50)
            return s.substring(0, 49) + "...";
        return s;
    }

    private JSONObject getAlfrescoElement(Element e) {
        String url = ExportUtility.getUrlWithWorkspace();
        if (url == null)
            return null;
        String id = ExportUtility.getElementID(e);
        id = id.replace(".", "%2E");
        url += "/elements/" + id;
        String response = ExportUtility.get(url, false);
        if (response == null)
            return null;
        JSONObject result = (JSONObject)JSONValue.parse(response);
        JSONArray elements = (JSONArray)result.get("elements");
        if (elements == null || elements.isEmpty())
            return null;
        return (JSONObject)elements.get(0);
    }
    
    private JSONObject getManyAlfrescoElements(Set<Element> es) {
        if (es.isEmpty())
            return null;
        JSONArray elements = new JSONArray();
        for (Element e: es) {
            JSONObject ob = new JSONObject();
            ob.put("sysmlid", ExportUtility.getElementID(e));
            elements.add(ob);
        }
        JSONObject tosend = new JSONObject();
        tosend.put("elements", elements);
        String url = ExportUtility.getUrlWithWorkspace();
        url += "/elements";
        String response = ExportUtility.getWithBody(url, tosend.toJSONString());
        if (response == null) {
            JSONObject res = new JSONObject();
            res.put("elements", new JSONArray());
            return res;
        }
        return (JSONObject)JSONValue.parse(response);
    }
}
