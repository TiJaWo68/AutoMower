package de.in.autoMower.sim;

public class Simulation implements Runnable {

	GroundModel groundModel;

	public Simulation(GroundModel groundModel, AutoMowerModel mower, MultiLine2D line) {
		this.groundModel = groundModel;
		this.mower = mower;
		this.line = line;
	}

	AutoMowerModel mower;
	MultiLine2D line;

	public void run() {
		mower.start(line, groundModel);

	}

	public void stop() {
		mower.stop();
	}

	public void resume() {
		mower.resume();
	}

	public void cancel() {
		mower.cancel();

	}

}
