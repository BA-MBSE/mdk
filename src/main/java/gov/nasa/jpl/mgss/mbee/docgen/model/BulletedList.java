package gov.nasa.jpl.mgss.mbee.docgen.model;

import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.DBHasContent;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.DBParagraph;

import java.util.List;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;

public class BulletedList extends Table {
	private boolean orderedList;
	private boolean showTargets;
	private boolean showStereotypePropertyNames;
	
	public BulletedList() {
		orderedList = false;
		showTargets = true;
		showStereotypePropertyNames = true;
		setSortElementsByName(false);
	}
	
	public void setOrderedList(boolean b) {
		orderedList = b;
	}
	
	public void setShowTargets(boolean b) {
		showTargets = b;
	}
	
	public void setShowStereotypePropertyNames(boolean b) {
		showStereotypePropertyNames = b;
	}
	
	public boolean isOrderedList() {
		return orderedList;
	}

	public boolean isShowTargets() {
		return showTargets;
	}

	public boolean isShowStereotypePropertyNames() {
		return showStereotypePropertyNames;
	}

	public void addStereotypeProperties(DBHasContent parent, Element e, Property p) {
		List<Object> results = Utils.getStereotypePropertyValues(e, p);
		for (Object o: results) {
			if (o instanceof NamedElement)
				parent.addElement(new DBParagraph(((NamedElement)o).getName()));
			else if (o instanceof String)
				parent.addElement(new DBParagraph((String)o));
			else if (o instanceof Comment)
				parent.addElement(new DBParagraph(((Comment)o).getBody()));
			else
				parent.addElement(new DBParagraph(o.toString()));
		}
	}

	@Override
	public void accept(IModelVisitor v) {
		v.visit(this);
	}
}