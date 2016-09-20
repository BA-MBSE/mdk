package gov.nasa.jpl.mbee.ems.emf;

import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.BaseElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLFactory;
import com.nomagic.uml2.ext.magicdraw.metadata.UMLPackage;
import gov.nasa.jpl.mbee.api.function.TriFunction;
import gov.nasa.jpl.mbee.api.incubating.json.ToJsonFunction;
import gov.nasa.jpl.mbee.ems.ImportException;
import gov.nasa.jpl.mbee.ems.ReferenceException;
import gov.nasa.jpl.mbee.lib.Changelog;
import gov.nasa.jpl.mbee.lib.ClassUtils;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.UniqueEList;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

/**
 * Created by igomes on 9/19/16.
 */
public class EMFImporter2 implements ToJsonFunction {
    @Override
    public Changelog.Change<Element> apply(JSONObject jsonObject, Project project, Boolean strict) throws ImportException {
        // TODO Handle multi-thread synchronization @donbot
        UMLFactory.eINSTANCE.setRepository(project.getRepository());
        project.getCounter().setCanResetIDForObject(true);

        Object o = jsonObject.get("sysmlId");
        if (!(o instanceof String)) {
            return null;
        }
        Element element = ELEMENT_LOOKUP_FUNCTION.apply((String) o, project);
        Changelog.ChangeType changeType = Changelog.ChangeType.UPDATED;
        if (element == null) {
            o = jsonObject.get("type");
            if (!(o instanceof String)) {
                return null;
            }
            EClassifier eClassifier = UMLPackage.eINSTANCE.getEClassifier((String) o);
            if (!(eClassifier instanceof EClass)) {
                return null;
            }
            EObject eObject = UMLFactory.eINSTANCE.create((EClass) eClassifier);
            if (!(eObject instanceof Element)) {
                return null;
            }
            element = (Element) eObject;
            changeType = Changelog.ChangeType.CREATED;
        }

        for (EStructuralFeature eStructuralFeature : element.eClass().getEAllStructuralFeatures()) {
            final Element finalElement = element;
            ImportFunction function = Arrays.stream(EStructuralFeatureOverride.values()).filter(override -> override.getPredicate()
                    .test(jsonObject, eStructuralFeature, project, strict, finalElement)).map(EStructuralFeatureOverride::getFunction)
                    .findAny().orElse(DEFAULT_E_STRUCTURAL_FEATURE_FUNCTION);
            element = function.apply(jsonObject, eStructuralFeature, project, strict, element);
        }
        return new Changelog.Change<>(element, changeType);
    }

    private static final Function<EStructuralFeature, String> KEY_FUNCTION = eStructuralFeature -> {
        String key = eStructuralFeature.getName();
        if (eStructuralFeature instanceof EReference && EObject.class.isAssignableFrom(((EReference) eStructuralFeature).getEReferenceType().getInstanceClass())) {
            key += "Id" + (eStructuralFeature.isMany() ? "s" : "");
        }
        return key;
    };

    private static final ElementLookupFunction ELEMENT_LOOKUP_FUNCTION = (id, project) -> {
        if (id.equals(project.getPrimaryProject().getProjectID())) {
            return project.getModel();
        }
        BaseElement baseElement = project.getElementByID(id);
        return baseElement instanceof Element ? (Element) baseElement : null;
    };

