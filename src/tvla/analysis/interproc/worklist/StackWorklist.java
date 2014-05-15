/*
 * File: StackWorklist.java 
 * Created on: 17/10/2004
 */

package tvla.analysis.interproc.worklist;

/** A stack based implementation for the worklist.
 * The newest event is handled first. 
 * 
 * @author maon
 */

import java.io.PrintStream;
import java.util.LinkedList;

public class StackWorklist implements Worklist {
	static final boolean xdebug = false;
	static final PrintStream out = System.out;
	private final LinkedList lowPriorityStack;
	private final LinkedList midPriorityStack;
	private final LinkedList highPriorityStack;

	public StackWorklist() {
		super();
		if (xdebug)
			out.println("StackWorklist - constructor");
		lowPriorityStack = new LinkedList();
		midPriorityStack = new LinkedList();
		highPriorityStack = new LinkedList();
	}

		
	public void addEvent(Event event) {
		if (xdebug)
			out.println("addEvent: " + event.toString());

		highPriorityStack.addFirst(event);
	}

	public void addEvent(Event  event,  Priority priority) {
		if (null == priority) 
			lowPriorityStack.addFirst(event);	
		else
			midPriorityStack.addFirst(event);	
	}

	public boolean hasEvent() {
		return ! (lowPriorityStack.isEmpty() && 
				  midPriorityStack.isEmpty() && 
				  highPriorityStack.isEmpty()  );
	}

	public boolean ignoresPriorites() {
		return true;
	}
	
	public boolean repsectLowAndHighPriorites() {
		return true;
	}
	
	public Event extractEvent() {
		if (! highPriorityStack.isEmpty())
			return (Event) highPriorityStack.removeFirst();
		
		if (! midPriorityStack.isEmpty())
			return (Event) midPriorityStack.removeFirst();

		if (! lowPriorityStack.isEmpty())
			return (Event) lowPriorityStack.removeFirst();

		throw new InternalError("StackWorklist.extractEvent: Extracting event from an empty stack");
	}
}

