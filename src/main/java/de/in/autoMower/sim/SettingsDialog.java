package de.in.autoMower.sim;

import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class SettingsDialog {

	protected JFormattedTextField jTfSpeed, jTfWidth, jTfBattery, jTfConsumption, jTfCharge;

	public SettingsDialog(AbstractAutoMowerModel autoMowerModel) {
		JDialog dialog = new JDialog();
		dialog.setTitle("Mower Specifications");
		dialog.setLayout(new GridLayout(0, 2, 5, 5));

		NumberFormat numberformat = NumberFormat.getInstance(Locale.US);
		numberformat.setMaximumFractionDigits(4);

		JLabel jLTextWidth = new JLabel("   Width (cm)");
		jTfWidth = new JFormattedTextField(numberformat);
		jTfWidth.setText(numberformat.format(autoMowerModel.getMowingWidthInCm()));

		JLabel jLTextSpeed = new JLabel("   Speed (cm/sec)");
		jTfSpeed = new JFormattedTextField(numberformat);
		jTfSpeed.setText(numberformat.format(autoMowerModel.getSpeedInCmPerSec()));

		JLabel jLTextBattery = new JLabel("   Battery Cap (Wh)");
		jTfBattery = new JFormattedTextField(numberformat);
		jTfBattery.setText(numberformat.format(autoMowerModel.batteryCapacityWh));

		JLabel jLTextConsumption = new JLabel("   Cons. (Wh/cm)");
		jTfConsumption = new JFormattedTextField(numberformat);
		jTfConsumption.setText(numberformat.format(autoMowerModel.energyConsumptionWhPerCm));

		JLabel jLTextCharge = new JLabel("   Charge (Wh/s)");
		jTfCharge = new JFormattedTextField(numberformat);
		jTfCharge.setText(numberformat.format(autoMowerModel.chargeRateWhPerSec));

		JLabel jLTextVersion = new JLabel("   Model Version");
		AbstractAutoMowerModel[] versions = { new AutoMowerModel(), new AutoMowerModelV2(), new AutoMowerModelV3(),
				new AutoMowerModelV4() };
		javax.swing.JComboBox<AbstractAutoMowerModel> jCbVersion = new javax.swing.JComboBox<>(versions);

		// Set current selection
		for (int i = 0; i < versions.length; i++) {
			if (versions[i].getModelVersion() == autoMowerModel.getModelVersion()) {
				jCbVersion.setSelectedIndex(i);
				break;
			}
		}

		JButton jBCancel = new JButton("Cancel");
		jBCancel.addActionListener(e -> {
			dialog.dispose();
		});

		JButton jbSave = new JButton("Save");
		jbSave.addActionListener(e -> {
			try {
				double width = Double.parseDouble(jTfWidth.getText().replace(",", ""));
				double speed = Double.parseDouble(jTfSpeed.getText().replace(",", ""));
				double battery = Double.parseDouble(jTfBattery.getText().replace(",", ""));
				double consumption = Double.parseDouble(jTfConsumption.getText().replace(",", ""));
				double charge = Double.parseDouble(jTfCharge.getText().replace(",", ""));

				AbstractAutoMowerModel selectedModelTemplate = (AbstractAutoMowerModel) jCbVersion.getSelectedItem();
				AbstractAutoMowerModel targetMower = autoMowerModel;

				if (selectedModelTemplate.getModelVersion() != autoMowerModel.getModelVersion()) {
					targetMower = selectedModelTemplate.createNewInstance();

					// Copy state
					targetMower.setCurrentPosition(autoMowerModel.getCurrentPosition());
					// Note: If simulation is running, this might differ from live state if not
					// careful.
					// But usually settings are changed when stopped or it will just apply to next
					// run.

					App.getApp().setMower(targetMower);
					// If we have a simulation panel, we might need to tell it about the new mower?
					// The simulation panel accesses mower via App.getMower() usually or passed in
					// constructor.
					// Existing SimulationPanel holds a reference to mower? No, check
					// App.createSimulation
					// Simulation holds reference. App holds simulation.
					// If simulation running, we might need to stop it or update it.
					if (App.getApp().getSimulation() != null) {
						App.getApp().getSimulation().cancel();
						App.getApp().resetSimulation();
					}
				}

				targetMower.setMowingWidthInCm(width);
				targetMower.setSpeedInCmPerSec(speed);
				targetMower.batteryCapacityWh = battery;
				targetMower.energyConsumptionWhPerCm = consumption;
				targetMower.chargeRateWhPerSec = charge;

				if (targetMower.currentBatteryWh > targetMower.batteryCapacityWh) {
					targetMower.currentBatteryWh = targetMower.batteryCapacityWh;
				}

				dialog.dispose();
			} catch (NumberFormatException ex) {
				JOptionPane.showMessageDialog(null, "Incorrect format: " + ex.getMessage());
			}
		});

		dialog.add(jLTextWidth);
		dialog.add(jTfWidth);
		dialog.add(jLTextSpeed);
		dialog.add(jTfSpeed);
		dialog.add(jLTextBattery);
		dialog.add(jTfBattery);
		dialog.add(jLTextConsumption);
		dialog.add(jTfConsumption);
		dialog.add(jLTextCharge);
		dialog.add(jTfCharge);
		dialog.add(jLTextVersion);
		dialog.add(jCbVersion);
		dialog.add(jBCancel);
		dialog.add(jbSave);

		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setSize(350, 300); // Increased height
		dialog.setLocationRelativeTo(App.getApp());
		dialog.setResizable(false);
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	public JFormattedTextField getjTfSpeed() {
		return jTfSpeed;
	}

}
