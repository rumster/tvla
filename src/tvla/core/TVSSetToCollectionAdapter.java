package tvla.core;

import java.util.Collection;
import java.util.Iterator;

/** Adjusts the interface of TVSSet to that of java.util.Collection.
 * @author Roman Manevich.
 */
public class TVSSetToCollectionAdapter implements Collection<HighLevelTVS> {
	/** A reusable instance.
	 */
	private static TVSSetToCollectionAdapter instance = new TVSSetToCollectionAdapter(null);
	
	/** The TVSSet that's backing this collection.
	 */
	private TVSSet tvsSet;
	
	/** Returns a reusable static instance of this class.
	 */
	public static TVSSetToCollectionAdapter staticInstance(TVSSet set) {
		instance.tvsSet = set;
		return instance;
	}
	
	/** Constructs and initializes a collection backed by the
	 * specified TVSSet instance.
	 */
	public TVSSetToCollectionAdapter(TVSSet set) {
		this.tvsSet = set;
	}
	
	/** Returns an iterator of the states of the set.
	 */
	public Iterator<HighLevelTVS> iterator()  {
		return tvsSet.iterator();
	}

	/// Unsupported.
	public boolean add(HighLevelTVS o) {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public boolean addAll(Collection c)  {
		throw new UnsupportedOperationException();
	}

	/// Unsupported.
	public void clear()  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public boolean contains(Object o)  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public boolean containsAll(Collection c)  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public boolean equals(Object o)  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public int hashCode()  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public boolean isEmpty()  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public boolean remove(Object o)  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public boolean removeAll(Collection c)  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public boolean retainAll(Collection c)  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public int size()  {
		throw new UnsupportedOperationException();
	}
	
	/// Unsupported.
	public Object[] toArray()  {
		throw new UnsupportedOperationException();
	}

	/// Unsupported.
	public Object[] toArray(Object[] a)  {
		throw new UnsupportedOperationException();
	}	
}