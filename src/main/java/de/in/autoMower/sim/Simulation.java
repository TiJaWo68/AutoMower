package de.in.autoMower.sim;

import java.awt.geom.Point2D;

public class Simulation {

	GroundModel groundModel;

	public Simulation(GroundModel groundModel, AutoMowerModel mower, MultiLine2D line) {
		this.groundModel = groundModel;
		this.mower = mower;
		this.line = line;
	}

	AutoMowerModel mower;
	MultiLine2D line;

	public void start() {
		MultiLine2D border = groundModel.getBorder();
		Point2D center = border.getCenterPoint();
		line.addPoint(border.getStartPoint());
		mower.setCurrentPosition(border.getStartPoint());
		mower.setDestination(center);
		mower.start(line, groundModel);

	}

	public void setCanceled(boolean canceled) {
		mower.stop();
	}

}
