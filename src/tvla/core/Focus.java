package tvla.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import tvla.core.assignments.Assign;
import tvla.formulae.Formula;
import tvla.util.ProgramProperties;

/** An abstract class representing the Focus operation as defined in the Shape Analysis
 * article. 
 * @see tvla.core.TVS
 * @see tvla.formulae.Formula
 * @author Tal Lev-Ami
 */
public abstract class Focus {
	protected static final boolean FILTER_IN_FOCUS = ProgramProperties.getBooleanProperty("tvla.focus.filterDuringFocus", false);
    protected static final boolean COERCE_IN_FOCUS = ProgramProperties.getBooleanProperty("tvla.focus.coerceDuringFocus", false);
    
    /** Signals whether there is a need to focus on maybe-active nodes.
	 * This is turned off by default to some optimizations possible.
	 * This is only needed if maybe-active nodes are can possibly be
	 * created, usually when single-graph join is used.
	 * @author Roman Manevich.
	 * @since tvla-2-alpha November 18 2002, Initial creation.
	 */
	public static boolean needToFocusOnActive = false;
	
	public static void reset() {
		needToFocusOnActive = false;
	}
	
	/** Pre-registers the given focus formula. 
	 * This operation is optional.
	 */
	public void registerFormula(Formula formula) {
	}

	/** Returns a collection of structures, such that the specified formula evaluates
	 * to either true or false for each assignment of the free variables, and every
	 * concrete structure embedded in the given structure is embeded in one of the 
	 * returned structures.
	 * @param structure A structure to focus.
	 * @param formula A formula that should be brought into "focus".
	 * @return A collection of structures.
	 */
	public abstract Collection focus(TVS structure, Formula formula);

    public static Collection focus(TVS origStructure, List<Formula> formulae) {
        return focus(origStructure, formulae, null);
    }	
    
	/** Focuses the given structure using the given focus formulae in order.
	 * @param formula 
	 * @param structure the structure to focus.
	 * @param formula the formulae that should evaluate to either true or false.
	 */
	public static Collection focus(TVS origStructure, List<Formula> formulae, Formula filterFormula) {
		Collection<TVS> structures = new ArrayList<TVS>();
		structures.add(origStructure);
		origStructure.commit();

        for (Iterator<Formula> formulaeIter = formulae.iterator(); formulaeIter.hasNext(); ) {
            Formula formula = formulaeIter.next();
            Collection<TVS> answer = new ArrayList<TVS>();
            boolean coerceInFocus = COERCE_IN_FOCUS && formulaeIter.hasNext();
            for (Iterator<TVS> iter = structures.iterator(); iter.hasNext(); ) {
                HighLevelTVS structure = (HighLevelTVS) iter.next(); // Cheating...
                if (FILTER_IN_FOCUS && filterFormula != null) {
                    if (!structure.evalFormula(filterFormula, Assign.EMPTY).hasNext()) {
                        continue;
                    }
                }                
                Collection<HighLevelTVS> focused = structure.focus(formula);
                for (HighLevelTVS fstructure : focused) {
                    if (coerceInFocus) {
                        fstructure.setOriginalStructure(structure);
                        if (!fstructure.coerce()) continue;
                    } 
                    answer.add(fstructure); 
                }
            }
            structures = answer;
        }
        for (TVS structure : structures) {
            structure.setOriginalStructure(origStructure);
        }

		return structures;
	}
	
	/** Focuses all the structures in the given collection using all the given focus
	 * formulae.
	 * @param structures the structures to focus.
	 * @param filterFormula 
	 * @param formula the formulae that should evaluate to either true or false.
	 */
	public static Collection<TVS> focusAll(Collection<TVS> structures, List<Formula> formulae, Formula filterFormula) {
		return structures;
	}
}
