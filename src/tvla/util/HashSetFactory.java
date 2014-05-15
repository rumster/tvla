//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.util;

import gnu.trove.THashSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class HashSetFactory {

    private final static int BASIC = 0;

    private final static int TROVE = 1;

    private final static int LINKED = 2;

    private final static int QUICK = 3;

    private final static int Method = LINKED;

    @SuppressWarnings("unchecked")
    public static <T> Set<T> make(int size) {
        switch (Method) {
        case BASIC:
            return new HashSet(size);
        case TROVE:
            return new THashSet(size);
        case LINKED:
            return new LinkedHashSet(size);
        case QUICK:
            return (new QuickHashMap(size)).keySet();
        default:
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> make() {
        switch (Method) {
        case BASIC:
            return new HashSet();
        case TROVE:
            return new THashSet();
        case LINKED:
            return new LinkedHashSet();
        case QUICK:
            return (new QuickHashMap()).keySet();
        default:
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<T> make(Collection<T> s) {
        switch (Method) {
        case BASIC:
            return new HashSet(s);
        case TROVE:
            return new THashSet(s);
        case LINKED:
            return new LinkedHashSet(s);
        case QUICK:
            Set set = make();
            set.addAll(s);
            return set;
        default:
            return null;
        }
    }
}
