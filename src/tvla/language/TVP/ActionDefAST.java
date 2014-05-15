package tvla.language.TVP;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tvla.exceptions.SemanticErrorException;
import tvla.transitionSystem.Action;
import tvla.util.StringUtils;

/** An abstract syntax node for an action definition (the action body).
 * @author Tal Lev-Ami, Eran Yahav.
 */
public class ActionDefAST extends AST {
	protected MessageAST title;
	protected FormulaAST newFormula;
	protected FormulaAST cloneFormula;
	protected FormulaAST retainFormula;
	protected FormulaAST composeFormula;
	protected FormulaAST decomposeFormula;
	protected FormulaAST precond;
	protected List focus;
	protected List<ReportMessageAST> messages;
	protected List<ReportMessageAST> postMessages;
	// A list of predicate update AST objects (UpdateAST).
	protected List updates;
	ActionMacroAST myMacro;
    private List parameters;
	private FormulaAST frame_pre;
	private FormulaAST frame;

	public ActionDefAST(MessageAST title, List focus, FormulaAST precond,
			List messages, FormulaAST newFormula, FormulaAST cloneFormula,
			List updates, FormulaAST retainFormula, List postMessages) {
		this(title, focus, precond, messages, newFormula, cloneFormula, updates, retainFormula, postMessages,
				null, null, new ArrayList(), null, null);
	}    
	
	public ActionDefAST(MessageAST title, List focus, FormulaAST precond,
			List messages, FormulaAST newFormula, FormulaAST cloneFormula,
			List updates, FormulaAST retainFormula, List postMessages,
			FormulaAST composeFormula,
			FormulaAST decomposeFormula,
			List parameters, FormulaAST frame_pre, FormulaAST frame) {
		this.title = title;
		this.focus = focus;
		this.precond = precond;
		this.messages = messages;
		this.newFormula = newFormula;
		this.cloneFormula = cloneFormula;
		this.updates = updates;
		this.retainFormula = retainFormula;
		this.composeFormula = composeFormula;
		this.decomposeFormula = decomposeFormula;
		this.postMessages = postMessages;
		this.parameters = parameters;
		this.frame_pre = frame_pre;
		this.frame = frame;
	}

	public AST copy() {
		return new ActionDefAST((MessageAST) title.copy(), copyList(focus),
				precond == null ? null : (FormulaAST) precond.copy(),
				copyList(messages), newFormula == null ? null
						: (FormulaAST) newFormula.copy(),
				cloneFormula == null ? null : (FormulaAST) cloneFormula.copy(),
				copyList(updates),
				retainFormula == null ? null : (FormulaAST) retainFormula.copy(),
						copyList(postMessages),
				composeFormula == null ? null : (FormulaAST) composeFormula
						.copy(), decomposeFormula == null ? null
						: (FormulaAST) decomposeFormula.copy(),
						copyList(parameters),
						frame_pre == null ? null : (FormulaAST) frame_pre.copy(),
						frame == null ? null : (FormulaAST) frame.copy()
						);
	}

	public void substitute(PredicateAST from, PredicateAST to) {
		try {
		substituteList(focus, from, to);
		substituteList(messages, from, to);
		try {
		substituteList(updates, from, to);
		}
		catch (SemanticErrorException e) {
			e.append("while expanding the updates " + updates.toString());
			throw e;
		}
		title.substitute(from, to);
		if (precond != null) {
			precond.substitute(from, to);
		}
		if (newFormula != null) {
			newFormula.substitute(from, to);
		}
		if (cloneFormula != null) {
			cloneFormula.substitute(from, to);
		}
		if (retainFormula != null) {
			retainFormula.substitute(from, to);
		}
		if (composeFormula != null) {
			composeFormula.substitute(from, to);
		}
		if (decomposeFormula != null) {
			decomposeFormula.substitute(from, to);
		}
		substituteList(postMessages, from, to);
		if (frame_pre != null) {
			frame_pre.substitute(from, to);
		}
		if (frame != null) {
			frame.substitute(from, to);
		}
		
		}
		catch (SemanticErrorException e) {
			e.append("while expanding the macro definition " + toString());
			throw e;
		}
	}

	public void evaluate() {
	    // Substitute paramters
	    for (Parameter parameter : (List<Parameter>) parameters) {
	        substitute(parameter.getParametricId(), parameter.getActualId());
	    }
	    
		List newUpdates = new ArrayList();
		for (Iterator i = this.updates.iterator(); i.hasNext();) {
			AST anUpdate = (AST) i.next();
			if (anUpdate instanceof ForeachAST) {
				newUpdates.addAll(((ForeachAST) anUpdate).evaluate());
			} else {
				newUpdates.add(anUpdate);
			}
		}
		this.updates = newUpdates;

		List newFocus = new ArrayList();
		for (Iterator i = this.focus.iterator(); i.hasNext();) {
			AST aFocus = (AST) i.next();
			if (aFocus instanceof ForeachAST) {
				newFocus.addAll(((ForeachAST) aFocus).evaluate());
			} else {
				newFocus.add(aFocus);
			}
		}
		this.focus = newFocus;
	}

