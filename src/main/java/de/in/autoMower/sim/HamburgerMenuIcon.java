package de.in.autoMower.sim;

import javax.swing.ImageIcon;

import com.formdev.flatlaf.util.ScaledImageIcon;

public class HamburgerMenuIcon extends ScaledImageIcon {

	public HamburgerMenuIcon(int size) {
		super(new ImageIcon(ClassLoader.getSystemResource("HamburgerMenu.png")), size, size);
	}

}
