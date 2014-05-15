package tvla.io;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.TransitionRelation;
import tvla.util.StringUtils;
import tvla.util.graph.Graph;

/** Converts a string to a DOT comment.
 * @author Roman manevich.
 */
public class TransitionRelationToTVS extends StringConverter {
	public static String newLine = StringUtils.newLine;
	public static TransitionRelationToTVS defaultInstance = new TransitionRelationToTVS();
	
	/** Converts a transition rerlation to a string.
	 * In case it gets a map, it prints the tvs lists.
	 * If it get ta graph, the configuration graph.
	 * This allows redirecting the output to differetn streams.
	 */
	public String convert(Object o) {
		return convert(o,null);
	}

	public String convert(Object o, String header) {
		assert(o!= null);
		TransitionRelation tr = (TransitionRelation) o;
		String locs = convertLocations(tr, header);
		String msgs = convertMessages(tr,header);
		String transtions = convertTransitions(tr, header);
		return locs + newLine + msgs + newLine + transtions;
	}

	
	public void print(PrintStream outStream, Object o, String header) {
		assert(o!= null);
		TransitionRelation tr = (TransitionRelation) o;
		printLocations(outStream, tr, header);
		outStream.println(newLine);
		printMessages(outStream, tr, header);
		outStream.println(newLine);
		printTransitions(outStream,tr, header);
	}	
	
	
	/*************************************
	 *  TVS list generation 
	 *************************************/
	public String convertLocations(TransitionRelation tr, String header) {
		Map loc2abstractStates = tr.getAbstractStatesAtLocations();
		Iterator locItr = loc2abstractStates.keySet().iterator();
		String tvsList = locationsHeader(header);
		while (locItr.hasNext()) {
			Object loc = locItr.next();
			Collection abstractStates = (Collection) loc2abstractStates.get(loc);
			tvsList += convertLocation(tr,loc,abstractStates,null);
		}
		tvsList += locationsFooter();
		return tvsList;
	}

	public void printLocations(PrintStream outStream, TransitionRelation tr, String header) {
		Map loc2abstractStates = tr.getAbstractStatesAtLocations();
		Iterator locItr = loc2abstractStates.keySet().iterator();
		String tvsListHeader = locationsHeader(header);
		
		outStream.print(tvsListHeader);
		while (locItr.hasNext()) {
			Object loc = locItr.next();
			Collection abstractStates = (Collection) loc2abstractStates.get(loc);
			String  tvsList = convertLocation(tr,loc,abstractStates,null);
			outStream.print(tvsList);
		}
		
		String tvsListFooter = locationsFooter();
		outStream.print(tvsListFooter);
	}

	public String locationsHeader(String header) {
		String ret = genComment("TVS list");
		if (header != null)
			ret += newLine + genComment(header);
		ret += newLine;
		ret += "<structures>" + newLine;
		return ret;
	}
	
	public String locationsFooter() {
		String ret = "</structures>";
		return ret;
	}	
	
	public String convertLocation(TransitionRelation tr, Object loc, Collection abstractStates, String header) {
		StructureToTVS tvsConvertor = StructureToTVS.defaultInstance;
		String result = new String("%location " + loc.toString() + " = {" + newLine);
		
		Iterator asItr = abstractStates.iterator();
		while (asItr.hasNext()) {
			TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) asItr.next();
			assert(as.getLocation()==loc);
			result += " %structure " + tr.getId(as) + " = {" + newLine;
			result += tvsConvertor.convert(as.getStructure())  + newLine;  
			result += " }" + newLine;
		}
		