    private static final DeserializationFunction DEFAULT_DESERIALIZATION_FUNCTION = (key, value, ignoreMultiplicity, jsonObject, eStructuralFeature, project, strict, element) -> {
        if (value == null) {
            return null;
        }
        else if (!ignoreMultiplicity && eStructuralFeature.isMany()) {
            if (!(value instanceof Collection)) {
                if (strict) {
                    throw new ImportException(element, jsonObject, "Expected a Collection for key \"" + key + "\" in JSON, but instead got a " + value.getClass().getSimpleName() + ".");
                }
                return null;
            }
            Collection<Object> collection = eStructuralFeature.isUnique() ? new UniqueEList<>() : new BasicEList<>();
            for (Object o : ((Collection<?>) value)) {
                Object deserialized = EMFImporter2.DEFAULT_DESERIALIZATION_FUNCTION.apply(key, o, true, jsonObject, eStructuralFeature, project, strict, element);
                if (deserialized == null && o != null) {
                    if (strict) {
                        throw new ImportException(element, jsonObject, "Failed to deserialize " + eStructuralFeature + " for " + element + ": " + value + " - " + value.getClass());
                    }
                    continue;
                }
                if (deserialized != null && !eStructuralFeature.getEType().getInstanceClass().isAssignableFrom(deserialized.getClass())) {
                    if (strict) {
                        throw new ImportException(element, jsonObject, "Expected a " + eStructuralFeature.getEType().getInstanceClass().getSimpleName() + " upon deserializing \"" + key + "\", but instead got a " + deserialized.getClass());
                    }
                    continue;
                }
                collection.add(deserialized);
            }
            return collection;
        }
        else if (eStructuralFeature instanceof EReference) {
            EReference eReference = (EReference) eStructuralFeature;
            if (!(value instanceof String)) {
                if (strict) {
                    throw new ReferenceException(element, jsonObject, "Expected a String for key \"" + key + "\" in JSON, but instead got a " + value.getClass().getSimpleName() + ".");
                }
                return null;
            }
            String id = (String) value;
            Element referencedElement = ELEMENT_LOOKUP_FUNCTION.apply(id, project);
            if (referencedElement == null) {
                if (strict) {
                    throw new ReferenceException(element, jsonObject, "Could not find referenced element " + id + "in model for key \"" + key + "\" in JSON.");
                }
                return null;
            }
            if (!eReference.getEReferenceType().getInstanceClass().isAssignableFrom(referencedElement.getClass())) {
                if (strict) {
                    throw new ReferenceException(element, jsonObject, "Expected a " + eReference.getEReferenceType().getInstanceClass().getSimpleName() + " for key \"" + key + "\" in JSON, but instead got a " + referencedElement.getClass().getSimpleName() + ".");
                }
                return null;
            }
            return referencedElement;
        }
        else if (eStructuralFeature.getEType() instanceof EDataType && value instanceof String) {
            return EcoreUtil.createFromString((EDataType) eStructuralFeature.getEType(), (String) value);
        }
        else if (value instanceof String || ClassUtils.isPrimitive(value)) {
            return value;
        }
        // if we get here we have no idea what to do with this object
        return null;
    };

    private static final ImportFunction DEFAULT_E_STRUCTURAL_FEATURE_FUNCTION = (jsonObject, eStructuralFeature, project, strict, element) -> {
        if (!eStructuralFeature.isChangeable() || eStructuralFeature.isVolatile() || eStructuralFeature.isTransient() || eStructuralFeature.isUnsettable() || eStructuralFeature.isDerived() || eStructuralFeature.getName().startsWith("_")) {
            return EMFImporter2.EMPTY_E_STRUCTURAL_FEATURE_FUNCTION.apply(jsonObject, eStructuralFeature, project, strict, element);
        }
        String key = KEY_FUNCTION.apply(eStructuralFeature);
        if (!jsonObject.containsKey(key)) {
            /*if (strict) {
                throw new ImportException(element, jsonObject, "Required key \"" + key + "\" missing from JSON.");
            }*/
            return element;
        }

        Object value = jsonObject.get(key);
        Object deserialized = DEFAULT_DESERIALIZATION_FUNCTION.apply(key, value, false, jsonObject, eStructuralFeature, project, strict, element);

        if (deserialized == null && value != null) {
            if (strict) {
                throw new ImportException(element, jsonObject, "Failed to deserialize " + eStructuralFeature + " for " + element + ": " + value + " - " + value.getClass());
            }
            return element;
        }
        if (eStructuralFeature.isMany() && !(deserialized instanceof Collection)) {
            if (strict) {
                throw new ImportException(element, jsonObject, "Expected a Collection for key \"" + key + "\" in JSON, but instead got a " + value.getClass().getSimpleName() + ".");
            }
            return element;
        }
        try {
            EMFImporter2.UNCHECKED_SET_E_STRUCTURAL_FEATURE_FUNCTION.apply(deserialized, eStructuralFeature, element);
        } catch (ClassCastException | IllegalArgumentException e) {
            if (strict) {
                throw new ImportException(element, jsonObject, "An unexpected exception occurred while setting " + eStructuralFeature + " of type " + eStructuralFeature.getEType() + " for " + element + " to " + value, e);
            }
            return element;
        }
        return element;
    };

