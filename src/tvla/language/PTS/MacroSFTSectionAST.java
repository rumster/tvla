package tvla.language.PTS;

/**
 * This class is used to insert the macros definition inot the abstract domain.
 * It is a "hacky" implmentwtion using the pasta as a base class an overriding  the 
 * generate function which now adds the macros to the abstract domain instead of the InterProcEngine 
 */

import java.util.Iterator;

import tvla.analysis.interproc.api.tvlaadapter.TVLAAPI;
import tvla.api.TVLAFactory;


public class MacroSFTSectionAST extends MacroSectionAST {
	public void generate() {
		TVLAAPI tvlaPastaAPI = (TVLAAPI) TVLAFactory.getTVLAAPI();
		for (Iterator i = macroList.iterator(); i.hasNext(); ) {
			ActionMacroAST macro = (ActionMacroAST) i.next();
			tvlaPastaAPI.actionAddDefinition(macro);
		}
	}
}
