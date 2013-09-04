package gov.nasa.jpl.mgss.mbee.docgen.generator;

import gov.nasa.jpl.graphs.DirectedEdgeVector;
import gov.nasa.jpl.graphs.DirectedGraphHashSet;
import gov.nasa.jpl.graphs.algorithms.TopologicalSort;
import gov.nasa.jpl.mbee.lib.Debug;
import gov.nasa.jpl.mbee.lib.GeneratorUtils;
import gov.nasa.jpl.mbee.lib.ScriptRunner;
import gov.nasa.jpl.mbee.lib.Utils;
import gov.nasa.jpl.mgss.mbee.docgen.DocGen3Profile;
import gov.nasa.jpl.mgss.mbee.docgen.DocGenUtils;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.DBBook;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.DBSerializeVisitor;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.From;
import gov.nasa.jpl.mgss.mbee.docgen.model.BillOfMaterialsTable;
import gov.nasa.jpl.mgss.mbee.docgen.model.BulletedList;
import gov.nasa.jpl.mgss.mbee.docgen.model.CombinedMatrix;
import gov.nasa.jpl.mgss.mbee.docgen.model.Container;
import gov.nasa.jpl.mgss.mbee.docgen.model.CustomTable;
import gov.nasa.jpl.mgss.mbee.docgen.model.DependencyMatrix;
import gov.nasa.jpl.mgss.mbee.docgen.model.DeploymentTable;
import gov.nasa.jpl.mgss.mbee.docgen.model.DocBookOutputVisitor;
import gov.nasa.jpl.mgss.mbee.docgen.model.DocGenElement;
import gov.nasa.jpl.mgss.mbee.docgen.model.Document;
import gov.nasa.jpl.mgss.mbee.docgen.model.GenericTable;
import gov.nasa.jpl.mgss.mbee.docgen.model.HierarchicalPropertiesTable;
import gov.nasa.jpl.mgss.mbee.docgen.model.Image;
import gov.nasa.jpl.mgss.mbee.docgen.model.LibraryMapping;
import gov.nasa.jpl.mgss.mbee.docgen.model.MissionMapping;
import gov.nasa.jpl.mgss.mbee.docgen.model.Paragraph;
import gov.nasa.jpl.mgss.mbee.docgen.model.PropertiesTableByAttributes;
import gov.nasa.jpl.mgss.mbee.docgen.model.Query;
import gov.nasa.jpl.mgss.mbee.docgen.model.Section;
import gov.nasa.jpl.mgss.mbee.docgen.model.TableStructure;
import gov.nasa.jpl.mgss.mbee.docgen.model.UserScript;
import gov.nasa.jpl.mgss.mbee.docgen.model.WorkpackageAssemblyTable;
import gov.nasa.jpl.mgss.mbee.docgen.model.WorkpackageTable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;

import javax.script.ScriptException;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.CallBehaviorAction;
import com.nomagic.uml2.ext.magicdraw.actions.mdbasicactions.CallOperationAction;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.ActivityEdge;
import com.nomagic.uml2.ext.magicdraw.activities.mdbasicactivities.InitialNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.Activity;
import com.nomagic.uml2.ext.magicdraw.activities.mdfundamentalactivities.ActivityNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.DecisionNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.ForkNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.JoinNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdintermediateactivities.MergeNode;
import com.nomagic.uml2.ext.magicdraw.activities.mdstructuredactivities.StructuredActivityNode;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Association;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.AggregationKind;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.AggregationKindEnum;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Class;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Diagram;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.ElementImport;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.EnumerationLiteral;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.NamedElement;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.PackageImport;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Property;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.TypedElement;
import com.nomagic.uml2.ext.magicdraw.commonbehaviors.mdbasicbehaviors.Behavior;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Type;


/**
 * <p>Given the document head or a view, generates the document element model classes and structure in gov.nasa.jpl.mgss.mbee.docgen.model.</p>
 * <p>Should explore use of java reflection to set stereotype property values, but right now the profile names and java object/field names MAY
 * not always match exactly, need some thorough scrubbing to be able to use reflection.</p>
 * <p>call behavior actions are parsed by their stereotype, and then their typed behavior stereotype if any - 
 * because the tags on the action can override tags on the behavior, there's a lot of seemingly duplicate code where it's just checking for tag values.</p>
 * @author dlam
 *
 */
