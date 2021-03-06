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
package au.gov.ga.worldwind.animator.layers.sky;

import static au.gov.ga.worldwind.animator.util.Util.isBlank;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.view.BasicView;
import gov.nasa.worldwind.view.orbit.OrbitView;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL2;

import org.w3c.dom.Element;

import au.gov.ga.worldwind.animator.util.AVKeyMore;
import au.gov.ga.worldwind.common.util.Validate;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * A layer that renders a hemisphere skysphere mapped with the specified image.
 * <p/>
 * Texture images should be provided in a dome projection to ensure correct
 * display of the image within the world window.
 * <p/>
 * The rotation of the image can be controlled using the
 * <code>&lt;Rotation&gt;</code> elements within the layer definition file.
 * 
 * @author Michael de Hoog (michael.dehoog@ga.gov.au)
 */
public class Skysphere extends AbstractLayer
{
	public static final String LAYER_TYPE = "SkysphereLayer";

	private int vertexCount, triCount;
	private DoubleBuffer vb, nb, tb;
	private IntBuffer ib;
	private Texture texture;
	private boolean inited = false;

	private URL context;
	private String textureLocation;
	private int slices = 20;
	private int segments = 20;
	private Angle rotation = Angle.fromDegrees(0);

	/**
	 * Create a new {@link Skysphere} from the provided parameters
	 */
	public Skysphere(AVList params)
	{
		Validate.notNull(params, "Parameters are required");

		initialiseFromParams(params);
	}

	/**
	 * Create a new {@link Skysphere} from the provided XML definition
	 */
	public Skysphere(Element domElement, AVList params)
	{
		if (params == null)
		{
			params = new AVListImpl();
		}

		AbstractLayer.getLayerConfigParams(domElement, params);

		WWXML.checkAndSetStringParam(domElement, params, AVKeyMore.URL, "TextureLocation", null);
		WWXML.checkAndSetIntegerParam(domElement, params, AVKeyMore.SKYSPHERE_SLICES, "Slices", null);
		WWXML.checkAndSetIntegerParam(domElement, params, AVKeyMore.SKYSPHERE_SEGMENTS, "Segments", null);
		WWXML.checkAndSetDoubleParam(domElement, params, AVKeyMore.SKYSPHERE_ANGLE, "Rotation", null);

		initialiseFromParams(params);
	}

	private void initialiseFromParams(AVList params)
	{
		String s = params.getStringValue(AVKey.DISPLAY_NAME);
		if (!isBlank(s))
		{
			setName(s);
		}

		s = params.getStringValue(AVKey.URL);
		Validate.notBlank(s, "A texture location must be provided");
		textureLocation = s;

		Integer i = (Integer) params.getValue(AVKeyMore.SKYSPHERE_SLICES);
		if (i != null)
		{
			slices = i;
		}

		i = (Integer) params.getValue(AVKeyMore.SKYSPHERE_SEGMENTS);
		if (i != null)
		{
			segments = i;
		}

		Double d = (Double) params.getValue(AVKeyMore.SKYSPHERE_ANGLE);
		if (d != null)
		{
			rotation = Angle.fromDegrees(d);
		}

		context = (URL) params.getValue(AVKeyMore.CONTEXT_URL);
		Validate.notNull(context, "A context URL must be provided");
	}

	private void initializeTextures(DrawContext dc)
	{
		try
		{
			URL textureUrl = new URL(context, textureLocation);
			InputStream stream = textureUrl.openStream();

			texture = TextureIO.newTexture(stream, true, null);
			texture.bind(dc.getGL().getGL2());
		}
		catch (IOException e)
		{
			String msg = Logging.getMessage("layers.IOExceptionDuringInitialization");
			Logging.logger().severe(msg);
			throw new WWRuntimeException(msg, e);
		}

		GL2 gl = dc.getGL().getGL2();
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		// Enable texture anisotropy, improves "tilted" world map quality.
		/*int[] maxAnisotropy = new int[1];
		gl
				.glGetIntegerv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT,
						maxAnisotropy, 0);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT,
				maxAnisotropy[0]);*/
	}

