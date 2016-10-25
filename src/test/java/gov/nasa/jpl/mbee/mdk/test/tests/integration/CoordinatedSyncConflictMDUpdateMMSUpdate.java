package gov.nasa.jpl.mbee.mdk.test.tests.integration;

import gov.nasa.jpl.mbee.mdk.test.tests.MDKTestCase;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;


/**
 * @author ablack
 * @JIRA MAGICDRAW-263
 *
 */
public class CoordinatedSyncConflictMDUpdateMMSUpdate extends MDKTestCase
{
    
    static String testName = "CoordinatedSyncMDElementUpdate";
    static Element targetPackage;
    static Element targetElement;
    
    public static void main(String[] args) throws Exception 
    {   
        parseArgs(args);
        System.out.println("In main method");
        JUnitCore jCore = new JUnitCore();
        Result results = jCore.run(CoordinatedSyncConflictMDUpdateMMSUpdate.class);
        printJunitResults(results);
        System.exit(0);
    }

    public CoordinatedSyncConflictMDUpdateMMSUpdate (String testMethodToRun, String testName) 
    {
        super(testMethodToRun, testName);
        setDoNotUseSilentMode(true);
        System.setProperty("jsse.enableSNIExtension", "false");
        setSkipMemoryTest(true);
        setDoNotUseSilentMode(true);    
    }

    /*
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();        
        
        suite.addTest(new CoordinatedSyncConflictMDUpdateMMSUpdate("executeTest", "execute test"));
        
        return suite;
    }
    
    @Override
    protected void setUpTest() throws Exception {
        super.setUpTest();
        // do setup here
        
        // load credentials from file
        loadCredentials("");

        // set MMS credentials
        MDKHelper.setMMSLoginCredentials(teamworkUsername, teamworkPassword);
        
        //open project
        projectName = "CoordinatedSyncTest";
        testProject = testRoot + "/mdk/resource/" + projectName + ".mdzip";
        super.openProject();
        
        //clean and prepare test environment
        MagicDrawHelper.createSession();
        MagicDrawHelper.deleteEditableContainerChildren(ElementFinder.getModelRoot());
        MagicDrawHelper.closeSession();
        
        //make sure expected stuff is in place
        MagicDrawHelper.createSession();
        targetPackage = MagicDrawHelper.createPackage("ConflictPkg", ElementFinder.getModelRoot());
        targetElement = MagicDrawHelper.createDocument("ConflictDoc", targetPackage);
        MagicDrawHelper.setElementDocumentation(targetElement, "Initial documentation.");
        MagicDrawHelper.closeSession();
        super.saveUpdatedProject();
    }

    public void executeTest() {
        
        String updatedDocumentation = "Modified documentation";
        String updatedMMSDocumentation = "Changed documentation";

        //confirm mms permissions
        super.confirmMMSPermissions();
        
        //create / change / delete
        MagicDrawHelper.createSession();
        MagicDrawHelper.setElementDocumentation(targetElement, updatedDocumentation);
        MagicDrawHelper.closeSession();
        //confirm local change
        assertTrue(MagicDrawHelper.getElementDocumentation(targetElement).equals(updatedDocumentation));
        
        //update element without md session so it's not tracked in model, export its json to mms to update it and trigger pending local update
        MDKHelper.setSyncTransactionListenerDisabled(true);
        MagicDrawHelper.createSession();
        MagicDrawHelper.setElementDocumentation(targetElement, updatedMMSDocumentation);
        JSONObject jsob = ExportUtility.fillElement(targetElement, null);
        MagicDrawHelper.cancelSession();
        MDKHelper.setSyncTransactionListenerDisabled(false);
        try {
            MDKHelper.postMmsElement(jsob);
        } catch (IllegalStateException e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
        //confirm mms changes
        JSONObject jo = MDKHelper.getMmsElement(targetElement);
        assertTrue(jo.get("documentation").equals(updatedMMSDocumentation));
        
        // save model to push changes
        referenceProject =  testRoot + "/mdk/reference/" + projectName + ".mdzip";
        super.saveUpdatedProject();
        
        //confirm conflict found
        Collection<Element> violationTargets = new ArrayList<Element>();
        violationTargets.add(targetElement);
        if (!MDKHelper.getCoordinatedSyncValidationWindow().confirmElementValidationTypeResult("[Doc]", violationTargets).isEmpty()) {
            fail("Conflict for target element not reported in violations.");
        }
        Collection<Element> syncElements = ElementFinder.getElement("Package", "__MMSSync__").getOwnedElement();
        for (Element se : syncElements) {
            if (!MagicDrawHelper.getElementDocumentation(se).contains(targetElement.getID())) {
                fail("Conflict not recorded in MMSSync element " + se.getHumanName());
            }
        }
    }
    
    @Override
    protected void tearDownTest() throws Exception {
        super.tearDownTest();
        // do tear down here
        
        //reset
        MDKHelper.getCoordinatedSyncValidationWindow().commitMDChangesToMMS("[DOC]");
        super.saveUpdatedProject();
        
        //close project
        super.closeProject();
    }
    */
}
