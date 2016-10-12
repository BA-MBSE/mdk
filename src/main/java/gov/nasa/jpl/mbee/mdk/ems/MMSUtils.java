package gov.nasa.jpl.mbee.mdk.ems;

import com.fasterxml.jackson.databind.JsonNode;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.task.ProgressStatus;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.auxiliaryconstructs.mdmodels.Model;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import gov.nasa.jpl.mbee.mdk.api.incubating.MDKConstants;
import gov.nasa.jpl.mbee.mdk.api.incubating.convert.Converters;
import gov.nasa.jpl.mbee.mdk.json.JacksonUtils;
import gov.nasa.jpl.mbee.mdk.lib.MDUtils;
import gov.nasa.jpl.mbee.mdk.lib.TicketUtils;
import gov.nasa.jpl.mbee.mdk.lib.Utils;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import gov.nasa.jpl.mbee.mdk.options.MDKOptionsGroup;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/**
 * Created by igomes on 9/26/16.
 * Expanded/refactored by ablack on 10/10/16
 */

// TODO Use URI builder or similar @donbot
public class MMSUtils {

    private static final int CHECK_CANCEL_DELAY = 100;
    
    private static String developerUrl = "";
    private static String developerSite = "";



    public enum HttpRequestType {
        GET, POST, PUT, DELETE
    }

    public enum ThreadRequestExceptionType {
        IOEXCEPTION, SERVEREXCEPTION, URISYNTAXEXCEPTION
    }

    public static ObjectNode getElement(Element element, Project project)
            throws IOException,  ServerException, URISyntaxException {
        return getElementById(Converters.getElementToIdConverter().apply(element), project);
    }

    public static ObjectNode getElementById(String id, Project project)
            throws IOException,  ServerException, URISyntaxException {
        // build request
        if (id == null) {
            return null;
        }
        URIBuilder requestUri = getServiceWorkspacesSitesProjectsElementsUri(project);
        id = id.replace(".", "%2E");
        requestUri.setPath(requestUri.getPath() + "/" + id);

        // do request
        ObjectNode response = sendMMSRequest(HttpRequestType.GET, requestUri);

        // parse response
        JsonNode value;
        if ((value = response.get("elements")) instanceof ArrayNode
                && (value = ((ArrayNode)value).get(0)) instanceof ObjectNode) {
            return (ObjectNode) value;
        }
        return null;
    }

    public static ObjectNode getElements(Collection<Element> elements, Project project, ProgressStatus ps)
            throws IOException,  ServerException, URISyntaxException {
        return getElementsById(elements.stream().map(Converters.getElementToIdConverter())
                .filter(id -> id != null).collect(Collectors.toList()), project, ps);
    }

    public static ObjectNode getElementsById(Collection<String> ids, Project project, ProgressStatus progressStatus)
            throws IOException,  ServerException, URISyntaxException {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        
        // create requests json
        final ObjectNode requests = JacksonUtils.getObjectMapper().createObjectNode();
        // put elements array inside request json, keep reference
        ArrayNode idsArrayNode = requests.putArray("elements");
        for (String id : ids) {
            // create json for id strings, add to request array
            ObjectNode element = JacksonUtils.getObjectMapper().createObjectNode();
            element.put(MDKConstants.SYSML_ID_KEY, id);
            idsArrayNode.add(element);
        }

        URIBuilder requestUri = getServiceWorkspacesSitesProjectsElementsUri(project);
        if (requestUri == null) {
            return null;
        }

        //do cancellable request
        Utils.guilog("[INFO] Searching for " + ids.size() + " elements from server...");
        return sendCancellableMMSRequest(HttpRequestType.GET, requestUri, requests, progressStatus);
    }

