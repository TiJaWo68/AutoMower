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

	public SettingsDialog(AutoMowerModel autoMowerModel) {
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

		JButton jBCancel = new JButton("Cancel");
		jBCancel.addActionListener(e -> {
			dialog.dispose();
		});

		JButton jbSave = new JButton("Save");
		jbSave.addActionListener(e -> {
			try {
				autoMowerModel.setMowingWidthInCm(Double.parseDouble(jTfWidth.getText().replace(",", "")));
				autoMowerModel.setSpeedInCmPerSec(Double.parseDouble(jTfSpeed.getText().replace(",", "")));
				autoMowerModel.batteryCapacityWh = Double.parseDouble(jTfBattery.getText().replace(",", ""));
				autoMowerModel.energyConsumptionWhPerCm = Double.parseDouble(jTfConsumption.getText().replace(",", ""));
				autoMowerModel.chargeRateWhPerSec = Double.parseDouble(jTfCharge.getText().replace(",", ""));

				// Reset current battery if capacity changes significantly?
				// For now just cap it.
				if (autoMowerModel.currentBatteryWh > autoMowerModel.batteryCapacityWh) {
					autoMowerModel.currentBatteryWh = autoMowerModel.batteryCapacityWh;
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
		dialog.add(jBCancel);
		dialog.add(jbSave);

		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setSize(350, 250);
		dialog.setLocationRelativeTo(App.getApp());
		dialog.setResizable(false);
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	public JFormattedTextField getjTfSpeed() {
		return jTfSpeed;
	}

}
