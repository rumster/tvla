package tvla.api;

import tvla.api.ITVLAKleene.ITVLAKleeneValue;

public interface ITVLATVSBuilder {

	public abstract void newTVS(int numOfNodes);

	public abstract ITVLATVS getTVS();

	public abstract int addNode();

	
	/********************************************
	 * Builtin predicates
	 ********************************************/
	
	public boolean setInUc(int node, ITVLAKleeneValue val);
	public boolean setInUx(int node, ITVLAKleeneValue val);
	public boolean setSM(int node, ITVLAKleeneValue val);
	public boolean setKill(int node, ITVLAKleeneValue val);

	
	/********************************************
	 * User defined predicates
	 ********************************************/
	public abstract boolean setPredicate(String nullaryPred, ITVLAKleeneValue val);

	public abstract boolean setUnaryPredicate(String unaryPred, int node, ITVLAKleeneValue val);

	public abstract boolean setBinaryPredicate(String binaryPred, int nodeSrc, int nodeDst, ITVLAKleeneValue val);
}