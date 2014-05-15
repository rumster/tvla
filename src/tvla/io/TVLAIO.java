/*
 * File: TVLAIO.java 
 * Created on: 18/10/2004
 */

package tvla.io;

import java.util.Collection;

import tvla.transitionSystem.PrintableProgramLocation;
//import java.io.FileWriter;
/** 
 * @author maon
 */
public interface TVLAIO {
	/** Prints a structure to all desired outputs.
	 * @param structure A TVS.
	 * @param header An optional header.
	 */
	public abstract void printStructure(Object structure, String header);

	/** Prints a location with its structures to all desired outputs.
	 * @param location A Location.
	 */
	public abstract void printLocation(PrintableProgramLocation location);

	/** Prints a MethodTS to all desired outputs.
	 * @param program A collection of transitions.
	 */
	public abstract void printProgram(Object program);

	/** Prints the state of the analysis - transitions and their structures to
	 * all desired outputs.
	 * @param state A collection of transitions.
	 */
	public abstract void printAnalysisState(Object state);
	
	/**
	 * Prints a page containing only a banner string.
	 * @param banner
	 */
	public void printBanner(String banner);
	
	/**
	 * Directs next outputs to an output stream called name.
	 * @param banner
	 */
	public void redirectOutput(String streamName);
	
	/**
	 * Return a valid stream name  based on a given string
	 * This function is not injective.
	 * However if two string have a different regular character
	 * in the same position, their stream names will differ.
	 * 
	 * @author maon
	 */
	public String genValidStreamName(String baseName);
	
	/**
	 * Prints a page which contains only a banner to the messages stream if exists,
	 * or to the outputstream otherwise
	 */
	public void printMessageBanner(String banner);
	
	/**
	 * Prints a strucutre with an associated message.
	 * If the message stream is not null, output is directed to the messagfe stream.
	 * Otherwise, it is printed into the outputstream.  
	 */
	public void printStrucutreWithMessages(String header, Object tvs, Collection messages);
	
	
	//////////////////////
	// Optional Methods //
	//////////////////////

	/** 
	 * Prints the analysis vocabulary 
	 *  
	 * @author maon
	 */
	//public void printVocabulary();

	/** 
	 * Prints the analysis constraints
	 * @author maon
	 */
	//public void printConstraints();	
	
	//public FileWriter getFileWriter(String subDir, String fileName, String suffix);
}