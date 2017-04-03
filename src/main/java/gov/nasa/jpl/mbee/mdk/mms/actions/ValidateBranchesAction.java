package gov.nasa.jpl.mbee.mdk.mms.actions;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.task.ProgressStatus;
import com.nomagic.task.RunnableWithProgress;
import com.nomagic.ui.ProgressStatusRunner;
import gov.nasa.jpl.mbee.mdk.mms.validation.BranchValidator;
import gov.nasa.jpl.mbee.mdk.options.MDKOptionsGroup;

import java.awt.event.ActionEvent;

public class ValidateBranchesAction extends MMSAction {
    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_ID = "ValidateBranches";

    public ValidateBranchesAction() {
        super(DEFAULT_ID, "Branches", null, null);
    }

    public class ValidationRunner implements RunnableWithProgress {

        @Override
        public void run(ProgressStatus arg0) {
            BranchValidator v = new BranchValidator(Application.getInstance().getProject());
            v.validate(arg0, false);
            v.showWindow();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ProgressStatusRunner.runWithProgressStatus(new ValidationRunner(), "Validating Branches", true, 0);
    }

    @Override
    public void updateState() {
        setEnabled(MDKOptionsGroup.getMDKOptions().isMDKAdvancedOptions());
    }


}