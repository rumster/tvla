package tvla.core.base;

import tvla.core.TVS;
import tvla.predicates.DynamicVocabulary;

/** An implementation of the high-level TVS interface based on BaseTVS.
 */
public class BaseHighLevelTVS extends BaseTVS {
  /** Constructs and initializes an empty NaseHighLevelTVS.
   */
  public BaseHighLevelTVS() {
    super();
  }
  
  public BaseHighLevelTVS(DynamicVocabulary vocabulary) {
      super(vocabulary);
  }
  
  /** Conversion constructor.
   * @author Roman Manevich.
   * @since 16.9.2001 Initial creation.
   */
  public BaseHighLevelTVS(TVS other) {
    super(other);
  }
  
  /** Returns a copy of this structure.
   * @author Roman Manevich.
   */
  public BaseHighLevelTVS copy() {
    BaseHighLevelTVS newValue = new BaseHighLevelTVS(this);
    return newValue;
  }
  
  /** Bounds the structure in-place.
   */
  public void blur() {
	commit();
    BaseBlur.defaultBaseBlur.blur(this);		
  }
  
  // The following is deprecated.
  
  /** @return An iterator to a set of assignments that satisfy the formula.
   * An optional operation.
   * @see tvla.core.generic.EvalFormula
   public Iterator evalFormula(Formula formula, Assign partialAssignment) {
   if (formula instanceof PredicateFormula &&
   ((PredicateFormula) formula).predicate().arity() == 1) {
   boolean allBound = partialAssignment.bound().containsAll(formula.freeVars());
   return new UnaryPredicateIterator((PredicateFormula) formula,
   partialAssignment,
   allBound);
   
   }
   else {		
   return EvalFormula.evalFormula(this, formula, partialAssignment);
   }
   }
   */
  
  /*
  private class UnaryPredicateIterator extends AssignKleeneIterator {
    public UnaryPredicateIterator(PredicateFormula formula, 
        Assign partial, 
        boolean full) {
    }
    
    public boolean hasNext() {
      return false;
    }
    
    public Object next() {
      throw new UnsupportedOperationException();
    }
  }
  
  protected Iterator evalUnaryPredicateFormula(final PredicateFormula formula, 
      final Assign partial, 
      final boolean full) {
    final ConcretePredicate predicate = (ConcretePredicate) predicates.get(formula.predicate());
    if (predicate == null)
      return java.util.Collections.EMPTY_SET.iterator();
    
    return new AssignKleeneIterator() {
      private Var variable = formula.getVariable(0);
      private Iterator iterator = null;
      
      public boolean hasNext() {
        if (hasResult) {
          return true;
        }
        if (iterator == null) {
          result.put(partial);
          if (full) {
            iterator = new AssignIterator();
            // Attempt to fix TC Cache bug
            formula.prepare(BaseHighLevelTVS.this);
            result.kleene = formula.eval(BaseHighLevelTVS.this, partial);
            hasResult = (result.kleene != Kleene.falseKleene);
            return hasResult;
          } 
          else {
            iterator = predicate.iterator();
          }
        }
        
        while (iterator.hasNext()) {
          Map.Entry entry = (Map.Entry) iterator.next();
          Node node = (Node) entry.getKey();
          Kleene value = (Kleene) entry.getValue();
          result.put(variable, node);
          result.kleene = value;
          hasResult = true;
          return true;
        }
        return false;
      }		
    };
  }
  */
  /** @return An iterator to a set of assignments that evaluate to the
   * specified value.
   * An optional operation.
   * @see tvla.core.generic.EvalFormulaForValue
   public Iterator evalFormulaForValue(Formula formula, 
   Assign partialAssignment, 
   Kleene desiredValue) {
   return EvalFormulaForValue.EvalFormulaForValue(this, 
   formula, 
   partialAssignment, 
   desiredValue);
   }
   */
  
}