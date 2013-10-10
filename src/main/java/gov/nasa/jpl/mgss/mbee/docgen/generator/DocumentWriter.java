package gov.nasa.jpl.mgss.mbee.docgen.generator;

import gov.nasa.jpl.mgss.mbee.docgen.docbook.DBBook;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.DBSerializeVisitor;
import gov.nasa.jpl.mgss.mbee.docgen.docbook.DocumentElement;
import gov.nasa.jpl.mgss.mbee.docgen.model.DocBookOutputVisitor;
import gov.nasa.jpl.mgss.mbee.docgen.model.Document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.task.ProgressStatus;
import com.nomagic.task.RunnableWithProgress;

/**
 * runs the generation as a runnable to not stall magicdraw main thread, also allow user to cancel
 * @author dlam
 *
 */
public class DocumentWriter implements RunnableWithProgress {

	private Document dge;
	private File realfile;
	private File dir;
	private boolean genNewImage;
	
	public DocumentWriter(Document dge, File realfile, boolean genNewImage, File dir) {
		this.dge = dge;
		this.realfile = realfile;
		this.dir = dir;
		this.genNewImage = genNewImage;
	}
	
	@Override
	public void run(ProgressStatus arg0) {
		GUILog gl = Application.getInstance().getGUILog();
		arg0.setIndeterminate(true);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(realfile));
			gl.log("output dir: " + dir.getAbsolutePath());
			DocBookOutputVisitor visitor = new DocBookOutputVisitor(false, dir.getAbsolutePath());
			dge.accept(visitor);
			DBBook book = visitor.getBook();
			if (book != null) {
			//List<DocumentElement> books = dge.getDocumentElement();
				DBSerializeVisitor v = new DBSerializeVisitor(genNewImage, dir, arg0);
				book.accept(v);
				writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
				writer.write("<!DOCTYPE doc [\n<!ENTITY % iso-lat1 PUBLIC \"ISO 8879:1986//ENTITIES Added Latin 1//EN//XML\" \"http://www.oasis-open.org/docbook/xmlcharent/0.3/iso-lat1.ent\">\n %iso-lat1;]>");
				writer.write(v.getOut());
			}
			writer.flush();
			writer.close();
			gl.log("Generation Finished");
		} catch (IOException ex) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			ex.printStackTrace(pw);
			gl.log(sw.toString()); // stack trace as a string
			ex.printStackTrace();
		}
	}

}
