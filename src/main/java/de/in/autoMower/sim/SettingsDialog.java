package de.in.autoMower.sim;

import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class SettingsDialog {

	static JTextField jTfSpeed;
	static JTextField jTfWidth;
	AutoMowerModel mowerModel = new AutoMowerModel();

	public static JTextField getjTfSpeed() {
		return jTfSpeed;
	}

	public static JTextField getjTfWidth() {
		return jTfWidth;
	}

	public SettingsDialog() {
		JDialog dialog = new JDialog();
		dialog.setTitle("Mower Specifications");
		dialog.setLayout(new GridLayout(0, 2, 5, 5));

		JLabel jLTextWidth = new JLabel();
		jLTextWidth.setText("   Width (cm)");
		jTfWidth = new JTextField();

		JLabel jLTextSpeed = new JLabel();
		jLTextSpeed.setText("   Speed (cm/sec)");
		jTfSpeed = new JTextField();

		JButton jBCancel = new JButton("Cancel");
		jBCancel.addActionListener(e -> {
			dialog.dispose();
		});

		JButton jbSave = new JButton("Save");
		jbSave.addActionListener(e -> {
			mowerModel.setMowingWidthInCm(0);
			mowerModel.setSpeedInCmPerSec(0);
			jTfWidth.setText("");
			jTfSpeed.setText("");
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
}
