package gov.nasa.jpl.mbee.mdk.model;

import com.nomagic.diagramtable.columns.NumberColumn;
import com.nomagic.generictable.GenericTableManager;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.dependencymatrix.configuration.MatrixDataHelper;
import com.nomagic.magicdraw.dependencymatrix.datamodel.MatrixData;
import com.nomagic.magicdraw.dependencymatrix.datamodel.cell.AbstractMatrixCell;
import com.nomagic.magicdraw.properties.*;
import com.nomagic.magicdraw.properties.Property;
import com.nomagic.magicdraw.properties.ui.ObjectListProperty;
import com.nomagic.magicdraw.properties.ui.jideui.MultiplePropertyTable;
import com.nomagic.magicdraw.properties.ui.jideui.PropertyValue;
import com.nomagic.magicdraw.uml.DiagramType;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.reportwizard.tools.DiagramTableTool;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.*;
import gov.nasa.jpl.mbee.mdk.docgen.DocGenProfile;
import gov.nasa.jpl.mbee.mdk.docgen.docbook.*;
import gov.nasa.jpl.mbee.mdk.util.DependencyMatrixTool;
import gov.nasa.jpl.mbee.mdk.util.GeneratorUtils;
import gov.nasa.jpl.mbee.mdk.util.MatrixUtil;
import gov.nasa.jpl.mbee.mdk.util.Utils;
import groovy.model.DefaultTableModel;

