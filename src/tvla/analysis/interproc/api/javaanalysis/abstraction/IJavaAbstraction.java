//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction;

import tvla.api.ITVLAJavaAnalyzer.ITVLAAnlysisServices;
import tvla.api.ITVLAJavaAnalyzer.ITVLAMemoryModeler;


public interface IJavaAbstraction extends ITVLAAnlysisServices  {
  /**
   * Returns the abstarction specific memory modeler intetrface
   */
  ITVLAMemoryModeler getMemoryModeler();
 
  /**
   * Returns the vocabulary of core preduicates
   */
  IJavaVocabulary getVocabulary();

  
  /**
   * Initializes the abstract domain
   */
  void setParametericDomain(
      String[] commandLineArgs,
      String[] propertyFiles,
      String   anlaysisDirName,
      String   mainAnaysisFileName);
}