    // TODO Fix me and move me to MMSUtils @donbot
    // TODO Add both ?recurse and element list gets @donbot
    public static ObjectNode getServerElementsRecursively(Project project, Element element,
                                                                      boolean recurse, int depth,
                                                                      ProgressStatus progressStatus)
            throws ServerException, IOException, URISyntaxException {
        // configure request
        String id = Converters.getElementToIdConverter().apply(element);
        URIBuilder requestUri = getServiceWorkspacesSitesProjectsElementsUri(project);
        if (requestUri == null) {
            return null;
        }
        requestUri = MMSUtils.getServiceWorkspacesUri(project);
        requestUri.setPath(requestUri.getPath() + "/elements/" + id);
        if (depth > 0) {
            requestUri.setParameter("depth", java.lang.Integer.toString(depth));
        } else {
            requestUri.setParameter("recurse", java.lang.Boolean.toString(recurse));
        }
        requestUri.setParameter("qualified", "false");

        // do request in cancellable thread
        return sendCancellableMMSRequest(HttpRequestType.GET, requestUri, null, progressStatus);
    }

    /**
     * General purpose method for making http requests. Type of request is specified in method call.
     *
     * @param type Type of request, as selected from one of the options in the inner enum.
     * @param requestUri URI to send the request to. Methods to generate this URI are available in the class.
     * @param sendData Data to send as an entity/body along with the request, if desired. Support for GET and DELETE
     *                 with body is included.
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws ServerException contains both response code and response body
     */
    public static ObjectNode sendMMSRequest(HttpRequestType type, URIBuilder requestUri, ObjectNode sendData)
            throws IOException, URISyntaxException, ServerException {

        // build specified request type
        // assume that any request can have a body, and just build the appropriate one
        URI requestDest = requestUri.build();
        HttpRequestBase request = null;
        switch (type) {
            case DELETE:
                request = new HttpDeleteWithBody(requestDest);
                break;
            case GET:
                request = new HttpGetWithBody(requestDest);
                break;
            case POST:
                request = new HttpPost(requestDest);
                break;
            case PUT:
                request = new HttpPut(requestDest);
                break;
        }
        request.addHeader("Content-Type", "application/json");
        request.addHeader("charset", "utf-8");
        if (sendData != null) {
            String data = JacksonUtils.getObjectMapper().writeValueAsString(sendData);
            ((HttpPost)request).setEntity(new StringEntity(data, ContentType.APPLICATION_JSON));
        }

        // create client, execute request, parse response, store in thread safe buffer to return as string later
        // client, response, and reader are all auto closed after block
        ObjectNode responseJson;
        try (CloseableHttpClient httpclient = HttpClients.createDefault();
             CloseableHttpResponse response = httpclient.execute(request);
        ){
            int responseCode = response.getStatusLine().getStatusCode();
            String responseText = response.getEntity().getContent().toString();
            //TODO error processing
            if (processRequestErrors(responseText, responseCode)) {
                throw new ServerException(responseText, responseCode);
            }
            responseJson = JacksonUtils.getObjectMapper().readValue(response.getEntity().getContent(), ObjectNode.class);
        } catch (Exception e) {
            throw e;
        }
        return responseJson;
    }

    /**
     * Convenience method for requests without body. Not difficult to remove, but the code is easier to read if you
     * don't have to pass 'null' when calling it
     *
     * @param type Type of request, as selected from one of the options in the inner enum.
     * @param requestUri URI to send the request to. Methods to generate this URI are available in the class.
     * @return response as JSON
     *
     * @throws ServerException contains both response code and response body
     */
    public static ObjectNode sendMMSRequest(HttpRequestType type, URIBuilder requestUri)
            throws IOException, URISyntaxException, ServerException {
        return sendMMSRequest(type, requestUri, null);
    }

