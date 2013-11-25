package gov.nasa.jpl.mbee.actions;

import gov.nasa.jpl.mbee.generator.DocumentGenerator;
import gov.nasa.jpl.mbee.generator.DocumentValidator;
import gov.nasa.jpl.mbee.model.Document;
import gov.nasa.jpl.mbee.viewedit.ViewEditUtils;
import gov.nasa.jpl.mbee.viewedit.ViewHierarchyVisitor;
import gov.nasa.jpl.mbee.web.JsonRequestEntity;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

public class ExportViewHierarchyAction extends MDAction {
    private Element            doc;
    public static final String actionid = "ExportViewHierarchy";

    public ExportViewHierarchyAction(Element e) {
        super(actionid, "Export View Hierarchy", null, null);
        doc = e;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GUILog gl = Application.getInstance().getGUILog();
        PostMethod pm = null;
        DocumentValidator dv = null;
        try {
            dv = new DocumentValidator(doc);
            dv.validateDocument();
            if (dv.isFatal()) {
                dv.printErrors();
                return;
            }
            DocumentGenerator dg = new DocumentGenerator(doc, dv, null);
            Document dge = dg.parseDocument(true, true);
            dv.printErrors();
            ViewHierarchyVisitor vhv = new ViewHierarchyVisitor();
            dge.accept(vhv);
            String post = vhv.getResult().toJSONString();
            String url = ViewEditUtils.getUrl();
            if (url == null) {
                dv.printErrors();
                return;
            }
            gl.log("*** Starting export view hierarchy ***");
            String posturl = url + "/rest/views/" + doc.getID() + "/hierarchy";
            pm = new PostMethod(posturl);

            pm.setRequestHeader("Content-Type", "application/json");
            pm.setRequestEntity(JsonRequestEntity.create(post));
            HttpClient client = new HttpClient();
            ViewEditUtils.setCredentials(client, posturl);
            // gl.log(post);
            gl.log("[INFO] Sending...");
            int code = client.executeMethod(pm);
            if (ViewEditUtils.showErrorMessage(code))
                return;
            String response = pm.getResponseBodyAsString();
            if (response.equals("ok"))
                gl.log("[INFO] Export Successful.");
            else
                gl.log(response);
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            gl.log(sw.toString()); // stack trace as a string
            ex.printStackTrace();
        } finally {
            if (pm != null)
                pm.releaseConnection();
        }
        if (dv != null)
            dv.printErrors();
    }
}
