package de.in.autoMower.sim;

import java.awt.geom.Point2D;
import java.io.Serializable;

import javax.swing.JOptionPane;


public class AutoMowerModel implements Serializable {

	double speedInCmPerSec;
	double mowingWidthInCm;
	Point2D destination = null;
	Point2D currentPosition = null;

	public double getInCmPerSec() {
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

}
