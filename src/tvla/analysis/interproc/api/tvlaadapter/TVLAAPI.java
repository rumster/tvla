/**
 *  The main entry point for PASA when utilized as a library
 */

package tvla.analysis.interproc.api.tvlaadapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tvla.TVLAInitializer;
import tvla.analysis.interproc.api.TVLAAssertion;
import tvla.analysis.interproc.api.TVLAKleeneImpl;
import tvla.analysis.interproc.api.tvlaadapter.abstraction.ITVLAVocabulary;
import tvla.analysis.interproc.api.tvlaadapter.abstraction.TVLAJoin;
import tvla.analysis.interproc.api.tvlaadapter.abstraction.TVLATVS;
import tvla.analysis.interproc.api.tvlaadapter.transformers.TransformerFactory;
import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.analysis.interproc.api.utils.TVLAAPITrace;
import tvla.analysis.interproc.semantics.ActionInstance;
import tvla.api.AbstractTVLAAPI;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAKleene;
import tvla.api.ITVLATVS;
import tvla.api.ITVLATVSIndexIterator;
import tvla.api.ITVLATransformers;
import tvla.core.HighLevelTVS;
import tvla.core.Combine.INullaryCombiner;
import tvla.core.assignments.Assign;
import tvla.core.assignments.AssignKleene;
import tvla.formulae.Formula;
import tvla.language.PTS.ActionMacroAST;
import tvla.language.PTS.PTSParser;
import tvla.language.PTS.SFTAST;
import tvla.language.TVP.SetDefAST;
import tvla.language.TVS.TVSParser;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;
import tvla.util.HashMapFactory;
import tvla.util.HashSetFactory;
import tvla.util.SingleSet;


/** 
 * An highe level interface for TVLA.
 * Allows tvla to get a parameterci specification of the abstract domain which can be initiated
 * by populating the different sets.
 * 
 * @author maon
 * @todo add a "magic number" that allows to correlate the anlaysis specification file(s) 
 * with the client code
 */

public final class TVLAAPI extends AbstractTVLAAPI  {	
  protected static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(4);
  
  /**
   * Sanity check that the api is used only after it is intialized, and that
   * the definition of the analysis does not change after the api is initalized.
   * We use this logic, and not, e.g., initialize the api inside its constructor, 
   * becasue we want to allow creating it via reflection, where parmaeters cannot be passed.
   * (In particular we cannot pass the definition of the analysis)  
   */
  
  private boolean init = false;
  
  /****************************************************************
   * TVLAAPI fields
   ****************************************************************/
  
  private String analysisMainFileName; 
  private String analysisDirName; 
  
  private Map initialSets;
  private TVSRepository repository; 
  private Actions   actions; 
  
  /**
   * The current (API) vocabulary
   */
  protected ITVLAVocabulary vocabulary;
  
  /**
   * Compute the join
   */  
  private TVLAJoin joiner; 
  
  /**
   * Generates the transformers by wrapping an action with an API object. 
   */
  protected TransformerFactory transformerFactory;
  
  /**
   * Applies the transfromers
   */  
  private AbstractInterpreterAdapter applier;
  
  
  public TVLAAPI() {
    // It is important to leave this method empty and do all initializtion 
    // in SetParamtericDomain to allow TVLA to first read the proeprties files
  }
  
  
  /****************************************************************
   * Setting the service provider client 
   ****************************************************************/
   protected ITVLATabulatorServices chaoticEngine;
  
  // poor man constructor
  protected void doSetFrontendServices(
      ITVLATabulatorServices chaoticEngine, 
      ITVLAAPIDebuggingServices client) 
  {
    this.chaoticEngine = chaoticEngine;
    TVLAAPITrace.setClient(client);
    TVLAAPIAssert.setClient(client);
  }	
 
  
  /***************************************************************
   * Setting the current vocabulary
   ***************************************************************/
  
  public void setVocabulary(ITVLAVocabulary voc) {
    vocabulary = voc;
  }
  
