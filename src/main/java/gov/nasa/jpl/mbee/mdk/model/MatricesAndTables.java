package gov.nasa.jpl.mbee.mdk.model;

import com.nomagic.generictable.GenericTableManager;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.dependencymatrix.configuration.MatrixDataHelper;
import com.nomagic.magicdraw.dependencymatrix.datamodel.MatrixData;
import com.nomagic.magicdraw.dependencymatrix.datamodel.cell.AbstractMatrixCell;
import com.nomagic.magicdraw.properties.*;
import com.nomagic.magicdraw.uml.DiagramType;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.InstanceSpecification;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import gov.nasa.jpl.mbee.mdk.docgen.DocGenProfile;
import gov.nasa.jpl.mbee.mdk.docgen.docbook.*;
import gov.nasa.jpl.mbee.mdk.util.DependencyMatrixTool;
import gov.nasa.jpl.mbee.mdk.util.GeneratorUtils;
import gov.nasa.jpl.mbee.mdk.util.Matrix;
import gov.nasa.jpl.mbee.mdk.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MatricesAndTables extends Table {

    public static final String INSTANCE_TABLE  = "Instance Table";
    public static final String VERIFY_REQUIREMENTS_MATRIX = "Verify Requirement Matrix";
    public static final String ALLOCATION_MATRIX = "SysML Allocation Matrix";
    public static final String SATISFY_REQUIREMENTS_MATRIX = "Satisfy Requirement Matrix";

    private List<String> headers;
    private boolean skipIfNoDoc;
    private static ArrayList<String> skipColumnIDs = new ArrayList<String>() {{
        add("QPROP:Element:isEncapsulated");
        add("QPROP:Element:CUSTOM_IMAGE");
    }};
    private int numCols = 0;


    @Override
    public List<DocumentElement> visit(boolean forViewEditor, String outputDir) {
        List<DocumentElement> res = new ArrayList<DocumentElement>();
        if (getIgnore()) {
            return res;
        }

        // SessionManager.getInstance().createSession(Application.getInstance().getProject(), "Reading Generic Table");
        int tableCount = 0;
        List<Object> targets = isSortElementsByName() ? Utils.sortByName(getTargets()) : getTargets();
        for (Object e : targets) {
            if (e instanceof Diagram) {
                Diagram diagram = (Diagram) e;
                DiagramType diagramType = Application.getInstance().getProject().getDiagram(diagram).getDiagramType();

                if (diagramType.isTypeOf(DiagramType.GENERIC_TABLE) ||diagramType.getType().equals(INSTANCE_TABLE)) {
                    DBTable t = new DBTable();
                    GenericTableManager gtm = new GenericTableManager();
                    List<String> columnIds = gtm.getColumnIds(diagram);
                    t.setHeaders(getHeaders(diagram, columnIds, gtm));
                    List<Element> rowElements = gtm.getRowElements(diagram);
                    t.setBody(getBody(diagram, rowElements, columnIds, gtm, forViewEditor));
                    if (getTitles() != null && getTitles().size() > tableCount) {
                        t.setTitle(getTitlePrefix() + getTitles().get(tableCount) + getTitleSuffix());
                    } else {
                        t.setTitle(getTitlePrefix() + (diagram).getName() + getTitleSuffix());
                    }
                    if (getCaptions() != null && getCaptions().size() > tableCount && isShowCaptions()) {
                        t.setCaption(getCaptions().get(tableCount));
                    } else {
                        t.setCaption(ModelHelper.getComment(diagram));
                    }
                    t.setCols(numCols);
                    res.add(t);
                    t.setStyle(getStyle());
                    tableCount++;
                }else {
                    MatrixData matrixData;
                    if (MatrixDataHelper.isRebuildNeeded(diagram)){
                        matrixData = MatrixDataHelper.buildMatrix(diagram);
                    } else {
                        matrixData = MatrixDataHelper.getMatrixData(diagram);
                    }

                    DependencyMatrixTool tool = new DependencyMatrixTool();
                    Matrix matrix = tool.getMatrix(diagram);
                    List<Element> rowElements = matrix.getRows();
                    List<Element> columnElements = matrix.getColumns();
                    DBTable t = new DBTable();
                    List<List<DocumentElement>> matrixResult = new ArrayList<>();
                    List<String> columnHeaders = new ArrayList<>();
                    for (Element rowElement : rowElements) {
                        List<DocumentElement> matrixcolumn = new ArrayList<>();
                        if(rowElement instanceof NamedElement) {
                            matrixcolumn.add(new DBText(((NamedElement) rowElement).getName()));
                        }else{
                            matrixcolumn.add(new DBText(rowElement.getHumanName()));
                        }
                        for (Element columnElement : columnElements) {
                             AbstractMatrixCell val = matrixData.getValue(rowElement, columnElement);
                            if(val.getDescription() != null){
                                if(val.isEditable()) {
                                    matrixcolumn.add(new DBText("&#10004;")); // HTML Check mark
                                }else {
                                    matrixcolumn.add(new DBText("&#10003;"));
                                }
                            }else {
                                matrixcolumn.add(new DBText(""));
                            }
                        }
                        matrixResult.add(matrixcolumn);
                    }
                    for (Element element : columnElements) {
                        if(element instanceof NamedElement){
                            columnHeaders.add(((NamedElement) element).getName());
                        }
                    }
                    t.setHeaders(getHeaders(diagram, columnHeaders, null));
                    t.setBody(matrixResult);
                    if (getTitles() != null && getTitles().size() > tableCount) {
                        t.setTitle(getTitlePrefix() + getTitles().get(tableCount) + getTitleSuffix());
                    } else {
                        t.setTitle(getTitlePrefix() + (diagram).getName() + getTitleSuffix());
                    }
                    if (getCaptions() != null && getCaptions().size() > tableCount && isShowCaptions()) {
                        t.setCaption(getCaptions().get(tableCount));
                    } else {
                        t.setCaption(ModelHelper.getComment(diagram));
                    }
                    t.setCols(numCols);
                    res.add(t);
                    t.setStyle(getStyle());
                    tableCount++;
                }
            }
        }

         return res;
    }

    public List<List<DocumentElement>> getHeaders(Diagram d, List<String> columnIds, GenericTableManager gtm) {
        List<List<DocumentElement>> res = new ArrayList<List<DocumentElement>>();
        if (this.headers != null && !this.headers.isEmpty()) {
            List<DocumentElement> row = new ArrayList<DocumentElement>();
            for (String h : this.headers) {
                row.add(new DBText(h));
            }
            res.add(row);
        } else if (StereotypesHelper.hasStereotypeOrDerived(d, DocGenProfile.headersChoosable)) {
            List<DocumentElement> row = new ArrayList<DocumentElement>();
            for (String h : (List<String>) StereotypesHelper.getStereotypePropertyValue(d, DocGenProfile.headersChoosable, "headers")) {
                row.add(new DBText(h));
            }
            res.add(row);
        } else {
            List<DocumentElement> row = new ArrayList<DocumentElement>();
            int count = 0;
            for (String columnid : columnIds) {
                if(gtm == null) {
                    if(count == 0){
                        row.add(new DBText(""));
                        count++;
                        numCols++;
                    }
                    row.add(new DBText(columnid));
                    numCols++;
                }else {
                    if (count == 0) {
                        count++;
                        continue;
                    }
                    if (!skipColumnIDs.contains(columnid)) {
                        row.add(new DBText(gtm.getColumnNameById(d, columnid)));
                        numCols++;
                    }
                }
            }
            res.add(row);
        }
        return res;
    }


    public List<List<DocumentElement>> getBody(Diagram d, Collection<Element> rowElements, List<String> columnIds,
                                               GenericTableManager gtm, boolean forViewEditor) {
        List<List<DocumentElement>> res = new ArrayList<>();
        for (Element e : rowElements) {
            if (skipIfNoDoc && ModelHelper.getComment(e).trim().isEmpty()) {
                continue;
            }
            List<DocumentElement> row = new ArrayList<>();
            int count = 0;
            for (String cid : columnIds) {
                if (count == 0) {
                    count++;
                    continue;
                }
                if (skipColumnIDs.contains(cid)) {
                    continue;
                }
                 DBTableEntry entry = new DBTableEntry();


                Property cellValue = gtm.getCellValue(d, e, cid);
                if (cellValue instanceof ElementProperty) {
                    Element cellelement = ((ElementProperty) cellValue).getElement();
                    if (cellelement instanceof NamedElement) {
                        entry.addElement(new DBParagraph(((NamedElement) cellelement).getName(), cellelement, From.NAME));
                    }
                } else if (cellValue instanceof StringProperty) {
                    if(cid.contains("documentation")){
                        entry.addElement(new DBParagraph(cellValue.getValue(), e, From.DOCUMENTATION));

                    }else {
                        entry.addElement(new DBParagraph(cellValue.getValue()));
                    }
                } else if (cellValue instanceof ElementListProperty) {
                    for (Element listEl : ((ElementListProperty) cellValue).getValue()) {
                        if (listEl instanceof NamedElement) {
                            entry.addElement(new DBParagraph(((NamedElement) listEl).getName(), listEl, From.NAME));
                        }
                    }
                } else if(cellValue instanceof ElementInstanceProperty) {
                    //Object mval = cellValue.mValue;
                    Object value = cellValue.getValue();

                    if(value instanceof List){
                        for (Object o : (List) value) {
                            if(o instanceof InstanceSpecification){
                                 entry.addElement(new DBParagraph(((InstanceSpecification) o).getName(), (Element) o, From.NAME));
                            }
                        }
                    }
                }else {
                    System.out.print("[WARNING] Not added : " + cellValue.toString() + ".");
                }
                row.add(entry);
            }
            res.add(row);
        }
        return res;
    }

    @SuppressWarnings("rawtypes")
    public List<Object> getTableValues(Object o) {
        List<Object> res = new ArrayList<>();
        if (o instanceof Object[]) {
            Object[] a = (Object[]) o;
            for (int i = 0; i < a.length; i++) {
                res.addAll(getTableValues(a[i]));
            }
        } else if (o instanceof Collection) {
            for (Object oo : (Collection) o) {
                res.addAll(getTableValues(oo));
            }
        } else if (o != null) {
            res.add(o);
        }
        return res;
    }

    public void setSkipIfNoDoc(boolean b) {
        skipIfNoDoc = b;
    }

    public void setHeaders(List<String> h) {
        headers = h;
    }



    @SuppressWarnings("unchecked")
    @Override
    public void initialize() {
        super.initialize();
        setHeaders((List<String>) GeneratorUtils.getListProperty(dgElement, DocGenProfile.headersChoosable,
                "headers", new ArrayList<String>()));
        setSkipIfNoDoc((Boolean) GeneratorUtils.getObjectProperty(dgElement, DocGenProfile.docSkippable,
                "skipIfNoDoc", false));
    }

}
