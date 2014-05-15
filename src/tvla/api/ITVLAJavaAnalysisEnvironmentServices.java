package tvla.api;

import java.util.Set;

import tvla.api.ITVLAAPI.ITVLATabulatorServices;

/**
 * Services expected from the frontend
 * TVLA will use these callbacks during the analysis
 * 
 * @author maon
 */

public interface ITVLAJavaAnalysisEnvironmentServices {
  /*********************************************
   * ProgramModel
   *********************************************/
  
  /**
   * This clas just hold the various components of a program modeleing 
   * as sub interfaces to allow easier viewing
   */
  public static interface ITVLAJavaProgramModeler {

    public static interface IFieldModeler {
      /**
       * Returns a CLASS unique name of the field 
       * @return
       */
      String  fieldName(Object field);   		
      
      /**
       * Returns a PROGRAM unique id of the field 
       * @return
       */
      String  fieldUniqueId(Object field);   		
      
      /**
       * @param field
       * @return if field a static field
       */
      boolean fieldIsStatic(Object field);
      
      /**
       * @param field
       * @return if field a boolean field
       */
      boolean fieldIsBoolean(Object field);
      
      /**
       * @param field
       * @return if field a reference field
       */
      boolean fieldIsReference(Object field);		
      
      /**
       * returns whether the fields fpoints to an array object (as opposed to a
       * simple object)
       * @param field
       * @return is array
       */
      boolean fieldIsArray(Object field);     
    
      
      /**
       * Returns the class the field is declared in
       */
      Object fieldDeclaredInClass(Object field);	
      
      /**
       * Does klass represnts a klass  
       */
      boolean representsField(Object field);
      
      boolean fieldsAreEqual(Object field1, Object field2);
    }

    public static interface IAllocationSiteModeler {
      /**
       * @return  Returns an objet which represents the class being allocated
       */
      Object  allocationSiteAllocatedClass(Object allocSite);   		
      
      /**
       * Returns a unique tag for the allocation tag
       * @return
       */
      String  allocationSiteUniqueId(Object allocSite);
      
      /**
       * Does site represnts an allocation site
       */
      boolean representsAllocationSite(Object allocSite);	
      
      boolean allocationSitesAreEqual(Object allocSite1, Object allocSite2);
    }

    /**
     * An object representing an array is expecte to represnt a class (of type Array of ..)
     * @author maon
     *
     */
    public static interface IArrayModeler {
      /**
       * Returns the name of the array class 
       * @return
       */
      String  arrayClassName(Object array);   		
      
      /**
       * Returns a program unique id for the array class 
       * @return
       */
      String  arrayClassUniqueId(Object array);
      
      /**
       * Return the class representer of the declared type of the array elements 
       * or null if array is of primitive values
       */
      Object arrayClassGetElementType(Object array);
      
      /**
       * Returns the array dimension
       */
      int arrayClassGetDimension(Object array);
      
      /**
       * Does array represnts a class
       */
      boolean representsArrayClass(Object array);	
      
      boolean arrayClassesAreEqual(Object array1, Object array2);
    }

    public static interface IClassModeler {
      /**
       * Returns the name of the class 
       * @return
       */
      String  className(Object klass);   		
      
      /**
       * Returns a program unique id for the class 
       * @return
       */
      String  classUniqueId(Object klass);
      
      /**
       * Does klass represnts a class
       */
      boolean representsClass(Object klass);	
      
      boolean classesAreEqual(Object klass1, Object klass2);
    }

    public static interface InvocationModeler {		   
      /**
       * Returns the index of the caller local that can be used to inquire about 
       * the actual parameter number actualNum
       * @return
       */
      int invocationActualParameterIndex(Object invocation, int actualNum);
      
      /**
       * Returns the index of the caller local that can be used to inquire about 
       * the actual parameter number actualNum
       * @return
       */
      int invocationNumberOfParameters(Object invocation);
      
