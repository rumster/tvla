package tvla.analysis.interproc.api;

import tvla.analysis.interproc.api.javaanalysis.ITVSRepository;
import tvla.analysis.interproc.api.javaanalysis.TVLAJavaAnalysisTVSRepositry;
import tvla.analysis.interproc.api.javaanalysis.abstraction.AbstractionFactory;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.transformers.ITransformersAbstractFactory;
import tvla.analysis.interproc.api.javaanalysis.transformers.TVLATransformersAbstractFactoryBooter;
import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.api.AbstractTVLAAPI;
import tvla.api.AbstractTVLAJavaAdapter;
import tvla.api.ITVLAKleene;
import tvla.api.ITVLATVS;
import tvla.api.ITVLATVSIndexIterator;
import tvla.api.TVLAAnalysisOptions;
import tvla.api.ITVLAAPI.ITVLAAPIStatistics;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAssertion;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAssertionFactory;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaTVSBuilder;
import tvla.api.ITVLAKleene.ITVLAKleeneValue;
import tvla.api.ITVLATransformers.ITVSBinaryTransformer;
import tvla.api.ITVLATransformers.ITVSUnaryTransformer;


/**
 * The bridge connecting the DOMO engine with TVLA.
 * The bridge translates domoPrimitives (e.g., classes, methods, variables, VariableKeys, flowfunctions, factoid numerals etc.) 
 * to TVLA primitives (prediacates, actions, TVSs, and TVSSets) and vice versa
 * @author noam rinetzky
 */
public class TVLAJavaAdapter extends AbstractTVLAJavaAdapter {  
  /**
   * The underlying TVLA interface
   */
  private TVLAAPI tvlaapi; 						// A reference to the (one and only) TVLA backend 
   
 
  /**
   * The analysis components
   */
  private ITransformersAbstractFactory  transformers;
  private ITVSRepository    repository;
  private IJavaAbstraction  abstraction;
  
  
  /////////////////////////////////////////
  ///  Initializtion of the JavaAdapter ///
  /////////////////////////////////////////
  
  
  
  
  public TVLAJavaAdapter() {
  }
  
  protected boolean doSetParam(
      AbstractTVLAAPI tvlaAPI,
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
      int[] analysisCodes) {
    if (tvlaAPI == null || environmentServicesProvider == null || analysisCodes == null)
      return false;
    
    this.tvlaapi = (TVLAAPI) tvlaAPI;
 
    repository = new TVLAJavaAnalysisTVSRepositry(this.tvlaapi);
    abstraction = AbstractionFactory.genAbstraction(tvlaapi, environmentServicesProvider,  analysisCodes[TVLAAnalysisOptions.abstractionEntry]);    
    transformers = TVLATransformersAbstractFactoryBooter.genTransformerFactory(tvlaAPI, environmentServicesProvider,  abstraction.getVocabulary(), analysisCodes[TVLAAnalysisOptions.transformerEntry]);
     
    
    return repository != null && abstraction != null && transformers != null;
  }
  

  
  /********************************************
   * Adapter methods
   ********************************************/
  
  public boolean registerListner(ITVLAJavaApplierListener listener) {
    return tvlaapi.registerListner(listener);
  }
  
  public ITVLAKleene getKleene() {
    return  tvlaapi.getKleene();
  }
  
  public void setParametericDomain(
      String[] commandLineArgs,
      String[] propertyFiles,
      String   anlaysisDirName,
      String   mainAnaysisFileName, 
      String   outputDir) {

    tvlaapi.setParametericDomain(
        "api",
        commandLineArgs,
        propertyFiles,
        anlaysisDirName,
        mainAnaysisFileName, 
        outputDir);   
    
    abstraction.setParametericDomain(
        commandLineArgs,
        propertyFiles,
        anlaysisDirName,
        mainAnaysisFileName);
  }

  
  /********************************************
   * Delegated methods
   ********************************************/ 
 
  /********************************************
   * Program Builder
   ********************************************/ 

  public void addAllocationSite(Object allocSite) {
    abstraction.getMemoryModeler().addAllocationSite(allocSite);
  }

  public void addArrayAllocationSite(Object allocSite) {
    abstraction.getMemoryModeler().addArrayAllocationSite(allocSite);
  }

  public void addArrayClass(Object arrayClass) {
    abstraction.getMemoryModeler().addArrayClass(arrayClass);
  }

  public void addBooleanLocal(Object method, int indx) {
    abstraction.getMemoryModeler().addBooleanLocal(method, indx);
  }

  public void addClass(Object klass) {
    abstraction.getMemoryModeler().addClass(klass);
  }

  public void addField(Object field) {
    abstraction.getMemoryModeler().addField(field);
  }

  public void addMethod(Object method) {
    abstraction.getMemoryModeler().addMethod(method);
  }

  public void addRefLocal(Object method, int indx) {
    abstraction.getMemoryModeler().addRefLocal(method, indx);
  }

  public void addArrayLocal(Object method, int indx) {
    abstraction.getMemoryModeler().addArrayLocal(method, indx);
  }

