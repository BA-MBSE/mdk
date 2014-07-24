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

import gov.nasa.jpl.mbee.ems.ExportUtility;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.validation.IRuleViolationAction;
import gov.nasa.jpl.mgss.mbee.docgen.validation.RuleViolationAction;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.json.simple.JSONObject;

import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;

public class ImportName extends RuleViolationAction implements AnnotationAction, IRuleViolationAction {

    private static final long serialVersionUID = 1L;
    private NamedElement element;
    private String name;
    private JSONObject result;
    public ImportName(NamedElement e, String name, JSONObject result) {
    	//JJS--MDEV-567 fix: changed 'Import' to 'Accept'
    	//
        super("ImportName", "Accept name", null, null);
        this.element = e;
        this.name = name;
        this.result = result;
    }
    
    @Override
    public boolean canExecute(Collection<Annotation> arg0) {
        return true;
    }

    @Override
    public void execute(Collection<Annotation> annos) {
        SessionManager.getInstance().createSession("Change Names");
        Collection<Annotation> toremove = new HashSet<Annotation>();
        try {
            boolean noneditable = false;
            for (Annotation anno: annos) {
                Element e = (Element)anno.getTarget();
                if (!e.isEditable()) {
                    Application.getInstance().getGUILog().log("[ERROR] " + e.get_representationText() + " isn't editable");
                    noneditable = true;
                    continue;
                }
                String resultName = ExportUtility.unescapeHtml((String)((Map<String, JSONObject>)result.get("elementsKeyed")).get(e.getID()).get("name"));
                if (resultName == null)
                    continue;
                ((NamedElement)e).setName(resultName);
                //AnnotationManager.getInstance().remove(anno);
                toremove.add(anno);
            }
            SessionManager.getInstance().closeSession();
            if (noneditable) {
                Application.getInstance().getGUILog().log("[ERROR] There were some elements that're not editable");
            } else
                saySuccess();
            //AnnotationManager.getInstance().update();
            this.removeViolationsAndUpdateWindow(toremove);
        } catch (Exception ex) {
            SessionManager.getInstance().cancelSession();
        }
        
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!element.isEditable()) {
            Application.getInstance().getGUILog().log("[ERROR] " + element.getQualifiedName() + " is not editable!");
            return;
        }
        SessionManager.getInstance().createSession("Change Name");
        try {
            element.setName(name);
            SessionManager.getInstance().closeSession();
            saySuccess();
            //AnnotationManager.getInstance().remove(annotation);
            //AnnotationManager.getInstance().update();
            this.removeViolationAndUpdateWindow();
        } catch (Exception ex) {
            SessionManager.getInstance().cancelSession();
            Utils.printException(ex);
        }
    }
}
