package tvla.language.TVM;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.exceptions.SemanticErrorException;
import tvla.language.TVP.AST;
import tvla.language.TVP.ForeachAST;
import tvla.language.TVP.FormulaAST;
import tvla.language.TVP.MessageAST;
import tvla.language.TVP.PredicateAST;
import tvla.language.TVP.ReportMessageAST;
import tvla.language.TVP.UpdateAST;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.GlobalAction;

/**
 * ActionDefAST
 * Action definition
 */
public class ActionDefAST extends tvla.language.TVP.ActionDefAST {
	protected FormulaAST startFormula;
	protected FormulaAST waitFormula;
	protected FormulaAST stopFormula;
	protected FormulaAST haltCondition;
	protected ThreadUseAST threadUse;
	protected boolean explicitAtUpdate;
	protected boolean expanded = false;

	public ActionDefAST(MessageAST title,
			            List focus,
			            FormulaAST precond,
			            List messages, 
						FormulaAST newFormula,
						FormulaAST cloneFormula,
						List updates, 
						FormulaAST retainFormula,
						List postMessages,
						FormulaAST startFormula,
						FormulaAST waitFormula,
						FormulaAST stopFormula,
						ThreadUseAST threadUse,
						FormulaAST halting,
						Boolean explicitAtUpd) {
		super(title, focus, precond, messages, newFormula, cloneFormula,
			       updates, retainFormula, postMessages);
		this.startFormula = startFormula;
		this.waitFormula = waitFormula;
		this.stopFormula = stopFormula;
		this.threadUse = threadUse;
		this.haltCondition = halting;
		this.explicitAtUpdate = explicitAtUpd.booleanValue();
	}

    public AST copy() {
	return new ActionDefAST((MessageAST) title.copy(), 
				copyList(focus), 
				precond == null ? null : (FormulaAST) precond.copy(), 
				copyList(messages), 
				newFormula == null ? null : (FormulaAST) newFormula.copy(),
				cloneFormula == null ? null : (FormulaAST) cloneFormula.copy(), 
				copyList(updates),
				retainFormula == null ? null : (FormulaAST) retainFormula.copy(),
				copyList(postMessages), 
				startFormula == null ? null : (FormulaAST) startFormula.copy(),
				waitFormula == null ? null : (FormulaAST) waitFormula.copy(),
				stopFormula == null ? null : (FormulaAST) stopFormula.copy(),
				threadUse == null ? null : (ThreadUseAST) threadUse.copy(),
				haltCondition == null ? null : (FormulaAST) haltCondition.copy(),
				new Boolean(explicitAtUpdate)
				);
    }

	public void substitute(PredicateAST from, PredicateAST to) {
		super.substitute(from, to);
		if (startFormula != null)
			startFormula.substitute(from, to);
		if (waitFormula != null)
			waitFormula.substitute(from, to);
		if (stopFormula != null)
			stopFormula.substitute(from, to);
		// no need to substitute thread use
		if (haltCondition != null)
			haltCondition.substitute(from, to);
	}

	public void evaluate() {
		List newUpdates = new ArrayList();
		for (Iterator i = this.updates.iterator(); i.hasNext(); ) {
			AST anUpdate = (AST) i.next();
			if (anUpdate instanceof ForeachAST) {
				newUpdates.addAll(((ForeachAST) anUpdate).evaluate());
			} else {
				newUpdates.add(anUpdate);
			}
		}
		this.updates = newUpdates;
		expanded = true;
	}
	
	public void generate() {
		if (threadUse != null)
			threadUse.generate();
		if (!expanded) evaluate();
	}
	
	
	public Action getAction() {
		try {
		Action action = new Action();
		
		if (!expanded) evaluate();
		
		for (Iterator i = focus.iterator(); i.hasNext(); ) {
			FormulaAST formula = (FormulaAST) i.next();
			action.addFocusFormula(formula.getFormula());
		}
		for (Iterator i = messages.iterator(); i.hasNext(); ) {
			ReportMessageAST message = (ReportMessageAST) i.next();
			action.addMessage(message.getFormula(), message.getMessage(), null);
		}	    
		for (Iterator i = updates.iterator(); i.hasNext(); ) {
			UpdateAST update = (UpdateAST) i.next();
			update.addUpdate(action);
		}

		action.setTitle(title.getMessage());
		if (precond != null)
			action.precondition(precond.getFormula());
		if (newFormula != null)
			action.newFormula(newFormula.getFormula());
		if (cloneFormula != null)
			action.cloneFormula(cloneFormula.getFormula());
		if (retainFormula != null)
			action.retainFormula(retainFormula.getFormula());
		if (startFormula != null)
			action.startFormula(startFormula.getFormula());
		if (waitFormula != null)
			action.waitFormula(waitFormula.getFormula());
		if (stopFormula != null)
			action.stopFormula(stopFormula.getFormula());
		if (threadUse != null) {
			action.threadType(threadUse.getThreadDef().getThread());
		}
		if (haltCondition != null)
			action.haltCondition(haltCondition.getFormula());
		// if no explicit update is made, create automatic update formulae
		action.updateLocation(!explicitAtUpdate);
			
		return action;
		}
		catch (SemanticErrorException e) {
			e.append("whie generating the action definition " + toString());
			throw e;
		}
	}
	
	public GlobalAction getGlobalAction() {
		try {
		GlobalAction action = new GlobalAction();
		for (Iterator i = focus.iterator(); i.hasNext(); ) {
			FormulaAST formula = (FormulaAST) i.next();
			action.addFocusFormula(formula.getFormula());
		}
		for (Iterator i = messages.iterator(); i.hasNext(); ) {
			ReportMessageAST message = (ReportMessageAST) i.next();
			action.addMessage(message.getFormula(), message.getMessage(), null);
		}	    
		for (Iterator i = updates.iterator(); i.hasNext(); ) {
			UpdateAST update = (UpdateAST) i.next();
			update.addUpdate(action);
		}

		action.setTitle(title.getMessage());
		if (precond != null)
			action.precondition(precond.getFormula());
		if (newFormula != null)
			action.newFormula(newFormula.getFormula());
		if (cloneFormula != null)
			action.cloneFormula(cloneFormula.getFormula());
		if (retainFormula != null)
			action.retainFormula(retainFormula.getFormula());
		if (startFormula != null)
			action.startFormula(startFormula.getFormula());
		if (waitFormula != null)
			action.waitFormula(waitFormula.getFormula());
		if (stopFormula != null)
			action.stopFormula(stopFormula.getFormula());
		if (threadUse != null) {
			action.threadType(threadUse.getThreadDef().getThread());
		}
		if (haltCondition != null)
			action.haltCondition(haltCondition.getFormula());
		
		// if no explicit update is made, create automatic update formulae
		action.updateLocation(!explicitAtUpdate);
		
		return action;
		}
		catch (SemanticErrorException e) {
			e.append("while generating the global action " + toString());
			throw e;
		}
	}
}
