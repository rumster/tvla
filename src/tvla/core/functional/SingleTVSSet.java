package tvla.core.functional;

import java.util.Collection;
import java.util.LinkedList;

import tvla.core.HighLevelTVS;
import tvla.core.generic.GenericSingleTVSSet;


/** 
 * A test implementation used to just test the effect of changing
 * the collection class used by GenericSingleTVSSet to LinkedList.
 * @author G. Ramalingam
 */

public class SingleTVSSet extends GenericSingleTVSSet {
   public SingleTVSSet() {
	   this.structures = new LinkedList();
	}
   
   public boolean mergeWith(HighLevelTVS S, Collection mergedWith) {
	throw new UnsupportedOperationException() ;
   }

}
