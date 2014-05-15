//Licensed Materials - Property of IBM
//5724-D15
//(C) Copyright IBM Corporation 2004. All Rights Reserved. 
//Note to U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP  Schedule Contract with IBM Corp. 
//                                                                          
//--------------------------------------------------------------------------- 

package tvla.analysis.interproc.api.tvlaadapter;

import java.util.Collection;
import java.util.Map;

import tvla.core.TVS;
import tvla.transitionSystem.Action;

public interface IAPIActionApplier {
    /** Apply the action on the structure at program location label 
     * returning all possible resulting structures.
     * @param messages Map with messages generated for structures. 
     * Must be initialized. 
     * @since 8.2.2001 Added TVS output printing capabilities.
     */
    public Collection apply(Action action, 
                            TVS structure,
                            Map messages);


    public boolean doesFocus();
    public boolean doesCoerceAfterFocus();
    public boolean doesCoerceAfterUpdate();
    public boolean doesBlur();
    public boolean freezesStructuresWithMessages();
    public boolean breaksIfCoerceAfterUpdateFailed();
    
    public boolean setPrintStrucutreIfCoerceAfetFocusFailed(boolean shouldPrint);
}
