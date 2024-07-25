/**
 * 
 */
package de.in.autoMower.sim;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.formdev.flatlaf.FlatDarculaLaf;

/**
 * @author Till
 */
public class App extends JFrame {

	private static App app;
	GroundModel ground;
	private SimulationPanel panel;
	private AutoMowerModel mower;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			FlatDarculaLaf.setup();
			app = new App();
			app.setVisible(true);
		});
	}

	public App() {
		super("AutoMowerSimulation");

		ground = new GroundModel();
		panel = new SetupGroundPanel(ground);
		mower = new AutoMowerModel();
		getContentPane().add(panel);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(600, 600));
		pack();

		// Set the MenuBar
		this.setJMenuBar(MenuBar.create());

		setLocationRelativeTo(null);
	}

	public GroundModel getGroundModel() {
		return ground;
	}

	public void setMower(AutoMowerModel mower) {
		this.mower = mower;
	}

	public AutoMowerModel getMower() {
		return mower;
	}

	public static App getApp() {
		return app;
	}

	public void setModel(GroundModel model) {
		this.ground = model;
		panel.setModel(model);
	}

	public SimulationPanel getPanel() {
		return panel;
	}

	public void setPanel(SimulationPanel panel) {
		if (this.panel != null)
			getContentPane().remove(this.panel);
		this.panel = panel;
		getContentPane().add(panel);
		revalidate();
		repaint();
	}

}
