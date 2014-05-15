/**
 * Root of all ITVLAJavaAdapter implementation.
 * Contains the methods (and the hooks for their real implementation) needed 
 * for loading and initializing the actual implementation
 * 
 * The package protection for the method ensures that clients cannot use them.
 */

package tvla.api;

import tvla.api.ITVLAJavaAnalysisEnvironmentServices.ITVLAJavaAnalsyisEnvironmentServicesPovider;
import tvla.api.ITVLAJavaAnalyzer.ITVLAJavaAdapter;


public abstract class AbstractTVLAJavaAdapter implements ITVLAJavaAdapter {
	boolean setParam(
			AbstractTVLAAPI tvlaAPI,
            ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
            int[] analysisCodes) {
		return doSetParam(tvlaAPI, environmentServicesProvider, analysisCodes);
	}

	protected abstract boolean doSetParam(
        AbstractTVLAAPI tvlaAPI,
        ITVLAJavaAnalsyisEnvironmentServicesPovider environmentServicesProvider,
        int[] analysisCodes);
		
}
