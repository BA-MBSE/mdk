package gov.nasa.jpl.mbee.mdk.ems.emf;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLFactory;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;

public class EMFTest {

    public static void main(String[] args) {

        InstanceSpecification is = UMLFactory.eINSTANCE.createInstanceSpecification();
        for (EAttribute ea : is.eClass().getEAllAttributes()) {
            EDataType type = (ea).getEAttributeType();
            if (type instanceof EEnum) {
                System.out.println(type + " is an EEnum");
            }
            else {
                System.out.println(type + " is not an EEnum ");
            }
        }
    }
}
