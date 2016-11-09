package gov.nasa.jpl.mbee.mdk.test.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.openapi.uml.ReadOnlyElementException;
import com.nomagic.magicdraw.tests.MagicDrawTestRunner;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import gov.nasa.jpl.mbee.mdk.api.ElementFinder;
import gov.nasa.jpl.mbee.mdk.api.MDKHelper;
import gov.nasa.jpl.mbee.mdk.api.MagicDrawHelper;
import gov.nasa.jpl.mbee.mdk.api.incubating.convert.Converters;
import gov.nasa.jpl.mbee.mdk.docgen.validation.ValidationRuleViolation;
import gov.nasa.jpl.mbee.mdk.ems.ServerException;
import org.junit.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;


/**
 * @author ablack
 * @JIRA MAGICDRAW-263
 *
 */
@RunWith(MagicDrawTestRunner.class)
public class CoordinatedSyncConflictMDDeleteMMSUpdate {

    private static Element targetElement;
    private static Element targetPackage;
    private static String filename = "/Users/ablack/git/mdk/resources/tests/CSyncTest.mdzip";
    
    public CoordinatedSyncConflictMDDeleteMMSUpdate() {
    }

    @BeforeClass
    public static void setupProject() throws IOException, ServerException, URISyntaxException {
        System.out.println("********2");
//        System.getProperties().stringPropertyNames().forEach(System.out::println);
        System.out.println("JAVA_HOME" + System.getProperty("java.home"));
        System.out.println("********2");
        Assert.fail("quit");

        MDKTestHelper.setMmsCredentials("/Users/ablack/git/mdk/resources/mms.properties", "");

        MagicDrawHelper.openProject(filename);
//        MDKTestHelper.waitXSeconds(30);

        if (!MDKHelper.isSiteEditable()) {
            throw new IOException("User does not have permissions to site");
        }
        ////

        //clean and prepare test environment
        MagicDrawHelper.createSession();
        try {
            MagicDrawHelper.clearModel();
        } catch (ReadOnlyElementException roee)  {
            System.out.println(roee.getMessage() + ": " + roee.getElement().getHumanName());
        }
        MagicDrawHelper.closeSession();

        //make sure expected stuff is in place
        MagicDrawHelper.createSession();
        targetPackage = MagicDrawHelper.createPackage("ConflictPkg", ElementFinder.getModelRoot());
        targetElement = MagicDrawHelper.createDocument("ConflictDoc", targetPackage);
        MagicDrawHelper.setElementDocumentation(targetElement, "Initial documentation.");
        MagicDrawHelper.closeSession();
        ///
        MagicDrawHelper.saveProject(filename);
        MDKHelper.loadCoordinatedSyncValidations();
        MDKHelper.getValidationWindow().listPooledViolations();
        MDKHelper.getValidationWindow().commitAllMDChangesToMMS();
    }

