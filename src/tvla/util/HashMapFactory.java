//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.util;

import gnu.trove.THashMap;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HashMapFactory {

    private final static int TROVE = 1;

    private final static int LINKED = 2;

    private final static int QUICK = 3;

    private final static int BASIC = 4;

    private final static int Method = BASIC;

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> make(int size) {
        switch (Method) {
        case TROVE:
            return new THashMap(size);
        case LINKED:
            return new LinkedHashMap(size);
        case QUICK:
            return new QuickHashMap(size);
        case BASIC:
            return new HashMap(size);
        default:
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> make() {
        switch (Method) {
        case TROVE:
            return new THashMap();
        case LINKED:
            return new LinkedHashMap();
        case QUICK:
            return new QuickHashMap();
        case BASIC:
            return new HashMap();
        default:
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> make(Map m) {
        switch (Method) {
        case TROVE:
            return new THashMap(m);
        case LINKED:
            return new LinkedHashMap(m);
        case QUICK:
            return new QuickHashMap(m);
        case BASIC:
            return new HashMap(m);
        default:
            return null;
        }
    }

}