  public IVocabulary getVocabulary() {
    return vocabulary;
  }
  
  /****************************************************************
   * Setting the parametric domain + TVLA internal settings
   ****************************************************************/
  
  // poor man constructor 
  public void setParametericDomain(
      String   tvlaEngineType, 
      String[] commandLineArgs,
      String[] propertyFiles,
      String   analysisDirName,
      String   analysisMainFileName, 
      String   outputDir) {
    
    
    this.analysisMainFileName = analysisMainFileName;
    this.analysisDirName = analysisDirName;
    
    TVLAInitializer.initTVLA(
        tvlaEngineType, 
        commandLineArgs, 
        propertyFiles, 
        analysisDirName, 
        outputDir);
    
    initialSets = HashMapFactory.make(16);
    
    actions = new Actions();
    repository = new TVSRepository();
        
    joiner = new TVLAJoin(repository);
    
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(vocabulary != null);
    
    applier = new AbstractInterpreterAdapter(vocabulary, repository, chaoticEngine);
    
    transformerFactory = new TransformerFactory(applier);    
  }
  
  
  /****************************************************************
   * Setting the specific instance (i.e., program dependant) of the 
   * parametric domain
   ****************************************************************/
  
  /* (non-Javadoc)
   * @see tvla.analysis.interproc.export.TVLAAPI#createSet(java.lang.String)
   */
  public boolean createSet(String setName) {
    Set set = (HashSet) initialSets.get(setName);
    if (set != null)
      return false;
    // We can live w/o exceptions
    // client.handleException(new SemanticErrorException("Set " + setName + " was already created!"));
    
    set = HashSetFactory.make();
    initialSets.put(setName, set);
    
    return true;
  }
  
  /* (non-Javadoc)
   * @see tvla.analysis.interproc.export.TVLAAPI#addToSet(java.lang.String, java.lang.String)
   */
  public boolean addToSet(String setName, String e) {
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(!init);
    
    Set set = (Set) initialSets.get(setName);
    if (set == null) 
      return false;
    
    set.add(e);
    
    return true;
  }
  
  
  public boolean instantiateParametericDomain(Set expectedMacros) {
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(!init);
    
    // forces creation of srandard predicares ...
    //TVLAVocabulary voc = TVLAVocabulary.getVocabulary() ;
    //if (TVLAAPIAssert.ASSERT)
    //  TVLAAPIAssert.debugAssert(voc != null);
    
    // Convert the sets to lists as tvla requires
    Map nameToList = HashMapFactory.make();
    for (Iterator itr = initialSets.entrySet().iterator(); itr.hasNext(); ) {
      Map.Entry entry = (Map.Entry) itr.next();
      String setName = (String) entry.getKey();
      List members = new ArrayList((Set) entry.getValue());
      nameToList.put(setName,members);
      
      if (2 < DEBUG_LEVEL) {
        TVLAAPITrace.tracePrintln("Set generated by the client: " + setName + " (has " + members.size() + " elements)");
        for (Iterator memItr = members.iterator(); memItr.hasNext(); ) 
          TVLAAPITrace.tracePrintln("    " + memItr.next());
      }	
    }
    
    
    SetDefAST.allSets.putAll(nameToList);
    
    try {
      SFTAST analysisDef = PTSParser.readAnalysis(analysisMainFileName, analysisDirName);
      analysisDef.compileAll();
    } 
    catch (Exception e) {
      TVLAAPITrace.tracePrintln(" Failed to instanitate domain: " + e);
      init = false;
      return false;
    }
    
    // client.tracePrintln("TVLAAPIImpl.instantiateParametericDomain: all generated sets: " + SetDefAST.allSets.toString());
    if (2 < DEBUG_LEVEL) {
      TVLAAPITrace.tracePrintln("TVLAAPI: Predicates");
      for(Iterator predItr = Vocabulary.allPredicates().iterator(); predItr.hasNext(); ) 
        TVLAAPITrace.tracePrintln(" predicate " + ((Predicate) predItr.next()).description());
    }
    
    
    init = this.actions.init(expectedMacros);
    /*
    if (init) {
      differeningInitialize();
      
      List noDiff = ProgramProperties.getStringListProperty(TVLAAPIConstants.differencingSkipForActionsStr, null);
      if (noDiff != null) {
        Iterator itr = noDiff.iterator();
        while (itr.hasNext()) {
          String action = (String) itr.next();
          differencingSkippedActions.add(action);
          client.tracePrintln("Differencing will not be performed for action " + action);
        }
      }
    }
    */
    
    return init;
  }	
  
  
  