import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GenericTable extends Table {

    public static final String INSTANCE_TABLE = "Instance Table";
    public static final String VERIFY_REQUIREMENTS_MATRIX = "Verify Requirement Matrix";
    public static final String ALLOCATION_MATRIX = "SysML Allocation Matrix";
    public static final String SATISFY_REQUIREMENTS_MATRIX = "Satisfy Requirement Matrix";
    public static final String REQUIREMENTS_TABLE = "Requirement Table";

    private List<String> headers;
    private boolean skipIfNoDoc;
    private static ArrayList<String> skipColumnIds = new ArrayList<String>() {{
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

        int tableCount = 0;
        List<Object> targets = isSortElementsByName() ? Utils.sortByName(getTargets()) : getTargets();
        for (Object e : targets) {
            if (e instanceof Diagram) {
                Diagram diagram = (Diagram) e;
                //DiagramPresentationElement diagramPE = Application.getInstance().getProject().getDiagram(diagram);
               // diagramPE.open();
                DiagramType diagramType = Application.getInstance().getProject().getDiagram(diagram).getDiagramType();
                if (diagramType.isTypeOf(DiagramType.GENERIC_TABLE) || diagramType.getType().equals(INSTANCE_TABLE) || diagramType.getType().equals(REQUIREMENTS_TABLE)) {
                    boolean isnew = true;
                    if (isnew) {


                        DBTable t = new DBTable();
                        DiagramTableTool tool = new DiagramTableTool();
                        try {
                            com.nomagic.diagramtable.Table table = tool.getTable(diagram);
                            MultiplePropertyTable propertyTable = table.getPropertyTable();
                            t.setHeaders(getHeaders(propertyTable));
                            List<List<DocumentElement>> innerRes = new ArrayList<>();

                            for (int rowIndex = 0; rowIndex < propertyTable.getRowCount(); rowIndex++) {
                                List<DocumentElement> row = new ArrayList<>();

                                for (int columnIndex = 0; columnIndex < propertyTable.getColumnCount(); columnIndex++) {
                                    DBTableEntry entry = new DBTableEntry();

                                    TableColumn column = propertyTable.getColumnModel().getColumn(columnIndex);

                                    if (column != null && column.getWidth() > 0) {
                                        Property cellValue;
                                        if (NumberColumn.isNumberColumn(propertyTable, columnIndex)) {
                                            //property = new StringProperty("", rowIndex + 1);
                                            entry.addElement(new DBParagraph(""+rowIndex + 1));

                                        } else {
                                            cellValue = ((PropertyValue) propertyTable.getValueAt(rowIndex, columnIndex)).getProperty();
                                            if (cellValue instanceof ElementProperty) {
                                                Element cellelement = ((ElementProperty) cellValue).getElement();
                                                if (cellelement instanceof NamedElement) {
                                                    entry.addElement(new DBParagraph(((NamedElement) cellelement).getName(), cellelement, From.NAME));
                                                }
                                            } else if (cellValue instanceof StringProperty) {

                                                    entry.addElement(new DBParagraph(cellValue.getValueStringRepresentation()));

                                            } else if (cellValue instanceof NumberProperty) {
                                                entry.addElement(new DBParagraph(cellValue.getValue()));
                                            } else if (cellValue instanceof ElementListProperty) {
                                                for (Element listEl : ((ElementListProperty) cellValue).getValue()) {
                                                    if (listEl instanceof LiteralString) {
                                                        entry.addElement(new DBParagraph(((LiteralString) listEl).getValue()));
                                                    } else if (listEl instanceof LiteralReal) {
                                                        entry.addElement(new DBParagraph(((LiteralReal) listEl).getValue()));
                                                    } else if (listEl instanceof LiteralBoolean) {
                                                        entry.addElement(new DBParagraph(((LiteralBoolean) listEl).isValue()));
                                                    } else if (listEl instanceof LiteralInteger) {
                                                        entry.addElement(new DBParagraph(((LiteralInteger) listEl).getValue()));
//                        }else if (listEl instanceof  Property) {
//                            Object val = ((Property) listEl).getValue(); // Get default value and append it to the name (propName = defaultVal)
                                                    } else if (listEl instanceof ValueSpecification) {
                                                        entry.addElement(new DBParagraph(((ValueSpecification) listEl).getType() + ""));
                                                    } else if (listEl instanceof NamedElement) {
                                                        entry.addElement(new DBParagraph(((NamedElement) listEl).getName(), listEl, From.NAME));
                                                    }
                                                }
                                            } else if (cellValue instanceof ElementInstanceProperty) {
                                                Object value = cellValue.getValue();

                                                if (value instanceof List) {
                                                    for (Object o : (List) value) {
                                                        if (o instanceof InstanceSpecification) {
                                                            entry.addElement(new DBParagraph(((InstanceSpecification) o).getName(), (Element) o, From.NAME));
                                                        }
                                                    }
                                                }
                                            } else if (cellValue instanceof AbstractChoiceProperty) {
                                                if (cellValue instanceof ChoiceProperty) {
                                                    int index = ((ChoiceProperty) cellValue).getIndex();
                                                    if (index > -1) {
                                                        Object choice = ((ChoiceProperty) cellValue).getChoice().get(index);
                                                        entry.addElement(new DBParagraph(choice.toString()));
                                                    }
                                                } else {
                                                    for (Object choice : ((AbstractChoiceProperty) cellValue).getChoice()) {
                                                        if (choice instanceof String) {
                                                            entry.addElement(new DBParagraph(choice.toString()));
                                                        }
                                                    }
                                                }
                                            } else if (cellValue instanceof ObjectListProperty) {
                                                entry.addElement(new DBParagraph(((ObjectListProperty) cellValue).getValueStringRepresentation()));
                                            } else {
                                                Application.getInstance().getGUILog().log("[WARNING] Cell value omitted: " + cellValue.toString() + ".");
                                            }


                                           // entry.addElement(new DBParagraph(property.getValueStringRepresentation()));
                                            System.out.println(rowIndex + " " + columnIndex + ": " + cellValue.getValueStringRepresentation());
                                        }

                                    }
                                    row.add(entry);
                                }
                                innerRes.add(row);
                            }
                            t.setBody(innerRes);
                            res.add(t);
                            t.setStyle(getStyle());
                            tableCount++;
                        } finally {
                            tool.clearTablesCache();
                        }


                    } else {


                        DBTable t = new DBTable();
                        List<String> columnIds = GenericTableManager.getVisibleColumnIds(diagram);
                        t.setHeaders(getHeaders(diagram, columnIds, false));
                        List<Element> rowElements = null;
//                    try {
//                        rowElements = GenericTableManager.getVisibleRowElements(diagram);
//                    }catch(NullPointerException np){
                        rowElements = GenericTableManager.getRowElements(diagram);
//                    }
                        t.setBody(getBody(diagram, rowElements, columnIds, forViewEditor));
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
                } else {
                    MatrixData matrixData;
                    if (MatrixDataHelper.isRebuildNeeded(diagram)) {
                        matrixData = MatrixDataHelper.buildMatrix(diagram);
                    } else {
                        matrixData = MatrixDataHelper.getMatrixData(diagram);
                    }

                    DependencyMatrixTool tool = new DependencyMatrixTool();
                    MatrixUtil matrix = tool.getMatrix(diagram);
                    List<Element> rowElements = matrix.getRows();
                    List<Element> columnElements = matrix.getColumns();
                    DBTable t = new DBTable();
                    List<List<DocumentElement>> matrixResult = new ArrayList<>();
                    List<String> columnHeaders = new ArrayList<>();
                    for (Element rowElement : rowElements) {
                        List<DocumentElement> matrixcolumn = new ArrayList<>();
                        if (rowElement instanceof NamedElement) {
                            matrixcolumn.add(new DBText(((NamedElement) rowElement).getName()));
                        } else {
                            matrixcolumn.add(new DBText(rowElement.getHumanName()));
                        }
                        for (Element columnElement : columnElements) {
                            AbstractMatrixCell val = matrixData.getValue(rowElement, columnElement);
                            if (val.getDescription() != null) {
                                if (val.isEditable()) {
                                    matrixcolumn.add(new DBText("&#10004;")); // HTML Check mark
                                } else {
                                    matrixcolumn.add(new DBText("&#10003;"));
                                }
                            } else {
                                matrixcolumn.add(new DBText(""));
                            }
                        }
                        matrixResult.add(matrixcolumn);
                    }
                    for (Element element : columnElements) {
                        if (element instanceof NamedElement) {
                            columnHeaders.add(((NamedElement) element).getName());
                        }
                    }
                    t.setHeaders(getHeaders(diagram, columnHeaders, true));
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
             //   diagramPE.close();

            }
        }

        return res;
    }


    public List<List<DocumentElement>> getHeaders(MultiplePropertyTable propertyTable) {
        List<List<DocumentElement>> res = new ArrayList<List<DocumentElement>>();


        List<DocumentElement> row = new ArrayList<DocumentElement>();
        int count = 0;
        for (int columnIndex = 0; columnIndex < propertyTable.getColumnCount(); columnIndex++) {
            TableColumn column = propertyTable.getColumnModel().getColumn(columnIndex);
            Object headerVal = column.getHeaderValue();
//            if (count == 0) {
//                count++;
//                continue;
//            }
           // if (!skipColumnIds.contains(columnId)) {
                    row.add(new DBText(headerVal.toString()));
                numCols++;
         //   }

        }
        res.add(row);

        return res;
    }


    public List<List<DocumentElement>> getHeaders(Diagram genericTable, List<String> columnIds, boolean isMatrix) {
        List<List<DocumentElement>> res = new ArrayList<List<DocumentElement>>();
        if (this.headers != null && !this.headers.isEmpty()) {
            List<DocumentElement> row = new ArrayList<DocumentElement>();
            for (String h : this.headers) {
                row.add(new DBText(h));
            }
            res.add(row);
        } else {
            List<DocumentElement> row = new ArrayList<DocumentElement>();
            int count = 0;
            for (String columnId : columnIds) {
                if (isMatrix) {
                    if (count == 0) {
                        row.add(new DBText(""));
                        count++;
                        numCols++;
                    }
                    row.add(new DBText(columnId));
                    numCols++;
                } else {
                    if (count == 0) {
                        count++;
                        continue;
                    }
                    if (!skipColumnIds.contains(columnId)) {
                        try {
                            row.add(new DBText(GenericTableManager.getColumnNameById(genericTable, columnId)));
                        } catch (NullPointerException npe) {
                            row.add(new DBText(columnId));
                        }
                        numCols++;
                    }
                }
            }
            res.add(row);
        }
        return res;
    }


    public List<List<DocumentElement>> getBody(Diagram d, Collection<Element> rowElements, List<String> columnIds, boolean forViewEditor) {
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
                if (skipColumnIds.contains(cid)) {
                    continue;
                }
                DBTableEntry entry = new DBTableEntry();

                Property cellValue = GenericTableManager.getCellValue(d, e, cid);
                if (cellValue instanceof ElementProperty) {
                    Element cellelement = ((ElementProperty) cellValue).getElement();
                    if (cellelement instanceof NamedElement) {
                        entry.addElement(new DBParagraph(((NamedElement) cellelement).getName(), cellelement, From.NAME));
                    }
                } else if (cellValue instanceof StringProperty) {
                    if (cid.contains("documentation")) {
                        entry.addElement(new DBParagraph(cellValue.getValue(), e, From.DOCUMENTATION));

                    } else {
                        entry.addElement(new DBParagraph(cellValue.getValue()));
                    }
                } else if (cellValue instanceof NumberProperty) {
                    entry.addElement(new DBParagraph(cellValue.getValue()));
                } else if (cellValue instanceof ElementListProperty) {
                    for (Element listEl : ((ElementListProperty) cellValue).getValue()) {
                        if (listEl instanceof LiteralString) {
                            entry.addElement(new DBParagraph(((LiteralString) listEl).getValue()));
                        } else if (listEl instanceof LiteralReal) {
                            entry.addElement(new DBParagraph(((LiteralReal) listEl).getValue()));
                        } else if (listEl instanceof LiteralBoolean) {
                            entry.addElement(new DBParagraph(((LiteralBoolean) listEl).isValue()));
                        } else if (listEl instanceof LiteralInteger) {
                            entry.addElement(new DBParagraph(((LiteralInteger) listEl).getValue()));
//                        }else if (listEl instanceof  Property) {
//                            Object val = ((Property) listEl).getValue(); // Get default value and append it to the name (propName = defaultVal)
                        } else if (listEl instanceof ValueSpecification) {
                            entry.addElement(new DBParagraph(((ValueSpecification) listEl).getType() + ""));
                        } else if (listEl instanceof NamedElement) {
                            entry.addElement(new DBParagraph(((NamedElement) listEl).getName(), listEl, From.NAME));
                        }
                    }
                } else if (cellValue instanceof ElementInstanceProperty) {
                    Object value = cellValue.getValue();

                    if (value instanceof List) {
                        for (Object o : (List) value) {
                            if (o instanceof InstanceSpecification) {
                                entry.addElement(new DBParagraph(((InstanceSpecification) o).getName(), (Element) o, From.NAME));
                            }
                        }
                    }
                } else if (cellValue instanceof AbstractChoiceProperty) {
                    if (cellValue instanceof ChoiceProperty) {
                        int index = ((ChoiceProperty) cellValue).getIndex();
                        if (index > -1) {
                            Object choice = ((ChoiceProperty) cellValue).getChoice().get(index);
                            entry.addElement(new DBParagraph(choice.toString()));
                        }
                    } else {
                        for (Object choice : ((AbstractChoiceProperty) cellValue).getChoice()) {
                            if (choice instanceof String) {
                                entry.addElement(new DBParagraph(choice.toString()));
                            }
                        }
                    }
                } else if (cellValue instanceof ObjectListProperty) {
                    entry.addElement(new DBParagraph(((ObjectListProperty) cellValue).getValueStringRepresentation()));
                } else {
                    Application.getInstance().getGUILog().log("[WARNING] Cell value omitted: " + cellValue.toString() + ".");
                }
                row.add(entry);
            }
            res.add(row);
        }
        return res;
    }

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
