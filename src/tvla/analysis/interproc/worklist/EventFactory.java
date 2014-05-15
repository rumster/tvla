/*
 * File: EventFactory.java 
 * Created on: 24/10/2004
 */

package tvla.analysis.interproc.worklist;

import java.util.Collection;

import tvla.analysis.interproc.transitionsystem.AbstractState.Fact;
import tvla.analysis.interproc.transitionsystem.method.MethodTS;
import tvla.analysis.interproc.transitionsystem.method.TSNode;

/** 
 * @author maon
 */
public abstract class EventFactory {
	private static EventFactory theInstance = new SimpleEventFactory();

	public static  EventIntra intraEvent(
			MethodTS mtd, 
			TSNode site, 
			Fact fact) {
		return theInstance.genIntraEvent(mtd,site,fact);
	}

	public static EventStaticCall staticCallEvent(
			MethodTS mtd,
			TSNode site, 
			Fact ctx, 
			MethodTS callee){
		return theInstance.genStaticCallEvent(mtd,site,ctx,callee);
	}
	public static  EventVirtualCall virtualCallEvent(
			MethodTS mtd,
			TSNode site, 
			Fact ctx,
			Collection refinedCTXs,
			MethodTS callee){
		return theInstance.genVirtualCallEvent(mtd,site,ctx,refinedCTXs,callee);
	}
	
	public static  EventConstructorCall constructorCallEvent(
			MethodTS mtd,
			TSNode site, 
			Fact ctx, 
			MethodTS callee){
		return theInstance.genConstructorCallEvent(mtd,site,ctx,callee);
	}
	
	public static  EventRet retEvent(
			MethodTS mtd, 
			Fact entryS,
			Fact exitS){
		return theInstance.genRetEvent(mtd,entryS,exitS);
	}
	
	public static  EventTransition transitionEvent(
			MethodTS mtd, 
			TSNode fromNode,
			Fact  fromFact,
			TSNode toNode,
			Fact  toFact) {
		return theInstance.genTransitionEvent(
					mtd,fromNode,fromFact,toNode,toFact);
	}
	////////////////////////////////////////////////////////////////////
	////                   Hooks for implementations                ////
	////////////////////////////////////////////////////////////////////
	
	public abstract EventIntra genIntraEvent(
			MethodTS mtd, 
			TSNode site, 
			Fact asTVS); 

	public abstract EventStaticCall genStaticCallEvent(
			MethodTS mtd,
			TSNode site, 
			Fact ctx, 
			MethodTS callee);
	
	public abstract EventVirtualCall genVirtualCallEvent(
			MethodTS mtd,
			TSNode site, 
			Fact ctx, 
			Collection refinedCTXs,
			MethodTS callee);
	
	public abstract EventConstructorCall genConstructorCallEvent(
			MethodTS mtd,
			TSNode site, 
			Fact ctx, 
			MethodTS callee);
	
	public abstract EventRet genRetEvent(
			MethodTS mtd, 
			Fact entryS,
			Fact exitS); 
	
	public abstract EventTransition genTransitionEvent(
			MethodTS mtd, 
			TSNode fromNode,
			Fact  fromFact,
			TSNode toNode,
			Fact  toFact);
}