  /****************************************************************
   * Managing the abstract domain 
   ****************************************************************/
  
  public int addTVSToRepository(ITVLATVS tvs) {
    TVLATVS tvsImp = (TVLATVS) tvs;
    
    SingleSet singleTVSset = new SingleSet(true);
    singleTVSset.add(tvsImp.tvs());
    
    int[] tvsId = repository.addTVSs(singleTVSset);
    
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(tvsId == null || tvsId.length == 1);
    
    return tvsId[0];
  }
  
  
  public int[] loadTVSs(String tvsFile) {
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(init);
    
    List initialTVSs = null;
    try {
      initialTVSs = TVSParser.readStructures(tvsFile);
      if (initialTVSs != null) { 
        int[] tvsIds = repository.addTVSs(initialTVSs);
        
        if (0 < DEBUG_LEVEL) {
          TVLAAPITrace.tracePrintln("TVLA: loadTVSs pasrsing file " + tvsFile);
          for (int i=0 ; i<tvsIds.length; i++) 
            TVLAAPITrace.tracePrintln(" index = " + tvsIds[i] + " TVS = " + repository.getTVS(tvsIds[i]));
          TVLAAPITrace.tracePrintln("TVLA: loadTVSs successul pasrsing");
        }
        
        
        return tvsIds;
      }
    } 
    catch (Exception e) {
      TVLAAPITrace.tracePrintln("Failed to read initial TVSs: ");
    }
    
    return null;
  }
  
  
  public int[] join(int[] input, int[] inputToOutputMap) {
    return join(input.length, input, inputToOutputMap);
  }
  
  int[] join(int use, int[] input, int[] inputToOutputMap) {
    return joiner.join(use, input, inputToOutputMap);
  }
  
  public ITVLATVS getTVS(int indx) {
    HighLevelTVS tvs = repository.getTVS(indx);
    if (tvs == null)
      return null;
    
    ITVLATVS ret = new TVLATVS(tvs);
    return ret;
  }
  
  
  
  public int getMaxIndex() {
    return repository.getMaxIndex();
  }
  
  public int getRepositorySize() {
    return repository.getRepositorySize();
  }
  
  
  public int getMappedIndex(ITVLATVS tvs) {
    return repository.getIndex( ((TVLATVS) tvs).tvs() );
  }
  
  
  /****************************************************************
   * Mainting the TVS repository
   ****************************************************************/
  
  public ITVLATVSIndexIterator iterator() {
    return repository.iterator();
  }
  
  
  
  /****************************************************************
   * Interrogating the TVS repository
   ****************************************************************/
  
  public final ITVLAKleene getKleene() {
    return 	TVLAKleeneImpl.getInstance();
  }
  
  
  public ITVLAKleene.ITVLAKleeneValue eval(ITVLATVS tvs, ITVLAAssertion assertion) {
    if (tvs == null)
      return null;
    
    TVLATVS tvsImpl = (TVLATVS) tvs;
    HighLevelTVS tvlaTVS = tvsImpl.tvs();
    
    TVLAAssertion assertionImpl = (TVLAAssertion) assertion;
    Formula formula = assertionImpl.getFormula();
    
    Assign emptyAssign = new Assign();
    Iterator kleeneItr = tvlaTVS.evalFormula(formula, emptyAssign);
    
    if (!kleeneItr.hasNext()) 
      return TVLAKleeneImpl.TVLAKleeneValueImpl.falseKleene;
    
    Object val = kleeneItr.next();
    AssignKleene resKleene = (AssignKleene) val;
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(resKleene.isEmpty());
    TVLAKleeneImpl.TVLAKleeneValueImpl res = TVLAKleeneImpl.TVLAKleeneValueImpl.wrapKleene(resKleene.kleene);
    
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(!kleeneItr.hasNext());
    
    return res;
  }
  