		result += "}"  + newLine + newLine;
		return result;
	}
	
	
	/*************************************
	 *  Messages generation 
	 *************************************/

	public String convertMessages(TransitionRelation tr, String header) {
		Map abstractStatesToMsgs = tr.getMessages();
		Iterator msgItr = abstractStatesToMsgs.entrySet().iterator();
		String msgsList = messagesHeader(header);
		while (msgItr.hasNext()) {
			Map.Entry pair = (Map.Entry) msgItr.next();
			TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) pair.getKey();
			Collection msgs = (Collection) pair.getValue();
			msgsList += convertAbstractStateMessages(tr,as,msgs,null);
		}
		msgsList += messagesFooter();
		return msgsList;
	}

	public void printMessages(PrintStream outStream, TransitionRelation tr, String header) {
		Map abstractStatesToMsgs = tr.getMessages();
		Iterator msgItr = abstractStatesToMsgs.entrySet().iterator();
		outStream.print(messagesHeader(header));
		while (msgItr.hasNext()) {
			Map.Entry pair = (Map.Entry) msgItr.next();
			TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) pair.getKey();
			Collection msgs = (Collection) pair.getValue();
			String msgsList = convertAbstractStateMessages(tr,as,msgs,null);
			outStream.print(msgsList);
		}
		outStream.print(messagesFooter());
	}

	public String messagesHeader(String header) {
		String ret = genComment("Messages list");
		if (header != null)
			ret += newLine + genComment(header);
		ret += newLine;
		ret += "<messages>" + newLine;
		return ret;
	}
	
	public String messagesFooter() {
		String ret = "</messages>";
		return ret;
	}	
	
	public String convertAbstractStateMessages(TransitionRelation tr, TransitionRelation.AbstractState as, Collection messages, String header) {
		long id = tr.getId(as);
		String result = new String("<AbstractStateMessages id=\"" + id +"\">" + newLine);
		Iterator msgItr = messages.iterator();
		while (msgItr.hasNext()) {
			Object o = msgItr.next();
			String msg = (String) o;
			result += " <msg=\"" + msg +"\">" + newLine;
		}		
		result += "</AbstractStateMessages>"  + newLine + newLine;
		return result;
	}

	
	/*************************************
	 *  Transition generation 
	 *************************************/
/*
 * Generated output:
 * <graph Label="transition relation">
 *    <node id="1"/>
 *    <node id="2"/>
 *    <node id="3"/>
 *    <edge source="1" target="2" label="x=y"/> 
 *    <edge source="2" target="3" label="x=y"/> 
 *    <edge source="1" target="3"/> 
 *  </graph>
 */
	public String convertTransitions(TransitionRelation tr, String header) {
		Graph configurationGraph = tr.getConfigurationGraph();
		String transtionsListHeader = transitionsHeader(header);
		StringBuffer res = new StringBuffer(transtionsListHeader);
	
		Iterator nodeItr = configurationGraph.getNodes().iterator();
		while (nodeItr.hasNext()) {
			TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) nodeItr.next();
			Iterator edgeItr = configurationGraph.getOutgoingEdges(as).iterator();
			String nodeStr = genNodeString(tr.getId(as));
			res.append(nodeStr + newLine);
			while (edgeItr.hasNext()) {
				Graph.Edge edge = (Graph.Edge) edgeItr.next();
				assert(edge.getSource().equals(as));
				TransitionRelation.AbstractState dst = (TransitionRelation.AbstractState) edge.getDestination();
				Object label = edge.getLabel();
				String edgeStr = genEdgeString(tr.getId(as),tr.getId(dst),label);
				res.append(edgeStr + newLine);
			}
		}
		String footer = transitionsFooter();
		res.append(footer);
		return res.toString();
	}

	public void printTransitions(PrintStream outStream, TransitionRelation tr, String header) {
		Graph configurationGraph = tr.getConfigurationGraph();
		String transtionsListHeader = transitionsHeader(header);
		outStream.print(transtionsListHeader);
	
		Iterator nodeItr = configurationGraph.getNodes().iterator();
		while (nodeItr.hasNext()) {
			TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) nodeItr.next();
			Iterator edgeItr = configurationGraph.getOutgoingEdges(as).iterator();
			String nodeStr = genNodeString(tr.getId(as));
			outStream.print(nodeStr + newLine);
			while (edgeItr.hasNext()) {
				Graph.Edge edge = (Graph.Edge) edgeItr.next();
				assert(edge.getSource().equals(as));
				TransitionRelation.AbstractState dst = (TransitionRelation.AbstractState) edge.getDestination();
				Object label = edge.getLabel();
				String edgeStr = genEdgeString(tr.getId(as),tr.getId(dst),label);
				outStream.print(edgeStr + newLine);
			}
		}
		String footer = transitionsFooter();
		outStream.print(footer);
	}

	
	public String transitionsHeader(String header) {
		String ret = genComment("Transitions graph");
		if (header != null)
			ret += newLine + genComment(header);
		ret += newLine;
		ret += "<graph Label=\"transition relation\">" + newLine;
		return ret;
	}

	public String transitionsFooter() {
		String ret = "</graph>" + newLine;
		return ret;
	}
	
	
	public String genNodeString(long id) {
		return "<node id = \"" + id + "\" />";
	}

	public String genEdgeString(long srcId, long dstId, Object label) {
		String ret = "<edge source=\""+ srcId +"\" target=\"" + dstId + "\";";
		if (label != null)
			ret +=  "label=\"" + label.toString() + "\"";
		ret += "/>";
		return ret;
	}

	/*************************************
	 *  Utilities
	 *************************************/

	private String genComment(String str) {
		assert(str != null);
		return "// " + str;
	}
}