public class DocumentGenerator {

//	private Stack<List<Element>> targets;
//	private ActivityNode current;
//	private GUILog log;
	private GenerationContext context; // Easier for modular implementation. Contains previous three variables.
	private Element start;
	private Document doc;
	private Stereotype sysmlview;
	
	/**
	 * this is just some static method added as an experiment in triggering docgen from simulation toolkit
	 * Louise was trying this sometime ago, not sure if it's used by anyone
	 * @param e
	 * @param file
	 * @return
	 */
	public static boolean generateDocument(Element e, String file) {
		DocumentValidator dv = new DocumentValidator(e);
		dv.validateDocument();
		dv.printErrors();
		if (dv.isFatal())
			return false;
		DocumentGenerator dg = new DocumentGenerator(e, null);
		Document dge = dg.parseDocument();
		boolean genNewImage = dge.getGenNewImage();
		(new PostProcessor()).process(dge);
		File savefile = null;
		if (file == null) {
			String homedir = System.getProperty("user.home") + File.separator + "DocGenOutput";
			File dir = new File(homedir);
			dir.mkdirs();
			savefile = new File(homedir + File.separator + "out.xml");
		} else
			savefile = new File(file);
		File dir = savefile.getParentFile();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(savefile));
			//List<DocumentElement> books = dge.getDocumentElement();
			DocBookOutputVisitor visitor = new DocBookOutputVisitor(false);
			dge.accept(visitor);
			DBBook book = visitor.getBook();
			if (book != null) {
				DBSerializeVisitor v = new DBSerializeVisitor(genNewImage, dir, null);
				book.accept(v);
				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
				writer.write(v.getOut());
			}
			writer.flush();
			writer.close();
					
		} catch (IOException ex) {
			ex.printStackTrace();	
			return false;
					
		}
		return true;
	}
		
	
	public DocumentGenerator(Element e, PrintWriter wlog) {
		start = e;
		sysmlview = StereotypesHelper.getStereotype(Project.getProject(e), DocGen3Profile.viewStereotype, DocGen3Profile.sysmlProfile);
		StereotypesHelper.getStereotype(Project.getProject(e), DocGen3Profile.viewpointStereotype, DocGen3Profile.sysmlProfile);
		doc = new Document();
//		targets = new Stack<List<Element>>();
//		log = Application.getInstance().getGUILog();
		context = new GenerationContext(new Stack<List<Element>>(), null, Application.getInstance().getGUILog());
	}

	public Document parseDocument() {
		return this.parseDocument(false, true);
	}
	
	/**
	 * singleView: whether to only parse the passed in view
	 * recurse: only if singleView is true, whether to process all children views
	 * these options are to accommodate normal docgen to docbook xml and view editor export options
	 */
	public Document parseDocument(boolean singleView, boolean recurse) {
		if (StereotypesHelper.hasStereotypeOrDerived(start, sysmlview)) {
			if (StereotypesHelper.hasStereotypeOrDerived(start, DocGen3Profile.documentViewStereotype)) {
				doc.setDgElement(start); //only set the DgElement if this is actually a document view, this affects processing down the line for various things (like docweb visitors)
				Element first = GeneratorUtils.findStereotypedRelationship(start, DocGen3Profile.firstStereotype);
				if (first != null)
					parseView(first, doc, true, singleView, recurse, false);				
			} else {//starting from regular view, not document
				parseView(start, doc, true, singleView, recurse, true);	
			}
		} else if (StereotypesHelper.hasStereotypeOrDerived(start, DocGen3Profile.documentStereotype) && start instanceof Activity)
			parseActivityOrStructuredNode(start, doc);
		else {
		
		}
		docMetadata();
		for (DocGenElement e: doc.getChildren()) {
			if (e instanceof Section)
				((Section)e).isChapter(true);
		}
		return doc;
	}
	
	private void docMetadata() {
		GeneratorUtils.docMetadata(doc, start);
	}
	
	/**
	 * 
	 * @param view current view
	 * @param parent parent view
	 * @param section should current view be a section
	 * @param singleView parse only one view
	 * @param recurse if singleView is true, but want all children view from top view
	 * @param top is current view the top view
	 */
	private void parseView(Element view, Container parent, boolean section, boolean singleView, boolean recurse, boolean top) {
		Element viewpoint = GeneratorUtils.findStereotypedRelationship(view, DocGen3Profile.conformStereotype);
		
		Section viewSection = new Section(); //Section is a misnomer, should be View
		viewSection.setTitle(((NamedElement)view).getName());
		viewSection.setDgElement(view);
		viewSection.setView(true);
		parent.addElement(viewSection);
		if (!section && parent instanceof Section) //parent can be Document, in which case this view must be a section
			viewSection.setNoSection(true);
		viewSection.setId(view.getID());
		if (StereotypesHelper.hasStereotype(view, DocGen3Profile.appendixViewStereotype))// REVIEW -- hasStereotypeOrDerived()?
			viewSection.isAppendix(true);
		
		if (viewpoint != null && viewpoint instanceof Class) { //view conforms to a viewpoint
			if (!(view instanceof Diagram)) { //if it's a diagram, people most likely put image query in viewpoint already. this is to prevent showing duplicate documentation
				String viewDoc = ModelHelper.getComment(view);
				if (viewDoc != null) {
					Paragraph para = new Paragraph(viewDoc);
					para.setDgElement(view);
					para.setFrom(From.DOCUMENTATION);
					viewSection.addElement(para);
				}
			}
			Collection<Behavior> viewpointBehavior = ((Class)viewpoint).getOwnedBehavior();
			Behavior b = null;
			if (viewpointBehavior.size() > 0) 
				b = viewpointBehavior.iterator().next();
			else {
				//viewpoint can inherit other viewpoints, if this viewpoint has no behavior, check inherited behaviors
				Class now = (Class)viewpoint;
				while(now != null) {
					if (!now.getSuperClass().isEmpty()) {
						now = now.getSuperClass().iterator().next();
						if (now.getOwnedBehavior().size() > 0) {
							b = now.getOwnedBehavior().iterator().next();
							break;
						}
					} else {
						now = null;
					}
				}
			}
			if (b != null) { //parse and execute viewpoint behavior, giving it the imported/queried elements
				List<Element> elementImports = Utils.collectDirectedRelatedElementsByRelationshipJavaClass(view, ElementImport.class, 1, 1);
				List<Element> packageImports = Utils.collectDirectedRelatedElementsByRelationshipJavaClass(view, PackageImport.class, 1, 1);
				List<Element> expose = Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(view, DocGen3Profile.queriesStereotype, 1, false, 1);
				List<Element> queries = Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(view, DocGen3Profile.oldQueriesStereotype, 1, false, 1);
				if (elementImports == null) elementImports = new ArrayList<Element>();
				if (packageImports != null) elementImports.addAll(packageImports);
				if (expose != null) elementImports.addAll(expose); //all three import/queries relationships are interpreted the same
				if (queries != null) elementImports.addAll(queries); //all three import/queries relationships are interpreted the same
				if (elementImports.isEmpty())
					elementImports.add(view); //if view does not import/query anything, give the view element itself to the viewpoint
				context.pushTargets(elementImports); //this becomes the context of the activity going in
				if (b instanceof Activity) {
					parseActivityOrStructuredNode(b, viewSection);
				}
				context.popTargets();
			}
		} else { //view does not conform to a viewpoint, apply default behavior
			if (view instanceof Diagram) { //if a diagram, show diagram and documentation
				Image image = new Image();
				List<Element> images = new ArrayList<Element>();
				images.add(view);
				image.setTargets(images);
				String caption = (String)StereotypesHelper.getStereotypePropertyFirst(view, DocGen3Profile.dgviewStereotype, "caption");
				// Check for old stereotype name for backwards compatibility
				if (caption == null) caption = (String)StereotypesHelper.getStereotypePropertyFirst(view, DocGen3Profile.oldDgviewStereotype, "caption");
				List<String> captions = new ArrayList<String>();
				captions.add(caption);
				image.setCaptions(captions);
				image.setShowCaptions(true);
				viewSection.addElement(image);
			} else { //just show documentation
				String viewDoc = ModelHelper.getComment(view);
				if (viewDoc != null) {
					Paragraph para = new Paragraph(viewDoc);
					para.setDgElement(view);
					para.setFrom(From.DOCUMENTATION);
					viewSection.addElement(para);
				}
			}
		}
		
		if (!singleView) { //does everything from here including nexts
			Element content = GeneratorUtils.findStereotypedRelationship(view, DocGen3Profile.nosectionStereotype);
			if (content != null && section) //current view is a section, nosection children should go under it
				parseView(content,  viewSection, false, singleView, recurse, false);
			if (content != null && !section) //current view is not a section, further nosection children should be siblings
				parseView(content,  parent, false, singleView, recurse, false);
			Element first = GeneratorUtils.findStereotypedRelationship(view, DocGen3Profile.firstStereotype);
			if (first != null)
				parseView(first, viewSection, true, singleView, recurse, false);
			Element next = GeneratorUtils.findStereotypedRelationship(view, DocGen3Profile.nextStereotype);
			if (next != null) {
				parseView(next, parent, true, singleView, recurse, false);
			}
			
		} else if (recurse) {//single view, but recursive (gets everything underneath view including view, but not nexts from the top view
			Element content = GeneratorUtils.findStereotypedRelationship(view, DocGen3Profile.nosectionStereotype);
			if (content != null && section)
				parseView(content,  viewSection, false, singleView, recurse, false);
			if (content != null && !section)
				parseView(content,  parent, false, singleView, recurse, false);
			Element first = GeneratorUtils.findStereotypedRelationship(view, DocGen3Profile.firstStereotype);
			if (first != null)
				parseView(first, viewSection, true, singleView, recurse, false);
			if (!top) {
				Element next = GeneratorUtils.findStereotypedRelationship(view, DocGen3Profile.nextStereotype);
				if (next != null) {
					parseView(next, parent, true, singleView, recurse, false);
				}
			}
		}
	}
	
	/**
	 * parses activity/structured node - these usually indicate a new context of target elements
	 * @param a
	 * @param parent
	 */
	@SuppressWarnings("unchecked")
	private void parseActivityOrStructuredNode(Element a, Container parent) {
		InitialNode in = GeneratorUtils.findInitialNode(a);
		if (in == null)
			return;
		Collection<ActivityEdge> outs = in.getOutgoing();
		int pushed = 0;
		ActivityNode next2 = in;
		while (outs != null && outs.size() == 1) {
			ActivityNode next = outs.iterator().next().getTarget();
			next2 = null;
			if (next instanceof CallBehaviorAction || next instanceof StructuredActivityNode && StereotypesHelper.hasStereotypeOrDerived(next, DocGen3Profile.tableStructureStereotype)) { 
				Behavior b = (next instanceof CallBehaviorAction)?((CallBehaviorAction)next).getBehavior():null;
				if (StereotypesHelper.hasStereotypeOrDerived(next, DocGen3Profile.sectionStereotype) || b != null && StereotypesHelper.hasStereotypeOrDerived(b, DocGen3Profile.sectionStereotype)) {
					parseSection((CallBehaviorAction)next, parent);
					next2 = next;
				} else if (StereotypesHelper.hasStereotypeOrDerived(next, DocGen3Profile.templateStereotype) || b != null && StereotypesHelper.hasStereotypeOrDerived(b, DocGen3Profile.templateStereotype)) {
					parseQuery(next, parent);
					next2 = next;
				} else if (StereotypesHelper.hasStereotypeOrDerived(next, DocGen3Profile.collectFilterStereotype) || b != null && StereotypesHelper.hasStereotypeOrDerived(b, DocGen3Profile.collectFilterStereotype)) {
					CollectFilterParser.setContext(context);
					List<Element> results = CollectFilterParser.startCollectAndFilterSequence(next, null);
					this.context.pushTargets(results);
					pushed++;
					next2 = context.getCurrentNode();
				}
			} else if (next instanceof StructuredActivityNode) {
				Boolean loop = (Boolean)GeneratorUtils.getObjectProperty(next, DocGen3Profile.templateStereotype, "loop", false);
				Boolean ignore = (Boolean)GeneratorUtils.getObjectProperty(next, DocGen3Profile.templateStereotype, "ignore", false);
				Boolean createSections = (Boolean)GeneratorUtils.getObjectProperty(next, DocGen3Profile.structuredQueryStereotype, "createSections", false);
				Boolean useContextNameAsTitle = (Boolean)GeneratorUtils.getObjectProperty(next, DocGen3Profile.templateStereotype, "useSectionNameAsTitle", false);
				String titlePrefix = (String)StereotypesHelper.getStereotypePropertyFirst(a, DocGen3Profile.templateStereotype, "titlePrefix");
				String titleSuffix = (String)StereotypesHelper.getStereotypePropertyFirst(a, DocGen3Profile.templateStereotype, "titleSuffix");
				List<String> titles = (List<String>)StereotypesHelper.getStereotypePropertyValue(next, DocGen3Profile.templateStereotype, "titles");
				if (titles == null)
					titles = new ArrayList<String>();
				List<Element> targets = (List<Element>)StereotypesHelper.getStereotypePropertyValue(next, DocGen3Profile.templateStereotype, "targets");
				if (targets == null || targets.isEmpty()) {
					targets = Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(next, DocGen3Profile.queriesStereotype, 1, false, 1);
					targets.addAll(Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(next, DocGen3Profile.oldQueriesStereotype, 1, false, 1));
				}
				if (targets.isEmpty() && !this.context.targetsEmpty()) {
					targets = this.context.peekTargets();
				}
				if (!ignore) {
					if (loop) {
						int count = 0;
						for (Element e: targets) {
							List<Element> target = new ArrayList<Element>();
							target.add(e);
							this.context.pushTargets(target);
							Container con = parent;
							if (createSections) {
								Section sec = new Section();
								if (titles != null && titles.size() > count)
									sec.setTitle(titles.get(count));
								else if (e instanceof NamedElement)
									sec.setTitle(((NamedElement)e).getName());
								sec.setTitlePrefix(titlePrefix);
								sec.setTitleSuffix(titleSuffix);
								sec.setDgElement(next);
								parent.addElement(sec);
								con = sec;
							}
							parseActivityOrStructuredNode(next, con);
							this.context.popTargets();
							count++;
						}
					} else {
						this.context.pushTargets(targets);
						Container con = parent;
						if (createSections) {
							Section sec = new Section();
							if (titles != null && titles.size() > 0)
								sec.setTitle(titles.get(0));
							else if (!next.getName().equals(""))
								sec.setTitle(next.getName());
							sec.setUseContextNameAsTitle(useContextNameAsTitle);
							sec.setDgElement(next);
							sec.setTitlePrefix(titlePrefix);
							sec.setTitleSuffix(titleSuffix);
							parent.addElement(sec);
							con = sec;
						}
						parseActivityOrStructuredNode(next, con);
						this.context.popTargets();
					}
				}
				next2 = next;
			} else if (next instanceof ForkNode && StereotypesHelper.hasStereotype(next, DocGen3Profile.parallel)) {// REVIEW -- hasStereotypeOrDerived()?
				CollectFilterParser.setContext(context);
				List<Element> results = CollectFilterParser.startCollectAndFilterSequence(next, null);
				this.context.pushTargets(results);
				pushed++;
				next2 = context.getCurrentNode();
			}
			if (next2 == null) {
				next2 = next;
			}
			outs = next2.getOutgoing();
		} 
		while(pushed > 0) {
			this.context.popTargets();
			pushed--;
		}
	}
	
	private void parseSection(CallBehaviorAction cba, Container parent) {
		String titlePrefix = (String)GeneratorUtils.getObjectProperty(cba, DocGen3Profile.sectionStereotype, "titlePrefix", "");
		String titleSuffix = (String)GeneratorUtils.getObjectProperty(cba, DocGen3Profile.sectionStereotype, "titleSuffix", "");
		Boolean useContextNameAsTitle = (Boolean)GeneratorUtils.getObjectProperty(cba, DocGen3Profile.sectionStereotype, "useSectionNameAsTitle", false);
		String stringIfEmpty = (String)GeneratorUtils.getObjectProperty(cba, DocGen3Profile.sectionStereotype, "stringIfEmpty", "");
		Boolean skipIfEmpty = (Boolean)GeneratorUtils.getObjectProperty(cba, DocGen3Profile.sectionStereotype, "skipIfEmpty", false);
		Boolean ignore = (Boolean)GeneratorUtils.getObjectProperty(cba, DocGen3Profile.sectionStereotype, "ignore", false);
		Boolean loop = (Boolean)GeneratorUtils.getObjectProperty(cba, DocGen3Profile.sectionStereotype, "loop", false);
		Boolean isAppendix = false;

		if (StereotypesHelper.hasStereotype(cba, DocGen3Profile.appendixStereotype) || // REVIEW -- hasStereotypeOrDerived()?
		    (cba.getBehavior() != null &&
		     StereotypesHelper.hasStereotype(cba.getBehavior(), DocGen3Profile.appendixStereotype))) // REVIEW -- hasStereotypeOrDerived()?
			isAppendix = true;
		String title = (String)GeneratorUtils.getObjectProperty(cba, DocGen3Profile.sectionStereotype, "title", "");
		if (title == null || title.equals("")) {
			title = cba.getName();
			if (title.equals("") && cba.getBehavior() != null)
				title = cba.getBehavior().getName();
		}
		if (loop) {
			if (!context.targetsEmpty()) {
				for (Element e: context.peekTargets()) {
					List<Element> target = new ArrayList<Element>();
					target.add(e);
					context.pushTargets(target);
					Section sec = new Section();
					sec.isAppendix(isAppendix);
					sec.setTitlePrefix(titlePrefix);
					sec.setTitleSuffix(titleSuffix);
					if (e instanceof NamedElement)
						sec.setTitle(((NamedElement)e).getName());
					else
						sec.setTitle(title);
					sec.setStringIfEmpty(stringIfEmpty);
					sec.setSkipIfEmpty(skipIfEmpty);
					sec.setIgnore(ignore);
					sec.setUseContextNameAsTitle(useContextNameAsTitle);
					parent.addElement(sec);
					parseActivityOrStructuredNode(cba.getBehavior(), sec);
					context.popTargets();
				}
			}
		} else {
			Section sec = new Section();
			sec.isAppendix(isAppendix);
			sec.setTitlePrefix(titlePrefix);
			sec.setTitleSuffix(titleSuffix);
			sec.setTitle(title);
			sec.setStringIfEmpty(stringIfEmpty);
			sec.setSkipIfEmpty(skipIfEmpty);
			sec.setIgnore(ignore);
			sec.setUseContextNameAsTitle(useContextNameAsTitle);
			parent.addElement(sec);
			parseActivityOrStructuredNode(cba.getBehavior(), sec);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void parseQuery(ActivityNode an, Container parent) {
		String titlePrefix = (String)GeneratorUtils.getObjectProperty(an, DocGen3Profile.templateStereotype, "titlePrefix", "");
		String titleSuffix = (String)GeneratorUtils.getObjectProperty(an, DocGen3Profile.templateStereotype, "titleSuffix", "");
		Boolean useContextNameAsTitle = (Boolean)GeneratorUtils.getObjectProperty(an, DocGen3Profile.templateStereotype, "useSectionNameAsTitle", false);
		Boolean ignore = (Boolean)GeneratorUtils.getObjectProperty(an, DocGen3Profile.templateStereotype, "ignore", false);
		Boolean loop = (Boolean)GeneratorUtils.getObjectProperty(an, DocGen3Profile.templateStereotype, "loop", false);
		List<String> titles = (List<String>)GeneratorUtils.getListProperty(an, DocGen3Profile.templateStereotype, "titles", new ArrayList<String>());
		boolean structured = false;
		if (StereotypesHelper.hasStereotypeOrDerived(an, DocGen3Profile.structuredQueryStereotype) || (an instanceof CallBehaviorAction && ((CallBehaviorAction)an).getBehavior() != null && StereotypesHelper.hasStereotypeOrDerived(((CallBehaviorAction)an).getBehavior(), DocGen3Profile.structuredQueryStereotype)))
			structured = true;
		List<Element> targets = (List<Element>)StereotypesHelper.getStereotypePropertyValue(an, DocGen3Profile.templateStereotype, "targets");
		if (targets == null || targets.isEmpty()) {
			targets = Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(an, DocGen3Profile.queriesStereotype, 1, false, 1);
			targets.addAll(Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(an, DocGen3Profile.oldQueriesStereotype, 1, false, 1));
		}
		if ((targets == null || targets.isEmpty()) && an instanceof CallBehaviorAction && ((CallBehaviorAction)an).getBehavior() != null) {
			targets = (List<Element>)StereotypesHelper.getStereotypePropertyValue(((CallBehaviorAction)an).getBehavior(), DocGen3Profile.templateStereotype, "targets");
			if (targets == null || targets.isEmpty()) {
				targets = Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(((CallBehaviorAction)an).getBehavior(), DocGen3Profile.queriesStereotype, 1, false, 1);
				targets.addAll(Utils.collectDirectedRelatedElementsByRelationshipStereotypeString(((CallBehaviorAction)an).getBehavior(), DocGen3Profile.oldQueriesStereotype, 1, false, 1));
			}
		}
		if (targets.isEmpty() && !this.context.targetsEmpty()) {
			targets = this.context.peekTargets();
		}		
		if (structured && !ignore && an instanceof CallBehaviorAction) {
			Boolean createSections = (Boolean)GeneratorUtils.getObjectProperty(an, DocGen3Profile.structuredQueryStereotype, "createSections", false);
			if (loop) {
				int count = 0;
				for (Element e: targets) {
					List<Element> target = new ArrayList<Element>();
					target.add(e);
					this.context.pushTargets(target);
					Container con = parent;
					if (createSections) {
						Section sec = new Section();
						if (titles != null && titles.size() > count)
							sec.setTitle(titles.get(count));
						else if (e instanceof NamedElement)
							sec.setTitle(((NamedElement)e).getName());
						sec.setTitlePrefix(titlePrefix);
						sec.setTitleSuffix(titleSuffix);
						sec.setDgElement(an);
						parent.addElement(sec);
						con = sec;
					}
					parseActivityOrStructuredNode(((CallBehaviorAction)an).getBehavior(), con);
					this.context.popTargets();
				}
			} else {
				this.context.pushTargets(targets);
				Container con = parent;
				if (createSections) {
					Section sec = new Section();
					if (titles.size() > 0)
						sec.setTitle(titles.get(0));
					else if (!an.getName().equals(""))
						sec.setTitle(an.getName());
					else if (!((CallBehaviorAction)an).getBehavior().getName().equals(""))
						sec.setTitle(((CallBehaviorAction)an).getBehavior().getName());
					sec.setUseContextNameAsTitle(useContextNameAsTitle);
					sec.setDgElement(an);
					sec.setTitlePrefix(titlePrefix);
					sec.setTitleSuffix(titleSuffix);
					parent.addElement(sec);
					con = sec;
				}
				parseActivityOrStructuredNode(((CallBehaviorAction)an).getBehavior(), con);
				this.context.popTargets();
			}
		} else {
			Query dge = parseTemplate(an);
			if (dge != null) {
				dge.setDgElement(an);
				dge.setTargets(targets);
				dge.setTitles(titles);
				dge.setTitlePrefix(titlePrefix);
				dge.setTitleSuffix(titleSuffix);
				dge.setUseContextNameAsTitle(useContextNameAsTitle);
				dge.setIgnore(ignore);
				dge.setLoop(loop);
				dge.initialize();
				parent.addElement(dge);
			}
		}
		
		
	}
	
	/**
	 * parses query actions into classes in gov.nasa.jpl.mgss.mbee.docgen.model - creates class representation of the queries
	 * There's gotta be a way to make this less ugly... by sweeping the ugliness under multiple rugs!
	 * @param an
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Query parseTemplate(ActivityNode an) {
		
		Query dge = null;
		if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.imageStereotype)) {
			dge = new Image();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.paragraphStereotype)) {
			dge = new Paragraph();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.bulletedListStereotype)) {
			dge = new BulletedList();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.dependencyMatrixStereotype)) {
			dge = new DependencyMatrix();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.genericTableStereotype)) {
			dge = new GenericTable();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.tableStructureStereotype)) {
			// Get all the variables or whatever
			dge = new TableStructure();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.combinedMatrixStereotype)) {
			dge = new CombinedMatrix();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.customTableStereotype)) { 
			dge = new CustomTable();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.userScriptStereotype, true)) {
			dge = new UserScript();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.propertiesTableByAttributesStereotype)) {
			dge = new PropertiesTableByAttributes();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.billOfMaterialsStereotype)) {
			dge = new BillOfMaterialsTable();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.deploymentStereotype)) {
			dge = new DeploymentTable();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.workpakcageAssemblyStereotype)) {
			dge = new WorkpackageAssemblyTable();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.missionMappingStereotype)) {
			dge = new MissionMapping();
		} else if (GeneratorUtils.hasStereotypeByString(an, DocGen3Profile.libraryChooserStereotype)) {
			dge = new LibraryMapping();
		}
		return dge;
	}
 
		
}