    /**
     * General purpose method for running a cancellable request. Builds a new thread to run the request, and passes
     * any relevant exception information back out via atomic references and generates new exceptions in calling thread
     *
     * @param type Type of request, as selected from one of the options in the inner enum.
     * @param requestUri URI to send the request to. Methods to generate this URI are available in the class.
     * @param sendData Data to send as an entity/body along with the request, if desired. Support for GET and DELETE
     *                 with body is included.
     * @param progressStatus
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws ServerException contains both response code and response body
     */
    public static ObjectNode sendCancellableMMSRequest (HttpRequestType type, URIBuilder requestUri,
                                                        ObjectNode sendData, ProgressStatus progressStatus)
            throws IOException, URISyntaxException, ServerException {
        final AtomicReference<ObjectNode> resp = new AtomicReference<>();
        final AtomicReference<Integer> ecode = new AtomicReference<>();
        final AtomicReference<ThreadRequestExceptionType> etype = new AtomicReference<>();
        final AtomicReference<String> emsg = new AtomicReference<>();
        final AtomicReference<String> einput = new AtomicReference<>();
        Thread t = new Thread(() -> {
            ObjectNode response = JacksonUtils.getObjectMapper().createObjectNode();
            try {
                response = sendMMSRequest(type, requestUri, sendData);
                etype.set(null);
                ecode.set(200);
                emsg.set("");
            } catch (URISyntaxException e) {
                etype.set(ThreadRequestExceptionType.URISYNTAXEXCEPTION);
                emsg.set(e.getReason());
                einput.set(e.getInput());
                e.printStackTrace();
            } catch (ServerException ex) {
                etype.set(ThreadRequestExceptionType.SERVEREXCEPTION);
                ecode.set(ex.getCode());
                emsg.set(ex.getMessage());
                ex.printStackTrace();
            } catch (IOException e) {
                etype.set(ThreadRequestExceptionType.IOEXCEPTION);
                emsg.set(e.getMessage());
                e.printStackTrace();
            }
            resp.set(response);
        });
        t.start();
        try {
            t.join(CHECK_CANCEL_DELAY);
            while (t.isAlive()) {
                if (progressStatus.isCancel()) {
                    Application.getInstance().getGUILog().log("[INFO] Request to server for elements cancelled.");
                    //clean up thread?
                    return null;
                }
                t.join(CHECK_CANCEL_DELAY);
            }
        } catch (Exception e) {

        }
        if (etype.get() == ThreadRequestExceptionType.URISYNTAXEXCEPTION) {
            throw new URISyntaxException(einput.get(), emsg.get());
        } else if (etype.get() == ThreadRequestExceptionType.SERVEREXCEPTION) {
            throw new ServerException(emsg.get(), ecode.get());
        } else if (etype.get() == ThreadRequestExceptionType.IOEXCEPTION) {
            throw new IOException(emsg.get());
        }
        return resp.get();
    }

    /**
     * Convenience method for requests without body. Not difficult to remove, but the code is easier to read if you
     * don't have to pass 'null' when calling it
     *
     * @param type Type of request, as selected from one of the options in the inner enum.
     * @param requestUri URI to send the request to. Methods to generate this URI are available in the class.
     * @param progressStatus
     * @return response as JSON
     *
     * @throws ServerException contains both response code and response body
     */
    public static ObjectNode sendCancellableMMSRequest(HttpRequestType type, URIBuilder requestUri,
                                                       ProgressStatus progressStatus)
            throws IOException, URISyntaxException, ServerException {
        return sendCancellableMMSRequest(type, requestUri, null, progressStatus);
    }


    /**
     * Method to check if the currently logged in user has permissions to edit the specified site on
     * the specified server.
     *
     * @param project The project containing the mms url to check against.
     * @param site Site name (sysmlid) of the site you are querying for. If empty or null, will use the site from the
     *             project parameter.
     * @return true if the site lists "editable":"true" for the logged in user, false otherwise
     * @throws ServerException
     */
    public static boolean isSiteEditable(Project project, String site)
            throws IOException, URISyntaxException, ServerException {
        boolean print = MDKOptionsGroup.getMDKOptions().isLogJson();
        if (site == null || site.equals("")) {
            site = getSiteName(project);
        }

        // configure request
        //https://cae-ems.jpl.nasa.gov/alfresco/service/workspaces/master/sites
        URIBuilder requestUri = getServiceWorkspacesUri(project);
        requestUri.setPath(requestUri.getPath() + "/sites");

        // do request
        ObjectNode response = JacksonUtils.getObjectMapper().createObjectNode();
        try {
            response = sendMMSRequest(HttpRequestType.GET, requestUri);
        } catch (IOException e) {
            //TODO
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ServerException e) {
            e.printStackTrace();
        }

        // parse response
        JsonNode arrayNode;
        if ((arrayNode = response.get("sites")) != null && arrayNode instanceof ArrayNode) {
            JsonNode value;
            for (JsonNode node : (ArrayNode)arrayNode) {
                if ((value = node.get(MDKConstants.SYSML_ID_KEY)) != null
                        && value.isTextual() && value.asText().equals(site)
                        && (value = node.get("editable")) != null && value.isBoolean()) {
                    return value.asBoolean();
                }
            }
        }
        return false;
    }

