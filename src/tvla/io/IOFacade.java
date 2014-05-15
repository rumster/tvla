package tvla.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import tvla.analysis.TransitionRelation;
import tvla.transitionSystem.PrintableProgramLocation;
import tvla.util.HashMapFactory;
import tvla.util.ProgramProperties;
import tvla.util.PropertiesEx;
import tvla.util.StringUtils;

/** A class for directing information produced by the analysis to output streams.
 * This class is used to separate logical outputs from physical streams.
 * @author Roman Manevich.
 * @since tvla-2-alpha.
 */
public abstract class IOFacade implements TVLAIO {
  /** The one and only instance of this class.
   */
  private static IOFacade theInstance;
  
  /** Maps the name of an implementation to its corresponding ImplementationBundle.
   */
  protected Map implementations = HashMapFactory.make();
  
  protected boolean xdebug = false;
  
  
  /** 
   * Singleton pattern.
   */
  
  protected static final String engineTVLA = "tvla";
  protected static final String enginePASTA = "pasta";
  protected static final String engineAPI = "api";
  
  public static void reset() {
	  theInstance.close();
	  theInstance = null;
  }
  
  public static IOFacade instance() {
    if (theInstance != null)
      return theInstance;
    
    String engineType = ProgramProperties.getProperty("tvla.engine.type", engineTVLA);
    
    if (engineType.equals(enginePASTA))
      theInstance = new InterProcIOFacade();
    else if (engineType.equals(engineAPI))
      theInstance = new APIIOFacade();   
    else 
      theInstance = new IntraIOFacade();
    
    /* TODO remove this code fragment
     if (Engine.activeEngine instanceof InterProcEngine)
     theInstance = new InterProcIOFacade();
     else 
     theInstance = new IntraIOFacade();			
     */
    
    return theInstance;
  }
  
  public static String fileSeperator;
  
  ////////////////////////////////////////////////////////
  ////              Facade initialization            /////
  ////////////////////////////////////////////////////////
  
  protected IOFacade() {
    fileSeperator = File.separator; //ProgramProperties.getProperty("tvla.output.file.seperator", "\\");		
    initImplementations();
    printHeader();
  }	
  
  /** Loads the different output converter implementations into 
   * ImplementationBundles.
   */
  private void initImplementations() {
    PropertiesEx props = new PropertiesEx("/tvla/io/tvla.io.properties");
    List implementations = props.getStringListProperty("implementations", Collections.EMPTY_LIST);
    for (Iterator i = implementations.iterator(); i.hasNext(); ) {
      String implementation = (String) i.next();
      this.implementations.put(implementation, new ImplementationsBundle(implementation) );
    }
  }
  
  /** Prints general information, such as the TVLA version, to all output files.
   */
  private void printHeader() {
    String version = new PropertiesEx("/tvla/version.properties").getProperty("version", "Unknown TVLA version");
    String header = version;
    
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      StringConverter c = bundle.commentConverter;
      if (bundle.outputEnabled && c!=null) {
        String convertedHeader = c.convert(header);
        bundle.outputStream.println(convertedHeader);
      }
    }
  }
  
  
  ////////////////////////////////////////////////////////
  ////                    Printing                   /////
  ////////////////////////////////////////////////////////
  
  
  public void flush() {
    for (Iterator i = implementations.values().iterator(); i.hasNext();) {
        ImplementationsBundle bundle = (ImplementationsBundle) i.next();
        if (bundle.outputEnabled) {
            bundle.outputStream.flush();
        }
    }
  }
  
  public void close() {
	    for (Iterator i = implementations.values().iterator(); i.hasNext();) {
	        ImplementationsBundle bundle = (ImplementationsBundle) i.next();
	        if (bundle.outputEnabled) {
	            bundle.outputStream.close();
	        }
	    }
	  }
  
  /** Prints a structure to all desired outputs.
   * @param structure A TVS.
   * @param header An optional header.
   */
  protected void printStructure(Object structure, String header, String breachedHeader) {
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      StringConverter c = bundle.structureConverter;
      if (bundle.outputEnabled && c != null) {
        String converted = c.convert(structure, header);
        bundle.outputStream.println(converted);
      }
    }
  }
  
