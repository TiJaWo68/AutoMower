/**
 * 
 */
package de.in.autoMower.sim;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JSlider;
import javax.swing.WindowConstants;

import com.formdev.flatlaf.FlatDarculaLaf;

/**
 * @author Till
 */
public class App extends JFrame {

	private static App app;
	private GroundModel ground;
	private SimulationPanel panel;
	private AutoMowerModel mower;
	private Simulation simulation;
	private JSlider speedSlider;



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
		JMenuBar menu = MenuBar.create();
		speedSlider = MenuBar.createSpeedSlider( (int)mower.speedInCmPerSec);
		menu.add( getSpeedSlider() );
		this.setJMenuBar(menu);

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
	
	public Simulation getSimulation() {
		return simulation;
	}
	
	public Simulation createSimulation (MultiLine2D line) {
        simulation = new Simulation(ground, mower, line);
        return simulation;
	}

	public void setPanel(SimulationPanel panel) {
		if (this.panel != null)
			getContentPane().remove(this.panel);
		this.panel = panel;
		getContentPane().add(panel);
		revalidate();
		repaint();
	}

    public JSlider getSpeedSlider()
    {
        return speedSlider;
    }

}
