package gov.nasa.jpl.mbee.mdk.generator;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import gov.nasa.jpl.mbee.mdk.api.docgen.presentation_elements.PresentationElementEnum;
import org.json.simple.JSONObject;

import java.util.List;

public class PresentationElementInstance {
    private InstanceSpecification instance; //existing instance, null if no existing instance to use
    private JSONObject newspec; //the to be json
    private PresentationElementEnum type;
    private Element view; //view that generated this pe, can be null if not generated
    private String name;
    private PresentationElementInstance parent; //section if applicable, otherwise null, use view
    private List<PresentationElementInstance> children; //if section
    private Element loopElement; //if section is generated from model element from docgen
    private boolean manual = false; //if manual is true, just use existing instance, not generated from docgen
    private boolean viewDocHack = false; //if opaque para view doc generated, change it so it's not opaque and point to itself with a transclusion to view instead

    public PresentationElementInstance(InstanceSpecification instance, JSONObject spec, PresentationElementEnum type, Element view, String name, PresentationElementInstance parent, List<PresentationElementInstance> children) {
        this.instance = instance;
        this.newspec = spec;
        this.type = type;
        this.view = view;
        this.name = name;
        this.parent = parent;
        this.children = children;
    }

    public InstanceSpecification getInstance() {
        return instance;
    }

    public void setInstance(InstanceSpecification instance) {
        this.instance = instance;
    }

    public JSONObject getNewspec() {
        return newspec;
    }

    public void setNewspec(JSONObject newspec) {
        this.newspec = newspec;
    }

    public PresentationElementEnum getType() {
        return type;
    }

    public void setType(PresentationElementEnum type) {
        this.type = type;
    }

    public Element getView() {
        return view;
    }

    public void setView(Element view) {
        this.view = view;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PresentationElementInstance getParent() {
        return parent;
    }

    public void setParent(PresentationElementInstance parent) {
        this.parent = parent;
    }

    public List<PresentationElementInstance> getChildren() {
        return children;
    }

    public void setChildren(List<PresentationElementInstance> children) {
        this.children = children;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public Element getLoopElement() {
        return loopElement;
    }

    public void setLoopElement(Element loopElement) {
        this.loopElement = loopElement;
    }

    public boolean isViewDocHack() {
        return viewDocHack;
    }

    public void setViewDocHack(boolean viewDocHack) {
        this.viewDocHack = viewDocHack;
    }
}
