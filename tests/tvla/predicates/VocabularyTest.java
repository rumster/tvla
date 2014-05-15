/*
 * Created on 23/09/2004
 *
 */
package tvla.predicates;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import tvla.formulae.EqualityFormula;
import tvla.formulae.Formula;
import tvla.formulae.ValueFormula;
import tvla.formulae.Var;
import tvla.transitionSystem.Location;

/** Unit tests for tvla.predicates.Vocabulary.
 * Notes:
 * 1. There are no tests for predicates with arity > 2.
 * 2. Methods for removing predicates and changing properties of predicates
 *    are not tested.
 * 3. There are no tests for methods retrieveing subsets of predicates.
 * 
 * @author Roman Manevich
 */
public class VocabularyTest extends TestCase {
    static Predicate nullaryPredicate1 = Vocabulary.createPredicate("nullaryPredicate1", 0);
    static Predicate unaryPredicate1 = Vocabulary.createPredicate("unaryPredicate1", 1);
    static Predicate binaryPredicate1 = Vocabulary.createPredicate("binaryPredicate1", 2);

    static Predicate nullaryNonAbsPredicate1 = Vocabulary.createPredicate("nullaryNonAbsPredicate1", 0, false);
    static Predicate nullaryAbsPredicate1 = Vocabulary.createPredicate("nullaryAbsPredicate1", 0, true);
    static Predicate unaryNonAbsPredicate1 = Vocabulary.createPredicate("unaryNonAbsPredicate1", 1, false);
    static Predicate unaryAbsPredicate1 = Vocabulary.createPredicate("unaryAbsPredicate1", 1, true);
    static Predicate binaryNonAbsPredicate1 = Vocabulary.createPredicate("binaryNonAbsPredicate1", 2, false);

    static Predicate unaryUniqueAbsPredicate1 = Vocabulary.createPredicate("unaryUniqueAbsPredicate1", 1, true, true, false);
    static Predicate unaryUniqueNonAbsPredicate1 = Vocabulary.createPredicate("unaryUniqueNonAbsPredicate1", 1, true, false, false);
    static Predicate unaryNonUniqueAbsPredicate1 = Vocabulary.createPredicate("unaryNonUniqueAbsPredicate1", 1, false, true, false);
    static Predicate unaryNonUniqueNonAbsPredicate1 = Vocabulary.createPredicate("unaryNonUniqueNonAbsPredicate1", 1, false, false, false);

    static Instrumentation nullaryInstrumentation1;
    static Instrumentation unaryInstrumentation1;
    static Instrumentation binaryInstrumentation1;
    
    static Predicate locationPredicate1;
    
    static Location location1;
    
    static Formula simpleFormula = new ValueFormula(tvla.logic.Kleene.trueKleene);
    static Var var1 = Var.allocateVar();
    static Var var2 = Var.allocateVar();
    static Formula equalityFormula = new EqualityFormula(var1, var2);
    
    static List nullaryList = new ArrayList(0);
    static List unaryList = new ArrayList(1);
    static List binaryList = new ArrayList(2);
    static List illegalBinaryVarList = new ArrayList(2);
    
    static {
        unaryList.add(var1);
        binaryList.add(var1);
        binaryList.add(var2);

        illegalBinaryVarList.add(var1);
        illegalBinaryVarList.add(var1);        

        nullaryInstrumentation1 = Vocabulary.createInstrumentationPredicate("nullaryInstrumentation1", 0, true, simpleFormula, nullaryList);
        unaryInstrumentation1 = Vocabulary.createInstrumentationPredicate("unaryInstrumentation1", 1, true, simpleFormula, unaryList);
        binaryInstrumentation1 = Vocabulary.createInstrumentationPredicate("binaryInstrumentation1", 2, false, equalityFormula, binaryList);
        
        location1 = new Location("location1");
        locationPredicate1 = Vocabulary.createLocationPredicate("locationPredicate1", location1);
    }
    
    public VocabularyTest(String name) {
        super(name);
    }
    