	@Override
	protected void doRender(DrawContext dc)
	{
		GL2 gl = dc.getGL().getGL2();

		if (!inited)
		{
			setupGeometryBuffers(slices, segments, 1d, false, true);
			initializeTextures(dc);
			inited = true;
		}

		//set up projection matrix
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		dc.getGLU().gluPerspective(dc.getView().getFieldOfView().degrees,
				dc.getView().getViewport().getWidth() / dc.getView().getViewport().getHeight(), 0.1, 10.0);

		//set up modelview matrix
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		Angle heading = Angle.ZERO;
		Angle pitch = Angle.ZERO;
		Angle roll = Angle.ZERO;
		if (dc.getView() instanceof OrbitView)
		{
			heading = ((OrbitView) dc.getView()).getHeading();
			pitch = ((OrbitView) dc.getView()).getPitch();
		}
		if (dc.getView() instanceof BasicView)
		{
			roll = ((BasicView) dc.getView()).getRoll();
		}

		Matrix transform = Matrix.IDENTITY;
		transform = transform.multiply(Matrix.fromRotationZ(roll));
		transform = transform.multiply(Matrix.fromRotationX(pitch.addDegrees(90).multiply(-1.0)));
		transform = transform.multiply(Matrix.fromRotationY(heading.multiply(-1.0).add(rotation)));

		double[] matrixArray = new double[16];
		transform.toArray(matrixArray, 0, false);
		gl.glLoadMatrixd(matrixArray, 0);

		/*Vec4 up = Vec4.UNIT_Y;
		Vec4 forward = Vec4.UNIT_Z.transformBy4(transform);
		dc.getGLU().gluLookAt(0, 0, 0, forward.x, forward.y, forward.z, up.x,
				up.y, up.z);*/

		// Enable/Disable features
		gl.glPushAttrib(GL2.GL_ENABLE_BIT);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_BLEND);

		gl.glMatrixMode(GL2.GL_TEXTURE);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glScaled(1.0d, 2.0d, 1.0d);

		gl.glColor3d(1, 1, 1);
		texture.bind(gl);
		drawSphere(gl);

		gl.glPopMatrix();

