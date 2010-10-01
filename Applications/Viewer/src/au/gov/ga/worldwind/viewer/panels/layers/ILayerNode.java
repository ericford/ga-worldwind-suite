package au.gov.ga.worldwind.viewer.panels.layers;

import java.net.URL;

import au.gov.ga.worldwind.common.util.Loader;

public interface ILayerNode extends INode
{
	public URL getLayerURL();

	public URL getLegendURL();

	public void setLegendURL(URL legendURL);

	public URL getQueryURL();

	public void setQueryURL(URL queryURL);

	public boolean isEnabled();

	public void setEnabled(boolean enabled);

	public double getOpacity();

	public void setOpacity(double opacity);

	public boolean hasError();

	public Exception getError();

	public void setError(Exception error);

	public boolean isLayerLoading();

	public void setLayerLoading(boolean layerLoading);
	
	public boolean isLayerDataLoading();
	
	public void setLayerDataLoading(boolean layerDataLoading);
	
	public void setLoader(Loader loader);

	public Long getExpiryTime();

	public void setExpiryTime(Long expiryTime);
}
