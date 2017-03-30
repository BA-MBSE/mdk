package gov.nasa.jpl.mbee.mdk.ems.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.nomagic.ci.persistence.IPrimaryProject;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.ProjectUtilities;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.esi.EsiUtils;
import com.nomagic.task.ProgressStatus;

import gov.nasa.jpl.mbee.mdk.api.incubating.MDKConstants;
import gov.nasa.jpl.mbee.mdk.docgen.validation.*;
import gov.nasa.jpl.mbee.mdk.ems.MMSUtils;
import gov.nasa.jpl.mbee.mdk.ems.ServerException;
import gov.nasa.jpl.mbee.mdk.ems.actions.CommitBranchAction;
import gov.nasa.jpl.mbee.mdk.ems.json.JsonDiffFunction;
import gov.nasa.jpl.mbee.mdk.json.JacksonUtils;
import gov.nasa.jpl.mbee.mdk.json.JsonPatchUtils;
import gov.nasa.jpl.mbee.mdk.lib.Pair;
import gov.nasa.jpl.mbee.mdk.lib.Utils;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

public class BranchValidator {

    private final Project project;
    private ValidationSuite validationSuite = new ValidationSuite("structure");
//    private ValidationRule twcMissingBranchValidationRule = new ValidationRule("Missing in Client", "Branch shall exist in TWC if it exists in MMS.", ViolationSeverity.WARNING);
    private ValidationRule mmsMissingBranchValidationRule = new ValidationRule("Missing on Server", "Branch shall exist in MMS if it exists in TeamworkCloud.", ViolationSeverity.WARNING);
    private ValidationRule branchEquivalenceValidationRule = new ValidationRule("Branch Equivalence", "Branch shall be represented in MagicDraw and MMS equivalently.", ViolationSeverity.WARNING);

    public BranchValidator(Project project) {
        this.project = project;
//        validationSuite.addValidationRule(twcMissingBranchValidationRule);
        validationSuite.addValidationRule(mmsMissingBranchValidationRule);
        validationSuite.addValidationRule(branchEquivalenceValidationRule);
    }

