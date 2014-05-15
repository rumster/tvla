//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api;

import tvla.api.ITVLAAPI.ITVLAAssertion;
import tvla.formulae.Formula;

public class TVLAAssertion implements ITVLAAssertion {
    private Formula formula;
    
    public TVLAAssertion(Formula formula) {
        this.formula = formula;
    }
    
    public Formula getFormula() {
        return formula;
    }
}
