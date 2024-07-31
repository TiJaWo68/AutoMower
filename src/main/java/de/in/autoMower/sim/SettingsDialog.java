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

	protected JFormattedTextField jTfSpeed, jTfWidth;

	public SettingsDialog(AutoMowerModel autoMowerModel) {
		JDialog dialog = new JDialog();
		dialog.setTitle("Mower Specifications");
		dialog.setLayout(new GridLayout(0, 2, 5, 5));

		
		NumberFormat numberformat = NumberFormat.getInstance(Locale.US);
		numberformat.setMaximumFractionDigits(2);

		JLabel jLTextWidth = new JLabel();
		jLTextWidth.setText("   Width (cm)");
		jTfWidth = new JFormattedTextField(numberformat);
		jTfWidth.setText(String.valueOf(numberformat.format(autoMowerModel.getMowingWidthInCm())));

		JLabel jLTextSpeed = new JLabel();
		jLTextSpeed.setText("   Speed (cm/sec)");
		jTfSpeed = new JFormattedTextField(numberformat);
		jTfSpeed.setText(String.valueOf(numberformat.format(autoMowerModel.getSpeedInCmPerSec())));

		JButton jBCancel = new JButton("Cancel");
		jBCancel.addActionListener(e -> {
			dialog.dispose();
		});

		JButton jbSave = new JButton("Save");
		jbSave.addActionListener(e -> {

			try {
				if (jTfWidth.getText().isEmpty())
					JOptionPane.showMessageDialog(null, "Please insert the width of mover");
				else
					autoMowerModel.setMowingWidthInCm(Double.parseDouble(jTfWidth.getText()));

				if (jTfSpeed.getText().isEmpty())
					JOptionPane.showMessageDialog(null, "Please insert the speed of mover");
				else
					autoMowerModel.setSpeedInCmPerSec(Double.parseDouble(jTfSpeed.getText()));

			} catch (NumberFormatException ex) {
				System.err.println("Incorrect format");
				JOptionPane.showMessageDialog(null, "Incorrect format");
			}
		});

		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setLocationRelativeTo(App.getApp());
		dialog.setSize(300, 130);
		dialog.setResizable(false);

		dialog.add(jLTextWidth);
		dialog.add(jTfWidth);
		dialog.add(jLTextSpeed);
		dialog.add(jTfSpeed);
		dialog.add(jBCancel);
		dialog.add(jbSave);
		dialog.setModal(true);
		dialog.setVisible(true);
	}

	public JFormattedTextField getjTfSpeed() {
		return jTfSpeed;
	}

}
