package gov.nasa.jpl.mbee.actions.ems;

import javax.swing.KeyStroke;

import gov.nasa.jpl.mbee.ems.sync.jms.JMSSyncProjectEventListenerAdapter;
import gov.nasa.jpl.mbee.viewedit.ViewEditUtils;

import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;

public class MMSAction extends MDAction {

    public MMSAction(String arg0, String arg1, KeyStroke arg2, String arg3) {
        super(arg0, arg1, arg2, arg3);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void updateState() {    
        setEnabled(ViewEditUtils.isPasswordSet() && !JMSSyncProjectEventListenerAdapter.getProjectMapping(Application.getInstance().getProject()).getJmsMessageListener().isExceptionHandlerRunning());
    }


}
