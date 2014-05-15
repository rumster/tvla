//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.api;


import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalysisResultsServices;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaDebuggingServices;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModelerServices;
import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaTabulatorServices;


public class TVLAAnalysisOptions {
  // Analysis code
  public static final int abstractionEntry = 0;
  public static final int transformerEntry = 1;

  private static final String abstractions[] = { "typePiles", "allocationSites", "allocationSitesNN", "localHeaps" };
  public static final int typePilesAbstraction = 0;
  public static final int allocationSitesAbstraction = 1;
  public static final int allocationSitesAbstractionNN = 2;
  public static final int localHeapsAbstraction = 3;
  public static final int defaultAbstractionIndex = allocationSitesAbstraction;
  public static final int maxAbstractionIndex = abstractions.length - 1;

  private static final String transformers[] = { "FPlocalHeaps", "FPInterNone", "FPInterPreserveLocal", "FPusePartialheapPT" };
  public static final int localHeapsTransformer = 0;
  public static final int interNoneTransformer = 1;
  public static final int interPreserveLocalTransformer = 2;
  public static final int interPartialHeapsTransformer = 3;
  public static final int defaultTransformerIndex = localHeapsTransformer;
  public static final int maxTransformerIndex = transformers.length - 1;

  // API implementation
  public static final int nameIndx = 0;
  public static final int apiIndx = 1;
  public  static final int javaAdapterIndx = 2;
  public static final String imps[][] = 
  { 
    { "pastaapi", 
      "tvla.analysis.interproc.api.tvlaadapter.TVLAAPI",
      "tvla.analysis.interproc.api.TVLAJavaAdapter" 
    } 
  };
  public static final int pastaImplIndex = 0;
  public static final int defaultImplIndex = pastaImplIndex;


  /*********************************************************************************************
   * API Implementation
   */
  
  public static int getImplIndex(String implName) {
    if (implName != null) {
      for (int i = 0; i < imps.length; i++)
        if (imps[i][nameIndx].equals(implName))
          return i;
    }
    
    return -1;
  }


  /*********************************************************************************************
   * Analysis codes
   */

  public static int[] getAnalysisCodes(String analysisStr) {
    if (analysisStr == null)
      return null;

    final int sep = analysisStr.indexOf(':');
    if (sep < 0)
      return null;

    String abstraction = analysisStr.substring(0, sep);
    String transformer = analysisStr.substring(sep + 1);

    if (abstraction == null || transformer == null)
      return null;

    int absIndex = getAbstractionIndex(abstraction);
    int transIndex = getTransformerIndex(transformer);

    if (absIndex < 0 || transIndex < 0)
      return null;

    int[] ret = { absIndex, transIndex };
    return ret;
  }

  public static int getAbstractionIndex(String abstractionCode) {
    for (int i = 0; i < abstractions.length; i++)
      if (abstractions[i].equals(abstractionCode))
        return i;

    return -1;
  }

  public static int getTransformerIndex(String transformerCode) {
    for (int i = 0; i < transformers.length; i++)
      if (transformers[i].equals(transformerCode))
        return i;

    return -1;
  }

  /*********************************************************************************************
   * Verifier
   */

  /**
   * Return null if all is or a mesage with description of propblem
   */
  public static String consistent(String analysisCode) {
    int[] codes = getAnalysisCodes(analysisCode);
    if (codes == null)
      return "Ill formated / unrecognised options";
    
    
    // TODO add verification that the properties are ok
    return null;
  }
  
  /*********************************************************************************************
   * Utilities
   */

  
  public static String getAnalysisStr(int k) {
    if (0 < k || maxAbstractionIndex < k)
      return null;
    
    return abstractions[k];
  }
  
  public static void report(ITVLAAPIDebuggingServices client, String msg) {
    // instantiation failure
    if (client != null) {
      client.tracePrintln(msg);
    } else {
      System.err.println(msg);
    }
  }

  
  /***********************************************/
  /** Parameters needed for instantiating TVLA  **/
  /***********************************************/
  
  public static class TVLAOptions {
    protected String[] commandLineArgs;
    protected String[] propertyFiles;
    protected String   analysisDirName;
    protected String   analysisMainFileName; 
    protected String   outputDir;
   
    public TVLAOptions(
        String[] args, 
        String[] propFiles, 
        String analysisDir, 
        String rootFile, 
        String outDir) {
      commandLineArgs = args;
      propertyFiles = propFiles;
      analysisDirName = analysisDir;
      analysisMainFileName = rootFile;
      outputDir = outDir;
    }
    
    
    public void print(StringBuffer to) {
      addLine(to, "  TVLAOptions");
      if (commandLineArgs == null) {       
        addLine(to, "     commandLineArgs = null (i.e., no options set)");
      }
      else {
        for (int i=0; i < commandLineArgs.length; i++)
          addLine(to, "     commandLineArgs[" + i + "]  = " + commandLineArgs[i]);                   
      }
  
      if (propertyFiles == null) {       
        addLine(to, "     propertyFiles  = (i.e., no property files set)");
      }
      else {
        for (int i=0; i < propertyFiles.length; i++)
          addLine(to, "     propertyFiles[" + i + "]  = " + propertyFiles[i]);               
      }
      addLine(to, "     analysisDirName  = " + analysisDirName);
      addLine(to, "     analysisMainFileName  = " + analysisMainFileName);
      addLine(to, "     outputDir  = " + outputDir);
    }
    
    public String toString() {
      StringBuffer to = new StringBuffer();
      print(to);
      return to.toString();
    }


    public String getAnalysisDirName() {
      return analysisDirName;
    }


    public String getAnalysisMainFileName() {
      return analysisMainFileName;
    }


