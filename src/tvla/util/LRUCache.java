/*
 * File: LRUCache.java 
 * Created on: 11/10/2004
 */

package tvla.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** 
 * @author maon
 */
public class LRUCache extends LinkedHashMap implements Cache {
    static final int DEFAULT_INITIAL_CAPACITY = 16;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

	int maxCapacity;

	/**
	 * 
	 */
	public LRUCache(int maxCapacity) {
		super(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, 0 < maxCapacity);
		this.maxCapacity = maxCapacity;
	}

	/**
	 * @param initialCapacity
	 */
	public LRUCache(int maxCapacity, int initialCapacity) {
		super(initialCapacity,DEFAULT_LOAD_FACTOR,true);
		this.maxCapacity = maxCapacity;
	}

	/**
	 * @param initialCapacity
	 * @param loadFactor
	 */
	public LRUCache(int maxCapacity, int initialCapacity, float loadFactor) {
		super(initialCapacity,loadFactor,true);
		this.maxCapacity = maxCapacity;
	}

	protected boolean removeEldestEntry(Map.Entry eldest) {
        return (0 < maxCapacity && maxCapacity < size()) ;
     }
	
	public int getMaxCapacity() {
		return this.maxCapacity;
	}
	

}