//
///** Prints a location with its structures to all desired outputs.
//* @param location A Location.
//*/
//public void printLocation(PrintableProgramLocation location) {
//for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
//ImplementationsBundle bundle = (ImplementationsBundle) i.next();
//StringConverter c = bundle.locationConverter;
//if (bundle.outputEnabled && c != null) {
//String converted = convertNode(c, location);
//bundle.outputStream.println(converted);
//}
//}
//}
  
  /** Prints a location with its structures to all desired outputs.
   * @param location A Location.
   */
  public void printLocation(PrintableProgramLocation location) {
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      LocationConverter c = (LocationConverter) bundle.locationConverter;
      if (bundle.outputEnabled && c != null) {
        if (!bundle.messagesEnabled) {
          assert(bundle.messagesStream == null);
          String converted = c.convert(location);
          bundle.outputStream.println(converted);
        }
        else {
          assert(bundle.outputStream != null);
          assert(bundle.messagesStream != null);
          
          StringBuffer structStr = new StringBuffer(c.locationHeader(location.label()));
          structStr.append(c.convertStructures(location));
          structStr.append(c.locationFooter(location.label()));
          bundle.outputStream.println(structStr);
          structStr = null; // opportunity for compile time GC 
          
          StringBuffer msgsStr = new StringBuffer(c.locationHeader(location.label()));
          msgsStr.append(c.convertMessages(location));
          msgsStr.append(c.locationFooter(location.label()));
          bundle.messagesStream.println(msgsStr);
          msgsStr = null; // opportunity for compile time GC 
        }		
      }
    }
  }
  
  
