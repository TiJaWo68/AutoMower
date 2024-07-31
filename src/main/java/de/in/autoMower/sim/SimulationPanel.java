//@formatter:off
//////////////////////////////////////////////////////////////
// C O P Y R I G H T (c) 2024                               //
// Dedalus HealthCare and/or its affiliates                 //
// All Rights Reserved                                      //
//////////////////////////////////////////////////////////////
//                                                          //
// THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF           //
// Dedalus HealthCare and/or its affiliates.                //
// The copyright notice above does not evidence any         //
// actual or intended publication of such source code.      //
//                                                          //
//////////////////////////////////////////////////////////////
//@formatter:on
package de.in.autoMower.sim;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.JPanel;
import javax.swing.JViewport;

/**
 * @author aqadb - Till Woitendorf
 */
public class SimulationPanel extends JPanel {

	double scale = 1.0;

	protected GroundModel model;
	protected MultiLine2D line;

	public SimulationPanel(GroundModel model) {
		super(new FlowLayout(FlowLayout.RIGHT));
		setModel(model);
		setOpaque(true);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (model.getImage() != null && g instanceof Graphics2D g2d) {
			AffineTransform transform = createAffineTransform();
			g2d.drawImage(model.getImage(), transform, null);
			model.draw(g2d, transform);
			if (line != null)
				line.draw(g2d, transform);
		}
	}

	protected AffineTransform createAffineTransform() {

		double width = 1d * getWidth();
		double height = 1d * getHeight();
		double iWidth = (1d * model.getImage().getWidth());
		double iHeight = (1d * model.getImage().getHeight());
		double zoom = 1d / Math.max(iWidth / width, iHeight / height);
		AffineTransform transform = AffineTransform.getScaleInstance(zoom, zoom);
		double xoff = (width - zoom * iWidth) / 2d;
		double yoff = (height - zoom * iHeight) / 2d;
		transform.translate(xoff, yoff);

		MouseWheelListener mouseZoom = new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				double scaleFactor = 0.001 * e.getPreciseWheelRotation();
				scale += scaleFactor;

				revalidate();
				repaint();

				System.err.println(scale);
			}

		};
		
		Dimension size = new Dimension();
		if (App.getApp().getGroundModel().getImage() != null) {
			size.width = (int) Math.round(App.getApp().getGroundModel().getImage().getWidth() * scale);
			size.height = (int) Math.round(App.getApp().getGroundModel().getImage().getWidth() * scale);
		}
		transform.scale(scale, scale);
		System.err.println(size);
		addMouseWheelListener(mouseZoom);

		return transform;

	}

	public void setModel(GroundModel model) {
		this.model = model;
		model.setChangeListener(e -> repaint());
		repaint();
	}

	public MultiLine2D getLine() {
		return line;
	}

	public void setLine(MultiLine2D line) {
		this.line = line;
	}

}
