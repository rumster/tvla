package tvla.iawp.tp;

import tvla.predicates.Predicate;

/**
 * General Theorem Prover translation methods, shared by some theorem provers.
 * 
 * @author Eran Yahav (eyahav)
 */
public class CommonTranslation {

  public static String removeBrackets(String name) {

    String newName = name.replaceAll("\\[", "_LB_");
    newName = newName.replaceAll("\\]", "_RB_");
    newName = newName.replaceAll("\\,", "_C_");
    return newName;
  }

  public static String restoreBrackets(String name) {
    String newName = name.replaceAll("\\_LB\\_", "[");
    newName = newName.replaceAll("\\_RB\\_", "]");
    newName = newName.replaceAll("\\_C\\_", ",");
    return newName;
  }

  public static String flattenName(Predicate p) {
    return flattenName(p, '_');
  }

  public static String flattenName(Predicate p, char delim) {
    assert p.toString().indexOf(delim) < 0;
    StringBuffer result = new StringBuffer();
    result.append(p.toString());
    String name = result.toString();
    return name.replaceAll("[\\[\\]\\,]", String.valueOf(delim));
  }

  public static String unflattenName(String name) {
    return unflattenName(name, '_');
  }

  public static String unflattenName(String name, char delim) {
    StringBuffer newName = new StringBuffer(name);

    boolean isFirst = true;
    int lastIndex = -1;
    for (int i = 0; i < newName.length(); i++) {
      if (newName.charAt(i) == delim) {
        lastIndex = i;
        if (isFirst) {
          newName.setCharAt(i, '[');
          isFirst = false;
        } else {
          newName.setCharAt(i, ',');
        }
      }
    }
    if (lastIndex >= 0) {
      newName.setCharAt(lastIndex, ']');
    }
    return newName.toString();
  }

}
