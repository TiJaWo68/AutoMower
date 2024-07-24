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

	/**
	 * @param args
	 */

	GroundModel ground;

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			FlatDarculaLaf.setup();
			new App().setVisible(true);
		});
	}

	public App() {
		super("AutoMowerSimulation");

		ground = new GroundModel();
		getContentPane().add(new SetupGroundPanel(ground));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(600, 600));
		pack();

		// A new instance of class MenuBar
		MenuBar callMenuBar = new MenuBar("");

		// Set the MenuBar
		this.setJMenuBar(callMenuBar.menuBar());

		setLocationRelativeTo(null);
	}

}
