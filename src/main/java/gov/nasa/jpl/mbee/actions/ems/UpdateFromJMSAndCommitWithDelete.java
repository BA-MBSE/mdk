package gov.nasa.jpl.mbee.actions.ems;

import gov.nasa.jpl.mbee.ems.sync.ManualSyncRunner;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.validation.ValidationSuite;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import com.nomagic.ui.ProgressStatusRunner;

public class UpdateFromJMSAndCommitWithDelete extends MMSAction {
    private static final long serialVersionUID = 1L;
    public static final String actionid = "UpdateFromJMSAndCommitWithDelete";
    
    private List<ValidationSuite> vss = new ArrayList<ValidationSuite>();
    
    public UpdateFromJMSAndCommitWithDelete() {
        super(actionid, "Commit With Deletes to MMS", null, null);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (!Utils.recommendUpdateFromTeamwork())
            return;
        updateAction();
    }
    
    public List<ValidationSuite> updateAction() {
    	ManualSyncRunner msr = new ManualSyncRunner(true, true);
        ProgressStatusRunner.runWithProgressStatus(msr, "Delta Sync", true, 0);
        vss.addAll(msr.getValidations());
        return vss;
    }
    
    public List<ValidationSuite> getValidations() {
    	return vss;
    }
    
}