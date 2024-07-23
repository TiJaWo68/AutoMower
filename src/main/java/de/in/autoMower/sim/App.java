/**
 * 
 */
package de.in.autoMower.sim;

import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import com.formdev.flatlaf.FlatDarculaLaf;

/**
 * @author Till
 */
public class App extends JFrame {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			FlatDarculaLaf.setup();
			new App().setVisible(true);
		});
	}

	public App() {
		super("AutoMowerSimulation");
		getContentPane().add(new SetupGroundPanel(new GroundModel()));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(600, 600));
		pack();

		// The method for the menu
		menuBar();
		setLocationRelativeTo(null);
	}

	// The method menuBar
	private void menuBar() {

		// The complete menu bar
		JMenuBar menu = new JMenuBar();

		JMenu fileMenu = new JMenu("File");

		JMenuItem openProjekt = new JMenuItem("Open Projekt");
		openProjekt.setToolTipText("Opens an existing project" );
		fileMenu.add(openProjekt);

		JMenuItem changeProjekt = new JMenuItem("Change Projekt");
		changeProjekt.setToolTipText("Switches to another project");
		fileMenu.add(changeProjekt);

		fileMenu.addSeparator();

		JMenuItem exit = new JMenuItem("Exit");
		exit.setToolTipText("Quits the Application");
		fileMenu.add(exit);

		menu.add(fileMenu);

		this.setJMenuBar(menu);

	} 




}
