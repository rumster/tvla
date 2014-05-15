package tvla.api;

import tvla.api.ITVLAAPI.ITVLAAPIStatistics;
import tvla.api.ITVLAAPI.ITVLAApplierListener;
import tvla.api.ITVLAKleene.ITVLAKleeneValue;


public interface ITVLAJavaAnalyzer {	
  public static interface ITVLAMemoryModeler {
    
    /**
     * Adds a new allocation site to TVLA.
     * allocSite should represnet an allocation site at the program model
     * in which NOT an array is allocated
     * @param allocSite
     */
    public abstract void addAllocationSite(Object allocSite);
    
    /**
     * Adds a new array allocation site to TVLA.
     * allocSite should represnet an allocation site at the program model
     * in which an array is allocated
     * @param allocSite
     */
    public abstract void addArrayAllocationSite(Object allocSite);
    
    /**
     * Add a new array class that the program allocates / uses
     * @param array
     */
    public abstract void addArrayClass(Object arrayClass);
    
    /**
     * Adds a local variable of type boolean and index indx to method 
     * @param method
     * @param indx
     */
    public abstract void addBooleanLocal(Object method, int indx);
    
    /**
     * Adds a local variable of type (reference to) array and index indx to method 
     * @param method
     * @param indx
     */
    public abstract void addArrayLocal(Object method, int indx);
    
    /**
     * Add a new class that the program allocates / uses
     * @param klass
     */
    public abstract void addClass(Object klass);
    
    /**
     * Add a new field definitin of a class that the program allocates / uses
     * @param array
     */
    public abstract void addField(Object field);
    
    /**
     * Also adds all the method formal parameters
     * @param method
     */
    public abstract void addMethod(Object method);
    
    /**
     * We have 2 addTypeLocal methods becasue we still do ot have a good way
     * to infer whether a variable is a blean reference or something else
     * @param method
     * @param indx
     */
    
    public abstract void addRefLocal(Object method, int indx);
    
    /**
     * Give TVLA a chane to digest the program model + analysis definition, and initialize
     * its internal data strucutres.
     * Cannot add defition after this call (e.g., dynamic allocation of variables is 
     * not a particualry good idea)
     * @return true iff initialization succesded
     */
    public abstract boolean processProgramModel();
  }
  /*********************************************
   * Java Specific TVS factory
   *********************************************/
  
  public interface ITVLAJavaTVSBuilder  {
    /**
     * Creates a TVS for method method with numOfNodes abstract nodes
     * for method method
     * @param method the method for which the TVS is being built, or null if not known / relevant
     * @param numOfNodes
     */
    public abstract void newTVS(int numOfNodes);
    
    public abstract ITVLATVS getTVS();
    
    public abstract int addNode();
    
    // Built in predicates
    public boolean setInUc(int node, ITVLAKleeneValue val);
    public boolean setInUx(int node, ITVLAKleeneValue val);
    public boolean setSM(int node, ITVLAKleeneValue val);
    public boolean setKill(int node, ITVLAKleeneValue val);
    
    
    // Java Predicates
    public abstract boolean setRefLocal(Object method, int indx, int node, ITVLAKleeneValue val);
    
    public abstract boolean setAllocationSite(Object allocSite, int node, ITVLAKleeneValue val);
    public abstract boolean setArrayAllocationSite(Object arrayAllocSite, int node, ITVLAKleeneValue val);
    
    public abstract boolean setClass(Object klass, int node, ITVLAKleeneValue val);
    public abstract boolean setArrayClass(Object arrayClass, int node, ITVLAKleeneValue val);
    
    
    public abstract boolean setArrayContains(int arrayNode, int elemNode, ITVLAKleeneValue val);
    
    public abstract boolean setInstanceReferenceField(Object field, int from, int to, ITVLAKleeneValue val);
    public abstract boolean setStaticReferenceField(Object field, int node, ITVLAKleeneValue val);
    
    public abstract boolean setInstanceArrayField(Object field, int from, int to, ITVLAKleeneValue val);
    public abstract boolean setStaticArrayField(Object field, int node, ITVLAKleeneValue val);
  };
  
