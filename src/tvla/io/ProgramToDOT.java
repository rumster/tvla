package tvla.io;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import tvla.analysis.interproc.transitionsystem.method.CFG;
import tvla.analysis.interproc.transitionsystem.method.CFGEdge;
import tvla.analysis.interproc.transitionsystem.method.CFGNode;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.AnalysisGraph;
import tvla.transitionSystem.Location;
import tvla.util.ProgramProperties;
import tvla.util.graph.Graph;


/** A class that converts a CFG to DOT format.
 * @author Roman manevich.
 */
public class ProgramToDOT extends StringConverter {
	/** A convenience instance.
	 */
	public static ProgramToDOT defaultInstance = new ProgramToDOT();
	
	/** DOT attributes for the CFG printout.
	 */
	private static final String cfgAttributes = ProgramProperties.getProperty(
			"tvla.dot.cfgAttributes",
			"size = \"7.5,10\"\ncenter = true; fontsize=6; node [fontsize=10, style=filled]; " +
			"edge [fontsize=10]; nodesep=0.1; ranksep=0.1;\n");
	
	/** Converts a set of transitions to a DOT representation of the CFG.
	 */
	public String convert(Object o) {
		if (o instanceof AnalysisGraph) {
			AnalysisGraph cfg = (AnalysisGraph) o;
			return convertAnalysisGraph(cfg);
		}
		
		if (o instanceof MethodTS) {
			CFG cfg = ((MethodTS) o).getCFG();
			return convertCFGCollection(cfg);
		}
		throw new InternalError("ProgramToDOT.convert: Unknown ptogram type");
	}
	
	public String convertAnalysisGraph(AnalysisGraph cfg) 
	{	
		Collection locations = cfg.getLocations();
		StringBuffer result = new StringBuffer(CommentToDOT.getPageComment());

		String programName = ProgramProperties.getProperty("tvla.programName", "program");
		// Replace ':','.' and '/' (or '\') chars in program name with '_' to appease dot.
		programName = programName.replace(':', '_').replace('.', '_').replace(File.separatorChar, '_');
		if (ProgramProperties.getBooleanProperty("tvla.dot.meaningfulTitles", false))
			result.append("digraph " + programName + " {\n");
		else result.append("digraph program {\n");
		result.append(cfgAttributes + "\n");

		// Print an edge fhat connects the name of the program to the first CFG node.
		{
			Location first = cfg.getEntryLocation();
			result.append(programName + "[style=bold];\n");
			result.append(programName + "->\"" + first.label() + "\";\n");
		}
		
		// Print a graph node for every CFG node.
		for (Iterator locationIt = locations.iterator(); locationIt.hasNext(); ) {
			Location location = (Location)locationIt.next();
			String color = "lightgray";
			if (location.doJoin)
				color = "gray, style=filled";
			result.append("\"" + location.label() + "\" [label=\"" + location.label() + 
						  "\", shape=box, style=bold, color=" + color + "];\n");
		}
		
		// Print a graph edge for every CFG edge.
		for (Iterator locationIt = locations.iterator(); locationIt.hasNext(); ) {
			Location location = (Location)locationIt.next();
			for (int i = 0; i < location.getActions().size(); i++) {
				Action action = (Action)location.getActions().get(i);
				String target = (String)location.getTargets().get(i);
				result.append("\"" + location.label() + "\"->\"" + target + "\"" +
							  "[label=\"" + action + "\"];\n");
			}
		}
		result.append("}\n");
		return result.toString();
	}
	
	
	/** Converts a set of transitions to a DOT representation of the MethodTS.
	 * 
	 */
	public String convertCFGCollection(CFG cfg) { 
		StringBuffer result = new StringBuffer(CommentToDOT.getPageComment());
		
		String methodName = quote(cfg.getSig());

		result.append("digraph " + methodName + " {\n");
		result.append(cfgAttributes + "\n");
		
		// Print if this is the main method.
		
		
		// Print an edge fhat connects the name of the method to the first MethodTS node.
		CFGNode first = cfg.getEntrySite();
		if (cfg.isMain()) {
			result.append(methodName + "[shape=box,color=red,style=filled];\n");
			result.append(methodName + "->\"" + first.label() + "\";\n");
		}
		else {
			result.append(methodName + "[shape=box,color=blue,style=filled];\n");
			result.append(methodName + "->\"" + first.label() + "\";\n");			
		}
		
		Collection nodes = cfg.getNodes();

		// Print a graph node for every MethodTS node.
		for (Iterator nodeIt = nodes.iterator(); nodeIt.hasNext(); ) {
			CFGNode node = (CFGNode)nodeIt.next();
			String nodeProperties;
			
			if (node.isEntrySite() || node.isExitSite())
				nodeProperties = "shape=ellipse, style=bold, color=red";	
			else if(node.isCallSite() || node.isRetSite())
				nodeProperties = "shape=box, style=\"setlinewidth(4)\",color=blue";					
			else 
				nodeProperties = "shape=box, style=bold, color=lightgray";
					
			result.append("\"" + node.label() + "\" [label=\"" + node.label() + 
						  "\",  " + nodeProperties + "];\n");
		}
		
		// Print a graph edge for every CFG edge.
		for (Iterator nodeIt = nodes.iterator(); nodeIt.hasNext(); ) {
			CFGNode source = (CFGNode)nodeIt.next();
			Collection outgoingEdges = cfg.getOutgoingEdges(source);
			String edgeProperties ="style=bold, color=gray"; 
			if (source.isStaticCallSite())
				edgeProperties = "style=bold, color=purple";
			else if (source.isVirtualCallSite())
				edgeProperties = "style=bold, color=red";
			else if (source.isConstructorCallSite())
				edgeProperties = "style=bold, color=green";

			for (Iterator edgeIt = outgoingEdges.iterator(); edgeIt.hasNext(); ) {
				Object objEdge = edgeIt.next();
				Graph.Edge graphEdge = (Graph.Edge) objEdge; 
				CFGEdge edge = (CFGEdge) graphEdge.getLabel();
				assert(source.equals(edge.getSource()));
				CFGNode target = edge.getDestination();			
				result.append("\"" + source.label() + "\"->\"" + target.label() + "\"" +
							  "[label=\"" + edge.title() + "\", " + edgeProperties + "];\n");
			}
		}
		
		result.append("}\n");
		return result.toString();
	}
}


