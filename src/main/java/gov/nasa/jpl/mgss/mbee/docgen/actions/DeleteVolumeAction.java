package gov.nasa.jpl.mgss.mbee.docgen.actions;

import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.ViewEditorProfile;
import gov.nasa.jpl.mgss.mbee.docgen.viewedit.ViewEditUtils;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

public class DeleteVolumeAction extends MDAction {
    private Element            proj;
    public static final String actionid = "DeleteVolume";

    public DeleteVolumeAction(Element e) {
        super(actionid, "Remove From View Editor", null, null);
        proj = e;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GUILog gl = Application.getInstance().getGUILog();
        String volid = proj.getID();
        List<Element> projects = Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(proj,
                ViewEditorProfile.hasVolume, 2, false, 1);
        boolean root = false;

        for (Element p: projects) {
            if (StereotypesHelper.hasStereotype(p, ViewEditorProfile.project))
                root = true;
        }

        if (!root) {
            Utils.showPopupMessage("You cannot remove a non-root volume from view editor directly");
            return;
        }
        String url = ViewEditUtils.getUrl();
        if (url == null || url.equals(""))
            return;
        url += "/rest/projects/volume/" + volid + "/delete";
        PostMethod pm = new PostMethod(url);
        try {
            HttpClient client = new HttpClient();
            ViewEditUtils.setCredentials(client, url);
            int code = client.executeMethod(pm);
            if (ViewEditUtils.showErrorMessage(code))
                return;
            String response = pm.getResponseBodyAsString();
            if (response.equals("ok"))
                gl.log("[INFO] Remove Successful.");
            else if (response.equals("NotFound"))
                gl.log("[ERROR] Volume not found.");
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