    @Test
    public void executeTest() throws IOException, ServerException, URISyntaxException {
        Assert.fail("died");
        String updatedMMSDocumentation = "Changed documentation.";
        String targetSysmlID = targetElement.getID();

        //update mms element without md session so it's not tracked in model, export its json to mms to update it and trigger pending
        MDKHelper.setSyncTransactionListenerDisabled(true);
        MagicDrawHelper.createSession();
        MagicDrawHelper.setElementDocumentation(targetElement, updatedMMSDocumentation);
        try {
            Collection<ObjectNode> postData = new ArrayList<>();
            ObjectNode jsob = Converters.getElementToJsonConverter().apply(targetElement, Application.getInstance().getProject());
            postData.add(jsob);
            MDKHelper.postMmsElementJson(postData, Application.getInstance().getProject());
        } catch (IllegalStateException e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
        MagicDrawHelper.cancelSession();
        MDKHelper.setSyncTransactionListenerDisabled(false);

        //confirm mms element update
        ObjectNode jo = MDKHelper.getMmsElement(targetElement, Application.getInstance().getProject());
        JsonNode value;
        if ((value = jo.get("documentation")) != null && value.isTextual()) {
            Assert.assertEquals(value.asText(),updatedMMSDocumentation);
        }
        else {
            throw new IOException("Unable to retrieve element documentation from MMS");
        }

        //delete local element
        MagicDrawHelper.createSession();
        try {
            MagicDrawHelper.deleteMDElement(targetElement);
        } catch (ReadOnlyElementException e) {
            MagicDrawHelper.cancelSession();
            throw new IOException("Unable to delete element id " + targetElement.getID());
        }
        MagicDrawHelper.closeSession();

        //confirm local delete
        Assert.assertNull(ElementFinder.getElement("Document", "UpdateDoc", targetPackage));

        // save model to push changes
        MagicDrawHelper.saveProject(filename);

        //confirm conflict found and recorded properly
        boolean foundViolation = false;
        MDKHelper.loadCoordinatedSyncValidations();
        for (ValidationRuleViolation vrv : MDKHelper.getValidationWindow().getPooledValidations("Element Equivalence")) {
            if (vrv.getComment().contains(targetSysmlID)) {
                foundViolation = true;
                break;
            }
        }
        if (!foundViolation) {
            Assert.fail("Conflict for target element not reported in violations.");
        }
        Collection<Element> syncElements = ElementFinder.getElement("Package", "__MMSSync__").getOwnedElement();
        for (Element se : syncElements) {
            if (!MagicDrawHelper.getElementDocumentation(se).contains(targetSysmlID)) {
                Assert.fail("Conflict not recorded in MMSSync element " + se.getHumanName());
            }
        }

    }


    @AfterClass
    public static void closeProject() {
        MagicDrawHelper.closeProject();
    }

    /*

    public void executeTest() {
        
        String updatedMMSDocumentation = "Changed documentation.";
        
        String targetSysmlID = targetElement.getID();

        //confirm mms permissions
        super.confirmMMSPermissions();
        
        //update mms element without md session so it's not tracked in model, export its json to mms to update it and trigger pending
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
        //confirm mms element update
        JSONObject jo = MDKHelper.getMmsElement(targetElement);
        assertTrue(jo.get("documentation").equals(updatedMMSDocumentation));
        
        //delete local element
        MagicDrawHelper.createSession();
        try {
            MagicDrawHelper.deleteMDElement(targetElement);
        } catch (ReadOnlyElementException e) {
            MagicDrawHelper.cancelSession();
            fail("Unable to delete element id " + targetElement.getID());
        }
        MagicDrawHelper.closeSession();
        //confirm local delete
        assertNull(ElementFinder.getElement("Document", "UpdateDoc", targetPackage));
        
        // save model to push changes
        super.saveUpdatedProject();
        
        //confirm conflict found and recorded properly
        boolean foundViolation = false;
        searchViolations: for (ValidationRuleViolation vrv : MDKHelper.getCoordinatedSyncValidationWindow().getPooledValidations("[EXIST ON MMS]")) {
            if (vrv.getComment().contains(targetSysmlID)) {
                foundViolation = true;
                break searchViolations;
            }
        }
        if (!foundViolation) {
            fail("Conflict for target element not reported in violations.");
        }
        Collection<Element> syncElements = ElementFinder.getElement("Package", "__MMSSync__").getOwnedElement();
        for (Element se : syncElements) {
            if (!MagicDrawHelper.getElementDocumentation(se).contains(targetSysmlID)) {
                fail("Conflict not recorded in MMSSync element " + se.getHumanName());
            }
        }
    }
    
    @Override
    protected void tearDownTest() throws Exception {
        super.tearDownTest();
        // do tear down here
        
        //clear pending messages
//        MDKHelper.getCoordinatedSyncValidationWindow().commitMDChangesToMMS("[EXIST ON MMS]");
        super.saveUpdatedProject();
        
        //close project
        super.closeProject();
    }

    */
}