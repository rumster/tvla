package tvla.iawp.tp.spass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import tvla.formulae.AllQuantFormula;
import tvla.formulae.Formula;
import tvla.formulae.Var;
import tvla.iawp.tp.NativeProcess;
import tvla.iawp.tp.TheoremProverOutput;
import tvla.iawp.tp.TheoremProverResult;
import tvla.iawp.tp.TheoremProverStatus;
import tvla.iawp.tp.base.AbstractTheoremProver;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/**
 * SPASS theorem prover wrapper The following parameters are crucial for SPASS
 * output parsing: -PStatistic=0 disables statistics output -PGiven=0 disables
 * output of the initial given clauses -PProblem=0 disables output of the
 * initial problem -Stdin=1 enables SPASS to read input from stdio
 * 
 * @author Eran Yahav (yahave)
 */
public class Spass extends AbstractTheoremProver {

  private static long spassCalls = 0;

  private final static boolean debug = false;

  private final static boolean allowAssumptionsWithTC = ProgramProperties.getBooleanProperty(
      "tvla.tp.spass.allowAssumptionsWithTC", true);

  private final static boolean useFunctions = ProgramProperties.getBooleanProperty("tvla.tp.spass.useFunctions", false);

  protected String invokeString;

  public Spass() {
    String exec = ProgramProperties.getProperty("tvla.tp.spass.executable", "spass");
    String path = ProgramProperties.getProperty("tvla.tp.spass.path", "c:\\spass-2.1\\src");
    String options = ProgramProperties.getProperty("tvla.tp.spass.parameters", "-PStatistic=0 -PGiven=0 -PProblem=0 -Stdin=1");
    this.invokeString = path + "\\" + exec + " " + options;
    this.translator = SpassTranslation.getInstance();

    addedPredicates = HashSetFactory.make();
    theoremProverPredicates = new TreeSet();
    addedAssumptions = HashSetFactory.make();
    theoremProverAssumptions = HashSetFactory.make();
  }

  /**
   * adds an assumption to the theorem prover by defining it as an axiom.
   */
  public void addAssumption(Formula f) {
    if (!allowAssumptionsWithTC) {
      List tcList = new ArrayList();
      Formula.getAllTC(f, tcList);
      if (!tcList.isEmpty()) {
        Logger.println("WARNING: Assumption with TC ignored!");
        return;
      }
    }

    Formula qf = f.copy();
    for (Iterator it = f.freeVars().iterator(); it.hasNext();) {
      Var currV = (Var) it.next();
      qf = new AllQuantFormula(currV, qf);

    }

    assert qf.freeVars().isEmpty() : "assumption still has free vars";

    String assumptionCmd = translator.translate(qf) + "\n";

    if (debug)
      Logger.println(assumptionCmd);

    theoremProverAssumptions.add(qf);
    execute(assumptionCmd);
  }

  /**
   * @see tvla.iawp.tp.TheoremProver#execute(String)
   */
  public void execute(String tpCommand) {
    if (np == null)
      initNativeProcess();
    initQuery();
    np.send(tpCommand);
  }

  /**
   * adds a predicate to the TheoremProver vocabulary and to its internal proxy
   * object (theoremProverPredicates)
   */
  public void addTCPredicate(Predicate pred) {

    StringBuffer defString = new StringBuffer();
    defString.append("(");
    defString.append("_TC_");
    defString.append(translator.translate(pred));
    defString.append(',');
    defString.append(pred.arity());
    defString.append(")");
    theoremProverPredicates.add(pred);
    execute(defString.toString());
  }

  /**
   * adds a predicate to the TheoremProver vocabulary and to its internal proxy
   * object (theoremProverPredicates)
   */
  public void addPredicate(Predicate pred) {

    StringBuffer defString = new StringBuffer();
    defString.append("(");
    defString.append(translator.translate(pred));
    defString.append(',');
    defString.append(pred.arity());
    defString.append(")");
    theoremProverPredicates.add(pred);
    execute(defString.toString());
  }

  /**
   * adds a predicate to the TheoremProver vocabulary and to its internal proxy
   * object (theoremProverPredicates)
   */
  public void addFunction(Predicate pred) {

    StringBuffer defString = new StringBuffer();
    defString.append("(");
    defString.append(translator.translate(pred));
    defString.append(',');
    defString.append(pred.arity() - 1);
    defString.append(")");
    theoremProverPredicates.add(pred);
    execute(defString.toString());
  }

  public TheoremProverResult prove(Formula f) {
    generateTCEnvironment(f);
    initNativeProcess();
    return prove(translator.translate(f));
  }

  /**
   * Tries to proves a query returning the full TheoremProverOutput
   */
  public TheoremProverOutput proveQuery(String exp) {
    initQuery();
    if (debug)
      Logger.println("Query to Theorem Prover: " + exp);

    StringBuffer query = new StringBuffer();
    query.append(SpassTranslation.startConjectures);
    query.append(exp);
    query.append("\n");
    query.append(SpassTranslation.endConjectures);
    query.append(SpassTranslation.problemFooter);

    currQuery = new SpassOutput(np, query.toString());
    return currQuery;
  }