      /**
       * Returns whehter the k's parameter is of type a  reference 
       * @return
       */
      boolean invocationParametersTypeIsReference(Object invocation, int k);
      
      
      /**
       * Returns whehter the k's parameter is of type a boolean  
       * @return
       */
      boolean invocationParametersTypeIsBoolean(Object invocation, int k);
      
      
      /**
       * does the caller assigns the return value to a variable?
       * @return
       */
      boolean invocationHasDef(Object invocation);	    	   
      
      /**
       * Returns whehter the k's parameter is of type a reference 
       * @return
       */
      boolean invocationReturnTypeIsReference(Object invocation);
      
      
      /**
       * Returns whehter the callee return value is of type a boolean   
       * @return
       */
      boolean invocationReturnTypeIsBoolean(Object invocation);
      
      /**
       * Returns whehter the callee return value is void (i.e. ,has no return value)   
       * @return
       */
      boolean invocationReturnTypeIsVoid(Object invocation);
      
      /**
       *  Returns the index of the local variable of the caller into which 
       *  the return value is assigned  
       * @return
       */
      int invocationGetDef(Object invocation);           	
      
      
      /**
       * Returns the signature of the invoked method 
       */
      String invocationCalleeSig(Object invocation);
      
      /**
       * Returns the  name of the invoked method 
       */
      String invocationCalleeName(Object invocation);
      
      /**
       * Does invokeAssn represnts an assignemnt of the result of a method invocation
       */
      boolean representsInvocation(Object invocation);
    }

    public static interface IMethodModler {
      /**
       * Returns the name of the method
       * @return
       */
      String  methodName(Object method);   
      
      
      /**
       * Returns a program unique id for the method
       * @return
       */
      String  methodUniqueId(Object method);		    		
      
      /**
       * Is this method the main of the JVM, i.e., is this method the 
       * one that invokes the propgam main method  
       * @return
       */
      boolean methodIsJVMMain(Object method);
      
      /**
       * is the methods return value of type boolean
       * @param method
       * @return
       */
      boolean methodReturnTypeIsBoolean(Object method);
      
      /**
       * is the methods return value a reference type (i.e., a pointer to an object or to an array)
       * @param method
       * @return
       */
      boolean methodReturnTypeIsReference(Object method);
      
      /**
       * is the methods return value a reference t to an array?
       * @param method
       * @return
       */
      boolean methodReturnTypeIsArray(Object method);

      /**
       * does the methods return void (i.e., has no return value) 
       * @param method
       * @return
       */
      boolean methodReturnTypeIsVoid(Object method);	
      
      /**
       * Returns the number of formal parameters (including this for virtual methods)
       * @return
       */
      int methodNumberOfParameters(Object method);	
      
      
      /**
       * progivdes the local index of the parameter numbered paramNum in
       * mehtod  
       * 
       * The reason we need this obnoxious function is that the value number assigned to the formal
       * parameter might not match the formal index in the signature.
       * 
       * @param method  the method
       * @param paramNum the number of the parameter in the "signature" of the 
       * method. Numbers starts at 0. If the method has this, than this 
       * number is 0 (but not neceserly its index!).
       * @return
       */
      int methodParameterNumberToLocalIndex(Object method, int paramNum);
      
      /**
       * Returns the number of local parameters (i.e., all local variables including parameters)
       * @return
       */
      int methodNumberOfLocals(Object method);	
          
      /**
       * Returns the name of the local variabl with index localIndex. 
       * The method's formal parameters number k has index k. 
       * For virtual methods, this is the 0 parameter.
       * For static methods, the first parameter is 0 
       * @return
       */	
      String methodLocalName(Object method, int localIndex);
      
      /**	 
       * Returns a program unique id for the local variable with index localIndex. 
       * The method's formal parameters number k has index k. 
       * For virtual methods, this is the 0 parameter.
       * For static methodsm, the first parameer is 0 
       * @return
       */	
      String methodLocalUniqueId(Object method, int localIndex);
      
    
      /**
       * The type of the variable, is it boolean? 
       * @return
       */
      boolean methodLocalTypeIsBoolean(Object method, int localIndex);
      
