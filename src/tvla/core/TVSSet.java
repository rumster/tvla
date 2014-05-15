package tvla.core;

import java.util.Collection;
import java.util.Iterator;

import tvla.util.Pair;

/** A set of three-valued structures.
 * In essence, this is the class of abstract elements of the TVLA lattice.
 * @TODO: add an iterator over pending structures, and remove the corresponding
 * collection from the different engines (unprocessed).
 * @TODO: consider adding a mergeWith operation that takes a set of structures.
 * 
 * @author Ganesan Ramalingam.
 * @author Roman Manevich.
 * @author Deepak Goyal.
 * @author John Field.
 * @author Mooly Sagiv.
 */
public abstract class TVSSet implements Iterable<HighLevelTVS> {
    protected int shareCount = 0;
	protected boolean cachingMode = false;
    
	public void share() {
        shareCount++;
    }
    
    public void unshare() {
        if (shareCount > 0) {
            shareCount--;
        }
    }

    public TVSSet modify() {
        if (shareCount > 0) {
            shareCount--;
            return copy();
        } else {
            return this;
        }
    }
    
   /** Applies the Join confluence operator.
    * @return The difference between the updated set
    * and the old set or null if there is no difference.
    * @TODO: change the type of the structure to TVS.
    * THis method is deprecated. It is left for backward compatability.
    */
   public abstract HighLevelTVS mergeWith(HighLevelTVS structure);
   
   /** Applies the Join confluence operator.
    * @param structure the added TVS
    * @param mergureMap, a map from the set before the merge + the new structure
    * to the set after the join. The mapping provides the information which structures
    * were merged together. The mapping is implemented as a collection of Pairs.
    * The structures (the actual objects) after the join are a subset of structures 
    * before the join with the possible addition of the new structure. The mapping
    * does not contain the identity mapping. 
    * @return whether there is a difference between the set before and after
    * the join. 
    * @pre structure and mergureMap are not null.
    * @TODO: change the type of the structure to TVS.
    */
   public abstract boolean mergeWith(HighLevelTVS structure, Collection<Pair<HighLevelTVS,HighLevelTVS>> mergureMap);

   
   /** Returns the current number of structures in this set.
    */
   public abstract int size();
   
   /** A test for emptiness.
    */
   public final boolean isEmpty() {
	   return size() == 0;
   }
	   
   /** Returns an iterator to the structures contained in this set 
    */
   public abstract Iterator<HighLevelTVS> iterator();
   
   public TVSSet copy() {
       TVSSet copy = TVSFactory.getInstance().makeEmptySet();
       copy.cachingMode = this.cachingMode;
       for (HighLevelTVS structure : this) {
           copy.mergeWith(structure.copy());
       }
       return copy;
   }
   
   /**
    * Make best effort to check isomorphism between two sets
    * @return null - don't know, true - isomorphic, false - not
    */
   public Boolean isomorphic(TVSSet other) {
       return null;
   }

	public void setCachingMode(boolean cachingMode) {
		this.cachingMode = cachingMode;
	}

	public boolean getCachingMode() {
		return cachingMode;
	}

    public boolean contains(HighLevelTVS structure) {
        throw new UnsupportedOperationException();
    }

    public void mergeWith(Iterator<HighLevelTVS> iterator) {
        while (iterator.hasNext()) {
            mergeWith(iterator.next());
        }        
    }

}