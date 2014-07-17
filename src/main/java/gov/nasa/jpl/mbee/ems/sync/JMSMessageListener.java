package gov.nasa.jpl.mbee.ems.sync;

import java.util.Map;

import gov.nasa.jpl.mbee.ems.ExportUtility;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.openapi.uml.SessionManager;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;

public class JMSMessageListener implements MessageListener {

    private Project project;
    
    public JMSMessageListener(Project project) {
        this.project = project;
    }
    
    @Override
    public void onMessage(Message msg) {
        try {
            TextMessage message = (TextMessage)msg;
            JSONObject ob = (JSONObject)JSONValue.parse(message.getText());
            final JSONArray updated = (JSONArray)ob.get("updatedElements");

            Runnable runnable = new Runnable() {
                public void run() {
                    Map<String, ?> projectInstances = ProjectListenerMapping.getInstance().get(project);
                    AutoSyncCommitListener listener = (AutoSyncCommitListener)projectInstances.get("AutoSyncCommitListener");
                    if (listener != null)
                        listener.disable();
                    SessionManager sm = SessionManager.getInstance();
                    sm.createSession("mms sync change");
                    try {
                        for (Object element: updated) {
                            makeChange((JSONObject)element);
                        }
                        sm.closeSession();
                    } catch (Exception e) {
                       
                       sm.cancelSession();
                    } 
                    if (listener != null)
                        listener.enable();
                }
                
                private void makeChange(JSONObject ob) {
                    Element ele = ExportUtility.getElementFromID((String)(ob).get("sysmlid"));
                    if (ele == null) {
                        Application.getInstance().getGUILog().log("element not found from mms sync change");
                        return;
                    }
                    String newName = (String)(ob).get("name");
                    if (ele instanceof NamedElement && newName!= null && !((NamedElement)ele).getName().equals(newName)) 
                        ((NamedElement)ele).setName(newName);
                }
            };
            project.getRepository().invokeAfterTransaction(runnable);
            
        } catch (Exception e) {
            
        }
        
    }

}
