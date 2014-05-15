package tvla.api;

import tvla.analysis.interproc.api.utils.TVLAAPIAssert;
import tvla.api.ITVLAAPI.ITVLATabulatorServices;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaDebuggingServices;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAdapter;
import tvla.api.TVLAAnalysisOptions.TVLAAPIOptions;
import tvla.api.TVLAAnalysisOptions.TVLAJavaAdapeterOptions;
import tvla.api.TVLAAnalysisOptions.TVLAOptions;

/**
 * A factory class wrapping the single object implementing the TVLAAPI inerface.
 * 
 * @author maon
 */

public final class TVLAFactory {
  private static int DEBUG_LEVEL = 0;
  private static AbstractTVLAAPI theTVLA = null;
  private static AbstractTVLAJavaAdapter theJavaAdapter = null;
  private static final int analysisCodes[] = { -1, -1 };

  private static int implIndex = -1;

  /*****************************************************************************
   * TVLAJavaAdapter generation
   *****************************************************************************/

  public static ITVLAJavaAdapter getJavaAnalyzer(TVLAAnalysisOptions options) {
    if (options == null)
      return null;
    
    // TVLAClassLoader.setTVLAClassLoader();
    
    TVLAOptions tvlaOptions = options.getTvlaOptions();
    TVLAAPIOptions apiOptions = options.getApiOptions();
    TVLAJavaAdapeterOptions javaAdapterOptions = options.getJavaAdapterOptions();

    ITVLAAPI tvlaapi = genTVLAAPIForJavaAdapter(options);
    if (tvlaapi == null)
      return null;
    
    if (TVLAAPIAssert.ASSERT) {
      TVLAAPIAssert.debugAssert(tvlaapi == theTVLA);
    }
    
    ITVLAJavaAdapter adapter = getTVLAJavaAdapter(
        javaAdapterOptions.getEnvironmentServicesProvider(),
        javaAdapterOptions.getAnalysisStr(),
        apiOptions.getImpl());
    
    
    adapter.setParametericDomain(
        tvlaOptions.getCommandLineArgs(), 
        tvlaOptions.getPropertyFiles(), 
        tvlaOptions.getAnalysisDirName(), 
        tvlaOptions.getAnalysisMainFileName(), 
        tvlaOptions.getOutputDir());
    
    return adapter;     
  }
  
  
 
  protected static ITVLAJavaAdapter getTVLAJavaAdapter(
      ITVLAJavaAnalsyisEnvironmentServicesPovider  environmentServicesProvider,
      String analysisStr, 
      String impl) {

    ITVLAJavaDebuggingServices client = environmentServicesProvider.getJavaDebuggingServices();
    
    if (0 < DEBUG_LEVEL)
      client.tracePrintln("TVLAFactory.getTVLAJavaAdapter analysisStr = " + analysisStr + " impl = " + impl);

    if (analysisStr == null) {
      report(client, "abstraction not specificed");
      return null;
    }

    int[] analCodes = TVLAAnalysisOptions.getAnalysisCodes(analysisStr);
    if (analCodes == null) {
      report(client, "abstraction not recognaized");
      return null;
    }
    
    if (-1 < TVLAFactory.analysisCodes[TVLAAnalysisOptions.abstractionEntry]
        && analCodes[TVLAAnalysisOptions.abstractionEntry] != TVLAFactory.analysisCodes[TVLAAnalysisOptions.abstractionEntry]) {
      report(client, " JavaAdapter already instanitated with a different abstraction");
      return null;
    }

    if (-1 < TVLAFactory.analysisCodes[TVLAAnalysisOptions.transformerEntry]
        && analCodes[TVLAAnalysisOptions.transformerEntry] != TVLAFactory.analysisCodes[TVLAAnalysisOptions.transformerEntry]) {
      report(client, " JavaAdapter already instanitated with different tranformers");
      return null;
    }


    if (theJavaAdapter != null)
      // Already instantiated the Java Adpater with the right implementation
      return theJavaAdapter;

 
    client.debugAssert(-1 < TVLAFactory.implIndex);
    theJavaAdapter = (AbstractTVLAJavaAdapter) genObject(TVLAAnalysisOptions.imps[implIndex][TVLAAnalysisOptions.javaAdapterIndx], client);
    TVLAFactory.analysisCodes[TVLAAnalysisOptions.abstractionEntry] = analCodes[TVLAAnalysisOptions.abstractionEntry];
    TVLAFactory.analysisCodes[TVLAAnalysisOptions.transformerEntry] = analCodes[TVLAAnalysisOptions.transformerEntry];

    theJavaAdapter.setParam(theTVLA, environmentServicesProvider,  analCodes);

    return theJavaAdapter;
  }

  
  protected static ITVLAAPI genTVLAAPIForJavaAdapter(TVLAAnalysisOptions options)       
  {      
    ITVLATabulatorServices tabulationServices = options.getJavaAdapterOptions().getEnvironmentServicesProvider().getJavaTabulationServices();
    ITVLAAPIDebuggingServices client =  options.getJavaAdapterOptions().getEnvironmentServicesProvider().getJavaDebuggingServices();
    String impl = options.getApiOptions().getImpl();
    
    if (impl == null) {
      report(client, "implementation not specificed");
      return null;
    }

    if (theTVLA == null)
      getTVLAAPI(tabulationServices, client, impl);

     return theTVLA;
  }