	/**
	 * creates an action from the ActionDefAST
	 * @return a new Action object
	 */
	public Action getAction() {
		try {
			Action action = new Action();
			for (Iterator i = focus.iterator(); i.hasNext();) {
				FormulaAST formula = (FormulaAST) i.next();
				try {
					action.addFocusFormula(formula.getFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating the focus formula "
							+ formula.toString());
					throw e;
				}
			}
			for (ReportMessageAST message : messages) {
				try {
					action.addMessage(message.getFormula(), message
							.getMessage(), message.getComposeFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating the message " + message);
					throw e;
				}
			}
			for (ReportMessageAST message : postMessages) {
				try {
					action.addPostMessage(message.getFormula(), message
							.getMessage(), message.getComposeFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating the post message " + message);
					throw e;
				}
			}
			for (Iterator i = updates.iterator(); i.hasNext();) {
				UpdateAST update = (UpdateAST) i.next();
				try {
					update.addUpdate(action);
				} catch (SemanticErrorException e) {
					e.append("while generating the update " + update);
					throw e;
				}
			}

			action.setTitle(title.getMessage());
			if (precond != null) {
				try {
					action.precondition(precond.getFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating the precondition " + precond);
					throw e;
				}
			}
			if (newFormula != null) {
				try {
					action.newFormula(newFormula.getFormula());
				} catch (SemanticErrorException e) {
					e
							.append("while generating the new operation "
									+ newFormula);
					throw e;
				}
			}
			if (cloneFormula != null) {
				try {
					action.cloneFormula(cloneFormula.getFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating the clone operation "
							+ cloneFormula);
					throw e;
				}
			}
			if (retainFormula != null) {
				try {
					action.retainFormula(retainFormula.getFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating the retain operation "
							+ retainFormula);
					throw e;
				}
			}
			if (composeFormula != null) {
				try {
					action.composeFormula(composeFormula.getFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating the compose operation "
							+ composeFormula);
					throw e;
				}
			}
			if (decomposeFormula != null) {
				try {
					action.decomposeFormula(decomposeFormula
							.getFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating the decompose operation "
							+ decomposeFormula);
					throw e;
				}
			}
			if (frame_pre != null) {
				try {
					if (isAllocating()) {
						throw new SemanticErrorException("Can't use %frame_pre for actions with %new");
					}
					action.framePre(frame_pre
							.getFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating frame pre-composition"
							+ frame_pre);
					throw e;
				}				
			}
			if (frame != null) {
				try {
					if (isAllocating()) {
						throw new SemanticErrorException("Can't use %frame for actions with %new");
					}
					action.frame(frame
							.getFormula());
				} catch (SemanticErrorException e) {
					e.append("while generating frame pre-composition"
							+ frame_pre);
					throw e;
				}				
			}
			return action;
		} catch (SemanticErrorException e) {
			String message = "while generating the action definition ";
			if (myMacro != null)
				message = message + "for " + myMacro.name + StringUtils.newLine;
			message = message + toString();
			e.append(message);
			throw e;
		}
	}

	/**
	 * does this actionDef allocate a new individual (via new or clone)
	 * @return true when actionDef uses allocation/cloning
	 */
	public boolean isAllocating() {
		return (newFormula != null) || (cloneFormula != null);
	}

	/**
	 * does this actionDef defines a skip action (has no effect)?
	 * @return true if action has no effect
	 */
	public boolean isSkipActionDef() {
		return ((focus.size() == 0) && (precond == null)
				&& (newFormula == null) && (retainFormula == null)
				&& (messages.size() == 0) && (updates.size() == 0));
	}

	/**
	 * return a string representation of the ActionDefAST
	 * @return string representation of actionDefAST
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		String separator = "";
		String indent = "  ";
		String doubleIndent = "    ";

		// title
		result.append("%t ");
		result.append(title.toString());
		result.append('\n');
		
		if (frame_pre != null) {
            result.append(indent);
		    result.append("%frame_pre ");
		    result.append(frame_pre.toString());
	        result.append('\n');			
		}
		if (composeFormula != null) {
            result.append(indent);
		    result.append("%compose ");
		    result.append(composeFormula.toString());
	        result.append('\n');
		}
		if (frame != null) {
            result.append(indent);
		    result.append("%frame ");
		    result.append(frame.toString());
	        result.append('\n');			
		}
		// focus
		if (focus != null && !focus.isEmpty()) {
			result.append(indent);
			result.append("%f { ");
			for (Iterator it = focus.iterator(); it.hasNext();) {
				result.append(separator);
				result.append(it.next().toString());
				separator = ",";
			}
			result.append(" }\n");
		}
		// precondition
		if (precond != null) {
			result.append(indent);
			result.append("%p ");
			result.append(precond.toString());
			result.append('\n');
		}
		// messages
		if (!messages.isEmpty()) {
            for (ReportMessageAST message : messages) {
                result.append(indent);
                result.append(message.toString());
                result.append('\n');
            }

		}

		//  new
		if (newFormula != null) {
			result.append(indent);
			result.append("%new ");
			String newString = newFormula.toString();
			if (!newString.equals("1")) {
				result.append(newString);
			}
			result.append('\n');
		}
		// clone
		if (cloneFormula != null) {
			result.append(indent);
			result.append("%clone ");
			String cloneString = cloneFormula.toString();
			if (!cloneString.equals("1")) {
				result.append(cloneString);
			}
			result.append('\n');
		}
		if (!updates.isEmpty()) {
			result.append(indent);
			result.append("{\n");
			for (Iterator it = updates.iterator(); it.hasNext();) {
				result.append(doubleIndent);
				result.append(it.next().toString());
				result.append('\n');
			}
			result.append(indent);
			result.append("}");
			result.append('\n');
		}
		if (retainFormula != null) {
			result.append(indent);
			result.append("%retain ");
			result.append(retainFormula.toString());
			result.append('\n');
		}
        // post messages
        if (!postMessages.isEmpty()) {
            for (ReportMessageAST message : postMessages) {
                result.append(indent);
                result.append(message.toString());
                result.append('\n');
            }
        }
        if (decomposeFormula != null) {
            result.append(indent);
            result.append("%decompose ");
            result.append(decomposeFormula.toString());
            result.append('\n');
        }
		return result.toString();
	}

	public String getTitle() {
		return title.getMessage();
	}

}