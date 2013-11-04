package gov.nasa.jpl.mgss.mbee.docgen.actions;

import gov.nasa.jpl.mbee.docweb.JsonRequestEntity;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.DocWebProfile;
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

public class DeleteProjectAction extends MDAction {
	private Element proj;
	public static final String actionid = "DeleteProject";
	
	public DeleteProjectAction(Element e) {
		super(actionid, "Remove From View Editor", null, null);
		proj = e;
	}
	
	public void actionPerformed(ActionEvent e) {
		GUILog gl = Application.getInstance().getGUILog();
		
		String url = ViewEditUtils.getUrl();
		if (url == null || url.equals(""))
			return;
		url += "/rest/projects/" + proj.getID() + "/delete";
		PostMethod pm = new PostMethod(url);
		try {
			//pm.setRequestHeader("Content-Type", "text/json");
			//pm.setRequestEntity(JsonRequestEntity.create(vol.getID()));
			//Protocol easyhttps = new Protocol("https", new EasySSLProtocolSocketFactory(), 443);
			//Protocol.registerProtocol("https", easyhttps);
			HttpClient client = new HttpClient();
			ViewEditUtils.setCredentials(client, url);
			int code = client.executeMethod(pm);
			if (code == 401) {
			    ViewEditUtils.showUnauthroziedMessage();
                return;
            }
			String response = pm.getResponseBodyAsString();
			if (response.equals("ok"))
				gl.log("[INFO] Remove Successful.");
			else if (response.equals("NotFound"))
				gl.log("[ERROR] Project not found.");
			else
				gl.log(response);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} finally{
			pm.releaseConnection();
		}
	}

}
