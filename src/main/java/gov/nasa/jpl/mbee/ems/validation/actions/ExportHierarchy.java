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
import gov.nasa.jpl.mbee.model.Document;
import gov.nasa.jpl.mbee.viewedit.ViewEditUtils;
import gov.nasa.jpl.mbee.viewedit.ViewHierarchyVisitor;
import gov.nasa.jpl.mgss.mbee.docgen.validation.IRuleViolationAction;
import gov.nasa.jpl.mgss.mbee.docgen.validation.RuleViolationAction;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;

public class ExportHierarchy extends RuleViolationAction implements AnnotationAction, IRuleViolationAction {
    private static final long serialVersionUID = 1L;
    private Element view;
    private GUILog gl = Application.getInstance().getGUILog();
    
    public ExportHierarchy(Element e) {
        super("ExportHierarchy", "Export Hierarchy", null, null);
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
            if (exportHierarchy(e)) {
                toremove.add(anno);
            }
        }
        if (!toremove.isEmpty()) {
            this.removeViolationsAndUpdateWindow(toremove);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (exportHierarchy(view)) {
            this.removeViolationAndUpdateWindow();
        }
    }
    
    @SuppressWarnings("unchecked")
    private boolean exportHierarchy(Element view) {
        DocumentGenerator dg = new DocumentGenerator(view, null, null);
        Document dge = dg.parseDocument(true, true);
        ViewHierarchyVisitor vhv = new ViewHierarchyVisitor();
        dge.accept(vhv);
        String url = ExportUtility.getUrl();
        boolean document = false;
        Stereotype documentView = StereotypesHelper.getStereotype(Application.getInstance().getProject(),
                DocGen3Profile.documentViewStereotype, "Document Profile");
        if (StereotypesHelper.hasStereotypeOrDerived(view, documentView))
            document = true;
        
        JSONObject view2view = vhv.getView2View();
        if (document) {
            String docurl = url + "/javawebscripts/products";
            
            JSONObject send = new JSONObject();
            JSONArray documents = new JSONArray();
            JSONObject doc = new JSONObject();
            doc.put("view2view", ExportUtility.formatView2View(view2view));
            doc.put("noSections", vhv.getNosections());
            doc.put("id", view.getID());
            documents.add(doc);
            send.put("products", documents);
            if (!ExportUtility.send(docurl, send.toJSONString()))
                return false;
        } else {
            JSONArray views = new JSONArray();
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
        return true;
        
    }
    
}
