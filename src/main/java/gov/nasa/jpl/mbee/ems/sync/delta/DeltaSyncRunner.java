package gov.nasa.jpl.mbee.ems.sync.delta;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.teamwork.application.TeamworkUtils;
import com.nomagic.task.ProgressStatus;
import com.nomagic.task.RunnableWithProgress;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Constraint;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import gov.nasa.jpl.mbee.ems.ExportUtility;
import gov.nasa.jpl.mbee.ems.ImportException;
import gov.nasa.jpl.mbee.ems.ImportUtility;
import gov.nasa.jpl.mbee.ems.ServerException;
import gov.nasa.jpl.mbee.ems.jms.JMSUtils;
import gov.nasa.jpl.mbee.ems.sync.Request;
import gov.nasa.jpl.mbee.ems.sync.common.CommonSyncProjectEventListenerAdapter;
import gov.nasa.jpl.mbee.ems.sync.common.CommonSyncTransactionCommitListener;
import gov.nasa.jpl.mbee.ems.validation.ModelValidator;
import gov.nasa.jpl.mbee.ems.validation.actions.DetailDiff;
import gov.nasa.jpl.mbee.lib.Changelog;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationRule;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationRuleViolation;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationSuite;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ViolationSeverity;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.*;

public class DeltaSyncRunner implements RunnableWithProgress {
    private boolean delete = false;
    private boolean commit;

    private Logger log = Logger.getLogger(DeltaSyncRunner.class);
    private Project project = Application.getInstance().getProject();
    private SessionManager sm = SessionManager.getInstance();

    private boolean isFromTeamwork = false;
    private boolean failure = false;
    private boolean skipUpdate = false;

    private ValidationSuite suite = new ValidationSuite("Updated Elements/Failed Updates");
    private ValidationRule updated = new ValidationRule("updated", "updated", ViolationSeverity.INFO);
    private ValidationRule cannotUpdate = new ValidationRule("cannotUpdate", "cannotUpdate", ViolationSeverity.ERROR);
    private ValidationRule cannotRemove = new ValidationRule("cannotDelete", "cannotDelete", ViolationSeverity.WARNING);
    private ValidationRule cannotCreate = new ValidationRule("cannotCreate", "cannotCreate", ViolationSeverity.ERROR);
    private Set<String> cannotChange;

    private List<ValidationSuite> vss = new ArrayList<>();

    public DeltaSyncRunner(boolean commit, boolean skipUpdate, boolean delete) {
        this.commit = commit;
        this.skipUpdate = skipUpdate;
        this.delete = delete;
    }

    public DeltaSyncRunner(boolean commit, boolean delete) {
        this.commit = commit;
        this.delete = delete;
    }