    /**
     *
     * @param code
     * @param response
     * @return
     */
    public static boolean processRequestErrors(String response, int code) throws IOException {
        // disabling of popup messages is handled by Utils, which will redirect them to the GUILog if disabled
        if (code != 200) {
            if (code >= 500) {
                Utils.showPopupMessage("Server Error. See message window for details.");
                if (response != null) {
                    Utils.guilog(response);
                }
            }
            else if (code == 403) {
                Utils.showPopupMessage("You do not have permission to do this.");
            }
            else if (code == 401) {
                Utils.showPopupMessage("You are not authorized or don't have permission. You can login and try again.");
                TicketUtils.clearUsernameAndPassword();
            }
            else {
                try {
                    ObjectNode responseJson = JacksonUtils.getObjectMapper().readValue(response, ObjectNode.class);
                    JsonNode value;
                    if (responseJson != null) {
                        if ((value = responseJson.get("message")) != null && value.isTextual()) {
                            Utils.guilog(value.asText());
                        } else {
                            Utils.guilog("Server response: " + code +
                                    (MDKOptionsGroup.getMDKOptions().isLogJson() ?
                                            " " + JacksonUtils.getObjectMapper().writeValueAsString(responseJson) : ""));
                        }
                    } else {
                        Utils.guilog("Server response: " + code + " " + response);
                    }
                } catch (IOException e) {
                    Utils.guilog("[ERROR] Unexpected error processing MMS response.");
                    Utils.guilog("Server response: " + code + " " + response);
                    e.printStackTrace();
                }
                if (code == 400) {
                    return false;
                }
            }
            return true;
        }
        ObjectNode responseJson = JacksonUtils.getObjectMapper().readValue(response, ObjectNode.class);
        JsonNode value;
        if ((responseJson != null) && ((value = responseJson.get("message")) != null) && value.isTextual()) {
            Utils.guilog("Server message: 200 " + value.asText());
        }
        return false;
    }

