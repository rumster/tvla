//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction;

import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAssertion;
import tvla.formulae.Formula;

public interface IJavaAssertion extends ITVLAJavaAssertion {
  public Formula getFormula();
}