//protected abstract String convertNode(StringConverter c, Object node);
  
  /** Prints a MethodTS to all desired outputs.
   * @param program A collection of transitions.
   */
  public void printProgram(Object program) {
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      StringConverter c = bundle.programConverter;
      if (bundle.outputEnabled && c != null) {
        printProgram(bundle.outputStream, c, program);
      }
    }
  }
  
  protected abstract void printProgram(java.io.PrintStream bundleStream, StringConverter c, Object program);
  
  
  /** Prints the state of the analysis - transitions and their structures to
   * all desired outputs.
   * @param state A collection of transitions.
   */
  public abstract void printAnalysisState(Object state);
  
  /**
   * Prints a page which contains only a banner
   */
  public void printBanner(String banner) {
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      if (bundle.bundleEnabled && bundle.outputEnabled) {
        String convertedBanner = bundle.bannerConverter.convert(banner);
        bundle.outputStream.println(convertedBanner);
      }
    }
  }
  
  /**
   * Prints a page which contains only a banner to the messages stream if exists,
   * or to the outputstream otherwise
   */
  public void printMessageBanner(String banner) {
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      if (bundle.bundleEnabled && bundle.outputEnabled) {
        String convertedBanner = bundle.bannerConverter.convert(banner);
        if (bundle.messagesEnabled) {
          assert(bundle.messagesStream != null);
          bundle.messagesStream.println(convertedBanner);
        }
        else if (bundle.outputEnabled) {
          assert(bundle.outputStream != null);
          bundle.outputStream.println(convertedBanner);
        }
      }
    }	
  }	
  
  /**
   * Prints a strucutre with an associated message.
   * If the message stream is not null, output is directed to the messagfe stream.
   * Otherwise, it is printed into the outputstream.  
   */
  public void printStrucutreWithMessages(String header, Object tvs, Collection messages) {
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      
      ///////////
      PrintStream printTo = null;
      StringConverter c = bundle.structureConverter;
      if (c!= null) {
        if (bundle.messagesEnabled)
          printTo = bundle.messagesStream;
        else if (bundle.outputEnabled)
          printTo = bundle.outputStream;
      }
      
      if (printTo == null)
        continue;
      
      StringBuffer msgs = new StringBuffer(header + ": ");
      Iterator messagesItr = messages.iterator();
      
      int msgNum = 1;
      while (messagesItr.hasNext()) {
        String msg = (String) messagesItr.next();
        msgs.append(StringUtils.newLine +  msgNum + ". " + msg);
        msgNum++;
      }
      
      // print the structure with the accompnied messages to the messages stream
//    String quotedMsgs = c.quote(msgs.toString());
      String tvsWithMessages = c.convert(tvs,msgs.toString());
      printTo.println(tvsWithMessages);
      messages = null; // Opportunity for compile-time GC.
    }
  }
  
  public void redirectOutput(String streamName) {
    assert(genValidStreamName(streamName).equals(streamName));
    
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      if (bundle.bundleEnabled) 
        bundle.redirectOutput(streamName);
    }
    
  }
  
  public String genValidStreamName(String baseName) {
    String cleanName = baseName;
    cleanName = cleanName.replaceAll(":","");
    cleanName = cleanName.replaceAll(" ", "_");
    cleanName = cleanName.replaceAll("<","");
    cleanName = cleanName.replaceAll(">","");
    cleanName = cleanName.replace('\\','_');
    cleanName = cleanName.replace('/','_');
    cleanName = cleanName.replace('|','_');
    cleanName = cleanName.replace('?','_');
    cleanName = cleanName.replace('*','_');
    cleanName = cleanName.replace('\"','_');
    
    return cleanName;
  }
  
  
  /******************************************************/
  /**                 Optional Method                  **/
  /******************************************************/
  
  /** Prints a structure to all desired outputs.
   * @param structure A TVS.
   * @param header An optional header.
   */
  protected void printConstraintBreach(Object origStructure, 
      Object focusedStructure,
      Object updateStrucutre,
      Object ConstraintBreach,
      String header, 
      String breachedHeader) {
    throw new UnsupportedOperationException("printConstraintBreach not supported");
  }
  
  
  
  public void printVocabulary() {
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      StringConverter c = bundle.vocabularyConverter;
      if (bundle.outputEnabled && c != null) {
        String converted = c.convert(null);
        bundle.outputStream.println(converted);
      }
    }
  }
  
  