  public interface ITVLAJavaLocalHeapsTVSFactory extends  ITVLAJavaTVSBuilder {
    public abstract boolean setIsObj(int node, ITVLAKleeneValue val);
    public abstract boolean setIsOLabel(int node, ITVLAKleeneValue val);
    public abstract boolean setIsCPLabel(int node, ITVLAKleeneValue val);		
    public abstract boolean setLbl(int from, int to, ITVLAKleeneValue val);		
  };
  
  
  /*********************************************
   * TVS / TVS Repository manipualtion
   *********************************************/
  public static interface ITVLATVSRepositry {
    
    /** Adds a gien tvs to the repository 
     * Used to give TVLA "manfactured" TVSs.
     * @param tvs - an api eniry that represents a tvs
     *  
     * @return the id of the tvs or -1 on error
     */
    public int addTVSToRepository(ITVLATVS tvs);
    
    /**
     * Returns the index of the given tvs
     * @param tvs
     * @return
     */
    public int getMappedIndex(ITVLATVS tvs);
    
    /**
     * @return the maximal index used in the repository
     */
    public int getMaxIndex();
    
    /**
     * @return the number of TVSs in the repository
     */
    public int getRepositorySize();
    
    /**
     * Retuns the tvs assoicated withthe given indx
     * @param indx
     * @return
     */
    public ITVLATVS getTVS(int indx);
    
    public ITVLATVSIndexIterator iterator();
    
    public int[] join(int[] input, int[] inputToOutputMap);
    
    /**
     * Reads a set of TVS from a file into the TVS repository
     * @param tvsFile the (full path nameo) of the tvs file
     * @returns an array with the indices of the read elements
     */
    public abstract int[] loadTVSsIntoRepository(String tvsFile);
    
  }
  
  
  /*********************************************
   * Intorogating the TVSs
   *********************************************/
  
  /*********************************************
   * Transformers
   *********************************************/
  
  public static interface ITVLATransformerFactory {
    
    /****************** Allocations *******************/
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a transfomer which allocates a new object for a given TVS   
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer  
    makeAllocFlowFunction(Object method, int lhsRef, Object allocationSite);
    
    /**
     * returns a transfomer which puts an element into an array of booleans
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer  makeArrayBooleanPutFlowFunction(Object method, int lhsRef, int rhsRef) ;
    
    /**
     * returns a transfomer which gets an element from an array of references
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer  makeArrayGetFlowFunction(Object method, int lhsRef, int rhsRef) ;
    
    /**
     * returns a transfomer which returns the length of an array
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer  makeArrayLengthFlowFunction(Object method, int lhs, int rhsRef) ;
    
    /**
     * returns a transfomer which puts an element into an array of references
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer  makeArrayPutFlowFunction(Object method, int lhsRef, int rhsRef);
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a transfomer which sets the value of one boolean local variable (or parameter) 
     * to given value.  
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeAssignConstToBooleanFlowFunction(Object method, int lhsBool, boolean val);
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a transfomer which assigns null to a local variable 
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeAssignNullToReferenceFlowFunction(Object method, int lhsRef);
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a transfomer which sets the unkown value (1/2) to a boolean variable 
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeAssignUnknownToBooleanFlowFunction(Object method, int lhsBool);
    
    // 5. Sq'' = makeCallAndExitToReturnBinaryTransformer[y=p(x)](Sq',Sp'')
    public abstract ITVLATransformers.ITVSBinaryTransformer makeCallAndExitToReturnBinaryTransformer(
        Object caller, Object invocation);
    
    // 3. Sp = makeCalleeEntryTransformer[y=p(x)](Sp')
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCalleeEntryTransformer(
        Object callee);
    
    // 4. Sp'' = makeCalleeExitTransformer[y=p(x)](Sp')
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCalleeExitTransformer(
        Object callee);
    
    // 6. Sq''' = makeCallerPostCallTransformer(Sq'')
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCallerPostCallTransformer(Object caller, Object invocation);
    
    /****************** Interprocedural *******************/
    
    
    /**
     * The 6 transformers are ordered in thje order of as call
     * Call y=p(x) in state Sq will be translated into
     * 	1. Sq' = makeCallerPreCallTransformer[y=p(x)](Sq)
     *  2. Sp' = makeCallToEntryTransformer[y=p(x)](Sq')
     *  3. Sp = makeCalleeEntryTransformer[y=p(x)](Sp')
     *  ... 
     *  ... // Callee reached state Sp'
     *  4. Sp'' = makeCalleeExitTransformer[y=p(x)](Sp')
     *  5. Sq'' = makeCallAndExitToReturnBinaryTransformer[y=p(x)](Sq',Sp'')
     *  -- removed -- 6. Sq''' = makeCallerPostCallTransformer(Sq'')
     *  
     *  We use the convension that in virtual calls the "this" parameter 
     *  is the 0 parameter
     */
    
