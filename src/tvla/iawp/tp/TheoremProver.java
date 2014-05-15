package tvla.iawp.tp;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import tvla.formulae.Formula;
import tvla.predicates.Predicate;

/**
 * Interface to a theorem prover. allows to query the theorem prover for the
 * validity of a given FOTC formula, and manage a set of assumptions used by the
 * theorem prover to prove the given claims.
 * 
 * @author Eran Yahav
 * @author Guy Erez
 * @author Greta Yorsh
 */
public interface TheoremProver {
  /**
   * attempt to prove a first-order formula.
   * 
   * @param f -
   *          an FOTC formula
   * @return a TheoremProverResult denoting whether the formula is valid under
   *         the assumptions.
   */
  public TheoremProverResult prove(Formula f);

  /**
   * Enumerate over models satisfying the given formula
   * 
   * @param f
   *          formula
   * @return a List of the models found.
   * @author Guy Erez
   */
  public Iterator getModels(Formula f);

  /**
   * adds an assumption to the theorem prover "known facts" the assumption is
   * given as a first-order formula that is assumed to be valid.
   * 
   * @author Eran Yahav
   * @param f -
   *          an FOTC formula to be used as an assumption
   */
  public void addAssumption(Formula f);

  /**
   * returns the set of assumptions used by the theorem prover. This does not
   * return the initial set of axioms used by the theorem prover, but only the
   * user added custom assumption.
   * 
   * @author Eran Yahav
   * @return a set of formulae used as assumptions.
   */
  public Set getAssumptions();

  /**
   * adds a predicate to the theorem-prover's vocabulary
   * 
   * @param p -
   *          a Predicate to be added to the theorem prover's vocabulary
   */
  public void addPredicate(Predicate p);

  /**
   * executes a native command of the theorem prover.
   * 
   * @param tpCommand -
   *          a String containing a valid theorem-prover command
   */
  public void execute(String tpCommand);

  /**
   * return theorem-prover status object for statistics purposes
   */
  public TheoremProverStatus status();

  /**
   * Sets minimum model size, in case of model enumeration
   * 
   * @author Guy Erez
   */
  public void setMinModelSize(int min);

  /**
   * Sets maximum model size, in case of model enumeration
   * 
   * @author Guy Erez
   */
  public void setMaxModelSize(int max);

  /**
   * @param formula1
   * @return
   */
  public List generateTCEnvironment(Formula formula1);

  /**
   * @param singleton
   * @return
   */
  public String generateQuery(Collection singleton);

}
