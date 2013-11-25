package gov.nasa.jpl.mbee.actions;

import gov.nasa.jpl.mbee.ViewEditorProfile;
import gov.nasa.jpl.mbee.viewedit.ViewEditUtils;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

public class DeleteDocumentAction extends MDAction {
    private Element            proj;
    public static final String actionid = "DeleteDocument";

    public DeleteDocumentAction(Element e) {
        super(actionid, "Remove From View Editor", null, null);
        proj = e;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GUILog gl = Application.getInstance().getGUILog();
        String docid = proj.getID();
        if (StereotypesHelper.hasStereotypeOrDerived(proj, ViewEditorProfile.document)) {
            docid = (String)StereotypesHelper.getStereotypePropertyFirst(proj, ViewEditorProfile.document,
                    "documentId");
        }
        String url = ViewEditUtils.getUrl();
        if (url == null || url.equals(""))
            return;
        url += "/rest/projects/document/" + docid + "/delete";
        PostMethod pm = new PostMethod(url);
        try {
            // pm.setRequestHeader("Content-Type", "text/json");
            // pm.setRequestEntity(JsonRequestEntity.create(vol.getID()));
            // Protocol easyhttps = new Protocol("https", new
            // EasySSLProtocolSocketFactory(), 443);
            // Protocol.registerProtocol("https", easyhttps);
            HttpClient client = new HttpClient();
            ViewEditUtils.setCredentials(client, url);
            int code = client.executeMethod(pm);
            if (ViewEditUtils.showErrorMessage(code))
                return;
            String response = pm.getResponseBodyAsString();
            if (response.equals("ok"))
                gl.log("[INFO] Remove Successful.");
            else if (response.equals("NotFound"))
                gl.log("[ERROR] Document not found.");
            else
                gl.log(response);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            pm.releaseConnection();
        }
    }

}