    public Set<String> getCannotChange() {
        return cannotChange;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run(ProgressStatus ps) {
        if (!skipUpdate) {
            Utils.guilog("[INFO] Getting changes from MMS...");
        }
        suite.addValidationRule(updated);
        suite.addValidationRule(cannotUpdate);
        suite.addValidationRule(cannotRemove);
        suite.addValidationRule(cannotCreate);

        if (ProjectUtilities.isFromTeamworkServer(project.getPrimaryProject())) {
            isFromTeamwork = true;
            if (TeamworkUtils.getLoggedUserName() == null) {
                failure = true;
                Utils.guilog("[ERROR] You need to be logged in to teamwork first.");
                return;
            }
        }

        CommonSyncTransactionCommitListener listener = CommonSyncProjectEventListenerAdapter.getProjectMapping(project).getCommonSyncTransactionCommitListener();
        if (listener == null) {
            Utils.guilog("[ERROR] Unexpected error happened, cannot get commit listener.");
            failure = true;
            return; //some error here
        }
        listener.setDisabled(true);
        DeltaSyncProjectEventListenerAdapter.lockSyncFolder(project);
        List<String> jmsTextMessages = JMSUtils.getJMSTextMessages(Application.getInstance().getProject(), true);
        Changelog<String, Void> jmsChangelog = new Changelog<>();
        for (String textMessage : jmsTextMessages) {
            Application.getInstance().getGUILog().log(textMessage);
            JSONObject ob = (JSONObject) JSONValue.parse(textMessage);
            boolean fromMagicDraw = ob.get("source") != null && ob.get("source").equals("magicdraw");
            JSONObject workspace2 = (JSONObject) ob.get("workspace2");
            if (workspace2 == null) {
                continue;
            }

            JSONArray updated = (JSONArray) workspace2.get("updatedElements");
            JSONArray added = (JSONArray) workspace2.get("addedElements");
            JSONArray deleted = (JSONArray) workspace2.get("deletedElements");
            JSONArray moved = (JSONArray) workspace2.get("movedElements");

            Map<String, Void> addedChanges = jmsChangelog.get(Changelog.ChangeType.CREATED),
                    modifiedChanges = jmsChangelog.get(Changelog.ChangeType.UPDATED),
                    deletedChanges = jmsChangelog.get(Changelog.ChangeType.DELETED);

            for (Object e : updated) {
                String id = (String) ((JSONObject) e).get("sysmlid");
                if (!fromMagicDraw) {
                    modifiedChanges.put(id, null);
                }
                deletedChanges.remove(id);
            }
            for (Object e : added) {
                String id = (String) ((JSONObject) e).get("sysmlid");
                if (!fromMagicDraw) {
                    addedChanges.put(id, null);
                }
                deletedChanges.remove(id);
            }
            for (Object e : moved) {
                String id = (String) ((JSONObject) e).get("sysmlid");
                if (!fromMagicDraw) {
                    modifiedChanges.put(id, null);
                }
                deletedChanges.remove(id);
            }
            for (Object e : deleted) {
                String id = (String) ((JSONObject) e).get("sysmlid");
                if (!fromMagicDraw) {
                    modifiedChanges.put(id, null);
                }
                addedChanges.remove(id);
                modifiedChanges.remove(id);
            }
        }
        //Changelog<String, Void> jmsChangelog = DeltaSyncProjectEventListenerAdapter.getJMSChanges(Application.getInstance().getProject());
        listener.setDisabled(false);
        if (jmsChangelog == null) {
            failure = true;
            return;
        }
        Map<String, Element> localAdded = listener.getInMemoryChangelog().get(Changelog.ChangeType.CREATED),
                localUpdated = listener.getInMemoryChangelog().get(Changelog.ChangeType.UPDATED),
                localDeleted = listener.getInMemoryChangelog().get(Changelog.ChangeType.DELETED);

        JSONObject previousUpdates = DeltaSyncProjectEventListenerAdapter.getUpdatesOrFailed(Application.getInstance().getProject(), "update");
        if (previousUpdates != null) {
            for (String added : (List<String>) previousUpdates.get("added")) {
                if (localAdded.containsKey(added) || localUpdated.containsKey(added)) {
                    continue;
                }
                Element e = ExportUtility.getElementFromID(added);
                if (e != null) {
                    localAdded.put(added, e);
                }
            }
            for (String updated : (List<String>) previousUpdates.get("changed")) {
                if (!localUpdated.containsKey(updated)) {
                    Element e = ExportUtility.getElementFromID(updated);
                    if (e != null) {
                        localUpdated.put(updated, e);
                    }
                }
                localAdded.remove(updated);
            }
            for (String deleted : (List<String>) previousUpdates.get("deleted")) {
                if (ExportUtility.getElementFromID(deleted) != null) {
                    localDeleted.remove(deleted);
                    continue; //not deleted?
                }
                localDeleted.put(deleted, null);
                localAdded.remove(deleted);
                localUpdated.remove(deleted);
            }
        }

        Set<String> webAdded = jmsChangelog.get(Changelog.ChangeType.CREATED).keySet();
        Set<String> webChanged = jmsChangelog.get(Changelog.ChangeType.UPDATED).keySet();
        Set<String> webDeleted = jmsChangelog.get(Changelog.ChangeType.DELETED).keySet();

        Set<String> toGet = new HashSet<>(webChanged);
        toGet.addAll(webAdded);

        Set<String> cannotAdd = new HashSet<>();
        cannotChange = new HashSet<>();
        Set<String> cannotDelete = new HashSet<>();

        //get latest json for element added/changed from mms
        if (!toGet.isEmpty()) {
            JSONObject getJson = new JSONObject();
            JSONArray getElements = new JSONArray();
            getJson.put("elements", getElements);
            for (String e : toGet) {
                JSONObject el = new JSONObject();
                el.put("sysmlid", e);
                getElements.add(el);
            }
            String url = ExportUtility.getUrlWithWorkspace();
            url += "/elements";
            Utils.guilog("[INFO] Getting " + getElements.size() + " elements from MMS.");
            String response = null;
            try {
                response = ExportUtility.getWithBody(url, getJson.toJSONString());
            } catch (ServerException ex) {
                Utils.guilog("[ERROR] Get elements failed.");
            }
            if (response == null) {
                JSONObject abort = new JSONObject();
                JSONArray abortChanged = new JSONArray();
                JSONArray abortDeleted = new JSONArray();
                JSONArray abortAdded = new JSONArray();
                abortChanged.addAll(webChanged);
                abortDeleted.addAll(webDeleted);
                abortAdded.addAll(webAdded);
                abort.put("added", abortAdded);
                abort.put("deleted", abortDeleted);
                abort.put("changed", abortChanged);

                DeltaSyncProjectEventListenerAdapter.lockSyncFolder(project);
                listener.setDisabled(true);
                sm.createSession("failed changes");
                try {
                    DeltaSyncProjectEventListenerAdapter.setUpdatesOrFailed(project, abort, "error", true);
                    sm.closeSession();
                } catch (Exception ex) {
                    log.error("", ex);
                    sm.cancelSession();
                }
                listener.setDisabled(false);
                failure = true;
                Utils.guilog("[ERROR] Cannot get elements from MMS server, update aborted. (All changes will be attempted at next update)");
                return;
            }
            Map<String, JSONObject> webElements = new HashMap<String, JSONObject>();
            try {
                JSONObject webObject = (JSONObject) JSONValue.parse(response);
                JSONArray webArray = (JSONArray) webObject.get("elements");
                for (Object o : webArray) {
                    String webId = (String) ((JSONObject) o).get("sysmlid");
                    webElements.put(webId, (JSONObject) o);
                }
                //if (webElements.size() != toGet.size())
                //    return; //??
            } catch (Exception ex) {
                log.error("", ex);
            }
            //calculate order to create web added elements
            List<JSONObject> webAddedObjects = new ArrayList<JSONObject>();
            for (String webAdd : webAdded) {
                if (webElements.containsKey(webAdd)) {
                    webAddedObjects.add(webElements.get(webAdd));
                }
            }


            //calculate potential conflicted set and clean web updated set
            Set<String> localChangedIds = new HashSet<String>(localUpdated.keySet());
            localChangedIds.retainAll(webChanged);
            JSONArray webConflictedObjects = new JSONArray();
            Set<Element> localConflictedElements = new HashSet<Element>();
            if (!localChangedIds.isEmpty()) {
                for (String conflictId : localChangedIds) {
                    if (webElements.containsKey(conflictId)) {
                        webConflictedObjects.add(webElements.get(conflictId));
                        localConflictedElements.add(localUpdated.get(conflictId));
                    }
                }
            }
            //find web changed that are not conflicted
            List<JSONObject> webChangedObjects = new ArrayList<JSONObject>();
            for (String webUpdate : webChanged) {
                if (localChangedIds.contains(webUpdate)) {
                    continue;
                }
                if (webElements.containsKey(webUpdate)) {
                    webChangedObjects.add(webElements.get(webUpdate));
                }
            }

            Map<String, Element> mapping = new HashMap<String, Element>();
            //lock stuff that needs to be changed first
            Set<String> toLockIds = new HashSet<String>(webChanged);
            toLockIds.addAll(webAdded);
            toLockIds.addAll(webDeleted);
            for (String id : toLockIds) {
                Element e = ExportUtility.getElementFromID(id);
                if (e != null) {
                    Utils.tryToLock(project, e, isFromTeamwork);
                    mapping.put(id, e);
                }
                else {
                    continue;
                }
                Constraint c = Utils.getViewConstraint(e);
                if (c != null) {
                    Utils.tryToLock(project, c, isFromTeamwork);
                }
            }

            Utils.guilog("[INFO] Applying changes...");
            sm.createSession("mms delayed sync change");
            listener.setDisabled(true);
            try {
                Map<String, List<JSONObject>> toCreate = ImportUtility.getCreationOrder(webAddedObjects);

                List<JSONObject> webAddedSorted = toCreate.get("create");
                List<JSONObject> fails = toCreate.get("fail");
                //List<Map<String, Object>> toChange = new ArrayList<Map<String, Object>>();
                //take care of web added
                if (webAddedSorted != null) {
                    ImportUtility.outputError = false;
                    for (JSONObject element : webAddedSorted) {
                        try {
                            Element e = mapping.get(element.get("sysmlid"));
                            if (e != null && !e.isEditable()) {
                                //existing element and not editable
                                continue;
                            }
                            ImportUtility.createElement(element, false);
                        } catch (ImportException ex) {

                        }
                    }
                    ImportUtility.outputError = true;
                    for (JSONObject element : webAddedSorted) {
                        try {
                            Element e = mapping.get(element.get("sysmlid"));
                            if (e != null && !e.isEditable()) {
                                continue; //TODO log this? this is an element that's already been created and 
                                //currently not editable, most likely already processed by someone else,
                                //should be taken off the to be created list
                            }
                            Element newe = ImportUtility.createElement(element, true);
                            //Utils.guilog("[SYNC ADD] " + newe.getHumanName() + " created.");
                            updated.addViolation(new ValidationRuleViolation(newe, "[CREATED]"));
                        } catch (Exception ex) {
                            log.error("", ex);
                            cannotAdd.add((String) element.get("sysmlid"));
                            ValidationRuleViolation vrv = new ValidationRuleViolation(null, "[CREATE FAILED] " + ex.getMessage());
                            vrv.addAction(new DetailDiff(new JSONObject(), element));
                            cannotCreate.addViolation(vrv);
                        }
                    }
                }
                for (JSONObject element : fails) {
                    cannotAdd.add((String) element.get("sysmlid"));
                    ValidationRuleViolation vrv = new ValidationRuleViolation(null, "[CREATE FAILED] Owner or chain of owners not found");
                    vrv.addAction(new DetailDiff(new JSONObject(), element));
                    cannotCreate.addViolation(vrv);
                }


                //take care of updated
                for (JSONObject webUpdated : webChangedObjects) {
                    Element e = ExportUtility.getElementFromID((String) webUpdated.get("sysmlid"));
                    if (e == null) {
                        //TODO bad? maybe it was supposed to have been added?
                        continue;
                    }
                    JSONObject spec = (JSONObject) webUpdated.get("specialization");
                    if (spec != null && spec.get("contents") != null) {
                        Constraint c = Utils.getViewConstraint(e);
                        if (c != null) {
                            if (!c.isEditable()) {
                                cannotChange.add(ExportUtility.getElementID(e)); //this is right since contents is embedded in view
                                cannotUpdate.addViolation(new ValidationRuleViolation(c, "[UPDATE FAILED] - not editable"));
                                continue;
                            }
                        }
                    }
                    if (!e.isEditable()) {
                        cannotChange.add(ExportUtility.getElementID(e));
                        //Utils.guilog("[ERROR - SYNC UPDATE] " + e.getHumanName() + " not editable.");
                        cannotUpdate.addViolation(new ValidationRuleViolation(e, "[UPDATE FAILED] - not editable"));
                        continue;
                    }
                    try {
                        ImportUtility.updateElement(e, webUpdated);
                        if (!(e.getOwner() != null && webUpdated.get("qualifiedId") instanceof String &&
                                ((String) webUpdated.get("qualifiedId")).contains("holding_bin")))
                        //don't update owner if trying to update existing element's owner to under a holding bin
                        {
                            ImportUtility.setOwner(e, webUpdated);
                        }
                        updated.addViolation(new ValidationRuleViolation(e, "[UPDATED]"));
                        //Utils.guilog("[SYNC UPDATE] " + e.getHumanName() + " updated.");
                        /*if (webUpdated.containsKey("specialization")) { //do auto doc hierarchy? very risky
                            JSONArray view2view = (JSONArray)((JSONObject)webUpdated.get("specialization")).get("view2view");
                            if (view2view != null) {
                                JSONObject web = ExportUtility.keyView2View(view2view);
                                DocumentGenerator dg = new DocumentGenerator(e, null, null);
                                Document dge = dg.parseDocument(true, true, true);
                                ViewHierarchyVisitor vhv = new ViewHierarchyVisitor();
                                dge.accept(vhv);
                                JSONObject model = vhv.getView2View();
                                if (!ViewValidator.viewHierarchyMatch(e, dge, vhv, (JSONObject)webUpdated.get("specialization"))) {
                                    Map<String, Object> result = ImportHierarchy.importHierarchy(e, model, web);
                                    if (result != null && (Boolean)result.get("success")) {
                                        Utils.guilog("[SYNC] Document hierarchy updated for " + e.getHumanName());
                                        toChange.add(result);
                                    } else
                                        cannotChange.add(ExportUtility.getElementID(e));
                                }
                            }
                        }*/
                    } catch (Exception ex) {
                        ValidationRuleViolation vrv = new ValidationRuleViolation(e, "[UPDATE FAILED] " + ex.getMessage());
                        cannotUpdate.addViolation(vrv);
                        log.error("", ex);
                        cannotChange.add(ExportUtility.getElementID(e));
                    }
                }

                //take care of deleted
                for (String e : webDeleted) {
                    Element toBeDeleted = ExportUtility.getElementFromID(e);
                    if (toBeDeleted == null) {
                        continue;
                    }
                    if (!toBeDeleted.isEditable()) {
                        cannotDelete.add(e);
                        cannotRemove.addViolation(new ValidationRuleViolation(toBeDeleted, "[DELETE FAILED] - not editable"));
                        continue;
                    }
                    try {
                        ModelElementsManager.getInstance().removeElement(toBeDeleted);
                        Utils.guilog("[SYNC DELETE] " + toBeDeleted.getHumanName() + " deleted.");
                    } catch (Exception ex) {
                        log.error("", ex);
                        cannotDelete.add(e);
                    }
                }
                listener.setDisabled(true);
                sm.closeSession();
                listener.setDisabled(false);
                if (!skipUpdate) {
                    Utils.guilog("[INFO] Finished applying changes.");
                }
                //for (Map<String, Object> r: toChange) {
                //    ImportHierarchy.sendChanges(r); //what about if doc is involved in conflict?
                //}
            } catch (Exception ex) {
                //something really bad happened, save all changes for next time;
                log.error("", ex);
                sm.cancelSession();
                Utils.printException(ex);
                cannotAdd.clear();
                cannotChange.clear();
                cannotDelete.clear();
                updated.getViolations().clear();
                cannotUpdate.getViolations().clear();
                cannotRemove.getViolations().clear();
                cannotCreate.getViolations().clear();
                for (String e : webDeleted) {
                    cannotDelete.add(e);
                }
                for (JSONObject element : webAddedObjects) {
                    cannotAdd.add((String) ((JSONObject) element).get("sysmlid"));
                }
                for (JSONObject element : webChangedObjects) {
                    cannotChange.add((String) element.get("sysmlid"));
                }
                Utils.guilog("[ERROR] Unexpected exception happened, all changes will be reattempted at next update.");

            } finally {
                listener.setDisabled(false);
            }
            JSONObject failed = null;
            if (!cannotAdd.isEmpty() || !cannotChange.isEmpty() || !cannotDelete.isEmpty()) {
                failed = new JSONObject();
                JSONArray failedAdd = new JSONArray();
                failedAdd.addAll(cannotAdd);
                JSONArray failedChange = new JSONArray();
                failedChange.addAll(cannotChange);
                JSONArray failedDelete = new JSONArray();
                failedDelete.addAll(cannotDelete);
                failed.put("changed", failedChange);
                failed.put("added", failedAdd);
                failed.put("deleted", failedDelete);
            }
            listener.setDisabled(true);
            DeltaSyncProjectEventListenerAdapter.lockSyncFolder(project);
            sm.createSession("failed changes");
            try {
                DeltaSyncProjectEventListenerAdapter.setUpdatesOrFailed(project, failed, "error", true);
                sm.closeSession();
            } catch (Exception ex) {
                log.error("", ex);
                sm.cancelSession();
            }
            listener.setDisabled(false);
            if (failed != null) {
                Utils.guilog("[INFO] There were changes that couldn't be applied. These will be attempted on the next update.");
            }

            //show window of what got changed
            vss.add(suite);
            if (suite.hasErrors()) {
                Utils.displayValidationWindow(vss, "Delta Sync Log");
            }


            //conflicts
            JSONObject mvResult = new JSONObject();
            mvResult.put("elements", webConflictedObjects);
            ModelValidator mv = new ModelValidator(null, mvResult, false, localConflictedElements, false);
            try {
                mv.validate(false, null);
            } catch (ServerException ex) {

            }
            Set<Element> conflictedElements = mv.getDifferentElements();
            if (!conflictedElements.isEmpty()) {
                JSONObject conflictedToSave = new JSONObject();
                JSONArray conflictedElementIds = new JSONArray();
                for (Element ce : conflictedElements) {
                    conflictedElementIds.add(ExportUtility.getElementID(ce));
                }
                conflictedToSave.put("elements", conflictedElementIds);
                Utils.guilog("[INFO] There are potential conflicts between changes from MMS and locally changed elements, please resolve first and rerun update/commit.");
                listener.setDisabled(true);
                sm.createSession("failed changes");
                try {
                    DeltaSyncProjectEventListenerAdapter.setConflicts(project, conflictedToSave);
                    sm.closeSession();
                } catch (Exception ex) {
                    log.error("", ex);
                    sm.cancelSession();
                }
                listener.setDisabled(false);
                failure = true;
                vss.add(mv.getSuite());
                mv.showWindow();
                return;
            }
        }
        else {
            if (!skipUpdate) {
                Utils.guilog("[INFO] MMS has no updates.");
            }
            //DeltaSyncProjectEventListenerAdapter.setLooseEnds(project, null);
            //DeltaSyncProjectEventListenerAdapter.setFailed(project, null);
        }

        //send local changes
        if (commit) {
            Utils.guilog("[INFO] Committing local changes to MMS...");
            JSONArray toSendElements = new JSONArray();
            Set<String> alreadyAdded = new HashSet<String>();
            for (Element e : localAdded.values()) {
                if (e == null) {
                    continue;
                }
                String id = ExportUtility.getElementID(e);
                if (id == null) {
                    continue;
                }
                if (alreadyAdded.contains(id)) {
                    continue;
                }
                alreadyAdded.add(id);
                toSendElements.add(ExportUtility.fillElement(e, null));
            }
            for (Element e : localUpdated.values()) {
                if (e == null) {
                    continue;
                }
                String id = ExportUtility.getElementID(e);
                if (id == null) {
                    continue;
                }
                if (alreadyAdded.contains(id)) {
                    continue;
                }
                alreadyAdded.add(id);
                toSendElements.add(ExportUtility.fillElement(e, null));
            }
            JSONObject toSendUpdates = new JSONObject();
            toSendUpdates.put("elements", toSendElements);
            toSendUpdates.put("source", "magicdraw");
            toSendUpdates.put("mmsVersion", "2.3");
            if (toSendElements.size() > 100) {

            }
            //do foreground?
            if (!toSendElements.isEmpty()) {
                Utils.guilog("[INFO] Change requests are added to queue.");
                gov.nasa.jpl.mbee.ems.sync.queue.OutputQueue.getInstance().offer(new Request(ExportUtility.getPostElementsUrl(), toSendUpdates.toJSONString(), "POST", true, toSendElements.size(), "Sync Changes"));
            }
            localAdded.clear();
            localUpdated.clear();

            JSONArray toDeleteElements = new JSONArray();
            if (delete) {
                for (String e : localDeleted.keySet()) {
                    if (ExportUtility.getElementFromID(e) != null) //somehow the model has it, don't delete on server
                    {
                        continue;
                    }
                    JSONObject toDelete = new JSONObject();
                    toDelete.put("sysmlid", e);
                    toDeleteElements.add(toDelete);
                }
                toSendUpdates.put("elements", toDeleteElements);
                toSendUpdates.put("source", "magicdraw");
                toSendUpdates.put("mmsVersion", "2.3");
                if (!toDeleteElements.isEmpty()) {
                    Utils.guilog("[INFO] Delete requests are added to queue.");
                    gov.nasa.jpl.mbee.ems.sync.queue.OutputQueue.getInstance().offer(new Request(ExportUtility.getUrlWithWorkspace() + "/elements", toSendUpdates.toJSONString(), "DELETEALL", true, toDeleteElements.size(), "Sync Deletes"));
                }
                localDeleted.clear();
            }
            if (toDeleteElements.isEmpty() && toSendElements.isEmpty()) {
                Utils.guilog("[INFO] No changes to commit.");
            }
            if (!toDeleteElements.isEmpty() || !toSendElements.isEmpty() || !toGet.isEmpty()) {
                Utils.guilog("[INFO] Don't forget to save or commit to teamwork and unlock!");
            }

            JSONObject toSave = null;
            if (!delete && !localDeleted.isEmpty()) {
                toSave = new JSONObject();
                JSONArray toSaveDelete = new JSONArray();

                toSaveDelete.addAll(localDeleted.keySet());
                toSave.put("deleted", toSaveDelete);
                toSave.put("changed", new JSONArray());
                toSave.put("added", new JSONArray());
                localDeleted.clear();
            }
            listener.setDisabled(true);
            SessionManager sm = SessionManager.getInstance();
            sm.createSession("updates sent");
            try {
                DeltaSyncProjectEventListenerAdapter.setUpdatesOrFailed(project, toSave, "update", true);
                sm.closeSession();
            } catch (Exception ex) {
                log.error("", ex);
                sm.cancelSession();
            }
            listener.setDisabled(false);
        }
        if (!toGet.isEmpty() && !commit) {
            Utils.guilog("[INFO] Don't forget to save or commit to teamwork and unlock!");
        }
    }

    public boolean getFailure() {
        return failure;
    }

    //    public ValidationSuite getSuite() {
//        return suite;
//    }
//    
    public List<ValidationSuite> getValidations() {
        return vss;
    }

}