		// Restore enable bits and matrix
		gl.glPopAttrib();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
	}

	private void drawSphere(GL2 gl)
	{
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		vb.rewind();
		nb.rewind();
		tb.rewind();
		ib.rewind();

		gl.glVertexPointer(3, GL2.GL_DOUBLE, 0, vb);
		gl.glNormalPointer(GL2.GL_DOUBLE, 0, nb);
		gl.glTexCoordPointer(2, GL2.GL_DOUBLE, 0, tb);
		gl.glDrawElements(GL2.GL_TRIANGLES, ib.limit() / 2, GL2.GL_UNSIGNED_INT, ib);

		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
	}

	private void setupGeometryBuffers(int slices, int segments, double radius, boolean projected, boolean interior)
	{
		// allocate vertices
		vertexCount = (slices - 2) * (segments + 1) + 2;
		vb = Buffers.newDirectDoubleBuffer(vertexCount * 3);

		// allocate normals if requested
		nb = Buffers.newDirectDoubleBuffer(vertexCount * 3);

		// allocate texture coordinates
		tb = Buffers.newDirectDoubleBuffer(vertexCount * 2);

		// allocate index buffer
		triCount = 2 * (slices - 2) * segments;
		ib = Buffers.newDirectIntBuffer(triCount * 3);

		//sphere center
		Vec4 center = Vec4.UNIT_W;

		// generate geometry
		double fInvRS = 1.0f / segments;
		double yFactor = 2.0f / (slices - 1);
		double pi = Math.PI;
		double twopi = pi * 2d;
		double halfpi = pi / 2d;
		double invpi = 1d / pi;

		// Generate points on the unit circle to be used in computing the mesh
		// points on a sphere slice.
		double[] aSin = new double[segments + 1];
		double[] aCos = new double[segments + 1];
		for (int iR = 0; iR < segments; iR++)
		{
			double fAngle = twopi * fInvRS * iR;
			aCos[iR] = Math.cos(fAngle);
			aSin[iR] = Math.sin(fAngle);
		}
		aSin[segments] = aSin[0];
		aCos[segments] = aCos[0];

		// generate the sphere itself
		int i = 0;
		for (int iY = 1; iY < (slices - 1); iY++)
		{
			double yFraction = -1.0f + yFactor * iY; // in (-1,1)
			double y = radius * yFraction;

			// compute center of slice
			Vec4 sliceCenter = center.add3(new Vec4(0, y, 0));

			// compute radius of slice
			double sliceRadius = Math.sqrt(Math.abs(radius * radius - y * y));

			// compute slice vertices with duplication at end point
			int iSave = i;
			for (int r = 0; r < segments; r++)
			{
				double radialFraction = r * fInvRS; // in [0,1)
				Vec4 radial = new Vec4(aCos[r], 0, aSin[r]);
				Vec4 v = radial.multiply3(sliceRadius).add3(sliceCenter);
				vb.put(v.x).put(v.y).put(v.z);

				Vec4 normal = new Vec4(vb.get(i * 3), vb.get(i * 3 + 1), vb.get(i * 3 + 2));
				normal = normal.subtract3(center).normalize3();
				if (!interior) // later we may allow interior texture vs. exterior
				{
					nb.put(normal.x).put(normal.y).put(normal.z);
				}
				else
				{
					nb.put(-normal.x).put(-normal.y).put(-normal.z);
				}

				if (!projected)
				{
					tb.put(radialFraction).put(0.5f * (yFraction + 1.0f));
				}
				else
				{
					tb.put(radialFraction).put(invpi * (halfpi + Math.asin(yFraction)));
				}

				i++;
			}

			double[] d3 = new double[3];
			vb.position(iSave * 3);
			vb.get(d3);
			vb.position(i * 3);
			vb.put(d3);
			nb.position(iSave * 3);
			nb.get(d3);
			nb.position(i * 3);
			nb.put(d3);

			if (!projected)
			{
				tb.put(1.0f).put(0.5f * (yFraction + 1.0f));
			}
			else
			{
				tb.put(1.0f).put(invpi * (halfpi + Math.asin(yFraction)));
			}

			i++;
		}

		// south pole
		vb.position(i * 3);
		vb.put(center.x).put(center.y - radius).put(center.z);

		nb.position(i * 3);
		if (!interior) // allow for inner texture orientation later.
		{
			nb.put(0).put(-1).put(0);
		}
		else
		{
			nb.put(0).put(1).put(0);
		}

		tb.position(i * 2);
		tb.put(0.5f).put(0.0f);

		// i++;

		// north pole
		vb.put(center.x).put(center.y + radius).put(center.z);

		if (!interior)
		{
			nb.put(0).put(1).put(0);
		}
		else
		{
			nb.put(0).put(-1).put(0);
		}

		tb.put(0.5f).put(1.0f);


		// generate connectivity
		for (int iY = 0, iYStart = 0; iY < (slices - 3); iY++)
		{
			int i0 = iYStart;
			int i1 = i0 + 1;
			iYStart += (segments + 1);
			int i2 = iYStart;
			int i3 = i2 + 1;
			for (int j = 0; j < segments; j++)
			{
				if (!interior)
				{
					ib.put(i0++);
					ib.put(i1);
					ib.put(i2);
					ib.put(i1++);
					ib.put(i3++);
					ib.put(i2++);
				}
				else
				// inside view
				{
					ib.put(i0++);
					ib.put(i2);
					ib.put(i1);
					ib.put(i1++);
					ib.put(i2++);
					ib.put(i3++);
				}
			}
		}

		// south pole triangles
		for (int j = 0; j < segments; j++)
		{
			if (!interior)
			{
				ib.put(j);
				ib.put(vertexCount - 2);
				ib.put(j + 1);
			}
			else
			{ // inside view
				ib.put(j);
				ib.put(j + 1);
				ib.put(vertexCount - 2);
			}
		}

		// north pole triangles
		int iOffset = (slices - 3) * (segments + 1);
		for (int j = 0; j < segments; j++)
		{
			if (!interior)
			{
				ib.put(j + iOffset);
				ib.put(j + 1 + iOffset);
				ib.put(vertexCount - 1);
			}
			else
			{ // inside view
				ib.put(j + iOffset);
				ib.put(vertexCount - 1);
				ib.put(j + 1 + iOffset);
			}
		}
	}
}
