package tvla.core.generic;

import java.util.Iterator;

import tvla.core.Combine;
import tvla.core.TVS;
import tvla.core.common.EmptyTVS;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;

/** An abstract class representing the Combine operation. 
 * @see tvla.core.TVS
 * @see tvla.formulae.Formula
 * @author Noam Rinetzkty
 */
public class GenericCombine extends Combine {   
  /**
   * Combines 2 TVSs using a user supplied nullry combiner
   */
  public TVS combine(INullaryCombiner nullaryCombiner, TVS firstStructure, TVS secondStructure) {
    assert (firstStructure.getVocabulary() == secondStructure.getVocabulary());  
    int numNodesFirst = firstStructure.numOfNodes();
    assert(0 <= numNodesFirst);
    int numNodesSecond = secondStructure.numOfNodes();
    assert(0 <= numNodesSecond);
    assert(nullaryCombiner != null);
    
    TVS combinedStrucutre = null;
    
    if (firstStructure.numOfNodes() < secondStructure.numOfNodes()) {
      combinedStrucutre = secondStructure.copy();
      combinedStrucutre.addUniverse(firstStructure);              
    } else if (0 < firstStructure.numOfNodes()){
      combinedStrucutre = firstStructure.copy();
      combinedStrucutre.addUniverse(secondStructure); 
    } else {
      assert(secondStructure.numOfNodes() == 0);
      if (!(firstStructure instanceof EmptyTVS)) {
        combinedStrucutre = firstStructure.copy();  
      } else {
        combinedStrucutre = secondStructure.copy();
      } 
    }
    
    // Gets nullary predicates
    for (Predicate predicate : combinedStrucutre.getVocabulary().nullary()) {
      assert (predicate.arity() == 0);
      
      Kleene valFirst = firstStructure.eval(predicate);
      Kleene valSecond = secondStructure.eval(predicate);
      
      Kleene val = nullaryCombiner.combineNumarryPredicate(predicate, valFirst,valSecond);              
      // used to be: Kleene.or(valFirst,valSecond);
      
      combinedStrucutre.modify(predicate);
      combinedStrucutre.update(predicate,val);
    }               
    
    assert(combinedStrucutre != null);
    return combinedStrucutre;
  }
  
  /*
   public TVS combine(TVS firstStructure, TVS secondStructure) {
   int numNodesFirst = firstStructure.numOfNodes();
   assert(0 <= numNodesFirst);
   int numNodesSecond = secondStructure.numOfNodes();
   assert(0 <= numNodesSecond);
   
   TVS combinedStrucutre = null;
   
   if (firstStructure.numOfNodes() < secondStructure.numOfNodes()) {
   combinedStrucutre = secondStructure.copy();
   combinedStrucutre.addUniverse(firstStructure);              
   } else if (0 < firstStructure.numOfNodes()){
   combinedStrucutre = firstStructure.copy();
   combinedStrucutre.addUniverse(secondStructure); 
   } else {
   assert(secondStructure.numOfNodes() == 0);
   if (!(firstStructure instanceof EmptyTVS)) {
   combinedStrucutre = firstStructure.copy();  
   } else {
   combinedStrucutre = secondStructure.copy();
   } 
   }
   
   // Gets nullary predicates
    for (Iterator predIt = Vocabulary.allNullaryPredicates().iterator(); predIt.hasNext(); ) {
    Predicate predicate = (Predicate) predIt.next();
    assert (predicate.arity() == 0);
    
    Kleene valFirst = firstStructure.eval(predicate);
    Kleene valSecond = secondStructure.eval(predicate);
    Kleene val = Kleene.or(valFirst,valSecond);
    combinedStrucutre.update(predicate,val);
    }               
    
    assert(combinedStrucutre != null);
    return combinedStrucutre;
    }
    */ 
  
}