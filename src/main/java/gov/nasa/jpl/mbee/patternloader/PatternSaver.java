package gov.nasa.jpl.mbee.patternloader;

import gov.nasa.jpl.mbee.stylesaver.ViewSaver;

import java.util.HashSet;
import java.util.List;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.PresentationElement;

/**
 * A class used to store a JSON style pattern of a diagram.
 * 
 * @author Benjamin Inada, JPL/Caltech
 */
public class PatternSaver {
	private JSONObject pattern;

	/**
	 * Sets the pattern property by getting a style string representing
	 * the styles on the parameter diagram.
	 * 
	 * @param proj the project that the diagram is stored in.
	 * @param diag the diagram to save.
	 */
	public void savePattern(Project proj, DiagramPresentationElement diag) {
    	// get the style string
		String styleStr = ViewSaver.save(proj, diag, true);
		if(styleStr == null) {
			return;
		}
		
		setPattern(diag, styleStr);
	}
	
	/**
	 * A helper for parsing the pattern style string and loading the pattern property.
	 * 
	 * @param diag		the diagram to save.
	 * @param styleStr	the JSON style pattern string.
	 */
	@SuppressWarnings("unchecked")
	private void setPattern(DiagramPresentationElement diag, String styleStr) {
		List<PresentationElement> elemList = diag.getPresentationElements();

		// parse the style string
		JSONParser parser = new JSONParser();
		Object parsedStyle = null;
		try {
			parsedStyle = parser.parse(styleStr);
		} catch(ParseException e) {
			e.printStackTrace();
			Application.getInstance().getGUILog().log("Error parsing pattern. Pattern save cancelled.");
			
			return;
		}

		JSONObject styleObj = (JSONObject) parsedStyle;
		
		HashSet<String> typesSaved = new HashSet<String>();		// a Set to store the type names saved throughout the process
		pattern = new JSONObject();								// a HashMap that will store the style pattern of the diagram
		
		// TODO THIS MUST BE DONE RECURSIVELY SO THAT THE LOADER LOADS ALL ELEMENTS INCLUDING CHILDREN
		for(PresentationElement elem : elemList) {
			String typeKey = elem.getHumanType();
			
			// check that the type style hasn't been saved yet
			if(!typesSaved.contains(typeKey)) {
				String typeValue = getElementPatternString(elem, styleObj);
				
				// add the key/value style pair to the JSON object
				pattern.put(typeKey, typeValue);
				
				typesSaved.add(elem.getHumanType());
				styleObj.remove(elem.getID());
			}
		}
	}
	
	/**
	 * Getter for the pattern property.
	 * 
	 * @return the pattern property.
	 */
	public JSONObject getPattern() {
		return pattern;
	}
	
	/**
	 * Gets the pattern string for a specific presentation element.
	 * 
	 * @param elem		the element to get the pattern string for.
	 * @param styleObj	the JSON style pattern object.
	 * 
	 * @return			the pattern string for the parameter element, null if not found
	 */
	private static String getElementPatternString(PresentationElement elem, JSONObject styleObj) {
		// get the value associated with the element's ID
		String elemStyleStr;
		try {
			elemStyleStr = (String) styleObj.get(elem.getID());
		} catch(NullPointerException e) {
			return null;
		}
		
		// element has not had its style saved
		if(elemStyleStr == null) {
			return null;
		}
		
		return elemStyleStr;
	}
}