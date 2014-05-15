/*
 * File: EventConsumer.java 
 * Created on: 21/10/2004
 */

package tvla.analysis.interproc.transitionsystem;

import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;

/** 
 * @author maon
 */
public interface EventConsumer {
	public abstract void addIntraEvent(
			MethodTS mtdTS, 
			TSNode site,
			Fact newFact);

	public abstract void addStaticCallEvent(
			MethodTS callerTS, 
			TSNode site,
			Fact ctx, 
			MethodTS callee);

	public abstract void addVirtualCallEvent(
			MethodTS callerTS, 
			TSNode site,
			Fact ctx, 
			MethodTS callee);

	public abstract void addConstructorCallEvent(
			MethodTS callerTS,
			TSNode site,
			Fact ctx, 
			MethodTS callee);

	public abstract void addRetEvent(
			MethodTS callee,
			Fact entryS, 
			Fact exitS);
}