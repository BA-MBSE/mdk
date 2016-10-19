package gov.nasa.jpl.mbee.mdk.ems.sync.manual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.task.ProgressStatus;
import com.nomagic.task.RunnableWithProgress;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import gov.nasa.jpl.mbee.mdk.api.incubating.convert.Converters;
import gov.nasa.jpl.mbee.mdk.docgen.validation.ValidationRule;
import gov.nasa.jpl.mbee.mdk.docgen.validation.ValidationRuleViolation;
import gov.nasa.jpl.mbee.mdk.docgen.validation.ValidationSuite;
import gov.nasa.jpl.mbee.mdk.docgen.validation.ViolationSeverity;
import gov.nasa.jpl.mbee.mdk.ems.MMSUtils;
import gov.nasa.jpl.mbee.mdk.ems.ServerException;
import gov.nasa.jpl.mbee.mdk.ems.actions.CommitProjectAction;
import gov.nasa.jpl.mbee.mdk.ems.validation.ElementValidator;
import gov.nasa.jpl.mbee.mdk.lib.Pair;
import gov.nasa.jpl.mbee.mdk.lib.Utils;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by igomes on 9/26/16.
 */
public class ManualSyncRunner implements RunnableWithProgress {
    private final Collection<Element> rootElements;
    private final Project project;
    private final boolean recurse;
    private int depth;

    // TODO Move me to common sync pre-conditions @donbot
    private ValidationSuite validationSuite = new ValidationSuite("Sync Pre-Condition Validation");
    private ValidationRule projectExistenceValidationRule = new ValidationRule("Project Existence", "The project shall exist in the specified site.", ViolationSeverity.ERROR);

    {
        validationSuite.addValidationRule(projectExistenceValidationRule);
    }

    private ElementValidator elementValidator;

    public ManualSyncRunner(Collection<Element> rootElements, Project project, boolean recurse, int depth) {
        this.rootElements = rootElements;
        this.project = project;
        this.recurse = recurse;
        this.depth = depth;
    }

    @Override
    public void run(ProgressStatus progressStatus) {
        progressStatus.setDescription("Validating sync pre-conditions");
        progressStatus.setIndeterminate(true);
        if (!checkProject()) {
            if (validationSuite.hasErrors()) {
                Utils.displayValidationWindow(validationSuite, validationSuite.getName());
            }
            return;
        }

        progressStatus.setDescription("Processing and querying for " + rootElements.size() + " " + (recurse || depth > 0 ? "root " : "") + "element" + (rootElements.size() != 1 ? "s" : ""));
        progressStatus.setIndeterminate(false);
        progressStatus.setMax(rootElements.size());
        progressStatus.setCurrent(0);

        List<Pair<Element, ObjectNode>> clientElements = new ArrayList<>(rootElements.size());
        List<ObjectNode> serverElements = new ArrayList<>(rootElements.size());
        for (Element element : rootElements) {
            collectClientElementsRecursively(project, element, recurse, depth, clientElements);
            Collection<ObjectNode> jsonObjects = null;
            try {
                jsonObjects = collectServerElementsRecursively(project, element, recurse, depth, progressStatus);
            } catch (ServerException e) {
                //TODO @donbot process errors for recursive server element get
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            if (jsonObjects == null) {
                if (!progressStatus.isCancel()) {
                    Application.getInstance().getGUILog().log("[ERROR] Failed to get elements from the server. Aborting manual sync.");
                }
                return;
            }
            serverElements.addAll(jsonObjects);

            progressStatus.increase();
        }
        elementValidator = new ElementValidator(clientElements, serverElements, project);
        elementValidator.run(progressStatus);
    }

    public static void collectClientElementsRecursively(Project project, Element element, boolean recurse, int depth, List<Pair<Element, ObjectNode>> elements) {
        ObjectNode jsonObject = Converters.getElementToJsonConverter().apply(element, project);
        if (jsonObject == null) {
            return;
        }
        elements.add(new Pair<>(element, jsonObject));
        if (recurse || depth > 0) {
            for (Element e : element.getOwnedElement()) {
                collectClientElementsRecursively(project, e, recurse, --depth, elements);
            }
        }
    }

    // TODO Fix me and move me to MMSUtils @donbot
    // TODO Add both ?recurse and element list gets @donbot
    public static Collection<ObjectNode> collectServerElementsRecursively(Project project, Element element,
                                                                          boolean recurse, int depth,
                                                                          ProgressStatus progressStatus)
            throws ServerException, IOException, URISyntaxException {
        ObjectNode response = MMSUtils.getServerElementsRecursively(project, element, recurse, depth, progressStatus);

        // process response
        JsonNode value;
        if (response != null && (value = response.get("elements")) != null && value.isArray()) {
            return StreamSupport.stream(value.spliterator(), false)
                    .filter(JsonNode::isObject).map(jsonNode -> (ObjectNode) jsonNode).collect(Collectors.toList());
        }
        return null;
    }

    // TODO Make common across all sync types @donbot
    public boolean checkProject() {
        // build request for site element
        URIBuilder requestUri;
        requestUri = MMSUtils.getServiceWorkspacesElementsUri(project);
        if (requestUri == null) {
            return false;
        }
        requestUri.setPath(requestUri.getPath() + "/" + project.getPrimaryProject().getProjectID());

        // do request for site element
        ObjectNode response = null;
        try {
            response = MMSUtils.sendMMSRequest(MMSUtils.buildRequest(MMSUtils.HttpRequestType.GET, requestUri));
        } catch (Exception e) {
            Application.getInstance().getGUILog().log("[ERROR] Unexpected error when querying site element on MMS. " +
                    "Reason: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        // process response for site element
        // missing projects will return {}
        if (response.get("elements") == null) {
            ValidationRuleViolation v;

            String workspace = MMSUtils.getServiceWorkspacesUri(project).getPath();
            if (workspace.contains("master")) {
                v = new ValidationRuleViolation(project.getModel(), "The project doesn't exist on the web.");
                v.addAction(new CommitProjectAction(project, true));
            } else {
                v = new ValidationRuleViolation(project.getModel(), "The trunk project doesn't exist on the web. Export the trunk first.");
            }
            projectExistenceValidationRule.addViolation(v);
            return false;
        }


        //TODO re-imagineer later when we've confirmed if these errors still happen
        /*
        String respons = null;
        try {
            respons = ExportUtility.get(projectUrl, false);
        } catch (ServerException ex) { }
        if (respons == null || respons.contains("Site node is null") || respons.contains("Could not find project")) {//tears

            ValidationRuleViolation v = new ValidationRuleViolation(Application.getInstance().getProject().getModel(), "The project exists on the server already under a different site.");
            //v.addAction(new CommitProjectAction(false));
            projectExistenceValidationRule.addViolation(v);
            return false;
        }
        for (Element start : rootElements) {
            if (ProjectUtilities.isElementInAttachedProject(start)) {
                Utils.showPopupMessage("You should not validate or export elements not from this project! Open the right project and do it from there");
                return false;
            }
        }
        */
        return true;
    }

    public ValidationSuite getValidationSuite() {
        return validationSuite.hasErrors() ? validationSuite : (elementValidator != null ? elementValidator.getValidationSuite() : null);
    }
}
