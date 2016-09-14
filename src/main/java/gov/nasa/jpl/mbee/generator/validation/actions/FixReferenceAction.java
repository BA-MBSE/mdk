package gov.nasa.jpl.mbee.generator.validation.actions;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.*;
import gov.nasa.jpl.mbee.generator.PresentationElementUtils;
import gov.nasa.jpl.mbee.lib.Utils;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FixReferenceAction extends MDAction implements AnnotationAction {

    private static final long serialVersionUID = 1L;
    private Element viewOrSection;
    private Element view;
    private boolean all;
    private Map<Element, List<InstanceSpecification>> opaque;
    private Map<Element, List<InstanceSpecification>> manual;

    public FixReferenceAction(boolean all, Element viewOrSection, Element view, Map<Element, List<InstanceSpecification>> opaque, Map<Element, List<InstanceSpecification>> manual) {
        super(all ? "FixAllReference" : "FixReference", all ? "Remove All Duplicated Reference(s)" : "Remove DocGen Generated Duplicated Reference(s)", null, null);
        //JJS--MDEV-567 fix: changed 'Import' to 'Accept'
        this.viewOrSection = viewOrSection;
        this.view = view;
        this.opaque = opaque;
        this.manual = manual;
        this.all = all;
    }

    @Override
    public boolean canExecute(Collection<Annotation> arg0) {
        return true;
    }

    @Override
    public void execute(Collection<Annotation> arg0) {
        SessionManager.getInstance().createSession("fix duplicate references");
        for (Annotation a : arg0) {
            Element e = (Element) a.getTarget();
            List<InstanceSpecification> toRemove = opaque.get(e);
            List<InstanceSpecification> maybeRemove = manual.get(e);
            Expression ex = PresentationElementUtils.getViewOrSectionExpression(e);
            if (ex == null) {
                continue;
            }
            Constraint c = Utils.getViewConstraint(e);
            if (!ex.isEditable()) {
                Utils.guilog("[ERROR] " + (c == null ? c.getQualifiedName() : ((NamedElement) e).getQualifiedName()) + " is not editable, skipping.");
                continue;
            }
            List<ValueSpecification> newOperand = new ArrayList<ValueSpecification>();
            for (ValueSpecification vs : ex.getOperand()) {
                if ((vs instanceof InstanceValue && toRemove.contains(((InstanceValue) vs).getInstance())) ||
                        (all && maybeRemove.contains(((InstanceValue) vs).getInstance()))) {
                    continue;
                }
                newOperand.add(vs);
            }

            ex.getOperand().clear();
            ex.getOperand().addAll(newOperand);
        }
        SessionManager.getInstance().closeSession();
        Utils.guilog("[INFO] Successful.");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<InstanceSpecification> toRemove = opaque.get(viewOrSection);
        List<InstanceSpecification> maybeRemove = manual.get(viewOrSection);
        Expression ex = PresentationElementUtils
                .getViewOrSectionExpression(viewOrSection);
        if (ex == null) {
            return;
        }
        Constraint c = Utils.getViewConstraint(viewOrSection);
        if (!ex.isEditable()) {
            Utils.guilog("[ERROR] " + (c == null ? c.getQualifiedName() : ((NamedElement) viewOrSection).getQualifiedName()) + " is not editable.");
            return;
        }
        List<ValueSpecification> newOperand = new ArrayList<ValueSpecification>();
        for (ValueSpecification vs : ex.getOperand()) {
            if ((vs instanceof InstanceValue && toRemove.contains(((InstanceValue) vs).getInstance())) ||
                    (all && maybeRemove.contains(((InstanceValue) vs).getInstance()))) {
                continue;
            }
            newOperand.add(vs);
        }
        SessionManager.getInstance().createSession("fix duplicate references");
        ex.getOperand().clear();
        ex.getOperand().addAll(newOperand);
        SessionManager.getInstance().closeSession();
        Utils.guilog("[INFO] Successful.");
    }
}