  /*****************************************************************************
   * TVLAAPI generation
   *****************************************************************************/  
  /**
   * @return the exisiting TVLAAPI implementation, or null if it was not created
   */
  public static ITVLAAPI getTVLAAPI() {
    return theTVLA;
  }

  
  public static ITVLAAPI getTVLAAPI(
      ITVLATabulatorServices chaoticEngine, 
      ITVLAAPIDebuggingServices client) {
    return getTVLAAPI(
        chaoticEngine, 
        client, 
        TVLAAnalysisOptions.imps[TVLAAnalysisOptions.defaultImplIndex][TVLAAnalysisOptions.nameIndx]);
  }

  public static ITVLAAPI getTVLAAPI(
      ITVLATabulatorServices chaoticEngine, 
      ITVLAAPIDebuggingServices client, 
      String impl) {
    if (0 < DEBUG_LEVEL)
      client.tracePrintln("TVLAFactory.getTVLAJavaAdapter getTVLAAPI " + " impl =" + impl);

    final int implCode = TVLAAnalysisOptions.getImplIndex(impl);

    if (implCode < 0) {
      report(client, "Implementation not recognaized");
      return null;
    }

    if (-1 < TVLAFactory.implIndex && TVLAFactory.implIndex != implCode) {
      report(client, "TVLAAPI already instanitated with a different implementation");
      return null;
    }

    if (theTVLA != null)
      return theTVLA;

    theTVLA = (AbstractTVLAAPI) genObject(TVLAAnalysisOptions.imps[implCode][TVLAAnalysisOptions.apiIndx], client);
    if (theTVLA != null)
      implIndex = implCode;
    else
      report(client, "TVLAFactory : failed to instantiate TVLAAPI implementation - " + TVLAAnalysisOptions.imps[implCode][TVLAAnalysisOptions.nameIndx]);

    if (theTVLA != null) {
      theTVLA.setFrontendServices(chaoticEngine, client);
    }
    
    return theTVLA;
  }


  protected static Object genObject(String fullClassName, ITVLAAPIDebuggingServices client) {
    Class klass = null;
    Object ret = null;

    TVLAClassLoader tcl = new TVLAClassLoader();

    try {
      klass = tcl.loadClass(fullClassName);
      ret = klass.newInstance();
    } catch (ClassNotFoundException e) {
      report(client, " TVLA Implmentation " + fullClassName + " not found!");
      ret = null;
    } catch (InstantiationException e) {
      report(client, " Failed to initialized TVLA Implmentation " + fullClassName);
      ret = null;
    } catch (IllegalAccessException e) {
      report(client, " Got an illegal access exception while initializing TVLA Implmentation " + fullClassName);
      ret = null;
    }

    return ret;
  }

  protected static void report(ITVLAAPIDebuggingServices client, String msg) {
    // instantiation failure
    if (client != null) {
      client.tracePrintln(msg);
    } else {
      System.err.println(msg);
    }
  }
}
