package tvla.analysis.decompose;

import java.util.Set;

import tvla.core.HighLevelTVS;
import tvla.core.decompose.CartesianElement;
import tvla.core.decompose.Decomposer;
import tvla.core.decompose.DecompositionName;
import tvla.core.decompose.ParametricSet;
import tvla.transitionSystem.Action;
import tvla.util.HashConsFactory;
import tvla.util.Pair;

public abstract class CompositionStrategy implements Iterable<Pair<DecompositionName, Iterable<HighLevelTVS>>> {
	static class Key {
		Action action;
		Set<? extends DecompositionName> currentNames; 
		boolean incremental;
		private final DecomposeLocation nextLocation;
        @Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + action.hashCode();
			result = prime * result + nextLocation.hashCode();
			if (!ParametricSet.isParamteric()) {
				result = prime * result + currentNames.hashCode();
			}
			result = prime * result + (incremental ? 1231 : 1237);
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Key other = (Key) obj;
			if (!action.equals(other.action)) return false;
			if (!nextLocation.equals(other.nextLocation)) return false;
			if (!currentNames.equals(other.currentNames)) return false;
			if (incremental != other.incremental) return false;
			return true;
		}
		public Key(Action action,
				Set<? extends DecompositionName> currentNames,
				boolean incremental, DecomposeLocation nextLocation) {
			super();
			this.action = action;
			this.currentNames = currentNames;
			this.incremental = incremental;
			this.nextLocation = nextLocation;
		}
		public String toString() {
			return "(" + action + "," + currentNames + "," + incremental + "," + nextLocation + ")";
		}
	}
	
	static HashConsFactory<CompositionStrategy, Key> factory = new HashConsFactory<CompositionStrategy, Key>() {
		@Override
		protected CompositionStrategy actualCreate(Key key) {
	        return new BasicCompositionStrategy(key.action, key.currentNames, key.incremental, key.nextLocation);
		}
	};

    public static CompositionStrategy getStrategy(Action action, Set<? extends DecompositionName> currentNames, 
            boolean incremental, DecomposeLocation nextLocation) {

        // TODO What is the general thing to do?
        if (ParametricSet.isParamteric()) {
            currentNames = Decomposer.getInstance().names();
        }
    	return factory.create(new Key(action, currentNames, incremental, nextLocation));
    }

    public abstract CartesianElement getDecomposed();

    public abstract void verify() throws DecompositionFailedException;

    /**
     * Attempt to compress after by sharing components with before
     */
    public void compress(CartesianElement after, CartesianElement before) {
    }

    public abstract void init(
            CartesianElement beforeOld, CartesianElement beforeDelta, CartesianElement beforeNew 
            );

	public abstract void after(DecompositionName name, Iterable<HighLevelTVS> results);

	public abstract void done();
	
}
