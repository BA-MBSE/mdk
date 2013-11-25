package gov.nasa.jpl.mbee.actions;

import java.awt.event.ActionEvent;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

/**
 * import changed elements in views
 * 
 * @author dlam
 * 
 */
@SuppressWarnings("serial")
public class ImportViewRecursiveAction extends MDAction {

    private Element            doc;
    public static final String actionid = "ImportViewRecursive";

    public ImportViewRecursiveAction(Element e) {
        super(actionid, "Import Model (Overwrite)", null, null);
        doc = e;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ImportViewAction.doImportView(doc, true, true, null);
    }

}
