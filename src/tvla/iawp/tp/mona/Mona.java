package tvla.iawp.tp.mona;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import tvla.analysis.AnalysisStatus;
import tvla.formulae.Formula;
import tvla.iawp.symbolic.PredicateVisitor;
import tvla.iawp.tp.NativeProcess;
import tvla.iawp.tp.TheoremProverOutput;
import tvla.iawp.tp.TheoremProverResult;
import tvla.iawp.tp.TheoremProverStatus;
import tvla.iawp.tp.base.AbstractTheoremProver;
import tvla.predicates.Predicate;
import tvla.util.HashSetFactory;
import tvla.util.Logger;
import tvla.util.ProgramProperties;

/**
 * MONA theorem prover wrapper The following parameters are crucial for Mona
 * output parsing: - q quite mode
 * 
 * @author gretay
 */
public class Mona extends AbstractTheoremProver {

  private static long Calls = 0;

  private static String fileName = "in.mona";

  private final static boolean debug = false;

  protected String invokeString;

  private static String mode = ProgramProperties.getProperty("tvla.tp.mona.mode", "m2l-tree");

  public Mona() {
    String exec = ProgramProperties.getProperty("tvla.tp.mona.executable", "mona");
    String path = ProgramProperties.getProperty("tvla.tp.mona.path", "");
    String options = ProgramProperties.getProperty("tvla.tp.mona.parameters", "-q ");
    this.invokeString = path + exec + " " + options;
    this.translator = MonaTranslation.getInstance();

    addedPredicates = HashSetFactory.make();
    theoremProverPredicates = new TreeSet();
    addedAssumptions = HashSetFactory.make();
    theoremProverAssumptions = HashSetFactory.make();
  }

  public TheoremProverResult prove(Formula f) {
    // initNativeProcess();
    // return prove(translator.translate(f));

    StringBuffer sb = new StringBuffer();
    sb.append(MonaTranslation.problemHeader);
    sb.append(mode + ";\n");
    defineVocabularyPredicates(f, sb);
    sb.append(translator.translate(f));

    if (AnalysisStatus.debug) {
      fileName = "in" + (Calls) + ".mona";
    }
    try {
      FileWriter debugOutputFile = new FileWriter(fileName);
      debugOutputFile.write(sb.toString());
      debugOutputFile.close();
    } catch (IOException e) {
      Logger.fatalError("IO Error opening debug file " + fileName);
    }
    Calls++;
    np = new NativeProcess("Mona", invokeString + " " + fileName);
    from = np.fromStream();
    currQuery = new MonaOutput(np, "");
    if (currQuery.hasNext())
      return (TheoremProverResult) currQuery.next();
    else
      return null;
  }

  protected void defineVocabularyPredicates(Formula f, StringBuffer sb) {
    Collection preds = PredicateVisitor.getAllPredicates(f);
    String comma = ", ";
    sb.append(MonaTranslation.startPredicates);
    sb.append(MonaTranslation.startUnaryPredicates);
    String sep = "var2 ";
    int count = 0;
    for (Iterator it = preds.iterator(); it.hasNext();) {
      Predicate pred = (Predicate) it.next();
      if (pred.arity() != 1)
        continue;
      // if (TwoStoreVocabulary.isSystemPredicate(pred)) continue;
      // if (!(Simulation.isSimulation(pred) &&
      // !TwoStoreVocabulary.isSystemPredicate(pred))) continue;
      sb.append(sep);
      if ((++count % 10) == 0)
        sb.append("\n");
      sb.append(translator.translate(pred));
      sep = comma;
    }
    if (sep.equals(comma))
      sb.append(";\n");
    sb.append(MonaTranslation.startNullaryPredicates);
    sep = "var0 ";
    count = 0;
    for (Iterator it = preds.iterator(); it.hasNext();) {
      Predicate pred = (Predicate) it.next();
      if (pred.arity() != 0)
        continue;
      // if (!Simulation.isSimulation(pred)) continue;
      sb.append(sep);
      if ((++count % 10) == 0)
        sb.append("\n");
      sb.append(translator.translate(pred));
      sep = comma;
    }
    if (sep.equals(comma))
      sb.append(";\n");
    sb.append(MonaTranslation.endPredicates);
    // execute(sb.toString());
  }