    public void validate(ProgressStatus progressStatus, boolean allBranches) {
        Project project = Application.getInstance().getProject();
        IPrimaryProject primaryProject = project.getPrimaryProject();

        if (!ProjectUtilities.isRemote(primaryProject)) {
            return;
        }
        if (EsiUtils.getLoggedUserName() == null) {
            Utils.guilog("[INFO] You need to logged in to Teamwork Cloud first to do branch validation. Aborting.");
            return;
        }

        String currentBranch = EsiUtils.getCurrentBranch(primaryProject).getName();
        if (currentBranch.equals("trunk")) {
            currentBranch = "master";
        }
        Map<String, Pair<EsiUtils.EsiBranchInfo, ObjectNode>> clientBranches = new HashMap<>();
        Map<String, ObjectNode> serverBranches = new HashMap<>();

        if (progressStatus != null) {
            progressStatus.setDescription("Mapping Teamwork Cloud branches");
            progressStatus.setIndeterminate(true);
        }

        Collection<EsiUtils.EsiBranchInfo> targetBranches = null;
        if (allBranches) {
            try {
                ProjectDescriptor projectDescriptor = ProjectDescriptorsFactory.createAnyRemoteProjectDescriptor(project);
                targetBranches = EsiUtils.getBranches(projectDescriptor);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (targetBranches == null || targetBranches.isEmpty()) {
                return;
            }
        }
        else {
            targetBranches = new ArrayList<>(1);
            targetBranches.add(EsiUtils.getCurrentBranch(primaryProject));
        }
        for (EsiUtils.EsiBranchInfo branch : targetBranches) {
            ObjectNode branchJson = getRefObjectNode(project, branch, true);
            JsonNode value;
            String entryKey;
            if ((value = branchJson.get(MDKConstants.ID_KEY)) != null && value.isTextual()) {
                entryKey = branchJson.get(MDKConstants.ID_KEY).asText();
                if (allBranches || entryKey.equals(currentBranch)) {
                    clientBranches.put(entryKey, new Pair<>(branch, branchJson));
                }
            }
        }

        if (progressStatus != null) {
            progressStatus.setDescription("Mapping MMS branches");
        }

        URIBuilder requestUri = MMSUtils.getServiceProjectsRefsUri(project);
        if (requestUri == null) {
            Application.getInstance().getGUILog().log("[ERROR] Unable to get MMS URL. Branch validation cancelled.");
            return;
        }
        if (!allBranches) {
            requestUri.setPath(requestUri.getPath() + "/" + currentBranch);
        }
        try {
            HttpRequestBase request = MMSUtils.buildRequest(MMSUtils.HttpRequestType.GET, requestUri);
            ObjectNode response = MMSUtils.sendMMSRequest(project, request);
            JsonNode refsArray, value;
            if ((refsArray = response.get("refs")) != null && refsArray.isArray()) {
                for (JsonNode refJson : refsArray) {
                    if (refJson.isObject()) {
                        String entryKey;
                        if ((value = refJson.get(MDKConstants.ID_KEY)) != null && value.isTextual()) {
                            entryKey = refJson.get(MDKConstants.ID_KEY).asText();
                            if (allBranches || entryKey.equals(currentBranch)) {
                                serverBranches.put(entryKey, (ObjectNode) refJson);
                            }
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException | ServerException e) {
            e.printStackTrace();
            Application.getInstance().getGUILog().log("[ERROR] Exception occurred while getting MMS branches. Branch validation cancelled. Reason: " + e.getMessage());
            return;
        }

        Set<String> keySet = new HashSet<>();
        keySet.addAll(clientBranches.keySet());
        keySet.addAll(serverBranches.keySet());

        if (progressStatus != null) {
            progressStatus.setDescription("Generating validation results for " + keySet.size() + " branch" + (keySet.size() != 1 ? "es" : ""));
            progressStatus.setIndeterminate(false);
            progressStatus.setMax(keySet.size());
            progressStatus.setCurrent(0);
        }

        for (String key : keySet) {

            // TODO @DONBOT remove this check/skip for master branch after master is updatable
            if (key.equals("master")) { continue; }

            Pair<EsiUtils.EsiBranchInfo, ObjectNode> clientBranch = clientBranches.get(key);
            ObjectNode serverBranch = serverBranches.get(key);

            if (clientBranch == null) {
                //TODO @donbot 3.0.1 - add support for importing MMS branch into TWC
//                ValidationRuleViolation v = new ValidationRuleViolation(project.getPrimaryModel(), "[BRANCH MISSING ON MMS] The MMS branch \"" + key + "\" does not have a corresponding Teamwork Cloud branch.");
//                // add actions here
//                twcMissingBranchValidationRule.addViolation(v);
            }
            else if (serverBranch == null) {
                ValidationRuleViolation v = new ValidationRuleViolation(project.getPrimaryModel(), "[BRANCH MISSING ON MMS] The Teamwork Cloud branch \"" + key + "\" does not have a corresponding MMS branch.");
                v.addAction(new CommitBranchAction(key, project, clientBranch.getFirst(), false, false));
                v.addAction(new CommitBranchAction(key, project, clientBranch.getFirst(), true, false));
                mmsMissingBranchValidationRule.addViolation(v);

            }
            else {
                JsonNode diff = JsonDiffFunction.getInstance().apply(clientBranch.getSecond(), serverBranch);
                if (JsonPatchUtils.isEqual(diff)) {
                    continue;
                }
                ValidationRuleViolation v = new ValidationRuleViolation(project.getPrimaryModel(), "[BRANCH NOT EQUIVALENT] The Teamwork Cloud branch \"" + key + "\" is not equivalent to the corresponding MMS branch.");
                v.addAction(new CommitBranchAction(key, project, clientBranch.getFirst(), false, true));
                v.addAction(new CommitBranchAction(key, project, clientBranch.getFirst(), true, true));
                branchEquivalenceValidationRule.addViolation(v);
            }
            if (progressStatus != null) {
                progressStatus.increase();
            }
        }
    }

    public static ObjectNode getRefObjectNode(Project project, EsiUtils.EsiBranchInfo branchInfo, boolean update) {
        ObjectNode refObjectNode = JacksonUtils.getObjectMapper().createObjectNode();
        /*  "id": "master",
            "name": "master",
            "twcId" : ""
            "uri" : ""
            "qualifiedId": "master",
            "qualifiedName": "master", */
        String name = branchInfo.getName();
        if (name.equals("master")) {
            return null;
        }
        if (name.equals("trunk")) {
            name = "master";
        }
        if (update) {
            refObjectNode.put(MDKConstants.ID_KEY, name);
        }
        refObjectNode.put(MDKConstants.NAME_KEY, name);
        refObjectNode.put(MDKConstants.TWC_ID_KEY, branchInfo.getID().toString());
        refObjectNode.put(MDKConstants.TWC_URI_KEY,
                EsiUtils.getDescriptorByBranchID(ProjectDescriptorsFactory.createAnyRemoteProjectDescriptor(project), branchInfo.getID()).getURI().toString());
        // TODO unlink this from "master" when we support non-head branching
        refObjectNode.put(MDKConstants.PARENT_REF_ID_KEY, "master");
//        refObjectNode.put("commitId", "c7513a67-0543-4a9c-b978-a40ba65a2d07");
        return refObjectNode;
    }

    public void showWindow() {
        List<ValidationSuite> vss = new ArrayList<>();
        vss.add(validationSuite);
        if (validationSuite.hasErrors()) {
            Utils.displayValidationWindow(project, vss, "Branch Differences");
        }
    }

    public ValidationSuite getValidationSuite() {
        return validationSuite;
    }
}
