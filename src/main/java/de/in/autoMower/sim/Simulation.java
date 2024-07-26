package de.in.autoMower.sim;

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
		mower.start(line, groundModel);

	}

	public void setCanceled(boolean canceled) {
		mower.stop();
	}

}
