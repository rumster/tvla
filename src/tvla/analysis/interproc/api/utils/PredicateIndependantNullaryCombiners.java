//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.utils;

import tvla.core.Combine.INullaryCombiner;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;


/**
 * Repositrory for convinent functions from: predicate name -> Kleene x Kleene -> Kleene
 * Used to combineth values of nullaris in binary operations
 * @author maon
 *
 */

public class PredicateIndependantNullaryCombiners {
  /********************************************************
   * The (singleton) or combniner
   ********************************************************/
  protected static final INullaryCombiner orNullaries = new INullaryCombiner() {
    public Kleene combineNumarryPredicate(Predicate pred, Kleene firstVal, Kleene secondVal) {
      Kleene val = Kleene.or(firstVal, secondVal);
      return val;
    }
  };
  
  /**
   * A nullary combiners that returns the or of its two arguments
   */  
  public static INullaryCombiner getOrNullaries() {
    return orNullaries;
  }

  
  /********************************************************
   * The (singleton) projectFirst combniner
   ********************************************************/
  protected static final INullaryCombiner proejctFirstNullaries = new INullaryCombiner() {
    public Kleene combineNumarryPredicate(Predicate pred, Kleene firstVal, Kleene secondVal) {
      return firstVal;
    }
  };

  /**
   * A nullary combiners that returns the first of its two arguments
   */
  public static INullaryCombiner getProejctFirstNullaries() {
    return proejctFirstNullaries;
  }

  
  /********************************************************
   * The (singleton) projectSecond combniner
   ********************************************************/

  public static final INullaryCombiner proejctSecondNullaries = new INullaryCombiner() {
    public Kleene combineNumarryPredicate(Predicate pred, Kleene firstVal, Kleene secondVal) {
      return secondVal;
    }
  };

  /**
   * A nullary combiners that returns the second of its two arguments
   */
  protected static INullaryCombiner getProejctSecondNullaries() {
    return proejctSecondNullaries;
  }

  /********************************************************
   * The (singleton) top combniner
   ********************************************************/

  protected static final INullaryCombiner top = new INullaryCombiner() {
    public Kleene combineNumarryPredicate(Predicate pred, Kleene firstVal, Kleene secondVal) {
      return Kleene.unknownKleene;
    }
  };

  /**
   * A nullary combiners that returns the 1/2
   */
  public static INullaryCombiner getTop() {
    return top;
  }
  
  /********************************************************
   * The (singleton) 0 combniner
   ********************************************************/

  protected static final INullaryCombiner zero = new INullaryCombiner() {
    public Kleene combineNumarryPredicate(Predicate pred, Kleene firstVal, Kleene secondVal) {
      return Kleene.falseKleene;
    }
  };

  /**
   * A nullary combiners that returns 0
   */
  public static INullaryCombiner getZero() {
    return zero;
  }

  /********************************************************
   * The (singleton) 1 combniner
   ********************************************************/

  protected static final INullaryCombiner one = new INullaryCombiner() {
    public Kleene combineNumarryPredicate(Predicate pred, Kleene firstVal, Kleene secondVal) {
      return Kleene.trueKleene;
    }
  };

  /**
   * A nullary combiners that returns 1
   */
   public static INullaryCombiner getOne() {
    return one;
  }

}