  public String generateQuery(Collection f) {
    initNativeProcess();
    StringBuffer query = new StringBuffer();
    query.append(np.toString());
    query.append(SpassTranslation.startConjectures);
    for (Iterator iter = f.iterator(); iter.hasNext();) {
      Formula f1 = (Formula) iter.next();
      query.append(translator.translate(f1));
      query.append("\n");
    }
    query.append(SpassTranslation.endConjectures);
    query.append(SpassTranslation.problemFooter);
    return query.toString();
  }

  private void initNativeProcess() {
    np = new NativeProcess("SPass", invokeString);
    currQuery = null;
    spassCalls++;
    from = np.fromStream();
    execute(SpassTranslation.problemHeader);
    adjustVocabulary();
    defineVocabularyPredicates();
    // Vocabulary.addChangeListener(this);
    adjustAssumptions();
    // defineAssumptions();
    // Assumptions.addChangeListener(this);
  }

  protected void defineVocabularyPredicates() {
    String sep = "";
    String comma = ",";
    // first add predicates that are function to
    // the "functions" list in the list of symbols in SPASS.
    // then add the rest of predicates to "predicates" list.
    execute(SpassTranslation.startSymbols);
    if (useFunctions) {
      execute(SpassTranslation.startFunctions);
      for (Iterator it = Vocabulary.allPredicates().iterator(); it.hasNext();) {
        Predicate pred = (Predicate) it.next();
        if (!pred.function())
          continue;
        execute(sep);
        addFunction(pred);
        sep = comma;
      }
      execute(SpassTranslation.endFunctions);
    }

    sep = "";
    comma = ",";
    execute(SpassTranslation.startPredicates);
    for (Iterator it = Vocabulary.allPredicates().iterator(); it.hasNext();) {
      Predicate pred = (Predicate) it.next();
      String baseName = pred.name(); // baseName();
      if ((baseName == "runnable") || (baseName == "isthread") || (baseName == "ready") ||
      // (baseName == "sm") ||
          (baseName == "isNew") || (baseName == "instance") || (baseName == "eps") || (baseName == "ac"))
        continue;
      if (useFunctions)
        if (pred.function())
          continue;
      execute(sep);
      sep = comma;
      addPredicate(pred);
      execute(sep);
      addTCPredicate(pred);
    }
    execute(SpassTranslation.endPredicates);
    execute(SpassTranslation.endSymbols);

  }

  /*
   * private void defineAssumptions() {
   * 
   * execute(SpassTranslation.startAxioms); for (Iterator it =
   * Assumptions.globalAssumptions().iterator(); it.hasNext(); ) { Formula
   * newAssumption = (Formula) it.next(); addAssumption(newAssumption); } for
   * (Iterator it = Assumptions.getInstance().assumptions().iterator();
   * it.hasNext(); ) { Formula newAssumption = (Formula) it.next();
   * addAssumption(newAssumption); } execute(SpassTranslation.endAxioms); }
   */
  /**
   * initializes the current query
   */
  protected void initQuery() {
    if (currQuery != null) {
      currQuery.complete();
      currQuery = null;
    }

  }

  /**
   * have to override since SPASS does not work with incremental queries if
   * vocabulary grew, add its new predicates
   */
  protected void adjustVocabulary() {
    if (!addedPredicates.isEmpty()) {
      for (Iterator it = addedPredicates.iterator(); it.hasNext();) {
        Predicate pred = (Predicate) it.next();
        theoremProverPredicates.add(pred);
      }
    }
    addedPredicates.clear();
  }

  /**
   * adjust assumptions with assumptions added since last query if
   * assumption-pool grew, add its new assumptions
   */
  protected void adjustAssumptions() {
    if (!addedAssumptions.isEmpty()) {
      for (Iterator it = addedAssumptions.iterator(); it.hasNext();) {
        Formula assumption = (Formula) it.next();
        if (validateAssumption(assumption))
          theoremProverAssumptions.add(assumption);
      }
    }
    addedAssumptions.clear();
  }

  /**
   * checks that an assumption is valid for addition
   */
  private boolean validateAssumption(Formula f) {
    if (!allowAssumptionsWithTC) {
      List tcList = new ArrayList();
      Formula.getAllTC(f, tcList);
      if (!tcList.isEmpty()) {
        Logger.println("WARNING: Assumption with TC ignored!");
        return true;
      }
    }

    Formula qf = f.copy();
    for (Iterator it = f.freeVars().iterator(); it.hasNext();) {
      Var currV = (Var) it.next();
      qf = new AllQuantFormula(currV, qf);
    }

    assert qf.freeVars().isEmpty() : "assumption still has free vars";

    return true;
  }

  /**
   * generates assumptions for treating TC formula as a predicate
   */
  public List generateTCEnvironment(Formula f) {
    List tcList = new ArrayList();
    Formula.getAllTC(f, tcList);
    if (tcList.isEmpty())
      return tcList;

    List tcPredicates = generateTCPredicates(tcList);
    addedPredicates.addAll(tcPredicates);
    List tcAssumptions = generateTCAssumptions(tcList);
    addedAssumptions.addAll(tcAssumptions);
    return tcAssumptions;
  }

  public TheoremProverStatus status() {
    return new TheoremProverStatus(spassCalls);
  }

}
