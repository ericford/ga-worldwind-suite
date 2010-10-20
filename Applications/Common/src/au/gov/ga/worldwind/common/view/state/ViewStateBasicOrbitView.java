package au.gov.ga.worldwind.common.view.state;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import au.gov.ga.worldwind.common.view.transform.TransformBasicOrbitView;

public class ViewStateBasicOrbitView extends TransformBasicOrbitView
{
	@Override
	public void copyViewState(View view)
	{
		this.globe = view.getGlobe();

		Vec4 centerPoint = view.getCenterPoint();
		Vec4 eyePoint = view.getCurrentEyePoint();
		Position eyePosition = globe.computePositionFromPoint(eyePoint);

		if (centerPoint != null)
		{
			Position centerPosition = globe.computePositionFromPoint(centerPoint);
			if (trySetOrientation(eyePosition, centerPosition))
				return;
		}

		//center point is not on the globe, so compute the closest point on the horizon
		double horizonDistance = view.computeHorizonDistance();
		Vec4 forward = view.getForwardVector().normalize3();
		Vec4 normal = globe.computeSurfaceNormalAtPoint(eyePoint);
		Vec4 left = normal.cross3(forward);
		forward = left.cross3(normal);

		//keep moving the horizon distance closer until modelCoords are valid
		while (horizonDistance > 1)
		{
			centerPoint = eyePoint.add3(forward.multiply3(horizonDistance));
			Position pos = globe.computePositionFromPoint(centerPoint);
			double elevation =
					Math.min(eyePosition.elevation, globe.getElevation(pos.latitude, pos.longitude));
			Position centerPosition = new Position(pos, elevation);

			if (trySetOrientation(eyePosition, centerPosition))
				return;

			horizonDistance /= 2d;
		}

		//if nothing worked, just set the view using the heading/pitch
		setEyePosition(eyePosition);
		setHeading(view.getHeading());
		setPitch(view.getPitch());
	}
}