package gov.nasa.jpl.mbee.mdk.docgen.docbook;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import gov.nasa.jpl.mbee.mdk.api.incubating.convert.Converters;

import java.util.Set;

import static gov.nasa.jpl.mbee.mdk.model.TomSawyerDiagram.DiagramType;

public class DBTomSawyerDiagram extends DocumentElement {
    private Element context;

    public Set<String> getElementIds() {
        return elements;
    }

    public void setElements(Set<String> elements) {
        this.elements = elements;
    }

    private Set<String> elements;
    private String caption;

    public String getContext() {
        return Converters.getElementToIdConverter().apply(context);
    }

    public void setContext(Element context) {
        this.context = context;
    }

    private DiagramType type;

    @Override
    public void accept(IDBVisitor v) {
        v.visit(this);
    }

    public void setCaption(String cap) {
        caption = cap;
    }

    public String getCaption() {
        return caption;
    }

    public void setType(DiagramType type) {
        this.type = type;
    }

    public DiagramType getType() {
        return type;
    }
}