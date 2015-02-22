package gov.nasa.jpl.mbee.actions.ems;

import gov.nasa.jpl.mbee.ems.ExportUtility;
import gov.nasa.jpl.mbee.ems.ImportUtility;
import gov.nasa.jpl.mbee.ems.sync.AutoSyncCommitListener;
import gov.nasa.jpl.mbee.ems.sync.AutoSyncProjectListener;
import gov.nasa.jpl.mbee.ems.sync.OutputQueue;
import gov.nasa.jpl.mbee.ems.sync.Request;
import gov.nasa.jpl.mbee.ems.validation.ModelValidator;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationSuite;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.ModelElementsManager;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.magicdraw.teamwork.application.TeamworkUtils;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

public class UpdateFromJMS extends MDAction {
    private static final long serialVersionUID = 1L;
    public static final String actionid = "UpdateFromJMS";
    
    private boolean commit;
    public UpdateFromJMS(boolean commit) {
        super(commit ? "CommitToMMS" : "UpdateFromJMS", commit ? "Check for Updates and Commit" : "Update", null, null);
        this.commit = commit;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed(ActionEvent ae) {
        Application.getInstance().getGUILog().log("[INFO] Getting changes from MMS...");
        Project project = Application.getInstance().getProject();
        Map<String, Set<String>> jms = AutoSyncProjectListener.getJMSChanges(Application.getInstance().getProject());
        AutoSyncCommitListener listener = AutoSyncProjectListener.getCommitListener(Application.getInstance().getProject());
        if (jms == null || listener == null)
            return; //some error here
        Map<String, Element> localAdded = listener.getAddedElements();
        Map<String, Element> localDeleted = listener.getDeletedElements();
        Map<String, Element> localChanged = listener.getChangedElements();
        
        Set<String> webChanged = jms.get("changed");
        Set<String> webAdded = jms.get("added");
        Set<String> webDeleted = jms.get("deleted");
        
        Set<String> toGet = new HashSet<String>(webChanged);
        toGet.addAll(webAdded);
        
        Map<String, JSONObject> cannotAdd = new HashMap<String, JSONObject>();
        Map<String, JSONObject> cannotChange = new HashMap<String, JSONObject>();
        Set<String> cannotDelete = new HashSet<String>();
        
        if (!toGet.isEmpty()) {
            TeamworkUtils.lockElement(project, project.getModel(), true);
            JSONObject getJson = new JSONObject();
            JSONArray getElements = new JSONArray();
            getJson.put("elements", getElements);
            for (String e: toGet) {
                JSONObject el = new JSONObject();
                el.put("sysmlid", e);
                getElements.add(el);
            }
            String url = ExportUtility.getUrlWithWorkspace();
            url += "/elements";
            String response = ExportUtility.getWithBody(url, getJson.toJSONString());
            if (response == null)
                return; //bad
            Map<String, JSONObject> webElements = new HashMap<String, JSONObject>();
            JSONObject webObject = (JSONObject)JSONValue.parse(response);
            JSONArray webArray = (JSONArray)webObject.get("elements");
            for (Object o: webArray) {
                String webId = (String)((JSONObject)o).get("sysmlid");
                webElements.put(webId, (JSONObject)o);
            }
            //if (webElements.size() != toGet.size())
            //    return; //??
                
            //calculate order to create web added elements
            List<JSONObject> webAddedObjects = new ArrayList<JSONObject>();
            for (String webAdd: webAdded) {
                if (webElements.containsKey(webAdd))
                    webAddedObjects.add(webElements.get(webAdd));
            }
            List<JSONObject> webAddedSorted = ImportUtility.getCreationOrder(webAddedObjects);
            
            //calculate potential conflicted set and clean web updated set
            Set<String> localChangedIds = new HashSet<String>(localChanged.keySet());
            localChangedIds.retainAll(webChanged);
            JSONArray webConflictedObjects = new JSONArray();
            Set<Element> localConflictedElements = new HashSet<Element>();
            if (!localChangedIds.isEmpty()) {
                for (String conflictId: localChangedIds) {
                    if (webElements.containsKey(conflictId)) {
                        webConflictedObjects.add(webElements.get(conflictId));
                        localConflictedElements.add(localChanged.get(conflictId));
                    }
                }
            }
            //find web changed that are not conflicted
            List<JSONObject> webChangedObjects = new ArrayList<JSONObject>();
            for (String webUpdate: webChanged) {
                if (localChangedIds.contains(webUpdate))
                    continue;
                if (webElements.containsKey(webUpdate))
                    webChangedObjects.add(webElements.get(webUpdate));
            }
            
            Application.getInstance().getGUILog().log("[INFO] Applying changes...");
            SessionManager sm = SessionManager.getInstance();
            sm.createSession("mms delayed sync change");
            try {
                //take care of web added
                if (webAddedSorted != null) {
                    for (Object element : webAddedSorted) {
                        ImportUtility.createElement((JSONObject) element, false);
                    }
                    for (Object element : webAddedSorted) { 
                        try {
                            ImportUtility.createElement((JSONObject) element, true);
                        } catch (Exception ex) {
                            cannotAdd.put((String)((JSONObject)element).get("sysmlid"), (JSONObject)element);
                        }
                    }
                } else {
                    for (Object element: webAddedObjects) {
                        cannotAdd.put((String)((JSONObject)element).get("sysmlid"), (JSONObject)element);
                    }
                }
            
                //take care of updated
                for (JSONObject webUpdated: webChangedObjects) {
                    Element e = ExportUtility.getElementFromID((String)webUpdated.get("sysmlid"));
                    if (e == null) {
                        //bad? maybe it was supposed to have been added?
                        continue;
                    }
                    try {
                        ImportUtility.updateElement(e, webUpdated);
                        ImportUtility.setOwner(e, webUpdated);
                    } catch (Exception ex) {
                        cannotChange.put(ExportUtility.getElementID(e), webUpdated);
                    }
                }
                
                //take care of deleted
                for (String e: webDeleted) {
                    Element toBeDeleted = ExportUtility.getElementFromID(e);
                    if (toBeDeleted == null)
                        continue;
                    try {
                        ModelElementsManager.getInstance().removeElement(toBeDeleted);
                    } catch (Exception ex) {
                        cannotDelete.add(e);
                    }
                }
                Application.getInstance().getGUILog().log("[INFO] Finished applying changes.");
                listener.disable();
                sm.closeSession();
                listener.enable();
              //conflicts???
                JSONObject mvResult = new JSONObject();
                mvResult.put("elements", webConflictedObjects);
                ModelValidator mv = new ModelValidator(null, mvResult, false, localConflictedElements, false);
                mv.validate(false, null);
                Set<Element> conflictedElements = mv.getDifferentElements();
                if (!conflictedElements.isEmpty()) {
                    Application.getInstance().getGUILog().log("[INFO] There are potential conflicts between changes from MMS and locally changed elements, please resolve first and rerun update/commit.");
                //?? popups or validation window?
                    mv.showWindow();
                    return;
                }
            } catch (Exception e) {
                sm.cancelSession();
            }
        } else {
            Application.getInstance().getGUILog().log("[INFO] MMS has no updates.");
        }
        
        //send local changes
        if (commit) {
            Application.getInstance().getGUILog().log("[INFO] Committing local changes to MMS...");
            JSONArray toSendElements = new JSONArray();
            for (Element e: localAdded.values()) {
                toSendElements.add(ExportUtility.fillElement(e, null));
            }
            for (Element e: localChanged.values()) {
                toSendElements.add(ExportUtility.fillElement(e, null));
            }
            JSONObject toSendUpdates = new JSONObject();
            toSendUpdates.put("elements", toSendElements);
            toSendUpdates.put("source", "magicdraw");
            if (toSendElements.size() > 100) {
                
            }
            //do foreground?
            if (!toSendUpdates.isEmpty())
                OutputQueue.getInstance().offer(new Request(ExportUtility.getPostElementsUrl(), toSendUpdates.toJSONString(), "POST", true));
            localAdded.clear();
            localChanged.clear();
            
            JSONArray toDeleteElements = new JSONArray();
            for (String e: localDeleted.keySet()) {
                JSONObject toDelete = new JSONObject();
                toDelete.put("sysmlid", e);
                toDeleteElements.add(toDelete);
            }
            toSendUpdates.put("elements", toDeleteElements);
            if (!toDeleteElements.isEmpty())
                OutputQueue.getInstance().offer(new Request(ExportUtility.getUrlWithWorkspace() + "/elements", toSendUpdates.toJSONString(), "DELETEALL", true));
            localDeleted.clear();
            if (toDeleteElements.isEmpty() && toSendElements.isEmpty())
                Application.getInstance().getGUILog().log("[INFO] No changes to commit.");
            else
                Application.getInstance().getGUILog().log("[INFO] Don't forget to commit to teamwork or save!");
            //commit automatically and send project version?
        }
    }
}