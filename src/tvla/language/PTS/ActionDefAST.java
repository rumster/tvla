package tvla.language.PTS;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.language.TVP.AST;
import tvla.language.TVP.ForeachAST;
import tvla.language.TVP.FormulaAST;
import tvla.language.TVP.MessageAST;
import tvla.language.TVP.PredicateAST;
import tvla.language.TVP.ReportMessageAST;
import tvla.language.TVP.UpdateAST;
import tvla.transitionSystem.Action;

/**
 * ActionDefAST
 * Action definition
 */
public class ActionDefAST extends tvla.language.TVP.ActionDefAST {
	/**
     *  Let construct
	 */
    protected final List lets;
  
	final private static boolean debug = false;
	protected boolean explicitAtUpdate;
	protected boolean expanded = false;

	public ActionDefAST(MessageAST title, List focus, FormulaAST precond, List messages, 
						FormulaAST newFormula, FormulaAST cloneFormula,
						List lets, 
                        List updates,
						FormulaAST retainFormula,
						List postMessages) {
		super(title, focus, precond, messages, newFormula, cloneFormula, updates,
				retainFormula, postMessages);
        this.lets = lets;
	}

    public AST copy() {
    	return new ActionDefAST((MessageAST) title.copy(), 
				copyList(focus), 
				precond == null ? null : (FormulaAST) precond.copy(), 
				copyList(messages), 
				newFormula == null ? null : (FormulaAST) newFormula.copy(),
				cloneFormula == null ? null : (FormulaAST) cloneFormula.copy(), 
                copyList(lets),
				copyList(updates),
				retainFormula == null ? null : (FormulaAST) retainFormula.copy(),
						copyList(postMessages));
    }

	public void substitute(PredicateAST from, PredicateAST to) {
		super.substitute(from, to);
        substituteList(lets, from, to);
	}

	public void evaluate() {
        // we do not allow foreach in let!
      
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
		if (!expanded) evaluate();
	}
	
	
	public Action getAction() {
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
        for (Iterator i = lets.iterator(); i.hasNext(); ) {
            UpdateAST let = (UpdateAST) i.next();
            let.addLet(action);
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

		// if no explicit update is made, create automatic update formulae
		action.updateLocation(!explicitAtUpdate);
		
		return action;
	}
}
	
