//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.javaanalysis.abstraction.basic;

import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAbstraction;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAssertionFactory;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaVocabulary;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAssertion;
import tvla.formulae.AllQuantFormula;
import tvla.formulae.EquivalenceFormula;
import tvla.formulae.Formula;
import tvla.formulae.NotFormula;
import tvla.formulae.PredicateFormula;
import tvla.formulae.Var;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

public class BasicAssertionFactory implements IJavaAssertionFactory {
  
  /**
   * The govering abstraction
   */  
  protected final IJavaAbstraction abstrction; 
  
  /**
   * Provider of the core predicatres pretaining to java variables
   */  
  protected IJavaVocabulary vocabulary;
  
  
  
  protected final Var v1;
  
  /**
   * @param abstraction1
   */
  public BasicAssertionFactory(IJavaAbstraction abstrction, IJavaVocabulary vocabulary) {
    this.vocabulary = vocabulary;
    this.abstrction = abstrction;
    
    v1 = Var.allocateVarPrefix("__V_JavaApiV1");
  }
  
  public IJavaAbstraction getAbstraction() {
    return abstrction;
  }
  
  public ITVLAJavaAssertion assertionIsNull(Object mtd, int varIndx) {
    String refPredName = vocabulary.refLocalToPred(mtd, varIndx);
    Predicate pred = Vocabulary.getPredicateByName(refPredName);
    if (pred == null)
      return null;
    
    PredicateFormula pointsTo = new PredicateFormula(pred, v1);  
    NotFormula doesNotPointTo = new NotFormula(pointsTo);  
    Formula isNull = new AllQuantFormula(v1, doesNotPointTo);
    
    return new BasicTVLAJavaAssertion(isNull);
  }
  
  
  public ITVLAJavaAssertion assertionAreAlias(Object mtd, int varIndx1, int varIndx2) {
    String refPredName1 = vocabulary.refLocalToPred(mtd, varIndx1);
    Predicate pred1 = Vocabulary.getPredicateByName(refPredName1);
    if (pred1 == null)
      return null;
    
    String refPredName2 = vocabulary.refLocalToPred(mtd, varIndx2);
    Predicate pred2 = Vocabulary.getPredicateByName(refPredName2);
    if (pred2 == null)
      return null;
    
    PredicateFormula pointsTo1 = new PredicateFormula(pred1, v1);  
    PredicateFormula pointsTo2 = new PredicateFormula(pred2, v1);
    
    Formula correspond =  new EquivalenceFormula(pointsTo1, pointsTo2);
    
    Formula alias = new AllQuantFormula(v1, correspond);
    
    return new BasicTVLAJavaAssertion(alias);
  }			
}