/**
 * 
 */
package de.in.autoMower.sim;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JSlider;
import javax.swing.WindowConstants;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	// private LogPanel logPanel;
	// private javax.swing.JSplitPane splitPane;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			FlatDarculaLaf.setup();
			app = new App();
			app.setVisible(true);
			if (args.length > 0) {
				File selectedFile = new File(args[0]);
				if (selectedFile.canRead())
					try {
						app.loadProject(selectedFile);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
			}
		});
	}

	public App() {
		super("AutoMowerSimulation");

		ground = new GroundModel();
		panel = new SetupGroundPanel(ground);
		mower = new AutoMowerModel();

		// logPanel = new LogPanel();
		// splitPane = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT,
		// panel, logPanel);
		// splitPane.setResizeWeight(0.8); // 80% space for simulation

		getContentPane().add(panel);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setMinimumSize(new Dimension(600, 600));
		pack();

		// Set the MenuBar
		JMenuBar menu = MenuBar.create();
		speedSlider = MenuBar.createSpeedSlider((int) mower.speedInCmPerSec);
		menu.add(getSpeedSlider());
		this.setJMenuBar(menu);

		setLocationRelativeTo(null);
	}

	// public void log(String message) {
	// if (logPanel != null) {
	// logPanel.log(message);
	// }
	// }

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

	public Simulation createSimulation(MultiLine2D line) {
		simulation = new Simulation(ground, mower, line);
		return simulation;
	}

	public void resetSimulation() {
		simulation = null;
	}

	public void setPanel(SimulationPanel panel) {
		if (this.panel != null) {
			getContentPane().remove(this.panel);
		}
		this.panel = panel;
		getContentPane().add(panel);
		revalidate();
		repaint();
	}

	public JSlider getSpeedSlider() {
		return speedSlider;
	}

	public void loadProject(File selectedFile) throws StreamReadException, DatabindException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		ProjectData data = mapper.readValue(selectedFile, ProjectData.class);

		GroundModel model = new GroundModel();
		model.calibration = data.calibration;
		model.border = data.border.toMultiLine();
		model.obstacles = data.obstacles.stream().map(ProjectData.MultiLineDTO::toMultiLine)
				.collect(Collectors.toList());
		if (data.chargingStation != null) {
			model.setChargingStation(data.chargingStation.toPoint());
			model.ensureChargingStationInside();
		}

		if (data.backgroundImageBase64 != null) {
			byte[] imageBytes = Base64.getDecoder().decode(data.backgroundImageBase64);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
			model.setImage(image);
		}

		AutoMowerModel mower = new AutoMowerModel();
		if (data.mower != null) {
			mower.setSpeedInCmPerSec(data.mower.speedInCmPerSec);
			mower.setMowingWidthInCm(data.mower.mowingWidthInCm);
			mower.batteryCapacityWh = data.mower.batteryCapacityWh;
			mower.currentBatteryWh = mower.batteryCapacityWh;
			mower.energyConsumptionWhPerCm = data.mower.energyConsumptionWhPerCm;
			mower.chargeRateWhPerSec = data.mower.chargeRateWhPerSec;
			if (data.mower.currentPosition != null) {
				mower.setCurrentPosition(data.mower.currentPosition.toPoint());
			}
		}

		app.setModel(model);
		app.setMower(mower);

	}

}