  public TheoremProverStatus status() {
    return new TheoremProverStatus(Calls);
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.base.AbstractTheoremProver#proveQuery(java.lang.String)
   */
  public TheoremProverOutput proveQuery(String exp) {

    throw new RuntimeException("Not implemented");
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.base.AbstractTheoremProver#addAssumption(tvla.formulae.Formula)
   */
  public void addAssumption(Formula f) {
    throw new RuntimeException("Not implemented");
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.base.AbstractTheoremProver#execute(java.lang.String)
   */
  public void execute(String tpCommand) {
    throw new RuntimeException("Not implemented");
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.base.AbstractTheoremProver#addPredicate(tvla.predicates.Predicate)
   */
  public void addPredicate(Predicate pred) {
    throw new RuntimeException("Not implemented");
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.base.AbstractTheoremProver#initQuery()
   */
  protected void initQuery() {
    throw new RuntimeException("Not implemented");
  }

  /*
   * (non-Javadoc)
   * 
   * @see tvla.tp.TheoremProver#generateQuery(java.util.Collection)
   */
  public String generateQuery(Collection f) {
    throw new RuntimeException("Not implemented");
  }

  // /**
  // * @see tvla.iawp.tp.TheoremProver#execute(String)
  // */
  // public void execute(String tpCommand) {
  // if (np == null)
  // initNativeProcess();
  // initQuery();
  // np.send(tpCommand);
  // }
  //
  // /**
  // * @see tvla.iawp.tp.TheoremProver#execute(List)
  // */
  // public void execute(List tpCommands) {
  // new NotImplementedException("Mona - execute list of commands");
  // }
  //
  // /**
  // * adds a predicate to the TheoremProver vocabulary
  // * and to its internal proxy object (theoremProverPredicates)
  // */
  // public void addPredicate(Predicate pred) {
  // // unary predicate is a set of individuals --> second order var in mona.
  // //if (pred.arity() == 1) {
  // StringBuffer defString = new StringBuffer();
  // defString.append(translator.translate(pred));
  // theoremProverPredicates.add(pred);
  // execute(defString.toString());
  // //}
  // }
  // /**
  // * Tries to proves a query
  // * returning the full TheoremProverOutput
  // */
  // public TheoremProverOutput proveQuery(String exp) {
  // initQuery();
  // if (debug)
  // Logger.println("Query to Theorem Prover: " + exp);
  // currQuery = new MonaOutput(np, exp + "\n");
  // return currQuery;
  // }
  //	
  // public String generateQuery(Collection f) {
  // initNativeProcess();
  // StringBuffer query = new StringBuffer();
  // query.append(np.toString());
  // for (Iterator iter = f.iterator(); iter.hasNext(); ) {
  // Formula f1 = (Formula) iter.next();
  // query.append(translator.translate(f1));
  // query.append("\n");
  // }
  // return query.toString();
  // }
  //	
  // private void initNativeProcess() {
  // np = new NativeProcess("Mona", invokeString);
  // currQuery = null;
  // Calls++;
  // from = np.fromStream();
  // execute(MonaTranslation.problemHeader);
  // defineVocabularyPredicates();
  // }
  //
  // protected void defineVocabularyPredicates() {
  // StringBuffer sb = new StringBuffer();
  // String comma = ", ";
  // sb.append(MonaTranslation.startPredicates);
  // sb.append(MonaTranslation.startUnaryPredicates);
  // String sep = "var2 ";
  // int count = 0;
  // for (Iterator it =
  // Vocabulary.allUnaryPredicates().iterator();it.hasNext();) {
  // Predicate pred = (Predicate) it.next();
  // boolean b = StructureToFormula.activeVoc.isActive(pred) ||
  // (StructureToFormula.isNodePredicate(pred));
  // b = !b;
  // b = b | TwoStoreVocabulary.isSystemPredicate(pred);
  // if (b) continue;
  // sb.append(sep);
  // if( (++count % 10) == 0)
  // sb.append("\n");
  // sb.append(translator.translate(pred));
  // sep = comma;
  // }
  // if (sep.equals(comma))
  // sb.append(";\n");
  // sb.append(MonaTranslation.startNullaryPredicates);
  // sep = "var0 ";
  // count = 0;
  // for (Iterator it = Vocabulary.allNullaryPredicates().iterator();
  // it.hasNext();) {
  // Predicate pred = (Predicate) it.next();
  // if ((!StructureToFormula.activeVoc.isActive(pred)) ||
  // (TwoStoreVocabulary.isSystemPredicate(pred))) continue;
  // sb.append(sep);
  // if( (++count % 10) == 0)
  // sb.append("\n");
  // sb.append(translator.translate(pred));
  // sep = comma;
  // }
  // if (sep.equals(comma))
  // sb.append(";\n");
  // sb.append(MonaTranslation.endPredicates);
  // execute(sb.toString());
  // }
  // /**
  // * initializes the current query
  // */
  // protected void initQuery() {
  // if (currQuery != null) {
  // currQuery.complete();
  // currQuery = null;
  // }
  // }
  //
  // /**
  // * generates assumptions for treating TC formula
  // * as a predicate
  // */
  // public List generateTCEnvironment(Formula f) {
  // List tcList = new ArrayList();
  // Formula.getAllTC(f, tcList);
  // if (tcList.isEmpty())
  // return tcList;
  //
  // List tcPredicates = generateTCPredicates(tcList);
  // addedPredicates.addAll(tcPredicates);
  // List tcAssumptions = generateTCAssumptions(tcList);
  // addedAssumptions.addAll(tcAssumptions);
  // return tcAssumptions;
  // }
  // /* (non-Javadoc)
  // * @see
  // tvla.iawp.tp.base.AbstractTheoremProver#addAssumption(tvla.formulae.Formula)
  // */
  // public void addAssumption(Formula f) {
  // // TODO Auto-generated method stub
  //		
  // }

}