  public boolean processProgramModel() {
   boolean abstractionInit = abstraction.getMemoryModeler().processProgramModel();
   if (!abstractionInit)
     return false;
   
   boolean transformersInit = transformers.processProgramModel();
   
   return transformersInit;
  }

  /********************************************
   * Factories
   ********************************************/ 
  
  public ITVLAJavaAssertionFactory getAssertionFactory() {
    return abstraction.getAssertionFactory();
  }

  public ITVLAKleeneValue eval(ITVLATVS tvs, ITVLAJavaAssertion assertion) {
    return abstraction.eval(tvs, assertion);
  }

  public ITVLAJavaTVSBuilder getJavaTVSBuilder() {
    return abstraction.getJavaTVSBuilder();
  }

  /********************************************
   * Repository
   ********************************************/ 

  public int addTVSToRepository(ITVLATVS tvs) {
    return repository.addTVSToRepository(tvs);
  }

  public int getMappedIndex(ITVLATVS tvs) {
    return repository.getMappedIndex(tvs);
  }

  public int getMaxIndex() {
    return repository.getMaxIndex();
  }

  public int getRepositorySize() {
    return repository.getRepositorySize();
  }

  public ITVLATVS getTVS(int indx) {
    return repository.getTVS(indx);
  }

  public ITVLATVSIndexIterator iterator() {
    return repository.iterator();
  }

  public int[] join(int[] input, int[] inputToOutputMap) {
    return repository.join(input, inputToOutputMap);
  }

  public int[] loadTVSsIntoRepository(String tvsFile) {
    return repository.loadTVSsIntoRepository(tvsFile);
  }

  
  /********************************************
   * transformers
   ********************************************/ 

  public ITVSUnaryTransformer makeAllocFlowFunction(Object method, int lhsRef, Object allocationSite) {
    return transformers.makeAllocFlowFunction(method, lhsRef, allocationSite);
  }

  public ITVSUnaryTransformer makeArrayAllocFlowFunction(Object method, int lhsRef, Object allocationSite) {
    return transformers.makeArrayAllocFlowFunction(method, lhsRef, allocationSite);
  }

  public ITVSUnaryTransformer makeArrayBooleanGetFlowFunction(Object method, int lhsRef, int rhsRef) {
    return transformers.makeArrayBooleanGetFlowFunction(method, lhsRef, rhsRef);
  }

  public ITVSUnaryTransformer makeArrayBooleanPutFlowFunction(Object method, int lhsRef, int rhsRef) {
    return transformers.makeArrayBooleanPutFlowFunction(method, lhsRef, rhsRef);
  }

  public ITVSUnaryTransformer makeArrayGetFlowFunction(Object method, int lhsRef, int rhsRef) {
    return transformers.makeArrayGetFlowFunction(method, lhsRef, rhsRef);
  }

  public ITVSUnaryTransformer makeArrayLengthFlowFunction(Object method, int lhs, int rhsRef) {
    return transformers.makeArrayLengthFlowFunction(method, lhs, rhsRef);
  }

  public ITVSUnaryTransformer makeArrayPutFlowFunction(Object method, int lhsRef, int rhsRef) {
    return transformers.makeArrayPutFlowFunction(method, lhsRef, rhsRef);
  }

  public ITVSUnaryTransformer makeAssignConstToBooleanFlowFunction(Object method, int lhsBool, boolean val) {
    return transformers.makeAssignConstToBooleanFlowFunction(method, lhsBool, val);
  }

  public ITVSUnaryTransformer makeAssignNullToReferenceFlowFunction(Object method, int lhsRef) {
    return transformers.makeAssignNullToReferenceFlowFunction(method, lhsRef);
  }

  public ITVSUnaryTransformer makeAssignUnknownToBooleanFlowFunction(Object method, int lhsBool) {
    return transformers.makeAssignUnknownToBooleanFlowFunction(method, lhsBool);
  }

  public ITVSBinaryTransformer makeCallAndExitToReturnBinaryTransformer(Object caller, Object invocation) {
    return transformers.makeCallAndExitToReturnBinaryTransformer(caller, invocation);
  }

  public ITVSUnaryTransformer makeCalleeEntryTransformer(Object callee) {
    return transformers.makeCalleeEntryTransformer(callee);
  }

  public ITVSUnaryTransformer makeCalleeExitTransformer(Object callee) {
    return transformers.makeCalleeExitTransformer(callee);
  }

  public ITVSUnaryTransformer makeCallerPostCallTransformer(Object caller, Object invocation) {
    return transformers.makeCallerPostCallTransformer(caller, invocation);
  }

  public ITVSUnaryTransformer makeCallerPreCallTransformer(Object caller, Object invocation) {
    return transformers.makeCallerPreCallTransformer(caller, invocation);
  }

  public ITVSUnaryTransformer makeCallToEntryTransformer(Object caller, Object invocation) {
    return transformers.makeCallToEntryTransformer(caller, invocation);
  }

