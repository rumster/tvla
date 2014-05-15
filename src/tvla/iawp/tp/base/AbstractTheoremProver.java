package tvla.iawp.tp.base;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import tvla.formulae.AllQuantFormula;
import tvla.formulae.AndFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.OrFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.TransitiveFormula;
import tvla.formulae.Var;
import tvla.iawp.tp.AssumptionsChangeListener;
import tvla.iawp.tp.NativeProcess;
import tvla.iawp.tp.TheoremProver;
import tvla.iawp.tp.TheoremProverOutput;
import tvla.iawp.tp.TheoremProverResult;
import tvla.iawp.tp.TheoremProverStatus;
import tvla.iawp.tp.Translation;
import tvla.iawp.tp.util.PeekableInputStream;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.predicates.VocabularyChangeListener;
import tvla.util.HashSetFactory;
import tvla.util.ProgramProperties;

/**
 * Skeletal implementation of a theorem prover wrapper.
 * @author Eran Yahav (eyahav)
 */
public abstract class AbstractTheoremProver 
	implements TheoremProver, VocabularyChangeListener, AssumptionsChangeListener {
	/**
	 * native process running the theorem prover
	 */
	protected NativeProcess np;
	/**
	 * input stream for reading theorem prover output
	 */
	protected PeekableInputStream from;
	/**
	 * result of the current (possibly ongoing) query
	 * should be reset for each new query sent to the theorem prover
	 */
	protected TheoremProverOutput currQuery;
	/**
	 * proxy set of the predicates already defined by the 
	 * theorem prover
	 */
	protected SortedSet theoremProverPredicates;
	/**
	 * dirty-set of predicates added by TVLA and not yet
	 * defined by the theorem prover
	 * this set should be flushed (and cleared) whenever
	 * a query is issued to the theorem prover
	 */
	protected Set addedPredicates;
	/**
	 * proxy set of the assumptions already defined by the
	 * theorem prover
	 */
	protected Set theoremProverAssumptions;
	/**
	 * dirty-set of assumptions added by TVLA and not yet
	 * defined by the theorem prover.
	 */
	protected Set addedAssumptions;
	/**
	 * translator from internal formulae represntation
	 * to theorem-prover representation
	 */
	protected Translation translator;

	/**
	 * Minimum model size, in case of enumeration
	 */
	protected int minModelSize = ProgramProperties.getIntProperty("tvla.counterexample.minModelSize",2);
	
	/**
	 * Maximum model size, in case of enumeration
	 */
	protected int maxModelSize = ProgramProperties.getIntProperty("tvla.counterexample.maxModelSize",3);
	

	/**
	 * initialization of the theorem prover
	 */
	public void initialize(String npName, String invokeString) {
		np = new NativeProcess(npName, invokeString);
		from = np.fromStream();
		addedPredicates = HashSetFactory.make();
		theoremProverPredicates = new TreeSet();
		addedAssumptions = HashSetFactory.make();
		theoremProverAssumptions = HashSetFactory.make();
	}

	/**
	 * @see tvla.iawp.tp.TheoremProver#prove(Formula)
	 */
	abstract public TheoremProverResult prove(Formula f);

	/**
	 * prove query as a theorem prover string
	 */
	abstract public TheoremProverOutput proveQuery(String exp);

	/**
	 * @see tvla.iawp.tp.TheoremProver#addAssumption(Formula)
	 */
	abstract public void addAssumption(Formula f);

	/**
	 * @see tvla.iawp.tp.TheoremProver#execute(String)
	 */
	abstract public void execute(String tpCommand);

	/**
	 * adds a predicate to the TheoremProver vocabulary
	 */
	abstract public void addPredicate(Predicate pred);

	/**
	 * returns the status of the theorem prover
	 * for statistics purposes
	 */
	abstract public TheoremProverStatus status();

	/**
	 * initialized current theorem prover query
	 */
	abstract protected void initQuery(); 

	/**
	 * @see tvla.iawp.tp.TheoremProver#getAssumptions()
	 */
	public Set getAssumptions() {
		return theoremProverAssumptions;
	}

	/**
	 * close the native-process
	 */
	public void close() {
		np.close();
	}

	/**
	 * adjust predicates that were added since the 
	 * last theorem prover adjustment
	 */
	protected void adjustVocabulary() {
		// if vocabulary grew, add its new predicates
		if (!addedPredicates.isEmpty()) {
			for (Iterator it = addedPredicates.iterator(); it.hasNext();) {
				Predicate pred = (Predicate) it.next();
				addPredicate(pred);
			}
		}
		addedPredicates.clear();
	}

	/**
	 * adjust assumptions that were added since the last
	 * theorem prover adjustment
	 */
	protected void adjustAssumptions() {
		// if assumption-pool grew, add its new assumptions
		if (!addedAssumptions.isEmpty()) {
			for (Iterator it = addedAssumptions.iterator(); it.hasNext();) {
				Formula assumption = (Formula) it.next();
				addAssumption(assumption);
			}
		}
		addedAssumptions.clear();
	}

	/**
	 * handle a predicate addition to the vocabulary
	 */
	public void predicateAddedEvent(Predicate p) {
		addedPredicates.add(p);
	}

	/**
	 * handle an assumption addition to the assumption pool
	 */
	public void assumptionAddedEvent(Formula f) {
		addedAssumptions.add(f);
	}

	/**
	 * handle an assumption removal from the assumption pool
	 */
	public void assumptionRemovedEvent(Formula f) {
		addedAssumptions.remove(f);
		theoremProverAssumptions.remove(f);
	}

	/**
	 * issue a query to the theorem prover
	 */
	protected TheoremProverResult prove(String s) {
		TheoremProverResult result = null;
		for (TheoremProverOutput results = proveQuery(s); results.hasNext();) {
			TheoremProverResult currResult =
				(TheoremProverResult) results.next();
			result = currResult;
		}
		return result;
	}

	public Iterator getModels (Formula f)
	{
		return proveQuery(translator.translate(f));	
	}


	/**
	 * Executes a list of theorem prover commands
	 * given as a list of strings.
	 * The thing here is to initQuery just once for
	 * all the commands.
	 */
	public void execute(List tpCommands) {
		StringBuffer commandSequence = new StringBuffer();

		for (Iterator it = tpCommands.iterator(); it.hasNext();) {
			commandSequence.append((String) it.next());
		}

		initQuery();
		np.send(commandSequence.toString());
	}

	/**
	 * returns a list of predicates to be added to 
	 * the vocabulary.
	 */
	protected List generateTCPredicates(List tcList) {
		List result = new ArrayList();

		for (Iterator tcIt = tcList.iterator(); tcIt.hasNext();) {
			TransitiveFormula currTcFormula = (TransitiveFormula) tcIt.next();
			String tcPredName = translator.tcPredicateName(currTcFormula);
			Predicate tcPred =
				Vocabulary.createPredicate(
					tcPredName,
					//Collections.EMPTY_LIST,
					2);
			result.add(tcPred);

		}
		return result;
	}

	/**
	 * returns a list of TC assumptions for the formula f
	 * assumes that TC predicates were already created in vocabulary
	 */
	protected List generateTCAssumptions(List tcList) {
		List result = new ArrayList();

		Var v1 = Var.allocateVar();
		Var v2 = Var.allocateVar();
		Var v3 = Var.allocateVar();

		for (Iterator tcIt = tcList.iterator(); tcIt.hasNext();) {
			TransitiveFormula currTcFormula = (TransitiveFormula) tcIt.next();

			String tcPredName = translator.tcPredicateName(currTcFormula);
			Predicate tcPred = Vocabulary.getPredicateByName(tcPredName);

			Formula transitivity =
				new AllQuantFormula(
					v1,
					new AllQuantFormula(
						v2,
						new AllQuantFormula(
							v3,
							new OrFormula(
								new NotFormula(
									new AndFormula(
										new PredicateFormula(tcPred, v1, v2),
										new PredicateFormula(tcPred, v2, v3))),
								new PredicateFormula(tcPred, v1, v3)))));
			Formula seed =
				new AllQuantFormula(
					v1,
					new AllQuantFormula(
						v2,
						new OrFormula(
							new NotFormula(currTcFormula.subFormula()),
							new PredicateFormula(tcPred, v1, v2))));

			result.add(transitivity);
			result.add(seed);
		}
		return result;
	}

	/** 
	 * @author Guy Erez
	 */
	public void setMinModelSize(int n)
	{
		minModelSize = n;
	}
	
	/**
	 * @author Guy Erez
	 */
	public void setMaxModelSize(int n)
	{
		maxModelSize = n;
	}

	/* (non-Javadoc)
	 * @see tvla.iawp.tp.TheoremProver#generateTCEnvironment(tvla.formulae.Formula)
	 */
	public List generateTCEnvironment(Formula formula1) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see tvla.iawp.tp.TheoremProver#generateQuery(java.util.Collection)
	 */
	public String generateQuery(Collection singleton) {
		// TODO Auto-generated method stub
		return null;
	}
	
		
	/**
	 * @return
	 */
	public Translation getTranslator() {
		return translator;
	}

}
