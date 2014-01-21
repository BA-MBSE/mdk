/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
 * U.S. Government sponsorship acknowledged.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of 
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list 
 *    of conditions and the following disclaimer in the documentation and/or other materials 
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
 *    nor the names of its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package gov.nasa.jpl.mbee.ems.validation.actions;

import gov.nasa.jpl.mbee.DocGen3Profile;
import gov.nasa.jpl.mbee.ems.ExportUtility;
import gov.nasa.jpl.mbee.generator.DocumentGenerator;
import gov.nasa.jpl.mbee.generator.PostProcessor;
import gov.nasa.jpl.mbee.model.DocBookOutputVisitor;
import gov.nasa.jpl.mbee.model.Document;
import gov.nasa.jpl.mbee.viewedit.DBAlfrescoVisitor;
import gov.nasa.jpl.mbee.viewedit.ViewEditUtils;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.DBBook;
import gov.nasa.jpl.mgss.mbee.docgen.validation.IRuleViolationAction;
import gov.nasa.jpl.mgss.mbee.docgen.validation.RuleViolationAction;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

public class ExportView extends RuleViolationAction implements AnnotationAction, IRuleViolationAction {
    private static final long serialVersionUID = 1L;
    private Element view;
    private boolean recurse;
    private GUILog gl = Application.getInstance().getGUILog();
    
    public ExportView(Element e, boolean recursive) {
        super(recursive ? "ExportViewRecursive" : "ExportView", recursive ? "Export view recursive" : "Export view", null, null);
        this.recurse = recursive;
        this.view = e;
    }
    
    @Override
    public boolean canExecute(Collection<Annotation> arg0) {
        return true;
    }

    @Override
    public void execute(Collection<Annotation> annos) {
        Collection<Annotation> toremove = new ArrayList<Annotation>();
        for (Annotation anno: annos) {
            Element e = (Element)anno.getTarget();
            if (exportView(e)) {
                toremove.add(anno);
            }
        }
        if (!toremove.isEmpty()) {
            this.removeViolationsAndUpdateWindow(toremove);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (exportView(view)) {
            this.removeViolationAndUpdateWindow();
        }
    }
    
    @SuppressWarnings("unchecked")
    private boolean exportView(Element view) {
        DocumentGenerator dg = new DocumentGenerator(view, null, null);
        Document dge = dg.parseDocument(true, recurse);
        (new PostProcessor()).process(dge);
        boolean document = false;
        
        Stereotype documentView = StereotypesHelper.getStereotype(Application.getInstance().getProject(),
                DocGen3Profile.documentViewStereotype, "Document Profile");
        if (StereotypesHelper.hasStereotypeOrDerived(view, documentView))
            document = true;

        DocBookOutputVisitor visitor = new DocBookOutputVisitor(true);
        dge.accept(visitor);
        DBBook book = visitor.getBook();
        if (book == null)
            return false;

        DBAlfrescoVisitor visitor2 = new DBAlfrescoVisitor(recurse);
        book.accept(visitor2);
        /*int numElements = visitor2.getNumberOfElements();
        if (numElements > 10000) {
            Boolean cont = Utils.getUserYesNoAnswer("Alert! You're about to publish " + numElements
                    + " elements in a view, this may take about " + numElements / 1000
                    + " minutes to complete if you're doing initial loading, do you want to continue?");
            if (cont == null || !cont) {
                return false;
            }
        }*/
        JSONObject elementsjson = visitor2.getElements();
        JSONArray elementsArray = new JSONArray();
        elementsArray.addAll(elementsjson.values());
        JSONObject send = new JSONObject();
        send.put("elements", elementsArray);
        //gl.log(send.toJSONString());
        String url = ExportUtility.getUrl();
        if (url == null)
            //url = "";
            return false;
        String sendElementsUrl = url + ExportUtility.getPostElementsUrl("europa");
        if (!ExportUtility.send(sendElementsUrl, send.toJSONString()))
            return false;
        
        //send elements first, then view info
        JSONObject viewjson = visitor2.getViews();
        JSONArray viewsArray = new JSONArray();
        viewsArray.addAll(viewjson.values());
        send = new JSONObject();
        send.put("views", viewsArray);
        //gl.log(send.toJSONString());
        String sendViewsUrl = url +  "/javawebscripts/views";
        if (!ExportUtility.send(sendViewsUrl, send.toJSONString()))
            return false;
        
        // Upload images to view editor (JSON keys are specified in
        // DBEditDocwebVisitor
        gl.log("[INFO] Updating Images...");
        Map<String, JSONObject> images = visitor2.getImages();
        boolean isAlfresco = true;
        for (String key: images.keySet()) {
            String filename = (String)images.get(key).get("abspath");
            String cs = (String)images.get(key).get("cs");
            String extension = (String)images.get(key).get("extension");

            File imageFile = new File(filename);
            
            String baseurl = url + "/artifacts/magicdraw/" + key + "?cs=" + cs + "&extension=" + extension;
           
            // check whether the image already exists
            GetMethod get = new GetMethod(baseurl);
            int status = 0;
            try {
                HttpClient client = new HttpClient();
                ViewEditUtils.setCredentials(client, baseurl);
                gl.log("[INFO] Checking if imagefile exists... " + key + "_cs" + cs + extension);
                client.executeMethod(get);

                status = get.getStatusCode();
            } catch (Exception ex) {
                //printStackTrace(ex, gl);
            } finally {
                get.releaseConnection();
            }

            if (status == HttpURLConnection.HTTP_OK) {
                gl.log("[INFO] Image file already exists, not uploading");
            } else {
                PostMethod post = new PostMethod(baseurl);
                try {
                    if (isAlfresco) {
                        Part[] parts = {new FilePart("content", imageFile)};
                        post.setRequestEntity(new MultipartRequestEntity(parts, post.getParams()));
                    } else {
                        post.setRequestEntity(new InputStreamRequestEntity(new FileInputStream(imageFile),
                                imageFile.length()));
                    }
                    HttpClient client = new HttpClient();
                    ViewEditUtils.setCredentials(client, baseurl);
                    gl.log("[INFO] Did not find image, uploading file... " + key + "_cs" + cs + extension);
                    client.executeMethod(post);

                    status = post.getStatusCode();
                    if (status != HttpURLConnection.HTTP_OK) {
                        gl.log("[ERROR] Could not upload image file to view editor");
                    }
                } catch (Exception ex) {
                    //printStackTrace(ex, gl);
                } finally {
                    post.releaseConnection();
                }
            }
        }

        // clean up the local images
        visitor2.removeImages();
        
        if (document && recurse) {
            String docurl = url + "/javawebscripts/products";
            send = new JSONObject();
            JSONArray documents = new JSONArray();
            JSONObject doc = new JSONObject();
            doc.put("view2view", ExportUtility.formatView2View(visitor2.getHierarchy()));
            doc.put("noSections", visitor2.getNosections());
            doc.put("id", view.getID());
            documents.add(doc);
            send.put("products", documents);
            if (!ExportUtility.send(docurl, send.toJSONString()))
                return false;
        } /*else if (recurse) {
            JSONArray views = new JSONArray();
            JSONObject view2view = visitor2.getHierarchy();
            for (Object viewid: view2view.keySet()) {
                JSONObject viewinfo = new JSONObject();
                viewinfo.put("id", viewid);
                viewinfo.put("childrenViews", view2view.get(viewid));
                views.add(viewinfo);
            }
            JSONObject send  = new JSONObject();
            send.put("views", views);
            if (!ExportUtility.send(url + "/javawebscripts/views", send.toJSONString()))
                return false;
        }
        */
        return true;
    }
    
}
