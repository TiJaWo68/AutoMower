package de.in.autoMower.sim;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

public class AutoMowerModel implements Serializable {

	double speedInCmPerSec = 1000 / 36d;
	double mowingWidthInCm = 14;
	// akkulaufZeit
	// Ladezeit
	Point2D destination = null;
	Point2D currentPosition = null;
	boolean stopped = true;

	public double getSpeedInCmPerSec() {
		return speedInCmPerSec;
	}

	public double getMowingWidthInCm() {
		return mowingWidthInCm;
	}

	public Point2D getDestination() {
		return destination;
	}

	public void setDestination(Point2D destination) {
		this.destination = destination;
	}

	public Point2D getCurrentPosition() {
		return currentPosition;
	}

	public void setCurrentPosition(Point2D currentPosition) {
		this.currentPosition = currentPosition;
	}

	public void setSpeedInCmPerSec(double speedInCmPerSec) {

		this.speedInCmPerSec = speedInCmPerSec;
	}

	public void setMowingWidthInCm(double mowingWidthInCm) {

		this.mowingWidthInCm = mowingWidthInCm;
	}

	public void start(MultiLine2D line, GroundModel groundModel) {
		long startTime = System.currentTimeMillis();
		Double cmProPixel = groundModel.getCalibration();
		Point2D cpoint = groundModel.getCollisionPoint(currentPosition, destination);
		Point2D nextPoint = currentPosition;
		line.addPoint(nextPoint);
		Line2D current = new Line2D.Double(currentPosition, cpoint);
		stopped = false;
		while (!stopped) {
			double diff = (System.currentTimeMillis() - startTime) / 1000d;
			double distanceInCm = speedInCmPerSec * diff;
			double pixelDistance = distanceInCm / cmProPixel;
			double x = currentPosition.getX();
			double y = currentPosition.getY();
			Ellipse2D circle = new Ellipse2D.Double(x - pixelDistance, y - pixelDistance, pixelDistance, pixelDistance);

			App.getApp().getPanel().repaint();
		}
	}

	public void stop() {
		// TODO Auto-generated method stub

	}

}
