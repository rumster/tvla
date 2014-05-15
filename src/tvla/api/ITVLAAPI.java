package tvla.api;


import java.util.List;
import java.util.Set;

import tvla.core.Combine.INullaryCombiner;


/**
 * @author maon
 *
 */
/**
 * @author maon
 *
 */
public interface ITVLAAPI {	
	/****************************************************************
	 * Setting the parametric domain + TVLA internal settings
	 ****************************************************************/

	/** Initizalize TVLA with a given parametric abstract domain 
	 * @param propertyFiles - an array of full name (i.e., with path) 
	 *        properties files 
	 * @param mainAnaysisFileName the file name (i.e., without path) 
	 * 		  of the root of main file of the analysis.
	 * 		  The file should be in the following format:
     *          Sets [optional]
	 *  		Predicates
	 *          %%
	 *          Action definition
	 *          %%
	 *        i.e., it has the "normal tvp format", except that the CFG section is missing.
	 *        Note that the file may contain sets defined by the analysis client. 
	 *        The  file may contain sets that are part of the program model.
	 *        In this case, the cntent of the sets as specified in the file is added 
	 *        to what ever comntnt the engine adds.
	 * @param outputDir TODO
	 * @param commandLineOptions - the strings of command 
	 *        line arguments as of parse by the comman line reader
	 *        when tvla is invoked from the comand line *Without* the 
	 *        tvp/tvs file names 
	 * @param analysisDirName - the name of directory in which the analysis resides.
	 *        required for the preprocesor
	 */
	public void setParametericDomain(
			String engine, 
			String[] commandLineArgs,
			String[] propertyFiles,
			String   anlaysisDirName,
			String   mainAnaysisFileName, 
            String   outputDir);
	
	/****************************************************************
	 * Setting the specific instance (i.e., program dependant) of the 
	 * parametric domain
	 ****************************************************************/

	/**
	 * Creates an empty set named setName.
	 */
	public boolean createSet(String setName);

	/**
	 * Adds element e to set set. 
	 * The setNeed to be defined ahead using createSet.
	 */
	public boolean addToSet(String setName, String e);
	
	/**
	 * Generates the needed abstract domain from its parameteric  definition.
	 * To be invoked once (and only once) all the sets were populated.
	 * 
	 * @param expectedMAcros the macros that the analysis is going to use
	 */
	public boolean instantiateParametericDomain(Set expectedMacors);
	
	
	/****************************************************************
	 * Instantiating program specific abstract transfomers 
	 ****************************************************************/
	
    
    /**
     * @param parameters is a list of Strings
     */
    public ITVLATransformers.ITVSUnaryTransformer 
      getUnaryTransformer(String actionName, List parameters);
    

    /**
     *  Abinary operation is implemented by combining the 2 TVSs into 1 TV and 
     *  setting inUc for the individuals in the first TVS adn inUx for the individuals in the 
     *  second universe.
     *  The nullry combiner determines how to combine the nullary values from the 2 TVSs.
     *  It is implmented as a fucntion from (nullary) predicate name to the a function
     *  of the predicate value at the first TVS (1st argument) and second TVS (2nd argument)
     *  
     * @param nullaryCombinr how to cobine the nullary values at the staged result and given result.
     * @param binaryActionName the binary opetaton
     * @param parameters s a list of Strings needed to instantiate the action 
     * @return
     */
    public ITVLATransformers.ITVSBinaryTransformer  getBinaryTransformer(
        INullaryCombiner nullaryCombiner,
        String actionName, List parameters);
    
    
    /**
     *  Staging is implementd as a binary operation that gets 
     *  the staged result as its first argumenet and the current result as its seconf argument.
     *  
     * @param nullaryCombinr how to cobine the nullary values at the staged result and given result.
     * @param binaryActionName the staging opetaton
     * @param parameters s a list of Strings needed to instantiate the action 
     * @return
     */
    public ITVLATransformers.ITVSUnaryTransformer getUnaryStagedTransformer(
        INullaryCombiner nullaryCombiner, 
        String binaryActionName, List parameters);
    
    
    /**
     * A staged binary operation is implementd as 2 binary actions.
     * The first action is applied to the stagd rsult and the first input
     * The second action is applied (pintwise) to the results of the first operation 
     * (if not the empty set) and the second operation
     *  the staged result as its first argumenet and the current result as its seconf argument.  
     * @param nullaryCombiner1
     * @param stagedActionName first (staging) action to execute
     * @param parametersStaged
     * @param nullaryCombiner2 second (combining) action to execute
     * @param actionName
     * @param parameters
     * @return
     */
    public ITVLATransformers.ITVSBinaryTransformer 
    getBinaryStagedTransformer(
        INullaryCombiner nullaryCombiner1, 
        String stagedActionName, List parametersStaged, 
        INullaryCombiner nullaryCombiner2, 
        String actionName, List parameters);     
    
    
    
	/****************************************************************
	 * Managing the abstract domain 
	 ****************************************************************/
	/** Adds a gien tvs to the repository 
	 * Used to give TVLA "manfactured" TVSs.
	 * @param tvs - an api eniry that represents a tvs
	 *  
	 * @return the id of the tvs or -1 on error
	 */
	public int addTVSToRepository(ITVLATVS tvs);
	
