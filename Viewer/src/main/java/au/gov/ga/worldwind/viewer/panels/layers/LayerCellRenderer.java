/*******************************************************************************
 * Copyright 2012 Geoscience Australia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package au.gov.ga.worldwind.viewer.panels.layers;

import java.awt.Frame;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import au.gov.ga.worldwind.common.util.DefaultLauncher;
import au.gov.ga.worldwind.common.util.Icons;
import au.gov.ga.worldwind.viewer.panels.dataset.AbstractCellRenderer;

/**
 * Cell renderer used to render layer tree nodes.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class LayerCellRenderer extends AbstractCellRenderer<INode, ILayerNode>
{
	private List<QueryClickListener> queryClickListeners = new ArrayList<QueryClickListener>();

	@Override
	protected AbstractButton createButton()
	{
		return new JCheckBox();
	}

	@Override
	protected void validateTree(JTree tree)
	{
		if (!(tree instanceof LayerTree))
			throw new IllegalArgumentException("Tree must be a LayerTree");
	}

	@Override
	protected INode getValue(Object value)
	{
		if (value != null && value instanceof INode)
			return (INode) value;
		return null;
	}

	@Override
	protected ILayerNode getLayerValue(INode value)
	{
		if (value != null && value instanceof ILayerNode)
			return (ILayerNode) value;
		return null;
	}

	@Override
	protected boolean isInfoRow(INode value)
	{
		return value.getInfoURL() != null;
	}

	@Override
	protected boolean isLegendRow(ILayerNode value)
	{
		return value.getLegendURL() != null;
	}

	@Override
	protected boolean isQueryRow(ILayerNode value)
	{
		return value.getQueryURL() != null;
	}

	/*@Override
	protected String getLinkLabelToolTipText(Object value)
	{
		INode node = getValue(value);
		if (node != null && node.getInfoURL() != null)
			return node.getInfoURL().toExternalForm();
		return null;
	}*/

	@Override
	protected void setupLabel(DefaultTreeCellRenderer label, INode value)
	{
		ILayerNode layer = getLayerValue(value);
		if (layer != null)
		{
			if (layer.hasError())
			{
				String message = layer.getError().getLocalizedMessage();
				if (message == null || message.length() == 0)
					message = layer.getError().toString();
				label.setText(label.getText() + " - " + message);
				label.setIcon(Icons.error.getIcon());
			}
			else if (layer.getOpacity() != 0d && layer.getOpacity() != 1d)
			{
				label.setText(label.getText() + " (" + (int) Math.round(layer.getOpacity() * 100d) + "%)");
			}
		}
	}

	@Override
	protected void setupButton(AbstractButton button, ILayerNode value, boolean mouseInsideButton, boolean rollover,
			boolean down)
	{
		button.getModel().setRollover(rollover);
		button.getModel().setPressed(down);
		button.getModel().setArmed(down);
		button.setSelected(value.isEnabled());
	}

	@Override
	protected void buttonPressed(int row)
	{
		TreePath path = getTree().getPathForRow(row);
		if (path != null)
		{
			ILayerNode layer = getLayerValue(getValue(path.getLastPathComponent()));
			if (layer != null)
			{
				LayerTreeModel model = (LayerTreeModel) getTree().getModel();
				model.setEnabled(layer, !model.isEnabled(layer));
			}
		}
	}

	@Override
	protected void infoClicked(int row)
	{
		TreePath path = getTree().getPathForRow(row);
		if (path != null)
		{
			INode node = getValue(path.getLastPathComponent());
			if (node != null && node.getInfoURL() != null)
				DefaultLauncher.openURL(node.getInfoURL());
		}
	}

	@Override
	protected void legendClicked(int row)
	{
		TreePath path = getTree().getPathForRow(row);
		if (path != null)
		{
			INode node = getValue(path.getLastPathComponent());
			ILayerNode layer = getLayerValue(node);
			if (layer != null && layer.getLegendURL() != null)
			{
				URL url = layer.getLegendURL();
				String title = layer.getName() + " legend";
				Frame frame = (Frame) SwingUtilities.getWindowAncestor(getTree());
				LegendViewer.openLegend(url, title, frame);
			}
		}
	}

	@Override
	protected void queryClicked(int row)
	{
		TreePath path = getTree().getPathForRow(row);
		if (path != null)
		{
			INode node = getValue(path.getLastPathComponent());
			ILayerNode layer = getLayerValue(node);
			if (layer != null && layer.getQueryURL() != null)
			{
				URL url = layer.getQueryURL();
				for (int i = queryClickListeners.size() - 1; i >= 0; i--)
					queryClickListeners.get(i).queryURLClicked(url);
			}
		}
	}

	public void addQueryClickListener(QueryClickListener listener)
	{
		queryClickListeners.add(listener);
	}

	public void removeQueryClickListener(QueryClickListener listener)
	{
		queryClickListeners.remove(listener);
	}
}
