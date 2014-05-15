package tvla.analysis.decompose;

import java.util.Collection;
import java.util.Iterator;

import tvla.core.HighLevelTVS;
import tvla.core.decompose.CartesianElement;
import tvla.transitionSystem.Location;
import tvla.util.EmptyIterator;
import tvla.util.Pair;
import tvla.util.ProgramProperties;
import tvla.util.SimpleIterator;

public class DecomposeLocation extends Location {
    protected CartesianElement element = new CartesianElement();
    protected CartesianElement old = new CartesianElement();
    protected CartesianElement delta = null;
    
    public static boolean cachingMode = ProgramProperties.getBooleanProperty("tvla.tvs.cache", false);
    
    public DecomposeLocation(String label) {
        super(label);
        element.setCachingMode(cachingMode);
        old.setCachingMode(cachingMode);
    }

    public DecomposeLocation(String label, boolean shouldPrint) {
        super(label, shouldPrint);
        element.setCachingMode(cachingMode);
        old.setCachingMode(cachingMode);
    }

    public Pair<CartesianElement, CartesianElement> retrieveDelta () {
        Pair<CartesianElement, CartesianElement> result = Pair.create(old, delta);
        delta = null;
        old = new CartesianElement();
        old.setCachingMode(cachingMode);
        return result;
    }
    
    @Override
    public void clearLocation() {
        super.clearLocation();
        unprocessed = null;
        element = new CartesianElement();
        element.setCachingMode(cachingMode);
        old = new CartesianElement();
        old.setCachingMode(cachingMode);
    }
    
    public Iterable<HighLevelTVS> everyStructure() {
    	return new Iterable<HighLevelTVS>() {
			public Iterator<HighLevelTVS> iterator() {
				return new SimpleIterator<HighLevelTVS>() {				
					Iterator<HighLevelTVS> oldIter = old.iterator();
					Iterator<HighLevelTVS> elementIter = element.iterator();
					Iterator<HighLevelTVS> emptyIter = EmptyIterator.instance();
					Iterator<HighLevelTVS> deltaIter = delta == null ? emptyIter : delta.iterator();
					@Override
					protected HighLevelTVS advance() {
						if (oldIter.hasNext()) return oldIter.next();
						if (elementIter.hasNext()) return elementIter.next();
						if (deltaIter.hasNext()) return deltaIter.next();
						return null;
					}
				};
			}
		};
    }

    public int size() {
        return element.size();
    }

    @Override
    public Iterator<HighLevelTVS> getStructuresIterator() {
        return element.iterator();
    }

    public Iterator<HighLevelTVS> frozenStructures() {
        return getStructuresIterator();
    }

    public Iterator<HighLevelTVS> allStructures() {
        return getStructuresIterator();
    }
    
    @Override
    public boolean join(HighLevelTVS structure, Collection<Pair<HighLevelTVS, HighLevelTVS>> mergedWith) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HighLevelTVS join(HighLevelTVS structure) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<HighLevelTVS> removeUnprocessed() {
        throw new UnsupportedOperationException();
    }

    public CartesianElement getElement() {
        return element;
    }
    
    /** Returns a string representing statistics 
     * about internal information.
     */
    public String status() {
        String result;
        result = "unprocessed=" + (delta==null ? 0 : delta.size()) + "\tsaved=" + element.size();
        result = label + " : \t" + result + "\t" + " #messages=" + messages.size();
        return result;
    }
    

    public boolean join(CartesianElement newDelta) {
        if (delta == null) {
            old = element.copy();
            delta = newDelta;
        } else {
            element = old.copy();
            delta.join(newDelta);            
        }
        delta = element.join(delta);
        return delta != null;
    }
}