	/** Adds a set of TVSs to the repository
	 * Used to give TVLA the initial TVSs.
	 * The TVSs are numbered in successive numbers according to their order in
	 * the tvsFile.
	 * @param tvsFile - the  full name (i.e., with path) 
	 *        of the file with the initial TVSs
	 * @return an array of TVSs ids or null id error occured.
	 */
	public int[] loadTVSs(String tvsFile);
	
	
	/** Joins a a given set of TVSs. 
	 * @param input an array with the ids of the TVSs to be joined
	 * @param inputToOutputMap (may be null) an indicator of which output TVS represent which input TVS:
	 * The TVS with id input[i] is reresented by inputToOutputMap[i].  
	 * @return an aray of indices which are the joined of all the inputs
	 * or null if the result of the join are the input  
	 * i.e., true if input != inputToOutputMap, false otherwise.
	 * @note that the returned array may be the same arry as the input array.
	 */
	public int[]  join(int[] input, int[] inputToOutputMap);
	
	/**
	 * Returns limited exposure of the underlying TVS
	 * @param indx the index of the needed TVS
	 * @return ab object representing the needed TVS or null if non is found
	 */
	ITVLATVS getTVS(int indx) ;
	
	
	/**
	 * @return an iterator over the indices of the TVSs sotred in the abstract 
	 * domain repository. These indices can be used to get the TVS using getTVS.
	 */
	public ITVLATVSIndexIterator iterator();
	
	/**
	 * @return the maximal index used in the repository
	 */
	public int getMaxIndex();
	
	/**
	 * Returns the index of the given tvs
	 * @param tvs
	 * @return
	 */
	public int getMappedIndex(ITVLATVS tvs);

	
	/***********************************************************
	 * Quering TVSs
	 ***********************************************************/

	public static interface IVocabulary {
		public static interface IPredicate {
			public int getArity() ;
			
			public String getPredId();
		}
		
		public IPredicate getSM();
		public IPredicate getInUc();
		public IPredicate getInUx();
		public IPredicate getKill();

		public IPredicate  getPredicate(String id);
	}
	
    public IVocabulary getVocabulary();
    
	/***********************************************************
	 * Quering TVSs
	 ***********************************************************/
	
	/**
	 * A marker for assertions that can be evaluated.
	 * The specific assertions allowed, depends on the 
	 * actul API implementation  
	 */
	public static interface ITVLAAssertion {

	}
	
	/**
	 * Evaluate assertion on the given tvs
	 * @param tvs
	 * @param assertion
	 * @return
	 */
	public ITVLAKleene.ITVLAKleeneValue eval(ITVLATVS tvs, ITVLAAssertion assertion);
	
	
	
	/**********************************************************
	 * Moitoring 
	 **********************************************************/
	
	/**
	 * The format of messages reported by TVLA
	 */
	public static interface ITVLAMessage {		
		/**
		 * @param tvsNum  == 0 or 1: An action may operate on 1 or 2 TVSs.
		 * The index tells which of them we are interested in.  
		 * 
		 * @return the index of the structure which caused the message (i.e.e, the message 
		 * was generated when the action was applied applied on this strucutre).
		 * return -1 if the oaction did not have tvsNum TVS parameter.  
		 */
		int getCause(int tvsNum);

		/**
		 * @return a (focused) TVS that provides a more detiled explanation 
		 * why the message was generated. 
		 * 
		 * @note it may be costly to record the specific reasons as they are not
		 * (and cannot for now) be cannonized.
		 */
		ITVLATVS specifyReason();
		
		/**
		 * @return the action whose evaluation generated the message
		 */
		ITVLATransformers.ITVSStagedTransformer getTransformer();
		
		/**
		 * @return A list of messages (Strings) that was generated by tvla for the these
		 * combintion of cause(s) and spcific reason 
		 */
		List getMessagesAsStrings();
	};

	
	/**
	 * Interface required from a client of the api in order to get messages etc.
	 * @note may be extended fror progress reports too.
	 */

	public static interface ITVLAApplierListener {
		/**
		 * Th API does not modify the message after it is generated
		 * @param msg
		 */
		void messageGenerated(ITVLAMessage msg);
	};
		
	/**
	 * Register listeners to the api action.
	 * Curenlty only used to report messages.
	 * @return true if registration succeeded, false otherwise
	 */
	public boolean registerListner(ITVLAApplierListener listener);
    

    /**
     * TODO should we scrap this, and add the info to the transformer?
     * 
     * Engine services,  used for debugging
     */
    public static interface ITVLATabulatorServices {
        /**
         * The fucntion allows TVLA to check the know the code being analyzed.
         * Used for debugging
         */
        public String getCurrentLocation();
    }

    
    /***************************************************
     * Statistics 
     **************************************************/
    
    /**
     * Interface for getting TVLA statistics.
     * Currently only support print (toString)
     */
    public static interface ITVLAAPIStatistics {
      public String toString();
    }
    
    public ITVLAAPIStatistics getTVLAStatistics();
}