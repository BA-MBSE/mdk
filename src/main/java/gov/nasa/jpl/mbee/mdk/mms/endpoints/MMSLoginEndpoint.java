package gov.nasa.jpl.mbee.mdk.mms.endpoints;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.task.ProgressStatus;
import gov.nasa.jpl.mbee.mdk.http.ServerException;
import gov.nasa.jpl.mbee.mdk.json.JacksonUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static gov.nasa.jpl.mbee.mdk.mms.MMSUtils.sendMMSRequest;

public class MMSLoginEndpoint extends MMSEndpoint {
    public MMSLoginEndpoint(String baseUri) throws URISyntaxException {
        super(baseUri);
    }

    @Override
    public void prepareUriPath() {
        uriBuilder.setPath(uriBuilder.getPath() + MMSEndpointType.LOGIN.getPath());
        uriBuilder.clearParameters();
    }

    public String buildLoginRequest(Project project, String username, String password, ProgressStatus progressStatus) throws IOException, URISyntaxException, ServerException {
        //build request
        URI requestDest = uriBuilder.build();
        HttpRequestBase request = new HttpPost(requestDest);
        request.addHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        request.addHeader("charset", (Consts.UTF_8).displayName());

        ObjectNode credentials = JacksonUtils.getObjectMapper().createObjectNode();
        credentials.put("username", username);
        credentials.put("password", password);
        StringEntity jsonData = new StringEntity(JacksonUtils.getObjectMapper().writeValueAsString(credentials), ContentType.APPLICATION_JSON);
        ((HttpEntityEnclosingRequest) request).setEntity(jsonData);

        // do request
        ObjectNode responseJson = JacksonUtils.getObjectMapper().createObjectNode();
        sendMMSRequest(project, request, progressStatus, responseJson);
        if(responseJson != null && responseJson.get(MMSEndpointType.AUTHENTICATION_RESPONSE_JSON_KEY) != null && responseJson.get(MMSEndpointType.AUTHENTICATION_RESPONSE_JSON_KEY).isTextual()) {
            return responseJson.get(MMSEndpointType.AUTHENTICATION_RESPONSE_JSON_KEY).asText();
        }
        return null;
    }
}
