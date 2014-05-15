/*
 * File: SimpleEventFactory.java 
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
public class SimpleEventFactory extends EventFactory {

	/* (non-Javadoc)
	 * @see tvla.analysis.pasta.worklist.EventFactory#genIntraEvent(tvla.analysis.pasta.transitionsystem.method.MethodTS, tvla.analysis.pasta.transitionsystem.method.TSNode, tvla.analysis.pasta.transitionsystem.AbstractState.TVS)
	 */
	public EventIntra genIntraEvent(MethodTS mtd, TSNode site, Fact fact) {
		return new EventIntra(mtd, site, fact);
	}

	/* (non-Javadoc)
	 * @see tvla.analysis.pasta.worklist.EventFactory#genStaticCallEvent(tvla.analysis.pasta.transitionsystem.method.MethodTS, tvla.analysis.pasta.transitionsystem.method.TSNode, tvla.analysis.pasta.transitionsystem.AbstractState.TVS, tvla.analysis.pasta.transitionsystem.method.MethodTS)
	 */
	public EventStaticCall genStaticCallEvent(
			MethodTS mtd, TSNode site, Fact ctx, MethodTS callee) {
		return new EventStaticCall(mtd, site, ctx, callee);
	}

	/* (non-Javadoc)
	 * @see tvla.analysis.pasta.worklist.EventFactory#genEventVirtualCall(tvla.analysis.pasta.transitionsystem.method.MethodTS, tvla.analysis.pasta.transitionsystem.method.TSNode, tvla.analysis.pasta.transitionsystem.AbstractState.TVS, tvla.analysis.pasta.transitionsystem.method.MethodTS)
	 */
	public EventVirtualCall genVirtualCallEvent(
			MethodTS mtd, TSNode site, Fact ctx, Collection refinedCTXs, MethodTS callee) {
		return new EventVirtualCall(mtd, site, ctx, refinedCTXs, callee);
	}

	/* (non-Javadoc)
	 * @see tvla.analysis.pasta.worklist.EventFactory#genEventConstructorCall(tvla.analysis.pasta.transitionsystem.method.MethodTS, tvla.analysis.pasta.transitionsystem.method.TSNode, tvla.analysis.pasta.transitionsystem.AbstractState.TVS, tvla.analysis.pasta.transitionsystem.method.MethodTS)
	 */
	public EventConstructorCall genConstructorCallEvent(
			MethodTS mtd, TSNode site, Fact ctx, MethodTS callee) {
		return new EventConstructorCall(
				mtd, site, ctx, callee);
	}

	/* (non-Javadoc)
	 * @see tvla.analysis.pasta.worklist.EventFactory#genEventRet(tvla.analysis.pasta.transitionsystem.method.MethodTS, tvla.analysis.pasta.transitionsystem.AbstractState.TVS, tvla.analysis.pasta.transitionsystem.AbstractState.TVS)
	 */
	public EventRet genRetEvent(MethodTS mtd, Fact entryS, Fact exitS) {
		return new EventRet(mtd, entryS, exitS);
	}

	public EventTransition genTransitionEvent(
			MethodTS mtd, 
			TSNode fromNode,
			Fact  fromFact,
			TSNode toNode,
			Fact  toFact) {
		return new EventTransition(
			mtd,fromNode,fromFact,toNode,toFact);
	}
}