    /** A tset for Vocabulary.createPredicate(String name, int arity)
     */
    public void testCreatePredicateStringInt() {
        assertTrue(nullaryPredicate1.abstraction());
        assertTrue(nullaryPredicate1.arity() == 0);
        assertTrue(nullaryPredicate1.name().equals("nullaryPredicate1"));        

        assertTrue(unaryPredicate1.abstraction());
        assertTrue(unaryPredicate1.arity() == 1);
        assertTrue(unaryPredicate1.name().equals("unaryPredicate1"));
        assertFalse(unaryPredicate1.unique());

        assertTrue(binaryPredicate1.abstraction());
        assertFalse(binaryPredicate1.acyclic());
        assertFalse(binaryPredicate1.function());
        assertFalse(binaryPredicate1.invfunction());
        assertTrue(binaryPredicate1.name().equals("binaryPredicate1"));
        assertFalse(binaryPredicate1.reflexive());

        try {
            Predicate negativeArityPredicate = Vocabulary.createPredicate("negativeArityPredicate", -1);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        
        // Now recreating the same predicates.
        try {
            Predicate nullaryPredicate2 = Vocabulary.createPredicate("nullaryPredicate1", 0);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        try {
            Predicate unaryPredicate2 = Vocabulary.createPredicate("unaryPredicate1", 0);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        try {
            Predicate binaryPredicate2 = Vocabulary.createPredicate("binaryPredicate1", 0);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
    
        // Now try recreating them with different arrities.
        try {
            Predicate nullaryPredicate2 = Vocabulary.createPredicate("nullaryPredicate1", 1);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        try {
            Predicate unaryPredicate2 = Vocabulary.createPredicate("unaryPredicate1", 2);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        try {
            Predicate binaryPredicate2 = Vocabulary.createPredicate("binaryPredicate1", 0);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
    }
    
    /** A test for Vocabulary.createPredicate(String name, int arity, boolean abstraction)
     */
    public void testCreatePredicateStringIntBoolean() {
        assertFalse(nullaryNonAbsPredicate1.abstraction());
        assertTrue(nullaryNonAbsPredicate1.arity() == 0);
        assertTrue(nullaryNonAbsPredicate1.name().equals("nullaryNonAbsPredicate1"));        

        assertTrue(nullaryAbsPredicate1.abstraction());
        assertTrue(nullaryAbsPredicate1.arity() == 0);
        assertTrue(nullaryAbsPredicate1.name().equals("nullaryAbsPredicate1"));        

        assertFalse(unaryNonAbsPredicate1.abstraction());
        assertTrue(unaryNonAbsPredicate1.arity() == 1);
        assertTrue(unaryNonAbsPredicate1.name().equals("unaryNonAbsPredicate1"));
        assertFalse(unaryNonAbsPredicate1.unique());

        assertTrue(unaryAbsPredicate1.abstraction());
        assertTrue(unaryAbsPredicate1.arity() == 1);
        assertTrue(unaryAbsPredicate1.name().equals("unaryAbsPredicate1"));
        assertFalse(unaryAbsPredicate1.unique());

        assertFalse(binaryNonAbsPredicate1.acyclic());
        assertFalse(binaryNonAbsPredicate1.function());
        assertFalse(binaryNonAbsPredicate1.invfunction());
        assertTrue(binaryNonAbsPredicate1.name().equals("binaryNonAbsPredicate1"));
        assertFalse(binaryNonAbsPredicate1.reflexive());
        
        try {
            Predicate binaryPredicate2 = Vocabulary.createPredicate("binaryNonAbsPredicate2", 2, true);
            fail("Vocabulary should disallow creating binary abstraction predicates!");
        }
        catch (Throwable t) {
        }
        
        // Now try recreating predicates.
        try {
            Predicate nullaryPredicate2 = Vocabulary.createPredicate("nullaryNonAbsPredicate1", 0, false);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        try {
            Predicate nullaryPredicate2 = Vocabulary.createPredicate("nullaryAbsPredicate1", 0, true);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        try {
            Predicate unaryPredicate2 = Vocabulary.createPredicate("unaryNonAbsPredicate1", 1, false);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        try {
            Predicate unaryPredicate2 = Vocabulary.createPredicate("unaryAbsPredicate1", 1, true);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
        try {
            Predicate binaryPredicate2 = Vocabulary.createPredicate("binaryNonAbsPredicate1", 0);
            fail("Vocabulary should disallow recreating predicates!");
        }
        catch(Throwable t) {            
        }
    }
    
    /** A test for Vocabulary.createPredicate(String name, int arity, boolean abstraction, boolean unique, boolean pointer)
     */
    public void testCreatePredicateStringIntBooleanBooleanBoolean() {
        assertTrue(unaryUniqueAbsPredicate1.unique());
        assertTrue(unaryUniqueAbsPredicate1.abstraction());
        
        assertFalse(unaryUniqueNonAbsPredicate1.unique());
        assertTrue(unaryUniqueNonAbsPredicate1.abstraction());

        assertTrue(unaryNonUniqueAbsPredicate1.unique());
        assertFalse(unaryNonUniqueAbsPredicate1.abstraction());

        assertFalse(unaryNonUniqueNonAbsPredicate1.unique());
        assertFalse(unaryNonUniqueNonAbsPredicate1.abstraction());
        
        try {
            Predicate nullaryUniquePredicate2 = Vocabulary.createPredicate("nullaryUniquePredicate2", 0, false, true, false);
            fail("Vocabulary should disallow creating predicates of arity different from 1 with the unique functional dependency!");
        }
        catch (Throwable t) {
        }
        try {
            Predicate binaryUniquePredicate2 = Vocabulary.createPredicate("binaryUniquePredicate2", 2, false, true, false);
            fail("Vocabulary should disallow creating predicates of arity different from 1 with the unique functional dependency!");
        }
        catch (Throwable t) {
        }        
    }
    
    /** A test for Vocabulary.createInstrumentationPredicate(String name, int arity, boolean abstraction, Formula formula, List vars)
     */
    public void testCreateInstrumentationPredicate() {
        assertTrue(nullaryInstrumentation1.abstraction());
        assertTrue(nullaryInstrumentation1.arity() == 0);
        assertTrue(nullaryInstrumentation1.name().equals("nullaryInstrumentation1"));
        assertTrue(nullaryInstrumentation1.getVars().equals(nullaryList));
        assertTrue(nullaryInstrumentation1.getVars().size() == 0);
        
        assertTrue(unaryInstrumentation1.name().equals("unaryInstrumentation1"));
        assertTrue(unaryInstrumentation1.abstraction());
        assertTrue(unaryInstrumentation1.arity() == 1);
        assertFalse(unaryInstrumentation1.unique());
        assertTrue(unaryInstrumentation1.getFormula() == simpleFormula);
        assertTrue(unaryInstrumentation1.getVars().equals(unaryList));
        assertTrue(unaryInstrumentation1.getVars().size() == 1);

        assertFalse(binaryInstrumentation1.acyclic());
        assertFalse(binaryInstrumentation1.function());
        assertFalse(binaryInstrumentation1.invfunction());
        assertTrue(binaryInstrumentation1.name().equals("binaryInstrumentation1"));
        assertFalse(binaryInstrumentation1.reflexive());
        assertTrue(binaryInstrumentation1.getFormula() == equalityFormula);
        assertTrue(binaryInstrumentation1.getVars().equals(binaryList));
        assertTrue(binaryInstrumentation1.getVars().size() == 2);

        // Try to create instrumentation predicates with illegal arguments
        try {
            Instrumentation nullaryInstrumentation2 = Vocabulary.createInstrumentationPredicate("nullaryInstrumentation2", 0, true, simpleFormula, unaryList);
            fail("Vocabulary should disallow creating instrumentation predicates with illegal arguments!");
        }
        catch (Throwable e) {            
        }
        try {
            Instrumentation unaryInstrumentation2 = Vocabulary.createInstrumentationPredicate("unaryInstrumentation2", 1, true, equalityFormula, unaryList);
            fail("Vocabulary should disallow creating instrumentation predicates with illegal arguments!");
        }
        catch (Throwable e) {            
        }
        try {
            Instrumentation binaryInstrumentation2 = Vocabulary.createInstrumentationPredicate("binaryInstrumentation2", 2, true, equalityFormula, illegalBinaryVarList);
            fail("Vocabulary should disallow creating instrumentation predicates with repeated variable arguments!");
        }
        catch (Throwable e) {            
        }
        
        // Try to recreate a predicate
        try {
            Predicate binaryInstrumentation2 = Vocabulary.createPredicate("binaryInstrumentation2", 2, false, true, false);
            fail("Vocabulary should disallow creating predicates of arity different from 1 with the unique functional dependency!");
        }
        catch (Throwable e) {
        }
    }
    
    /** A test for Vocabulary.createLocationPredicate(String name, Location loc)
     */
    public void testCreateLocationPredicate() {
        assertTrue(locationPredicate1.abstraction());
        assertTrue(locationPredicate1.arity() == 1);
        assertTrue(locationPredicate1.name().equals("locationPredicate1"));
        assertFalse(locationPredicate1.unique());
    }

    /** A test for Vocabulary.getPredicateByName(String predicateName)
     */
    public void testGetPredicateByName() {
        assertTrue(Vocabulary.getPredicateByName("nullaryPredicate1") == nullaryPredicate1);
        assertTrue(Vocabulary.getPredicateByName("unaryPredicate1") == unaryPredicate1);
        assertTrue(Vocabulary.getPredicateByName("binaryPredicate1") == binaryPredicate1);
        assertTrue(Vocabulary.getPredicateByName("nullaryNonAbsPredicate1") == nullaryNonAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("nullaryAbsPredicate1") == nullaryAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("unaryNonAbsPredicate1") == unaryNonAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("unaryAbsPredicate1") == unaryAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("binaryNonAbsPredicate1") == binaryNonAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("unaryUniqueAbsPredicate1") == unaryUniqueAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("unaryUniqueNonAbsPredicate1") == unaryUniqueNonAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("unaryNonUniqueAbsPredicate1") == unaryNonUniqueAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("unaryNonUniqueNonAbsPredicate1") == unaryNonUniqueNonAbsPredicate1);
        assertTrue(Vocabulary.getPredicateByName("nullaryInstrumentation1") == nullaryInstrumentation1);
        assertTrue(Vocabulary.getPredicateByName("unaryInstrumentation1") == unaryInstrumentation1);
        assertTrue(Vocabulary.getPredicateByName("binaryInstrumentation1") == binaryInstrumentation1);
    }
    
    /** A test for Vocabulary.findLocationPredicate(String loc)
     */
    public void testFindLocationPredicate() {
        assertTrue(Vocabulary.findLocationPredicate("location1") == locationPredicate1);
        
        try {
          assertTrue(Vocabulary.findLocationPredicate("location") == null);
          fail("Vocabulary should detect a non-existent location argument!");
        }
        catch (Throwable t) {
        }
    }
    
    /** A test for Vocabulary.size()
     */
    public void testSize() {
        final int numberOfBuiltInPredicates = 8;
        final int numberOfPredicatesCreatedHere = 16;
        final int expectedSize = numberOfBuiltInPredicates + numberOfPredicatesCreatedHere;
        assertTrue("Expected to find that vocabulary's size is " + expectedSize
                + " but got " + Vocabulary.size() + "!",
                Vocabulary.size() == expectedSize);
    }

    /** A test for Vocabulary.allPredicates()
     */
    //public void testAllPredicates() {
    //}
    
    /** A test for Vocabulary.allNullaryPredicates()
     */
    //public void testAllNullaryPredicates() {
    //}

    /** A test for Vocabulary.allNullaryRelPredicates()
     */
    //public void testAllNullaryRelPredicates() {
    //}
    
    /** A test for Vocabulary.allNullaryNonRelPredicates()
     */
    //public void testAllNularyNonRelPredicates() {
    //}
    
    /** A test for Vocabulary.allUnaryPredicates()
     */
    //public void testAllUnaryPredicates() {
    //}
    
    /** A test for Vocabulary.allUnaryRelPredicates()
     */
    //public void testAllUnaryRelPredicates() {
    //}
    
    /** A test for Vocabulary.allUnaryNonRelPredicates()
     */
    //public void testAllUnaryNonRelPredicates() {
    //}
    
    /** A test for Vocabulary.allBinaryPredicates()
     */
    //public void testAllBinaryPredicates() {
    //}
    
    /** A test for Vocabulary.allKaryPredicates()
     */
    //public void testAllKaryPredicates() {
    //}
    
    /** A test for Vocabulary.allPositiveArityPredicates()
     */
    //public void testAllPositiveArityPredicates() {
    //}
    
    /** A test for Vocabulary.allInstrumentationPredicates()
     */
    //public void testAllInstrumentationPredicates() {
    //}
}