    // 1. Sq' = makeCallerPreCallTransformer[y=p(x)](Sq)
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCallerPreCallTransformer(
        Object caller, Object invocation);
    
    // 2. Sp' = makeCallerToCalleeTransformer[y=p(x)](Sq')
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCallToEntryTransformer(
        Object caller, Object invocation);
    
    /****************** TESTS ********************/
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a check if a boolean varaibles is 
     *  true (if isTrue is true), or
     *  false (if isTrue is false)
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCheckBooleanFlowFunction(Object method, int rhsBool, boolean isTrue);
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a check if two boolean varaibles are:
     * 	equal (if eq is true), or
     *  different (if eq is false)
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCompareBooleansFlowFunction(Object method, int rhsBool1, int rhsBool2, boolean eq);
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply a check
     * if two reference variables are:
     * 	equal (if eq is true), or
     *  different (if eq is false)
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCompareReferencesFlowFunction(Object method, int rhsRef1, int rhsRef2, boolean eq);
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply a 
     * if a reference variable is:
     * 	equal to null (if eq is true), or
     *  different different from null (if eq is false)
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCompareReferenceToNullFlowFunction(Object method, int rhsRef, boolean eq);
    
    /****************** LOCALS *******************/
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a transfomer which copies the value of one boolean local variable or parameter into another. 
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCopyBooleanToBooleanFlowFunction(Object method, int lhsBool, int rhsBool);
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a transfomer which copies the value of one reference local variable or parameter into another. 
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeCopyReferenceToReferenceFlowFunction(Object method, int lhsRef, int rhsRef);
    
    /****************** Instacne fields *******************/
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which assign to a local variable the value of a static boolean field   
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeGetInstanceBooleanFieldFlowFunction(
        Object method, int lhsBool, int rhsRef, Object theInstanceField);
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which assign to a local variable the value of a static boolean field   
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeGetInstanceReferenceFieldFlowFunction(
        Object method, int lhsRef, int rhsRef, Object theInstanceField);
    
    /****************** STATIC Fields *******************/
    
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which assign to a local variable the value of a static boolean field   
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeGetStaticBooleanFieldFlowFunction(Object method, int lhsBool, Object theStaticField);
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which assign to a local variable the value of a static boolean field   
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeGetStaticReferenceFieldFlowFunction(Object method, int lhsRef, Object theStaticField);
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which assign the value of a static boolean field to a local variable    
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makePutInstanceBooleanFieldFlowFunction(
        Object method, int lhsRef, Object theInstanceField, int rhsBool);
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which nullifies the value of an instance referecne  field    
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeNullifyInstanceReferenceFieldFlowFunction(
        Object method, int lhsRef, Object theInstanceField);
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which assign the value of an instance referecne  field to a local variable    
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makePutInstanceReferenceFieldFlowFunction(
        Object method, int lhsRef, Object theInstanceField, int rhsRef);
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which assign the value of a static boolean field to a local variable    
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makePutStaticBooleanFieldFlowFunction(
        Object theStaticField, Object method, int rhsBoolean);
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which assign the value of a static boolean field to a local variable    
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makePutStaticReferenceFieldFlowFunction(
        Object theStaticField, Object method, int rhsRef);
    
    /**
     * returns a TVLAAPI.ITVLATransformers.ITVSTransformer wrapping a call to TVLA to apply 
     * a transfomer which nullifies a static boolean field    
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeNullifyStaticReferenceFieldFlowFunction(
        Object theStaticField, Object method);
    
    /**
     * Set the return value of method to the value of the local variable with index retValIndex.
     * The type of the return value is deciphered from the method
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer makeReturnValueFlowFunction(Object method, int retValIndex);
    
    /****************** Allocations *******************/
    
