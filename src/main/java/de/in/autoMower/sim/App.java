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
		getContentPane().add(new SetupGroundPanel(ground));
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

	public static App getApp() {
		return app;
	}

}
