package tvla.iawp.tp.spass;

import tvla.formulae.Formula;
import tvla.iawp.tp.NativeProcess;
import tvla.iawp.tp.TheoremProverOutput;
import tvla.iawp.tp.TheoremProverResult;
import tvla.util.ProgramProperties;

/**
 * @author user
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SpassCNFConverter extends Spass {

	/**
	 * Constructor for SpassCNFConverter.
	 */
	public SpassCNFConverter() {
		super();
		
		String exec =
			ProgramProperties.getProperty("tvla.tp.spass.executable", "spass");
		String path =
			ProgramProperties.getProperty(
				"tvla.tp.spass.path",
				"c:\\Program Files\\spass\\");
		String options =
			ProgramProperties.getProperty(
				"tvla.tp.cnfspass.parameters",
				"-PStatistic=0 -PGiven=0 -PProblem=0 -Stdin=1 -Flotter=1");
		this.invokeString = path + exec + " " + options;
	}
	
	public TheoremProverOutput proveQuery(String exp) {
		initQuery();
		
		StringBuffer query = new StringBuffer();
		query.append(SpassTranslation.startAxioms);
		query.append(exp);
		query.append("\n");
		query.append(SpassTranslation.endAxioms);
		query.append(SpassTranslation.problemFooter);
		
//		System.out.println("Meme:"+query.toString().length()+query.toString().substring(1,2000));
			
		currQuery = new SpassCNFOutput(np, query.toString());
		return currQuery;
	}
	
	public TheoremProverResult prove(Formula f) {
		//generateTCEnvironment(f);
		initNativeProcess();
		return prove(translator.translate(f));
	}

	private void initNativeProcess() {
		np = new NativeProcess("SPass", invokeString);
		currQuery = null;
		from = np.fromStream();
		execute(SpassTranslation.problemHeader);
		adjustVocabulary();
		defineVocabularyPredicates();
		//Vocabulary.addChangeListener(this);
		adjustAssumptions();
		//Assumptions.addChangeListener(this);
	}


}
