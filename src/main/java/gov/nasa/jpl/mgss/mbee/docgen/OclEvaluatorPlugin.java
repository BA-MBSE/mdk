/**
 * 
 */
package gov.nasa.jpl.mgss.mbee.docgen;

import gov.nasa.jpl.mbee.lib.Debug;
import gov.nasa.jpl.mgss.mbee.docgen.actions.OclQueryAction;

import java.awt.event.ActionEvent;
import java.lang.reflect.Method;

import com.nomagic.actions.NMAction;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

public class OclEvaluatorPlugin extends MDPlugin {

    // OclQueryAction action = null;
    /**
   * 
   */
    public OclEvaluatorPlugin() {
        this(OclQueryAction.class);
    }

    public OclEvaluatorPlugin(Class<? extends NMAction> cls) {
        super(cls);
    }

    // unused -- TODO -- remove after testing
    public static void doIt(ActionEvent event, Element element) {
        OclQueryAction action = new OclQueryAction(element);
        action.actionPerformed(event);
    }

    /*
     * (non-Javadoc)
     * 
     * @see gov.nasa.jpl.mgss.mbee.docgen.MDPlugin#initConfigurations()
     */
    @Override
    public void initConfigurations() {
        // //Debug.turnOn();
        // if ( !MDUtils.isDeveloperMode() ) {
        // Debug.outln(
        // "OclEvaluatorPlugin will be hidden since MD is not in developer mode."
        // );
        // return;
        // }
        Debug.outln("initializing OclEvaluatorPlugin!");

        // Method method = ClassUtils.getMethodsForName(
        // OclEvaluatorPlugin.class, "doIt")[ 0 ];
        // TODO -- shouldn't have to look this method up and pass it--just get
        // rid of
        // method argument in addConfiguration calls below.
        Method method = getNmActionMethod();

        String category = "MDK";

        addConfiguration("MainMenu", "", OclQueryAction.actionText, category, method, this);
        addConfiguration("ContainmentBrowserContext", "", OclQueryAction.actionText, category, method, this);
        addConfiguration("BaseDiagramContext", "Class Diagram", OclQueryAction.actionText, category, method,
                this);
        addConfiguration("BaseDiagramContext", "Activity Diagram", OclQueryAction.actionText, category,
                method, this);
        addConfiguration("BaseDiagramContext", "SysML Block Definition Diagram", OclQueryAction.actionText,
                category, method, this);
        addConfiguration("BaseDiagramContext", "SysML Internal Block Diagram", OclQueryAction.actionText,
                category, method, this);
        addConfiguration("BaseDiagramContext", "DocGen 3 View Diagram", OclQueryAction.actionText, category,
                method, this);
        addConfiguration("BaseDiagramContext", "DocGen 3 Diagram", OclQueryAction.actionText, category,
                method, this);
        addConfiguration("BaseDiagramContext", "View Diagram", OclQueryAction.actionText, category, method,
                this);
        addConfiguration("BaseDiagramContext", "DocumentView", OclQueryAction.actionText, category, method,
                this);

        Debug.outln("finished initializing TestPlugin!");
    }

}