    /**
     * returns a IFlowFunction wrapping a call to TVLA to apply 
     * a transfomer which allocates a new array with dim dimesnions 
     * for a given TVS at a given allcation site.
     * 
     * @note the dimension of an array Object[] is 1, of Object[][] is 2, etc.    
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer  makeArrayAllocFlowFunction(Object method, int lhsRef, Object allocationSite);
    
    /**
     * returns a transfomer which puts an element into an array of booleans
     */
    public abstract ITVLATransformers.ITVSUnaryTransformer  makeArrayBooleanGetFlowFunction(Object method, int lhsRef, int rhsRef) ;
    
  }
  
  /****************** TESTS ********************/
  
  public interface ITVLAJavaAssertion extends ITVLAAPI.ITVLAAssertion {
    
  }
  /**
   * An inteface that defiens the assertions a client can 
   * generate
   * @author maon
   *
   */
  public interface ITVLAJavaAssertionFactory {
    /**
     * Generates an assertion which checks whether a pointer has a null value
     * @param mtd  the method the pointer is defined in
     * @param varIndx the number of the pointer
     * @return an assertion that can be evalutes by the adapter
     */
    ITVLAJavaAssertion assertionIsNull(Object mtd, int varIndx);
    
    /**
     * Generates an assertion which checks whether the 2 pointers 
     * with varIndx is null or not 
     * @param mtd  the method the pointer is defined in
     * @param varIndx1  the number of one pointer
     * @param varIndx2  the number of the other pointer
     * @return an assertion that can be evalutes by the adapter
     */
    ITVLAJavaAssertion assertionAreAlias(Object mtd, int varIndx1, int varIndx2);
    
    /// may need to add more
  }
  
  
  /**
   * Avanced services supplied by the analysis
   */
  
  public static interface ITVLAAnlysisServices {
    /**
     * Get a handle to a builder of TVSs
     * @return
     */
    
    public ITVLAJavaTVSBuilder getJavaTVSBuilder();
    
    
    /**
     * Returns to the client a factory for assertions
     * @return
     */
    public ITVLAJavaAssertionFactory getAssertionFactory();
    
    /**
     * Evaluate assertion on the given tvs (A factory for Kleene values :))
     * @param tvs
     * @param assertion
     * @return
     */
    public ITVLAKleene.ITVLAKleeneValue eval(ITVLATVS tvs, ITVLAJavaAssertion assertion);
    
    /**
     * Access to the Kleene 3 valued inplementation
     * @return
     */
    public ITVLAKleene getKleene();  
  }
  
  /**
   * The interface exposed to the frontend
   * 
   * @author maon
   *
   */  
  
  public static interface ITVLAJavaAdapter extends
  ITVLATransformerFactory, 
  ITVLATVSRepositry,
  ITVLAMemoryModeler,
  ITVLAAnlysisServices
  {	
    
    /*********************************************
     * (Parametric) Abstract Domain
     * @param outputDir TODO
     *********************************************/
    public void setParametericDomain(
        String[] commandLineArgs,
        String[] propertyFiles,
        String   anlaysisDirName,
        String   mainAnaysisFileName, 
        String   outputDir);
    
    
    /**********************************************************
     * Moitoring 
     **********************************************************/
    /**
     * Allows clients to register to get information from the java transformer applier 
     */
    public static interface ITVLAJavaApplierListener extends ITVLAApplierListener {
      /**
       * The API does not modify the message after it is generated
       * We extend the APIListner for possible future enhanchemnts.
       */
    }
    
    
    /**
     * Register listeners to the api action.
     * Curenlty only used to report messages.
     * @return true if registration succeeded, false otherwise
     */
    public boolean registerListner(ITVLAJavaApplierListener listener);
    
    
    /***************************************************
     * Statistics 
     **************************************************/
    
    /**
     * Interface for getting TVLA statistics.
     * Currently only support print (toString)
     */
    public static interface ITVLAJavaStatistics extends ITVLAAPIStatistics {
      public String toString();
    }
    


    public ITVLAJavaStatistics getTVLAJavaStatistics();
  }
}