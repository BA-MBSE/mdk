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
package gov.nasa.jpl.mbee.mdk.ems.actions;

import com.nomagic.magicdraw.actions.ActionsStateUpdater;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import gov.nasa.jpl.mbee.mdk.MMSSyncPlugin;
import gov.nasa.jpl.mbee.mdk.lib.TicketUtils;
import gov.nasa.jpl.mbee.mdk.lib.Utils;

import java.awt.event.ActionEvent;

public class EMSLoginAction extends MDAction {
    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_ID = "Login";

    private EMSLogoutAction logout;

    public EMSLoginAction() {
        super(DEFAULT_ID, "Login to MMS", null, null);
    }

    public void setLogoutAction(EMSLogoutAction logout) {
        this.logout = logout;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        loginAction(Application.getInstance().getProject(), "", "");
        ActionsStateUpdater.updateActionsState();
    }

    public boolean loginAction(Project project, String username, String password) {
        return loginAction(project, username, password, true);
    }

    public static boolean loginAction(Project project, String username, String password, boolean initJms) {
        if (project == null) {
            Utils.showPopupMessage("You need to have a project open first!");
            return false;
        }
        TicketUtils.loginToMMS();

        if (TicketUtils.getTicket().isEmpty()) {
            return false;
        }
        if (initJms) {
            for (Project p : Application.getInstance().getProjectsManager().getProjects()) {
                MMSSyncPlugin.getInstance().getJmsSyncProjectEventListenerAdapter().projectOpened(p);
            }
        }
        Application.getInstance().getGUILog().log("Login complete.");
        return true;
    }

}
