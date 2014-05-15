package tvla.core.decompose;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tvla.predicates.Predicate;
import tvla.util.HashMapFactory;

public class ParametricSet {

    protected static Map<String, List<String>> toMembers = HashMapFactory.make();
    protected static Map<String, List<Predicate>> toPredicates = HashMapFactory.make();
    protected static Map<String, String> toSets = HashMapFactory.make();
    protected static Map<Predicate, String> predicatesToMembers = HashMapFactory.make();
    private static boolean multi;
    
    public static void add(String name, List<String> members) {
        assert !toMembers.containsKey(name);
        if (members.size() > 2) {
            multi = true;
        }
        toMembers.put(name, members);
        for (String member : members) {
            assert !toSets.containsKey(member);
            toSets.put(member, name);
            toPredicates.put(member, new ArrayList<Predicate>());
        }
    }

    public static String getPSet(String member) {
        return toSets.get(member);
    }
    
    public static String getPSetMember(Predicate predicate) {
        return predicatesToMembers.get(predicate);
    }
    

    public static List<String> getMembers(String set) {
        return toMembers.get(set);
    }

    public static List<Predicate> getPredicates(String member) {
        return toPredicates.get(member);
    }
    
    public static void addPredicate(String member, Predicate predicate) {
        toPredicates.get(member).add(predicate);
        predicatesToMembers.put(predicate, member);
    }

    public static boolean isParamteric() {
        return !toSets.isEmpty();
    }

    public static boolean isMulti() {
        return multi;
    }
    
}
