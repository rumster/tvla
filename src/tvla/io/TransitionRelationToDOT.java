package tvla.io;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import tvla.analysis.TransitionRelation;
import tvla.util.StringUtils;
import tvla.util.graph.Graph;

/** Converts an abstract transition relation to DOT comment.
 * @author Noam Rinetzky
 */
public class TransitionRelationToDOT extends StringConverter {
	public static String newLine = StringUtils.newLine;
	public static TransitionRelationToDOT defaultInstance = new TransitionRelationToDOT();
	
	/** Converts a transition relation to a string.
	 * In case it gets a map, it prints the tvs lists.
	 * If it get ta graph, the configuration graph.
	 * This allows redirecting the output to different streams.
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
		//return locs + newLine + msgs + newLine + transtions;
		return transtions + newLine + locs + newLine + msgs;
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
			String tvsList = convertLocation(tr,loc,abstractStates,null);
			outStream.print(tvsList);
		}
		
		String tvsListFooter = locationsFooter();
		outStream.print(tvsListFooter);
	}
	
	public String locationsHeader(String header) {
		StringBuffer result = new StringBuffer("");
		result.append(CommentToDOT.getPageComment());
		String locationProlog = "Structures\n" + (header == null ? "" : header);
		result.append(DOTMessage.defaultInstance.convert(locationProlog) + newLine);

		return result.toString();		
	}

	public String locationsFooter() {
		String ret = "";
		return ret;
	}	
	
	public String convertLocation(TransitionRelation tr, Object loc, Collection abstractStates, String header) {
		StructureToDOT dotConvertor = StructureToDOT.defaultInstance;
		Iterator asItr = abstractStates.iterator();
		String result = locationHeader(loc.toString());
		while (asItr.hasNext()) {
			TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) asItr.next();
			assert(as.getLocation()==loc);
			long id = tr.getId(as);
			
			// Get the action leading to this node
			String actionText = "";
			Iterator inEdgeItr = tr.getConfigurationGraph().getIncomingEdges(as).iterator();
			while (inEdgeItr.hasNext()) {
				Graph.Edge inEdge = (Graph.Edge) inEdgeItr.next();
				TransitionRelation.AbstractState predNode = (TransitionRelation.AbstractState) inEdge.getSource();
				if (predNode == as) // self-loop
					continue;
				if (tr.getId(predNode) < id)
					actionText = actionText + "\n" + inEdge.getLabel().toString();
			}
			
			result += dotConvertor.convert(as.getStructure(), "structure id = " + id + actionText);  
		}
		return result;
	}
	
	public String locationHeader(String locId) {
		StringBuffer result = new StringBuffer("");
		result.append(CommentToDOT.getPageComment());
		String locationProlog = "Program Location\n" + locId;
		result.append(DOTMessage.defaultInstance.convert(locationProlog) + newLine);

		return result.toString();
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
		StringBuffer result = new StringBuffer("");
		result.append(CommentToDOT.getPageComment());
		String msgsProlog = "Messages\n" + ((header == null) ? "" : header);
		result.append(DOTMessage.defaultInstance.convert(msgsProlog) + newLine);

		return result.toString();		
	}
	
	public String messagesFooter() {
		String ret = "";
		return ret;
	}	
	
	public String convertAbstractStateMessages(TransitionRelation tr, TransitionRelation.AbstractState as, Collection messages, String header) {
		
		long id = tr.getId(as);
		String result = new String("Messages for structure id=" + id + "\\n");
		Iterator msgItr = messages.iterator();
		while (msgItr.hasNext()) {
			Object o = msgItr.next();
			String msg = (String) o;
			result += msg + "\\n";
		}		
		return DOTMessage.defaultInstance.convert(result);
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
	
		//String states = getNodesAtLocations(tr);
		//outStream.print(states);
		
		Iterator nodeItr = configurationGraph.getNodes().iterator();
		while (nodeItr.hasNext()) {
			TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) nodeItr.next();
			Iterator edgeItr = configurationGraph.getOutgoingEdges(as).iterator();
			
			String nodeStr = genNodeString(tr.getId(as), "");
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
	
		//String states = getNodesAtLocations(tr);
		//outStream.print(states);
		
		Iterator nodeItr = configurationGraph.getNodes().iterator();
		while (nodeItr.hasNext()) {
			TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) nodeItr.next();
			Iterator edgeItr = configurationGraph.getOutgoingEdges(as).iterator();

			String nodeStr = genNodeString(tr.getId(as), "");			
			//String nodeStr = StructureToDOT.defaultInstance.convert(as.getStructure(), ""+tr.getId(as));
			
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
		StringBuffer result = new StringBuffer("");
		result.append(CommentToDOT.getPageComment());
		String msgsProlog = "Transitions graph\n" + ((header == null) ? "" : header);
		result.append(DOTMessage.defaultInstance.convert(msgsProlog) + newLine);
		result.append("digraph transitionRelation {" + newLine);
		result.append("size=\"7.5,10\";center=true;fontsize=6;node [fontsize=12, style=filled];edge [fontsize=12]; nodesep=0.1; ranksep=0.1;");
		return result.toString();		
	}

	public String transitionsFooter() {
		String ret = "}" + newLine;
		return ret;
	}
	
	
	public String genNodeString(long id, String extra) {
		return quote(id) + " [label=\"" + id + extra + "\", shape=box, style=bold, color=gray, style=filled];" + newLine;
	}

	public String genEdgeString(long srcId, long dstId, Object label) {
		String ret = quote(srcId) + "->" + quote(dstId);
		if (label != null)
			ret +=  " [label=" + quote(label.toString()) + "]" + newLine;
		return ret;
	}


	// This method clusters together abstract states of the same 
	// location. The idea is nice, however the dot output is very disappointing. 
	public String getNodesAtLocations(TransitionRelation tr) {
		StringBuffer res = new StringBuffer("");
		Map locationsToAbstractStates = tr.getAbstractStatesAtLocations();
		Iterator entryItr = locationsToAbstractStates.entrySet().iterator();
		while (entryItr.hasNext()) {
			Map.Entry entry = (Map.Entry) entryItr.next();
			Object loc = entry.getKey();
			Collection states = (Collection) locationsToAbstractStates.get(loc);
			Iterator asItr = states.iterator();
			if (asItr.hasNext()) {
				
				res.append(subGraphHeader(loc.toString()));
				while (asItr.hasNext()) {
					TransitionRelation.AbstractState as = (TransitionRelation.AbstractState) asItr.next();
					String nodeStr = genNodeString(tr.getId(as), "");
					res.append(nodeStr + newLine);				
				}
				res.append(subGraphFooter());
			}
		}
		
		return res.toString();
	}
	
	public String subGraphHeader(String label) {
		String res = "subgraph " + "cluster_" + label + 
		             "{ label=" + quote(label) + 
					 ";ranksep=0.2;nodesep=0.2;edge [fontsize=10];node [fontsize=10];" + newLine;
		return res;
	}
	
	public String subGraphFooter() {
		return "}" + newLine;
	}
	
	/*************************************
	 *  Utilities
	 *************************************/

//	String quote(String s) {
//		return "\"" + s + "\"" ; 
//	}

	String quote(long id) {
		return "\"" + id + "\"" ; 
	}
}