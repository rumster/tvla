package tvla.analysis.interproc.api.javaanalysis.abstraction.basic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.analysis.interproc.api.utils.TVLAAPIDebugControl;
import tvla.api.ITVLAAPIDebuggingServices;
import tvla.api.ITVLAKleene;
import tvla.api.ITVLATVS;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAssertion;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAssertionFactory;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaTVSBuilder;
import tvla.api.ITVLAJavaAnalyzer.ITVLAMemoryModeler;
import tvla.api.ITVLAKleene.ITVLAKleeneValue;


/**
 * The bridge connecting the DOMO engine with TVLA.
 * The bridge translates domoPrimitives (e.g., classes, methods, variables, VariableKeys, flowfunctions, factoid numerals etc.) 
 * to TVLA primitives (prediacates, actions, TVSs, and TVSSets) and vice versa
 * @author noam rinetzky
 */
public abstract class AbstractBasicAbstraction implements IJavaAbstraction {
  protected static int DEBUG_LEVEL = TVLAAPIDebugControl.getDebugLevel(3);

  /**
   * Underlying Frontend services
   */
  
  protected final TVLAAPI tvlaapi;                      // A reference to the (one and only) TVLA backend 

  protected final ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider;
  
  //Fast access to the environment services
  protected final ITVLAAPIDebuggingServices client;		// The analysis environement
  protected final ITVLAJavaProgramModelerServices program;	// allos to interrogate the program    
  
  // Allows gradual initialization
  protected AbstractBasicAbstraction(
      TVLAAPI tvlaapi, 
      ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider) {
    this.tvlaapi = tvlaapi;
    this.environmentServicesProvider = environmentServicesProvider;
    this.program = environmentServicesProvider.getJavaProgramModelerServices();
    this.client = environmentServicesProvider.getJavaDebuggingServices();
  }

  
  
  /***********************************
   * Generic serivces
   ***********************************/

  public final ITVLAKleeneValue eval(ITVLATVS tvs, ITVLAJavaAssertion assertion) {
    return tvlaapi.eval(tvs, assertion);
  }

  public final ITVLAKleene getKleene() {
    return  tvlaapi.getKleene();
  }
  
  public void setParametericDomain(
      String[] commandLineArgs,
      String[] propertyFiles,
      String   anlaysisDirName,
      String   mainAnaysisFileName)  {

    Collection sets = getSetsItr();
    for (Iterator setItr = sets.iterator(); setItr.hasNext(); ) {
      String set = (String) setItr.next();
      tvlaapi.createSet(set);
    }
  }
  
  /**
   * Returns the standard sets generated to represent the program
   */
  protected Collection getSetsItr() {
    return Arrays.asList(BasicJavaConstants.allSetsStrs);
  } 
  
  /***********************************************************
   * Access to the abstraction dependant serivces
   ***********************************************************/
  
  public abstract ITVLAMemoryModeler getMemoryModeler() ;
    
  public abstract ITVLAJavaTVSBuilder getJavaTVSBuilder() ;
  
  public abstract IJavaVocabulary getVocabulary() ;
  
  public abstract ITVLAJavaAssertionFactory getAssertionFactory();
  
  
  /*
  
  public  ITVLAMemoryModeler getMemoryModeler() {
    return memoryModeler;
  }
    
  public  ITVLAJavaTVSBuilder getJavaTVSBuilder() {
    return this.tvsBuilder;
  }
  
  public  IJavaVocabulary getVocabulary() {
    return this.vocabulary;
  }
  
  public  ITVLAJavaAssertionFactory getAssertionFactory() {
    return this.assertionFactory;
  }
  */
}