//public void printConstraints() {
//for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
//ImplementationsBundle bundle = (ImplementationsBundle) i.next();
//StringConverter c = bundle.coerceConverter;
//if (bundle.outputEnabled && c != null) {
//String converted = c.convert(null);
//bundle.outputStream.println(converted);
//}
//}
//}
  
  public FileWriter getFileWriter(String subDir, String fileName, String suffix) {
    String outputDirectory = ProgramProperties.getProperty("tvla.output.outputDirectory", "null");
    assert (outputDirectory != null && !outputDirectory.equals("null"));
    
    String outputFile = outputDirectory + IOFacade.fileSeperator + 
    subDir + IOFacade.fileSeperator +  
    fileName + "." + suffix;
    FileWriter ret = null;
    
    try {
      File dir = new File(outputDirectory  + IOFacade.fileSeperator + subDir);
      
      boolean dirExists = dir.exists();
      if (!dirExists) {
        if (xdebug) 
          System.err.println("Output directory " + outputDirectory + " created");					 	
        
        // Create output directory  if it does not exist
        dir.mkdirs();
      }
      else {
        // directory exits. If the file exists - erase it	
        File file = new File(outputFile);
        // why fd o we call this ? boolean exists = 
        file.exists();
        // erase existing file
        file.delete();
      }
      
      ret = new FileWriter(outputFile);
    }
    catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    
    return ret;
  }	
  
  public void printTransitionRelation(TransitionRelation transitionRelation) {
    for (Iterator i = implementations.values().iterator(); i.hasNext(); ) {
      ImplementationsBundle bundle = (ImplementationsBundle) i.next();
      StringConverter c = bundle.transitionRelationConverter;
      if (bundle.outputEnabled && bundle.transitionEnabled && c != null) {
        assert(bundle.outputStream != null && bundle.transitionStream != null);
        c.print(bundle.transitionStream, transitionRelation, null);
      }
    }
    
  }
  
  
  /** This class stores string converters.
   */
  protected static class ImplementationsBundle {
    public StringConverter analysisStateConverter;
    public StringConverter commentConverter;
    public LocationConverter locationConverter;
    public StringConverter programConverter;
    public StringConverter structureConverter;
    public StringConverter bannerConverter;
    
    // Optional converters
    public StringConverter vocabularyConverter;
//  public StringConverter coerceConverter;
    public StringConverter transitionRelationConverter;
    
    
    public PrintStream outputStream;
    public PrintStream messagesStream;
//  public PrintStream breacesStream;  /// A stream for printing "Coerce Breached" instances.
    public PrintStream transitionStream; /// A stream to print the transition relation
    
    public String implementation;
    public String fileSuffix;
    
    public String outputFile,  baseOutputFile; 
    public String messagesFile, baseMessagesFile; 
    //public String breachesFile, baseBreachesFile;
    public String transitionFile, baseTransitionFile;
    public String root;
    
    public boolean bundleEnabled;
    public boolean outputEnabled;
    public boolean messagesEnabled;
    //public boolean breachesEnabled;
    public boolean transitionEnabled;
    
    public boolean multipleOutputFiles;
    public boolean redirectOutputToDir; 
    
    public String subDirectory;
    public String outputDirectory;
    
    public ImplementationsBundle(String implementation) {
      PropertiesEx props = new PropertiesEx("/tvla/io/tvla.io.properties");
      this.implementation = implementation;
      
      try {
        Class analysisStateClass = props.getClassProperty(implementation + ".analysisStateConverter", null);
        Class commentConverterClass = props.getClassProperty(implementation + ".commentConverter", null);
        Class locationConverterClass = props.getClassProperty(implementation + ".locationConverter", null);
        Class programConverterClass = props.getClassProperty(implementation + ".programConverter", null);
        Class structureConverterClass = props.getClassProperty(implementation + ".structureConverter", null);
        Class bannerConverterClass = props.getClassProperty(implementation + ".bannerConverter", null);
        Class vocabularyConverterClass = props.getClassProperty(implementation + ".vocabularyConverter", null);
//      Class coerceConverterClass = props.getClassProperty(implementation + ".coerceConverter", null);
        Class transitionRelationConverterClass = props.getClassProperty(implementation + ".transitionRelationConverter", null);
        
        if (analysisStateClass != null)
          analysisStateConverter = (StringConverter) analysisStateClass.newInstance();
        if (commentConverterClass != null)
          commentConverter = (StringConverter) commentConverterClass.newInstance();
        if (locationConverterClass != null)
          locationConverter = (LocationConverter) locationConverterClass.newInstance();
        if (programConverterClass != null)
          programConverter = (StringConverter) programConverterClass.newInstance();
        if (structureConverterClass != null)
          structureConverter = (StringConverter) structureConverterClass.newInstance();
        if (bannerConverterClass != null)
          bannerConverter = (StringConverter) bannerConverterClass.newInstance();
        if (vocabularyConverterClass != null)
          vocabularyConverter = (StringConverter) vocabularyConverterClass.newInstance();
//      if (coerceConverterClass != null)
//      coerceConverter = (StringConverter) coerceConverterClass.newInstance();
        if (transitionRelationConverterClass != null)
          transitionRelationConverter = (StringConverter) transitionRelationConverterClass.newInstance();
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException("Unable to find class " + e.getMessage());
      }
      catch (InstantiationException e) {
        throw new RuntimeException(e.getMessage());
      }
      catch (InstantiationError e) {
        throw new RuntimeException(e.getMessage());
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e.getMessage());
      }
      
      bundleEnabled = ProgramProperties.getBooleanProperty("tvla." + implementation + ".enabled", false);
      if (!bundleEnabled)
        return;
      
      
      fileSuffix = ProgramProperties.getProperty("tvla." + implementation + ".fileSuffix", implementation);
      subDirectory = ProgramProperties.getProperty("tvla." + implementation + ".subDirectory", null);
      baseOutputFile = ProgramProperties.getProperty("tvla." + implementation + ".outputFile", null);
      baseMessagesFile = ProgramProperties.getProperty("tvla." + implementation + ".messagesFile", null);
      //baseBreachesFile = ProgramProperties.getProperty("tvla." + implementation + ".breachesFile", null);
      baseTransitionFile = ProgramProperties.getProperty("tvla.tr." + implementation + ".outputFile", null);
      
      outputEnabled = baseOutputFile != null && !baseOutputFile.equals("null");
      messagesEnabled = baseMessagesFile != null && !baseMessagesFile.equals("null");
//    breachesEnabled = baseBreachesFile != null && !baseBreachesFile.equals("null");
      transitionEnabled = 
        ProgramProperties.getBooleanProperty("tvla.tr.enabled", false) &&
        ProgramProperties.getBooleanProperty("tvla.tr." + implementation + ".enabled", false);
      
      redirectOutputToDir = ProgramProperties.getBooleanProperty("tvla.output.redirectToDirectory",false);
      if (redirectOutputToDir) {		
        outputDirectory = ProgramProperties.getProperty("tvla.output.outputDirectory", "null");
        assert(outputDirectory != null && !outputDirectory.equals("null")); 
        
        String bundleSubDirectory = ProgramProperties.getProperty("tvla." + implementation + ".subDirectory", "null");
        if (bundleSubDirectory != null && !bundleSubDirectory.equals("null"))
          outputDirectory = outputDirectory + IOFacade.fileSeperator +  bundleSubDirectory;
        
        if (outputEnabled) 
          baseOutputFile = outputDirectory + IOFacade.fileSeperator + baseOutputFile;
        
        if (messagesEnabled) 
          baseMessagesFile = outputDirectory + IOFacade.fileSeperator+ baseMessagesFile;
        
//      if (breachesEnabled) 
//      baseBreachesFile = outputDirectory + IOFacade.fileSeperator+ baseBreachesFile;
        
        if (transitionEnabled) 
          baseTransitionFile = outputDirectory + IOFacade.fileSeperator+ baseTransitionFile;
      }
      
      multipleOutputFiles = ProgramProperties.getBooleanProperty("tvla.output.multipleOutputFiles", false);
      root = ProgramProperties.getProperty("tvla.output.root", null);
      if (root != null && root .equals("null")) 
        root   = null;
      
      assert(!multipleOutputFiles || root != null);
      
      redirectOutput(root);
    }
    
    public void redirectOutput(String baseName) {
      if (multipleOutputFiles) {
        baseName = "." + baseName;
        outputFile = new String(baseOutputFile  + baseName + ".out." + fileSuffix);
        messagesFile = new String(baseMessagesFile  + baseName + ".msg." + fileSuffix);
//      breachesFile = new String(baseBreachesFile  + baseName + ".breach." + fileSuffix);
        transitionFile = new String(baseTransitionFile  + baseName + ".tr." + fileSuffix);
      }
      else {
        outputFile = new String(baseOutputFile);
        messagesFile = new String(baseMessagesFile);
//      breachesFile = new String(baseBreachesFile);
        transitionFile = new String(baseTransitionFile);
      }
      
      setStreams();				
    }
    
    
    private void setStreams() {
      // initialize the output stream
      if (bundleEnabled && outputEnabled) {
        try {
          if (outputStream != null) 
            outputStream.close();
          
          setStream(outputFile);
          
          outputStream = new PrintStream(new FileOutputStream(outputFile));
        }
        catch (IOException e) {
          throw new RuntimeException(e.getMessage());
        }
      }
      
      if (outputStream == null)
        outputEnabled = false;
      
      // initialize the messages stream
      if (bundleEnabled && messagesEnabled) {
        try {
          if (messagesStream != null) 
            messagesStream.close();
          
          setStream(messagesFile);
               
          messagesStream = new PrintStream(new FileOutputStream(messagesFile));
        }
        catch (FileNotFoundException e) {
          throw new RuntimeException(e.getMessage());
        }
      }
      
      
      // initialize the transition stream
      if (bundleEnabled && transitionEnabled) {
        try {
          if (transitionStream != null) 
            transitionStream.close();
                 
          setStream(transitionFile);
          
          transitionStream = new PrintStream(new FileOutputStream(transitionFile));
        }
        catch (IOException e) {
          throw new RuntimeException(e.getMessage());
        }
      }
      
      if (transitionStream == null)
        transitionEnabled = false;
    }
    
    
    private void setStream(String fileName) {
      if (redirectOutputToDir && outputDirectory != null) {
        File dir = new File(outputDirectory);
        
        boolean dirExists = dir.exists();
        if (!dirExists) {                   
          // Create output directory  if it does not exist
          dir.mkdirs();
          System.err.println("Output directory " + outputDirectory + " created");
        }
      }
      
      // directory exits. If the file exists - erase it   
      File file = new File(outputFile);
      boolean exists = file.exists();
      // erase existing file
      if (exists)
        file.delete();
    }
  }
}       