    @SuppressWarnings("unchecked")
    private static final TriFunction<Object, EStructuralFeature, Element, Element> UNCHECKED_SET_E_STRUCTURAL_FEATURE_FUNCTION = (object, eStructuralFeature, element) -> {
        Object currentValue = element.eGet(eStructuralFeature);
        if (object == null && currentValue == null) {
            return element;
        }
        if (eStructuralFeature.isMany() && object != null && currentValue != null) {
            Collection<Object> currentCollection = (Collection<Object>) currentValue;
            Collection<Object> newCollection = (Collection<Object>) object;
            currentCollection.clear();
            currentCollection.addAll(newCollection);
            return element;
        }
        element.eSet(eStructuralFeature, object);
        return element;
    };

    private static final ImportFunction EMPTY_E_STRUCTURAL_FEATURE_FUNCTION = (jsonObject, eStructuralFeature, project, strict, element) -> element;

    @FunctionalInterface
    interface ElementLookupFunction {
        Element apply(String id, Project project);
    }

    @FunctionalInterface
    interface DeserializationFunction {
        Object apply(String key, Object value, boolean ignoreMultiplicity, JSONObject jsonObject, EStructuralFeature eStructuralFeature, Project project, boolean strict, Element element) throws ImportException;
    }

    @FunctionalInterface
    interface ImportFunction {
        Element apply(JSONObject jsonObject, EStructuralFeature eStructuralFeature, Project project, boolean strict, Element element) throws ImportException;
    }

    @FunctionalInterface
    interface ImportPredicate {
        boolean test(JSONObject jsonObject, EStructuralFeature eStructuralFeature, Project project, boolean strict, Element element);
    }

    private enum EStructuralFeatureOverride {
        ID(
                (jsonObject, eStructuralFeature, project, strict, element) -> eStructuralFeature == element.eClass().getEIDAttribute(),
                (jsonObject, eStructuralFeature, project, strict, element) -> {
                    Object o = jsonObject.get("sysmlId");
                    if (!(o instanceof String)) {
                        if (strict) {
                            throw new ImportException(element, jsonObject, "Element JSON has missing/malformed ID.");
                        }
                        return null;
                    }
                    UNCHECKED_SET_E_STRUCTURAL_FEATURE_FUNCTION.apply(o, element.eClass().getEIDAttribute(), element);
                    return element;
                }
        ),
        OWNER(
                (jsonObject, eStructuralFeature, project, strict, element) -> UMLPackage.Literals.ELEMENT__OWNER == eStructuralFeature,
                (jsonObject, eStructuralFeature, project, strict, element) -> {
                    Object o = jsonObject.get("ownerId");
                    if (!(o instanceof String)) {
                        if (strict) {
                            throw new ImportException(element, jsonObject, "Element JSON has missing/malformed ID.");
                        }
                        return null;
                    }
                    Element owningElement = ELEMENT_LOOKUP_FUNCTION.apply((String) o, project);
                    if (owningElement == null) {
                        if (strict) {
                            throw new ImportException(element, jsonObject, "Owner for element " + jsonObject.get("sysmlid") + " not found: " + o + ".");
                        }
                        return null;
                    }
                    element.setOwner(owningElement);
                    return element;
                }
        );

        private ImportPredicate importPredicate;
        private ImportFunction importFunction;

        EStructuralFeatureOverride(ImportPredicate importPredicate, ImportFunction importFunction) {
            this.importPredicate = importPredicate;
            this.importFunction = importFunction;
        }

        public ImportPredicate getPredicate() {
            return importPredicate;
        }

        public ImportFunction getFunction() {
            return importFunction;
        }
    }
}
