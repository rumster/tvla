/**
 * 
 */
package tvla.core;

import tvla.logic.Kleene;
import junit.framework.TestCase;

/**
 * @author Roman Manevich
 *
 */
public class CanonicTest extends TestCase {
	/**
	 * Test method for {@link tvla.core.Canonic#agreesWith(tvla.core.Canonic)}.
	 */
	public void testAgreesWith() {
		Canonic c1 = new Canonic();
		Canonic c2 = new Canonic();
		
		// c1=<0,1,1/2>
		c1.add(Kleene.falseKleene);
		c1.add(Kleene.trueKleene);
		c1.add(Kleene.unknownKleene);		

		// c2=<0,1,1/2>
		c2.add(Kleene.falseKleene);
		c2.add(Kleene.trueKleene);
		c2.add(Kleene.unknownKleene);
		
		assertTrue(c1.agreesWith(c2));
		
		// c1=<0,1,1/2, 1/2,1/2>
		c1.add(Kleene.unknownKleene);
		c1.add(Kleene.unknownKleene);

		// c2=<0,1,1/2, 1,0>
		c2.add(Kleene.trueKleene);
		c2.add(Kleene.falseKleene);
		
		assertTrue(c1.agreesWith(c2));
		
		// c1=<0,1,1/2, 1/2,1/2, 1>
		c1.add(Kleene.trueKleene);
		
		// c2=<0,1,1/2, 1,0, 0>
		c2.add(Kleene.falseKleene);
		
		assertTrue(!c1.agreesWith(c2));
		
		Canonic c3 = new Canonic();
		Canonic c4 = new Canonic();		
		c3.add(Kleene.trueKleene);
		c4.add(Kleene.falseKleene);		
		assertTrue(!c3.agreesWith(c4));
	}

	/**
	 * Test method for {@link tvla.core.Canonic#lessThanOrEqual(tvla.core.Canonic)}.
	 */
	public void testLessThanOrEqual() {
		// test the partial ordering on all pairs of Kleene values.
		Canonic trueCanonic = new Canonic();
		Canonic falseCanonic = new Canonic();
		Canonic unknownCanonic = new Canonic();
		trueCanonic.add(Kleene.trueKleene);
		falseCanonic.add(Kleene.falseKleene);
		unknownCanonic.add(Kleene.unknownKleene);		
		
		assertTrue(trueCanonic.lessThanOrEqual(unknownCanonic));
		assertTrue(falseCanonic.lessThanOrEqual(unknownCanonic));
		assertFalse(unknownCanonic.lessThanOrEqual(trueCanonic));
		assertFalse(unknownCanonic.lessThanOrEqual(falseCanonic));
		assertFalse(falseCanonic.lessThanOrEqual(trueCanonic));
		assertFalse(trueCanonic.lessThanOrEqual(falseCanonic));
		
		// Now check longer canonical names.		
		Canonic c1 = new Canonic();
		Canonic c2 = new Canonic();
		
		// c1=<0,1,1/2>
		c1.add(Kleene.falseKleene);
		c1.add(Kleene.trueKleene);
		c1.add(Kleene.unknownKleene);		

		// c2=<0,1,1/2>
		c2.add(Kleene.falseKleene);
		c2.add(Kleene.trueKleene);
		c2.add(Kleene.unknownKleene);
		
		assertTrue(c1.lessThanOrEqual(c2));
		assertTrue(c2.lessThanOrEqual(c1));
		
		// c1=<0,1,1/2, 1/2,1/2>
		c1.add(Kleene.unknownKleene);
		c1.add(Kleene.unknownKleene);

		// c2=<0,1,1/2, 1,0>
		c2.add(Kleene.trueKleene);
		c2.add(Kleene.falseKleene);
		
		assertFalse(c1.lessThanOrEqual(c2));
		assertTrue(c2.lessThanOrEqual(c1));
		
		// c1=<0,1,1/2, 1/2,1/2, 1>
		c1.add(Kleene.trueKleene);
		
		// c2=<0,1,1/2, 1,0, 0>
		c2.add(Kleene.falseKleene);
		
		assertFalse(c1.lessThanOrEqual(c2));
		assertFalse(c2.lessThanOrEqual(c1));
	}
}