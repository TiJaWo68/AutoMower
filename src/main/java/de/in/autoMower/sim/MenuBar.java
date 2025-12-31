package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class MenuBar {
	// The inner class for the actions
	static abstract class MyAction extends AbstractAction {

		// The constructor for the inner class MyActions
		public MyAction(String text, ImageIcon icon, String toolTip, KeyStroke shortCut, String actionText) {
			super(text, icon);
			putValue(SHORT_DESCRIPTION, toolTip);
			putValue(ACCELERATOR_KEY, shortCut);
			putValue(ACTION_COMMAND_KEY, actionText);

		} // End of the constructor
	} // End of the inner class MyActions

	static MyAction openProjectAct = new MyAction("Open Project", null, "Opens an existing project", null, "open") {
		JFileChooser fc = new JFileChooser(new File("."));

		{
			fc.setFileFilter(new FileNameExtensionFilter("JSON Project", "json"));
		}

		public void actionPerformed(ActionEvent e) {
			if (fc.showOpenDialog(fc) == JFileChooser.APPROVE_OPTION) {
				try {
					App.getApp().loadProject(fc.getSelectedFile());
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}
		}
	};
	static MyAction changeImageAct = new MyAction("Change Image", null, "Change picture in the project", null,
			"change") {

		JFileChooser fc = new JFileChooser(new File("."));

		@Override
		public void actionPerformed(ActionEvent e) {
			if (fc.showOpenDialog(fc) == JFileChooser.APPROVE_OPTION) {
				App app = App.getApp();
				GroundModel groundModel = app.getGroundModel();
				try {
					BufferedImage image = ImageIO.read(fc.getSelectedFile());

					groundModel.setImage(image);
				} catch (IOException ex) {
					ex.printStackTrace();
				}

			}
		}

	};

	static MyAction saveProjectAct = new MyAction("Save Project", null, "Saves the project in a json file", null,
			"save") {
		JFileChooser fc = new JFileChooser(new File("."));

		{
			fc.setFileFilter(new FileNameExtensionFilter("JSON Project", "json"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			App app = App.getApp();

			if (fc.showSaveDialog(app) == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				if (file != null && !file.getName().toLowerCase().endsWith(".json")) {
					file = new File(file.getParentFile(), file.getName() + ".json");
				}

				GroundModel groundModel = app.getGroundModel();
				AbstractAutoMowerModel autoMowerModel = app.getMower();

				try {
					ProjectData data = new ProjectData();
					data.calibration = groundModel.getCalibration();
					data.border = new ProjectData.MultiLineDTO(groundModel.getBorder());
					data.obstacles = groundModel.obstacles.stream().map(ProjectData.MultiLineDTO::new)
							.collect(Collectors.toList());
					data.mower = new ProjectData.MowerDTO(autoMowerModel);
					if (groundModel.getChargingStation() != null) {
						data.chargingStation = new ProjectData.PointDTO(groundModel.getChargingStation());
					}

					if (groundModel.getZonePoints() != null) {
						data.zonePoints = groundModel.getZonePoints().stream()
								.map(ProjectData.ZonePointDTO::new).collect(Collectors.toList());
					}

					if (groundModel.getImage() != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(groundModel.getImage(), "png", baos);
						data.backgroundImageBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
					}

					ObjectMapper mapper = new ObjectMapper();
					mapper.enable(SerializationFeature.INDENT_OUTPUT);
					mapper.writeValue(file, data);

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

	};

	static MyAction exitAct = new MyAction("Quit", null, "Quits the Application", null, "exit") {

		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(0);

		}

	};

	static MyAction mowerDataAct = new MyAction("Einstellungen", null, "You can enter the data for the mower here",
			null,
			"data") {

		@Override
		public void actionPerformed(ActionEvent e) {
			App app = App.getApp();
			AbstractAutoMowerModel autoMowerModel = app.getMower();
			new SettingsDialog(autoMowerModel);
		}

	};

	// The method for the menuBar
	public static JMenuBar create() {

		// The complete menu bar
		JMenuBar menu = new JMenuBar();

		// The file menu is inserted in the menu bar
		menu.add(createFileMenu());

		// The mower menu is inserted in the menu bar
		menu.add(createMowerMenu());

		AbstractAction startAction = new AbstractAction("Start ") {

			@Override
			public void actionPerformed(ActionEvent e) {
				App app = App.getApp();

				MultiLine2D line = new MultiLine2D(Color.RED);
				SimulationPanel panel = new SimulationPanel(app.getGroundModel());

				if (app.getGroundModel().border.getNumberOfPoints() > 0) {

					panel.setLine(line);
					app.setPanel(panel);
					app.getSpeedSlider().setVisible(true);

					if (app.getSimulation() == null)
						new Thread(() -> app.createSimulation(line).run()).start();
					else
						app.getSimulation().resume();
				}

			}
		};

		AbstractAction stopSimulationAction = new AbstractAction("Stop") {

			@Override
			public void actionPerformed(ActionEvent e) {
				App app = App.getApp();
				app.getSimulation().stop();
			}
		};

		AbstractAction resumeSimulationAction = new AbstractAction("Resume") {

			@Override
			public void actionPerformed(ActionEvent e) {
				App app = App.getApp();
				app.getSimulation().resume();

			}
		};

		AbstractAction simulationCancelAct = new AbstractAction("Cancel") {

			@Override
			public void actionPerformed(ActionEvent e) {
				App app = App.getApp();
				if (app.getSimulation() != null) {
					app.getSimulation().cancel();
					app.resetSimulation();
				}
				app.setPanel(new SetupGroundPanel(app.getGroundModel()));
				app.getSpeedSlider().setVisible(false);
			}

		};

		AbstractAction estimateTimeAct = new AbstractAction("Estimate Mowing Time") {
			@Override
			public void actionPerformed(ActionEvent e) {
				App app = App.getApp();
				GroundModel ground = app.getGroundModel();
				AbstractAutoMowerModel mower = app.getMower();

				double areaCm2 = ground.getNetArea();
				double areaM2 = areaCm2 / 10000.0;

				double speed = mower.getSpeedInCmPerSec();
				double width = mower.getMowingWidthInCm();

				if (speed <= 0 || width <= 0) {
					JOptionPane.showMessageDialog(app, "Please set valid mower speed and width in Mower -> Data.",
							"Estimation Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				double tMinSeconds = areaCm2 / (speed * width);

				// Probabilistic model for random coverage:
				// T(coverage) = -ln(1 - coverage) * (Area / R)
				// T_95% = -ln(0.05) * t_min ~= 3.0 * t_min
				// T_99% = -ln(0.01) * t_min ~= 4.6 * t_min

				double t95Seconds = tMinSeconds * 3.0;
				double t99Seconds = tMinSeconds * 4.6;

				String tMinStr = formatDuration(tMinSeconds);
				String t95Str = formatDuration(t95Seconds);
				String t99Str = formatDuration(t99Seconds);

				String message = String.format("""
						Net Mowing Area: %.2f m²

						Theoretical Minimum (100%% efficiency): %s

						Random Walk Estimation:
						~ 95%% Coverage: %s
						~ 99%% Coverage: %s
						""", areaM2, tMinStr, t95Str, t99Str);

				SimulationPanel oldPanel = app.getPanel();
				app.setPanel(new AreaVisualizationPanel(ground));

				JOptionPane.showMessageDialog(app, message, "Mowing Estimation", JOptionPane.INFORMATION_MESSAGE);

				// Restore
				if (oldPanel != null) {
					app.setPanel(oldPanel);
				} else {
					app.setPanel(new SetupGroundPanel(ground));
				}
			}

			private String formatDuration(double totalSeconds) {
				int hours = (int) (totalSeconds / 3600);
				int minutes = (int) ((totalSeconds % 3600) / 60);
				int seconds = (int) (totalSeconds % 60);
				if (hours > 0) {
					return String.format("%dh %dm %ds", hours, minutes, seconds);
				} else {
					return String.format("%dm %ds", minutes, seconds);
				}
			}
		};

		AbstractAction kantenschneidenAction = new AbstractAction("Kantenschneiden") {

			@Override
			public void actionPerformed(ActionEvent e) {
				App app = App.getApp();

				MultiLine2D line = new MultiLine2D(Color.RED);
				SimulationPanel panel = new SimulationPanel(app.getGroundModel());

				if (app.getGroundModel().border.getNumberOfPoints() > 0) {

					panel.setLine(line);
					app.setPanel(panel);
					app.getSpeedSlider().setVisible(true);

					if (app.getSimulation() == null) {
						app.createSimulation(line);
						new Thread(() -> app.getMower().startEdgeCutting(line, app.getGroundModel())).start();
					} else {
						app.getSimulation().resume();
					}
				}
			}
		};

		JMenu simulationMenu = new JMenu("Simulation");
		simulationMenu.add(startAction);
		simulationMenu.add(kantenschneidenAction);
		simulationMenu.add(stopSimulationAction);
		simulationMenu.add(resumeSimulationAction);
		simulationMenu.add(estimateTimeAct);
		simulationMenu.add(simulationCancelAct);
		menu.add(simulationMenu);

		return menu;

	} // End of method menuBar()

	// The method for Menu File
	private static JMenu createFileMenu() {
		JMenu fileMenu = new JMenu();
		fileMenu.setText("File");

		fileMenu.add(openProjectAct);
		fileMenu.add(changeImageAct);
		fileMenu.add(saveProjectAct);
		fileMenu.addSeparator();
		fileMenu.add(exitAct);
		return fileMenu;
	}

	// The method for Menu Mower
	public static JMenu createMowerMenu() {
		JMenu mowerMenu = new JMenu();
		mowerMenu.setText("Mower");

		mowerMenu.add(mowerDataAct);
		return mowerMenu;
	}

	public static JSlider createSpeedSlider(int defaultValue) {

		JSlider sliderSpeed = new JSlider(JSlider.HORIZONTAL, 1, 50, 1);
		sliderSpeed.setPaintLabels(true);
		sliderSpeed.setMinorTickSpacing(1);
		sliderSpeed.setMajorTickSpacing(10);
		sliderSpeed.setPaintTicks(true);
		sliderSpeed.setValue(1);
		sliderSpeed.setVisible(false);
		sliderSpeed.setToolTipText("Simulation Speed Scaling");

		sliderSpeed.addChangeListener(e -> {
			App app = App.getApp();
			app.getMower().setTimeScale(sliderSpeed.getValue());
		});
		return sliderSpeed;
	}

}