/*
 private void setStreams() {
 // initialize the output stream
  if (bundleEnabled && outputEnabled) {
  try {
  if (outputStream != null) 
  outputStream.close();
  
  if (redirectOutputToDir && outputDirectory != null) {
  File dir = new File(outputDirectory);
  
  boolean dirExists = dir.exists();
  if (!dirExists) {					
  // Create output directory  if it does not exist
   dir.mkdirs();
   System.err.println("Output directory " + outputDirectory + " created");
   }
   }
   else {
   // directory exits. If the file exists - erase it	
    File file = new File(outputFile);
    boolean exists = file.exists();
    // erase existing file
     if (exists)
     file.delete();
     }
     
     outputStream = new PrintStream(new FileOutputStream(outputFile));
     }
     catch (IOException e) {
     throw new RuntimeException(e.getMessage());
     }
     }
     
     if (outputStream == null)
     outputEnabled = false;
     
     // initialize the messages stream
      if (bundleEnabled && messagesEnabled) {
      try {
      if (messagesStream != null) 
      messagesStream.close();
      
      
      messagesStream = new PrintStream(new FileOutputStream(messagesFile));
      }
      catch (FileNotFoundException e) {
      throw new RuntimeException(e.getMessage());
      }
      }
      
      
      // initialize the transition stream
       if (bundleEnabled && transitionEnabled) {
       try {
       if (transitionStream != null) 
       transitionStream.close();
       
       if (outputDirectory != null) {
       File dir = new File(outputDirectory);
       
       boolean dirExists = dir.exists();
       if (!dirExists) {
       // Create output directory  if it does not exist
        dir.mkdirs();
        System.err.println("Output directory " + outputDirectory + " created");					 						
        }
        }
        else {
        // directory exits. If the file exists - erase it	
         File file = new File(transitionFile);
         boolean exists = file.exists();
         // erase existing file
          if (exists) 
          file.delete();
          }
          
          transitionStream = new PrintStream(new FileOutputStream(transitionFile));
          }
          catch (IOException e) {
          throw new RuntimeException(e.getMessage());
          }
          }
          
          if (transitionStream == null)
          transitionEnabled = false;
          }
          }
          */
