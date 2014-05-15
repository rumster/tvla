/**
 * Root of all ITVLAAPI implementation.
 * Contains the methods (and the hooks for their real implementation) needed 
 * for loading and initializing the actual implementation
 * 
 * The package protection for the method ensures that clients cannot use them.
 */

package tvla.api;




public abstract class AbstractTVLAAPI implements ITVLAAPI , ITVLATransformers 
{
	void setFrontendServices(
        ITVLATabulatorServices chaoticEngine, 
        ITVLAAPIDebuggingServices client) {
      doSetFrontendServices(chaoticEngine, client);
	}
	
	protected abstract void doSetFrontendServices(
        ITVLATabulatorServices chaoticEngine, 
        ITVLAAPIDebuggingServices client);
    
}