      /**
       * The type of the variable, is it a reference? (i.e., is the variable 
       * a pointer to an object or to an array) 
       * @return
       */
      boolean methodLocalTypeIsReference(Object method, int localIndex);
       
      /**
       * The type of the variable, is it a reference to an array?
       * @return
       */
      boolean methodLocalTypeIsArray(Object method, int localIndex);
      
      
      
      /**
       * Returns the class the method is declared in
       */
      Object methodDeclaredInClass(Object method);
    
      /**
       * Returns the method this variable, or -1 if the method has no this variable.
       */
      int methodGetThis(Object method);
    
      
      /**
       * Does method represnts a method
       */
      boolean representsMethod(Object method);
      
    
      boolean methodsAreEqual(Object method1, Object method2);
    }
    
  }
  
  /**
   * The information about a program TVLA needs
   */
  public static interface ITVLAJavaProgramModelerServices 
    extends 
      ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModeler.IClassModeler, 
      ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModeler.IFieldModeler, 
      ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModeler.IMethodModler, 
      ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModeler.IAllocationSiteModeler,
      ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModeler.InvocationModeler,
      ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaProgramModeler.IArrayModeler
  {
    // TODO remove tis and find a suitable way to handle primitive types
/*
    public static final byte TYPE_VOID = 0;
    public static final byte TYPE_BOOLEAN = 1;
    public static final byte TYPE_BYTE = 2;
    public static final byte TYPE_SHORT = 3;
    public static final byte TYPE_INT = 4;
    public static final byte TYPE_LONG = 5;
    public static final byte TYPE_CHAR = 6;
    public static final byte TYPE_FLOAT = 7; 
    public static final byte TYPE_DOUBLE = 8; 
    
    public static final byte TYPE_REF = 9; 
*/
    }
  

  /**********************************************************
   * Access to prelimineary analyses 
   **********************************************************/
  /**
   * Allows clients to register to get information from the java transformer applier 
   * In the future, TVLA should be able to produce results that support this interface
   */
  public static interface ITVLAJavaAnalysisResultsServices {
    /**
     * Returns the set of allocations sites that are relevant to a method
     * @param method an object that represent a method in the PrgoramModeler
     * @return the set of allocation sites that contain an object that 
     * might be accessesd when method executes
     */
    Set relevantAllocationSites(Object method);

    /**
     * Returns the set of allocations sites that may be (transitively) 
     * accessed during a call to a method
     * @param invocation an object that represent a method invocation in the 
     * PrgoramModeler
     * @return the set of allocation sites that contain an object that 
     * might be accessesd during the invoked methd execution, or one of the method 
     * it (indirectly) invokes 
     */
    Set tranrelevantAllocationSites(Object invocation);
  }
  
  
  /*********************************************
   * Deugging environment
   *********************************************/
  /**
   * Allows TVLA to know the program statement being executed
   */
  public static interface ITVLAJavaTabulatorServices extends ITVLATabulatorServices {
    /**
     * The API does not modify the message after it is generated
     * We extend the APIListner for possible future enhanchemnts.
     */
  }
  
  public static interface ITVLAJavaDebuggingServices extends ITVLAAPIDebuggingServices 
  {
    
  }
  
  /********************************************
   * Analysis Enviroment services supplier
   ********************************************/
  public static interface ITVLAJavaAnalsyisEnvironmentServicesPovider {
    ITVLAJavaTabulatorServices getJavaTabulationServices();
    
    ITVLAJavaDebuggingServices getJavaDebuggingServices();
    
    ITVLAJavaAnalysisResultsServices getJavaAnalysisResultsServices();
    
    ITVLAJavaProgramModelerServices getJavaProgramModelerServices();
  }
  
}