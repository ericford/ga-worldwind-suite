package au.gov.ga.worldwind.wmsbrowser;

import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwindow.core.WMSLayerInfo;

import java.net.URL;
import java.util.List;

/**
 * An interface that captures information about a single WMS server
 */
public interface WmsServer
{
	/**
	 * @return The URL to use for retrieving this server's capabilities descriptor
	 */
	URL getCapabilitiesUrl();
	
	/**
	 * @return The list of layers available on this WMS server. May be empty. Will be <code>null</code>
	 * if the layers have not yet been loaded.
	 */
	List<WMSLayerInfo> getLayers();
	
	/**
	 * Load the layer information for this server
	 */
	void loadLayers();

	/**
	 * @return Whether or not the layers for this server have been loaded yet
	 */
	boolean isLayersLoaded();
	
	/**
	 * Register a load listener to be notified when the layers for this server are loaded
	 */
	void addLoadListener(LoadListener listener);
	
	/**
	 * Remove the provided load listener from this server
	 */
	void removeLoadListener(LoadListener listener);
	
	/**
	 * Inject a {@link WmsCapabilitiesService} to use for retrieving capabilities
	 * <p/>
	 * Useful for testing etc. when a working WMS server is not available.
	 */
	void setCapabilitiesService(WmsCapabilitiesService service);
	
	/**
	 * An interface for listeners that want to be notified of loading events
	 * for a {@link WmsServer}
	 */
	interface LoadListener 
	{
		/** Notified when a server is about to start loading it's layers list */
		void beginningLoad(WmsServer server);
		
		/** Notified when a server has finished loading it's layers list */
		void loaded(WmsServer server);
		
		/** Notified when loading has failed. Includes the exception that caused the failure */
		void loadFailed(WmsServer server, Exception e);
	}
	
	interface WmsCapabilitiesService
	{
		/** Retrieve the WMS Capabilites for the provided url */
		WMSCapabilities retrieveCapabilities(URL url) throws Exception;
	}
}
