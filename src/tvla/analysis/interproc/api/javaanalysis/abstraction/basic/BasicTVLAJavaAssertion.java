package tvla.analysis.interproc.api.javaanalysis.abstraction.basic;

import tvla.analysis.interproc.api.TVLAAssertion;
import tvla.analysis.interproc.api.javaanalysis.abstraction.IJavaAssertion;
import tvla.formulae.Formula;

/***************************************************************
 * Supported Analyses
 ***************************************************************/

class BasicTVLAJavaAssertion extends TVLAAssertion implements IJavaAssertion
{
	BasicTVLAJavaAssertion(Formula formula) {
		super(formula);
	}		
}