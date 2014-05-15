/*
 * File: ActionApplier.java 
 * Created on: 16/10/2004
 */

package tvla.analysis;

import java.util.Collection;
import java.util.Map;

import tvla.core.TVS;
import tvla.transitionSystem.Action;
import tvla.transitionSystem.PrintableProgramLocation;
/** 
 * @author maon
 */
public interface IActionApplier {
	/** Apply the action on the structure at program location label 
	 * returning all possible resulting structures.
	 * @param messages Map with messages generated for structures. 
	 * Must be initialized. 
	 * @since 8.2.2001 Added TVS output printing capabilities.
	 */
	public Collection apply(Action action, 
							TVS structure,
							PrintableProgramLocation processedLocation, 
							Map messages);


	public boolean doesFocus();
	public boolean doesCoerceAfterFocus();
	public boolean doesCoerceAfterUpdate();
	public boolean doesBlur();
	public boolean freezesStructuresWithMessages();
	public boolean breaksIfCoerceAfterUpdateFailed();
	
	public boolean setPrintStrucutreIfCoerceAfetFocusFailed(boolean shouldPrint);
}