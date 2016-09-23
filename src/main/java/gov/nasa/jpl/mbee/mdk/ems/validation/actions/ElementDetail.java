package gov.nasa.jpl.mbee.mdk.ems.validation.actions;

import com.nomagic.magicdraw.annotation.Annotation;
import com.nomagic.magicdraw.annotation.AnnotationAction;
import com.nomagic.magicdraw.ui.dialogs.MDDialogParentProvider;
import gov.nasa.jpl.mbee.mdk.docgen.validation.IRuleViolationAction;
import gov.nasa.jpl.mbee.mdk.docgen.validation.RuleViolationAction;
import org.json.simple.JSONObject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Collection;

public class ElementDetail extends RuleViolationAction implements AnnotationAction, IRuleViolationAction {

    private static final long serialVersionUID = 1L;
    private JSONObject result;

    public ElementDetail(JSONObject result) {
        super("Element Detail", "ElementDetail", null, null);
        this.result = result;
    }

    @Override
    public boolean canExecute(Collection<Annotation> arg0) {
        return false;
    }

    @Override
    public void execute(Collection<Annotation> annos) {

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String detail = "";
        
        JSONObject elementJson = new JSONObject();
        for (Object o : elementJson.keySet()) {
            String key = (String) o;
            if (elementJson.get(key) != null) {
                detail += key + ": " + elementJson.get(key).toString() + "\n\n";
            }
            else {
                detail += key + ": null\n\n";
            }
        }
        
        /*
         * old logic from before @donbot
         * 
        JSONObject spec = null;
        for (Object o : result.keySet()) {
            String key = (String) o;
            if (key.equals("specialization")) {
         *       spec = (JSONObject) result.get("specialization");
                continue;
            }
            if (result.get(key) != null) {
                detail += key + ": " + result.get(key).toString() + "\n\n";
            }
            else {
                detail += key + ": null\n\n";
            }
        }
        if (spec != null) {
            for (Object o : spec.keySet()) {
                String key = (String) o;
                if (spec.get(key) != null) {
                    detail += key + ": " + spec.get(key).toString() + "\n\n";
                }
                else {
                    detail += key + ": null\n\n";
                }
            }
        }
        */
        
        JTextArea web = new JTextArea(detail);
        web.setEditable(false);
        web.setLineWrap(true);
        JScrollPane webp = new JScrollPane(web);
        webp.setName("Web");
        JDialog show = new JDialog(MDDialogParentProvider.getProvider().getDialogParent());
        show.setTitle("Element Detail");
        show.setSize(600, 600);
        show.getContentPane().add(webp);
        show.setVisible(true);
    }

}
