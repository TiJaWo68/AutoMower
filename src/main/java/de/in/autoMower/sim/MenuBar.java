package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

		public void actionPerformed(ActionEvent e) {
			if (fc.showOpenDialog(fc) == JFileChooser.APPROVE_OPTION) {
				App app = App.getApp();

				try (ZipFile zf = new ZipFile(fc.getSelectedFile())) {

					ZipEntry ze = zf.getEntry("image");
					BufferedImage image = ImageIO.read(zf.getInputStream(ze));
					ze = zf.getEntry("groundModel");

					ObjectInputStream ois = new ObjectInputStream(zf.getInputStream(ze));
					GroundModel model = (GroundModel) ois.readObject();

					ze = zf.getEntry("autoMowerModel");
					ois = new ObjectInputStream(zf.getInputStream(ze));
					AutoMowerModel autoMowerModel = (AutoMowerModel) ois.readObject();
					model.setImage(image);
					app.setModel(model);
					app.setMower(autoMowerModel);
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

	static MyAction saveProjectAct = new MyAction("Save Project", null, "Saves the project in a zip file", null,
			"save") {
		JFileChooser fc = new JFileChooser(new File("."));

		@Override
		public void actionPerformed(ActionEvent e) {
			App app = App.getApp();
			if (fc.showSaveDialog(app) == JFileChooser.APPROVE_OPTION) {
				GroundModel groundModel = app.getGroundModel();
				AutoMowerModel autoMowerModel = app.getMower();
				;
				System.out.println("have fun " + fc.getSelectedFile().getAbsolutePath());
				try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(fc.getSelectedFile()))) {
					ZipEntry ze = new ZipEntry("groundModel");
					zip.putNextEntry(ze);
					ObjectOutputStream oos = new ObjectOutputStream(zip);
					oos.writeObject(groundModel);
					ze = new ZipEntry("image");
					zip.putNextEntry(ze);
					ImageIO.write(groundModel.getImage(), "png", zip);

					ze = new ZipEntry("autoMowerModel");
					zip.putNextEntry(ze);
					oos = new ObjectOutputStream(zip);
					oos.writeObject(autoMowerModel);

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

	static MyAction mowerDataAct = new MyAction("Data", null, "You can enter the data for the mower here", null,
			"data") {

		@Override
		public void actionPerformed(ActionEvent e) {
			App app = App.getApp();
			AutoMowerModel autoMowerModel = app.getMower();
			new SettingsDialog(autoMowerModel);
		}

	};

	private static Thread t = null;

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
				app.getSimulation().cancel();

			}

		};

		JMenu simulationMenu = new JMenu("Simulation");
		simulationMenu.add(startAction);
		simulationMenu.add(stopSimulationAction);
		simulationMenu.add(resumeSimulationAction);
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

		JSlider sliderSpeed = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
		sliderSpeed.setPaintLabels(true);
		sliderSpeed.setMinorTickSpacing(5);
		sliderSpeed.setMajorTickSpacing(10);
		sliderSpeed.setPaintTicks(true);
		sliderSpeed.setValue(defaultValue);
		sliderSpeed.setVisible(false);

		sliderSpeed.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				App app = App.getApp();
				app.getMower().setSpeedInCmPerSec(sliderSpeed.getValue());
			}
		});
		return sliderSpeed;
	}

}