    /**
     * Returns a URIBuilder object with a path = "/alfresco/service". Used as the base for all of the rest of the
     * URIBuilder generating convenience classes.
     *
     * @param project The project to gather the mms url and site name information from
     * @return URIBuilder
     * @throws URISyntaxException
     */
    public static URIBuilder getServiceUri(Project project) {
        Model primaryModel = project.getModel();
        if (project == null || primaryModel == null) {
            return null;
        }
        
        String urlString = getServerUrl(project);
        
        // [scheme:][//host][path][?query][#fragment]
        String uriPath = "/alfresco/service";
        String uriTicket = TicketUtils.getTicket(project);

        URIBuilder uri = null;
        try {
            uri = new URIBuilder(urlString);
            uri.setPath(uriPath)
                    .setParameter("alf_ticket", uriTicket);
            return uri;
        } catch (URISyntaxException e) {
            Application.getInstance().getGUILog().log("[ERROR] Unexpected error in generatation of MMS URL for " +
                    "project. Reason: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns a URIBuilder object with a path = "/alfresco/service/workspaces/{$WORKSPACE}".
     *
     * @param project The project to gather the mms url and site name information from
     * @return URIBuilder
     */
    public static URIBuilder getServiceWorkspacesUri(Project project) {
        URIBuilder workspaceUri = getServiceUri(project);
        if (workspaceUri == null) {
            return null;
        }
        //TODO add support for non-master workspaces
//        String workspace = getSiteName(project);
        String workspace = "master";
        workspaceUri.setPath(workspaceUri.getPath() + "/workspaces/" + workspace);
        return workspaceUri;
    }

    /**
     * Returns a URIBuilder object with a path = "/alfresco/service/workspaces/{$WORKSPACE}/sites/{$SITE}".
     *
     * @param project The project to gather the mms url and site name information from
     * @return URIBuilder
     */
    public static URIBuilder getServiceWorkspacesSitesUri(Project project) {
        URIBuilder siteUri = getServiceWorkspacesUri(project);
        if (siteUri == null) {
            return null;
        }
        String sites = getSiteName(project);
        siteUri.setPath(siteUri.getPath() + "/sites/" + sites);
        return siteUri;
    }

    /**
     *
     * Returns a URIBuilder object with a path = "/alfresco/service/workspaces/{$WORKSPACE}/sites/{$SITE}/projects/{$PROJECTID}".
     *
     * @param project The project to gather the mms url and site name information from
     * @return URIBuilder
     */
    public static URIBuilder getSerivceWorkspacesSitesProjectsUri(Project project) {
        URIBuilder projectUri = getServiceWorkspacesSitesUri(project);
        if (projectUri == null) {
            return null;
        }
        String projectId = project.getPrimaryProject().getProjectID();
        projectUri.setPath(projectUri.getPath() + "/projects/" + projectId);
        return projectUri;
    }

    /**
     * Returns a URIBuilder object with a path = "/alfresco/service/workspaces/{$WORKSPACE}/sites/{$SITE}/projects/{$PROJECTID}/elements".
     *
     * @param project The project to gather the mms url and site name information from
     * @return URIBuilder
     */
    public static URIBuilder getServiceWorkspacesSitesProjectsElementsUri(Project project) {
        URIBuilder elementsUri = getServiceWorkspacesSitesUri(project);
        if (elementsUri == null) {
            return null;
        }
        elementsUri.setPath(elementsUri.getPath() + "/elements");
        return elementsUri;
    }

    /**
     *
     * @param project
     * @return
     * @throws IllegalStateException
     */
    public static String getServerUrl(Project project) throws IllegalStateException {
        String urlString = null;
        if (project == null) {
            throw new IllegalStateException("Project is null.");
        }
        Element primaryModel = project.getModel();
        if (primaryModel == null) {
            throw new IllegalStateException("Model is null.");
        }

        if (StereotypesHelper.hasStereotype(primaryModel, "ModelManagementSystem")) {
            urlString = (String) StereotypesHelper.getStereotypePropertyFirst(primaryModel, "ModelManagementSystem", "MMS URL");
        }
        else {
            Utils.showPopupMessage("Your project root element doesn't have ModelManagementSystem Stereotype!");
        }
        if ((urlString == null || urlString.equals(""))) {
            if (!MDUtils.isDeveloperMode()) {
                Utils.showPopupMessage("Your project root element doesn't have ModelManagementSystem MMS URL stereotype property set!");
            } else {
                urlString = JOptionPane.showInputDialog("[DEVELOPER MODE] Enter the server URL:", developerUrl);
                developerUrl = urlString;
            }
        }
        if (urlString == null || urlString.equals("")) {
            throw new IllegalStateException("MMS URL is null or empty.");
        }
        return urlString;
    }
    
    public static String getSiteName(Project project) {
        String siteString = null;
        if (project == null) {
            throw new IllegalStateException("Project is null.");
        }
        Element primaryModel = project.getModel();
        if (primaryModel == null) {
            throw new IllegalStateException("Model is null.");
        }

        if (StereotypesHelper.hasStereotype(primaryModel, "ModelManagementSystem")) {
            siteString = (String) StereotypesHelper.getStereotypePropertyFirst(primaryModel, "ModelManagementSystem", "MMS Site");
        }
        else {
            Utils.showPopupMessage("Your project root element doesn't have ModelManagementSystem Stereotype!");
        }
        if ((siteString == null || siteString.equals(""))) {
            if (!MDUtils.isDeveloperMode()) {
                Utils.showPopupMessage("Your project root element doesn't have ModelManagementSystem MMS Site stereotype property set!");
            } else {
                siteString = JOptionPane.showInputDialog("[DEVELOPER MODE] Enter the site:", developerSite);
                developerSite = siteString;
            }
        }
        if (siteString == null || siteString.equals("")) {
            throw new IllegalStateException("MMS Site is null or empty.");
        }
        return siteString;
    }

}