  /****************************************************************
   * Managing the action definitions 
   ****************************************************************/
  
  /**
   * This methos is ivoked by the PTS/SFT parser
   * @param action
   */
  public void actionAddDefinition(ActionMacroAST action) {
    actions.actionAddDefinition(action);
  }
  
  
  protected ActionInstance findOrCreateAction(String actionName, List parameters) {
    if (TVLAAPIAssert.ASSERT)
      TVLAAPIAssert.debugAssert(init);
    
    ActionInstance actionInstance = actions.getActionInstance(actionName, parameters);
    //if (differencing)
    //  differencingGenerateUpdatesIfNeeded(actionInstance); 
    
    return actionInstance;
  }
  
  
  /*************************************************************
   * Monitoring
   *************************************************************/
  
  
  public boolean registerListner(ITVLAApplierListener listener) {
    return applier.registerListner(listener);
  }
  
  public ITVLAAPIStatistics getTVLAStatistics() {
    return new TVLAAPIStatistics(applier.getStatus());
  }
 
  
  /***************************************************************
   * Transformers generation
   ***************************************************************/

  
  /***************************************************************
   * Simple transformers generation
   **/
  
  
  public ITVSUnaryTransformer getUnaryTransformer(String actionName, List parameters) {
    ActionInstance actionInstance = findOrCreateAction(actionName, parameters);
    
    return transformerFactory.getUnaryTransformer(actionInstance);  
  }
  

  public ITVSBinaryTransformer getBinaryTransformer(
      INullaryCombiner nullaryCombiner, String actionName, List parameters) {
    ActionInstance actionInstance = findOrCreateAction(actionName, parameters);
    
    return transformerFactory.getBinaryTransformer(nullaryCombiner, actionInstance);
  }

  
  /***************************************************************
   * Staged transformers generation
   **/
  
  public ITVSUnaryTransformer getUnaryStagedTransformer(INullaryCombiner nullaryCombiner, String actionName, List parameters) {
    ActionInstance actionInstance = findOrCreateAction(actionName, parameters);
    ITVSBinaryTransformer stager = 
      transformerFactory.getBinaryTransformer(nullaryCombiner, actionInstance);
    
    return transformerFactory.getUnaryStagedTransformer(stager);
  }
  
  
  public ITVLATransformers.ITVSBinaryTransformer getBinaryStagedTransformer(
      INullaryCombiner nullaryCombiner1, String stagedActionName, List parametersStaged, 
      INullaryCombiner nullaryCombiner2, String actionName, List parameters) {
    
    ActionInstance firstActionInstance = findOrCreateAction(stagedActionName, parametersStaged);
    ActionInstance secondActionInstance = findOrCreateAction(actionName, parameters);
    
    ITVSBinaryTransformer firstTransformer = 
      transformerFactory.getBinaryTransformer(nullaryCombiner1, firstActionInstance);
    
    ITVSBinaryTransformer secondTransformer = 
      transformerFactory.getBinaryTransformer(nullaryCombiner2, secondActionInstance);

    return transformerFactory.getBinaryStagedTransformer(firstTransformer, secondTransformer);
  }
  
  
  /***************************************************************
   * Composed transformers
   **/
  
  public ITVSUnaryComposedTransformer composedTransformers(ITVSUnaryTransformer[] transformers, int use, String name) {
    return transformerFactory.composedTransformers(transformers, use, name);
  }
  
}
