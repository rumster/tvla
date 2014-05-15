/*
 * Created on 09/01/2005
 */
package tvla.formulae;

import junit.framework.TestCase;

/**
 * @author tla
 */
public class FormulaeTest extends TestCase {
    public FormulaeTest(String arg0) {
        super(arg0);
    }

    public void testDNFOrder() {
        Var v1 = new Var("v1");
        Var v2 = new Var("v2");
        Formula simple = new AllQuantFormula(v1, new ExistQuantFormula(v2, ValueFormula.kleeneTrueFormula));
        assertEquals(simple, Formula.toPrenexCNF(simple));
        assertEquals(simple, Formula.toPrenexDNF(simple));
    }
}
