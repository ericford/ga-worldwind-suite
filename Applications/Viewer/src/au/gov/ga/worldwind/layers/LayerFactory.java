package au.gov.ga.worldwind.layers;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.layers.BasicLayerFactory;
import gov.nasa.worldwind.layers.Layer;

import org.w3c.dom.Element;

import au.gov.ga.worldwind.layers.file.FileTiledImageLayer;
import au.gov.ga.worldwind.layers.mask.MaskTiledImageLayer;
import au.gov.ga.worldwind.util.XMLUtil;

public class LayerFactory extends BasicLayerFactory
{
	@Override
	protected Layer createTiledImageLayer(Element domElement, AVList params)
	{
		String serviceName = XMLUtil.getText(domElement, "Service/@serviceName");
		if ("MaskedTileService".equals(serviceName))
		{
			return new MaskTiledImageLayer(domElement, params);
		}
		else if ("FileTileService".equals(serviceName))
		{
			return new FileTiledImageLayer(domElement, params, false);
		}
		else if ("MaskedFileTileService".equals(serviceName))
		{
			return new FileTiledImageLayer(domElement, params, true);
		}
		return super.createTiledImageLayer(domElement, params);
	}
}