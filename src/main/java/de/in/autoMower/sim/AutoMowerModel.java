package de.in.autoMower.sim;

/**
 * Version 1 of the AutoMowerModel.
 */
public class AutoMowerModel extends AbstractAutoMowerModel {

	private static final long serialVersionUID = 1L;

	@Override
	public String getModelName() {
		return "Version 1 (Standard)";
	}

	@Override
	public int getModelVersion() {
		return 1;
	}

	@Override
	public AbstractAutoMowerModel createNewInstance() {
		return new AutoMowerModel();
	}
}