    public String[] getCommandLineArgs() {
      return commandLineArgs;
    }


    public String getOutputDir() {
      return outputDir;
    }


    public String[] getPropertyFiles() {
      return propertyFiles;
    }
    
    
    
   }
  
  /**********************************************/
  /** Parameters needed for creating a TVLAAPI **/
  /**********************************************/

  public static class TVLAAPIOptions {
    protected ITVLAJavaTabulatorServices chaoticEngine;
    protected ITVLAJavaDebuggingServices client; 
    protected String impl;

    public TVLAAPIOptions(
        ITVLAJavaTabulatorServices engine, 
        ITVLAJavaDebuggingServices client) {
      this(engine, client, imps[defaultImplIndex][nameIndx]);
    }    
    
    public TVLAAPIOptions(
        ITVLAJavaTabulatorServices engine, 
        ITVLAJavaDebuggingServices client, 
        String impl) {
      this.chaoticEngine = engine;
      this.client = client;
      this.impl = impl;
    }    

    public void print(StringBuffer to) {
      addLine(to, "  TVLAAPIOptions");
      addLine(to, "     engine  = " + chaoticEngine);
      addLine(to, "     client  = " + client);
      addLine(to, "     impl  = " + impl);
    }
    
    public String toString() {
      StringBuffer to = new StringBuffer();
      print(to);
      return to.toString();
    }
    
    public ITVLAJavaTabulatorServices getChaoticEngine() {
      return chaoticEngine;
    }

    public ITVLAJavaDebuggingServices getClient() {
      return client;
    }

    public String getImpl() {
      return impl;
    }
    
    
  }
  
  /******************************************************/
  /** Parameters needed for creating a TVLAJavaAdapter **/
  /******************************************************/

  public static class TVLAJavaAdapeterOptions  {
    protected ITVLAJavaAnalsyisEnvironmentServicesPovider  environmentServicesProvider;
    protected ITVLAJavaProgramModelerServices programModel;
    protected ITVLAJavaAnalysisResultsServices precedingAnalysisResults;
    protected String analysisStr;
 
    public TVLAJavaAdapeterOptions(
        ITVLAJavaAnalsyisEnvironmentServicesPovider  environmentServicesProvider,
        ITVLAJavaProgramModelerServices model, 
        ITVLAJavaAnalysisResultsServices precedingAnalysisResults) {
      this(
          environmentServicesProvider,
          model, 
          precedingAnalysisResults,
          abstractions[defaultAbstractionIndex] + ":" + transformers[defaultTransformerIndex]);
    }
  
    public TVLAJavaAdapeterOptions(
        ITVLAJavaAnalsyisEnvironmentServicesPovider  environmentServicesProvider,
        ITVLAJavaProgramModelerServices model, 
        ITVLAJavaAnalysisResultsServices precedingAnalysisResults,
        String str) {
      this.environmentServicesProvider = environmentServicesProvider;
      this.programModel = model;
      this.precedingAnalysisResults = precedingAnalysisResults;
      this.analysisStr = str;
    }
    
    public void print(StringBuffer to) {
      addLine(to, "  TVLAJavaAdapeterOptions");
      addLine(to, "     analysisStr  = " + analysisStr);
      addLine(to, "     programModel  = " + programModel);
      addLine(to, "     precedingAnalysiResults = " + precedingAnalysisResults);
    }
    
    public String toString() {
      StringBuffer to = new StringBuffer();
      print(to);
      return to.toString();
    }

    public String getAnalysisStr() {
      return analysisStr;
    }

    public ITVLAJavaProgramModelerServices getProgramModel() {
      return programModel;
    }
   
    public ITVLAJavaAnalsyisEnvironmentServicesPovider getEnvironmentServicesProvider() {
      return environmentServicesProvider;
    }
    
    ITVLAJavaAnalysisResultsServices getPrecedingAnalysisResults() {
      return precedingAnalysisResults;
    }  
  }  

  /****************************************/
  /** Parameters needed for the analysis **/
  /****************************************/

  protected TVLAOptions tvlaOptions;
  protected TVLAAPIOptions apiOptions;
  protected TVLAJavaAdapeterOptions javaAdapterOptions;


  public TVLAAnalysisOptions(
      TVLAOptions tvlaOptions, 
      TVLAAPIOptions apiOptions, 
      TVLAJavaAdapeterOptions javaAdapterOptions) {
     this.tvlaOptions = tvlaOptions;
     this.apiOptions = apiOptions;
     this.javaAdapterOptions = javaAdapterOptions;
  }
  
  
  public TVLAAPIOptions getApiOptions() {
    return apiOptions;
  }


  public TVLAJavaAdapeterOptions getJavaAdapterOptions() {
    return javaAdapterOptions;
  }


  public TVLAOptions getTvlaOptions() {
    return tvlaOptions;
  }


  public void setApiOptions(TVLAAPIOptions apiOptions) {
    this.apiOptions = apiOptions;
  }


  public void setJavaAdapterOptions(TVLAJavaAdapeterOptions javaAdapterOptions) {
    this.javaAdapterOptions = javaAdapterOptions;
  }


  public void setTvlaOptions(TVLAOptions tvlaOptions) {
    this.tvlaOptions = tvlaOptions;
  }


  public void print(StringBuffer to) {
    addLine(to, "TVLAAnalysisOptions");
    tvlaOptions.print(to);
    apiOptions.print(to);
    javaAdapterOptions.print(to);
  }
  
  public String toString() {
    StringBuffer to = new StringBuffer();
    print(to);
    return to.toString();
  }
 
  
  
  protected static void addLine(StringBuffer to, String str) {
    to.append(str);
    to.append("\n");
  }
  
}

