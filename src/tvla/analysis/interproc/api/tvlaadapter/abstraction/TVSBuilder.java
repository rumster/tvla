package tvla.analysis.interproc.api.tvlaadapter.abstraction;

import tvla.analysis.interproc.api.TVLAKleeneImpl.TVLAKleeneValueImpl;
import tvla.api.ITVLATVS;
import tvla.api.ITVLATVSBuilder;
import tvla.api.ITVLAAPI.IVocabulary;
import tvla.api.ITVLAKleene.ITVLAKleeneValue;
import tvla.core.HighLevelTVS;
import tvla.core.Node;
import tvla.core.TVSFactory;
import tvla.core.common.ModifiedPredicates;
import tvla.logic.Kleene;
import tvla.predicates.Predicate;
import tvla.predicates.Vocabulary;


public  class TVSBuilder implements ITVLATVSBuilder {
  protected final IVocabulary voc;
  
  protected  HighLevelTVS tvs;
  protected  Node[] nodes = null;
  protected  int free = 0;
  
  public static TVSBuilder getBuilder(IVocabulary voc) {
    return new TVSBuilder(voc);
  }
  
  protected TVSBuilder(IVocabulary voc) {
    this.voc = voc;
    tvs = null;
  }
  
  /* (non-Javadoc)
   * @see tvla.analysis.interproc.api.TVLATVSFactory#newTVS(int)
   */
  public void newTVS(int numOfNodes) {
    tvs = TVSFactory.getInstance().makeEmptyTVS();
    nodes = new Node[numOfNodes];
    free = 0;
  }
  
  /* (non-Javadoc)
   * @see tvla.analysis.interproc.api.TVLATVSFactory#getTVS()
   */
  public ITVLATVS getTVS() {
    if (tvs == null)
      return null;
    
    tvs.blur();
    TVLATVS ret = new TVLATVS(tvs);
    tvs = null;
    nodes = null;
    free = 0;
    return ret;
  }
  
  /* (non-Javadoc)
   * @see tvla.analysis.interproc.api.TVLATVSFactory#addNode()
   */
  public int addNode() {
    if (nodes.length <= free)
      return -1;
    
    nodes[free] = tvs.newNode();
    
    return free++; 
  }
  
  
  /*****************************************************
   * internal predicates
   *****************************************************/
  
  public boolean setInUc(int node, ITVLAKleeneValue val) {
    String inUc = voc.getInUc().getPredId();
    return setUnaryPredicate(inUc, node, val);			
  }
  
  public boolean setInUx(int node, ITVLAKleeneValue val) {
    String inUx = voc.getInUx().getPredId();
    return setUnaryPredicate(inUx, node, val);					
  }
  
  public boolean setSM(int node, ITVLAKleeneValue val) {
    String sm = voc.getSM().getPredId();
    return setUnaryPredicate(sm, node, val);						
  }
  
  public boolean setKill(int node, ITVLAKleeneValue val) {
    String sm = voc.getKill().getPredId();
    return setUnaryPredicate(sm, node, val);						
  }
  
  /*****************************************************
   * user defined predicates
   *****************************************************/
  
  /* (non-Javadoc)
   * @see tvla.analysis.interproc.api.TVLATVSFactory#setPredicate(java.lang.String, tvla.api.ITVLAKleene.ITVLAKleeneValue)
   */
  public boolean setPredicate(String nullaryPred, ITVLAKleeneValue val) {
    Predicate pred = Vocabulary.getPredicateByName(nullaryPred);
    if (pred == null || pred.arity() != 0)
      return false;
    
    Kleene v = ((TVLAKleeneValueImpl) val).val();
    
    tvs.update(pred, v);
    
    ModifiedPredicates.modify(tvs, pred);

    return true;
  }
  
  /* (non-Javadoc)
   * @see tvla.analysis.interproc.api.TVLATVSFactory#setUnaryPredicate(java.lang.String, int, tvla.api.ITVLAKleene.ITVLAKleeneValue)
   */
  public boolean setUnaryPredicate(String unaryPred, int node, ITVLAKleeneValue val) {
    Predicate pred = Vocabulary.getPredicateByName(unaryPred);
    if (pred == null || pred.arity() != 1 || node < 0 || free <= node)
      return false;
    
    Kleene v = ((TVLAKleeneValueImpl) val).val();
    
    tvs.update(pred, nodes[node], v);
    
    ModifiedPredicates.modify(tvs, pred);
    
    return true;
  }
  
  /* (non-Javadoc)
   * @see tvla.analysis.interproc.api.TVLATVSFactory#setBinaryPredicate(java.lang.String, int, int, tvla.api.ITVLAKleene.ITVLAKleeneValue)
   */
  public boolean setBinaryPredicate(String binaryPred, int node1, int node2, ITVLAKleeneValue val) {
    Predicate pred = Vocabulary.getPredicateByName(binaryPred);
    
    if (pred == null)
      return false;		
    if (pred.arity() != 2)
      return false;
    if (node1 < 0)
      return false;
    if (free <= node1)
      return false;
    if (node2 < 0)
      return false;
    if (free < node2)
      return false;
    
    Kleene v = ((TVLAKleeneValueImpl) val).val();
    
    tvs.update(pred, nodes[node1], nodes[node2], v);
    
    ModifiedPredicates.modify(tvs, pred);

    return true;
  }
  
}