  public ITVSUnaryTransformer makeCheckBooleanFlowFunction(Object method, int rhsBool, boolean isTrue) {
    return transformers.makeCheckBooleanFlowFunction(method, rhsBool, isTrue);
  }
  
  public ITVSUnaryTransformer makeCompareBooleansFlowFunction(Object method, int rhsBool1, int rhsBool2, boolean eq) {
    return transformers.makeCompareBooleansFlowFunction(method, rhsBool1, rhsBool2, eq);
  }

  public ITVSUnaryTransformer makeCompareReferencesFlowFunction(Object method, int rhsRef1, int rhsRef2, boolean eq) {
    return transformers.makeCompareReferencesFlowFunction(method, rhsRef1, rhsRef2, eq);
  }

  public ITVSUnaryTransformer makeCompareReferenceToNullFlowFunction(Object method, int rhsRef, boolean eq) {
    return transformers.makeCompareReferenceToNullFlowFunction(method, rhsRef, eq);
  }

  public ITVSUnaryTransformer makeCopyBooleanToBooleanFlowFunction(Object method, int lhsBool, int rhsBool) {
    return transformers.makeCopyBooleanToBooleanFlowFunction(method, lhsBool, rhsBool);
  }

  public ITVSUnaryTransformer makeCopyReferenceToReferenceFlowFunction(Object method, int lhsRef, int rhsRef) {
    return transformers.makeCopyReferenceToReferenceFlowFunction(method, lhsRef, rhsRef);
  }

  public ITVSUnaryTransformer makeGetInstanceBooleanFieldFlowFunction(Object method, int lhsBool, int rhsRef, Object theInstanceField) {
    return transformers.makeGetInstanceBooleanFieldFlowFunction(method, lhsBool, rhsRef, theInstanceField);
  }

  public ITVSUnaryTransformer makeGetInstanceReferenceFieldFlowFunction(Object method, int lhsRef, int rhsRef, Object theInstanceField) {
    return transformers.makeGetInstanceReferenceFieldFlowFunction(method, lhsRef, rhsRef, theInstanceField);
  }

  public ITVSUnaryTransformer makeGetStaticBooleanFieldFlowFunction(Object method, int lhsBool, Object theStaticField) {
    return transformers.makeGetStaticBooleanFieldFlowFunction(method, lhsBool, theStaticField);
  }

  public ITVSUnaryTransformer makeGetStaticReferenceFieldFlowFunction(Object method, int lhsRef, Object theStaticField) {
    return transformers.makeGetStaticReferenceFieldFlowFunction(method, lhsRef, theStaticField);
  }

  public ITVSUnaryTransformer makePutInstanceBooleanFieldFlowFunction(Object method, int lhsRef, Object theInstanceField, int rhsBool) {
    return transformers.makePutInstanceBooleanFieldFlowFunction(method, lhsRef, theInstanceField, rhsBool);
  }

  public ITVSUnaryTransformer makeNullifyInstanceReferenceFieldFlowFunction(
      Object method, int lhsRef, Object theInstanceField) {
    return transformers.makeNullifyInstanceReferenceFieldFlowFunction(method, lhsRef, theInstanceField);    
  }

  public ITVSUnaryTransformer makePutInstanceReferenceFieldFlowFunction(Object method, int lhsRef, Object theInstanceField, int rhsRef) {
    return transformers.makePutInstanceReferenceFieldFlowFunction(method, lhsRef, theInstanceField, rhsRef);
  }

  public ITVSUnaryTransformer makePutStaticBooleanFieldFlowFunction(Object theStaticField, Object method, int rhsBoolean) {
    return transformers.makePutStaticBooleanFieldFlowFunction(theStaticField, method, rhsBoolean);
  }
  
  public ITVSUnaryTransformer makeNullifyStaticReferenceFieldFlowFunction(
      Object theStaticField, Object method) {
    return transformers.makeNullifyStaticReferenceFieldFlowFunction(theStaticField, method);    
  }

  public ITVSUnaryTransformer makePutStaticReferenceFieldFlowFunction(Object theStaticField, Object method, int rhsRef) {
    return transformers.makePutStaticReferenceFieldFlowFunction(theStaticField, method, rhsRef);
  }

 
  

  
  public ITVSUnaryTransformer makeReturnValueFlowFunction(Object method, int retValIndex) {
    return transformers.makeReturnValueFlowFunction(method, retValIndex);
  }
  
  

  
  /************************************************
   * Monitoring
   ************************************************/
  
  
  public static class TVLAJavaStatistics implements ITVLAJavaStatistics {
    protected final ITVLAAPIStatistics apiStats;
    
    public TVLAJavaStatistics(ITVLAAPIStatistics apiStats) {
      this.apiStats = apiStats;
    }
    
    public String toString() {
      return apiStats.toString();
    }    
  }
  
  public ITVLAJavaStatistics getTVLAJavaStatistics() {
    return new TVLAJavaStatistics(tvlaapi.getTVLAStatistics());
  }
  
}