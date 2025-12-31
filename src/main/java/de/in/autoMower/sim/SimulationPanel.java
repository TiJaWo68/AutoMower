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

import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.swing.JPanel;

/**
 * @author aqadb - Till Woitendorf
 */
public class SimulationPanel extends JPanel {

	protected GroundModel model;
	protected MultiLine2D line;

	private double userScale = 1.0;

	private double userTranslateX = 0;
	private double userTranslateY = 0;

	public SimulationPanel(GroundModel model) {
		super(new FlowLayout(FlowLayout.RIGHT));
		setModel(model);
		setOpaque(true);

		addMouseWheelListener(e -> {
			if (e.isControlDown()) {
				// 1. Calculate current transform parameters
				double width = 1d * getWidth();
				double height = 1d * getHeight();
				double iWidth = (1d * model.getImage().getWidth());
				double iHeight = (1d * model.getImage().getHeight());
				double baseZoom = 1d / Math.max(iWidth / width, iHeight / height);

				// Base centering offset
				double baseTx = (width - baseZoom * iWidth) / 2d;
				double baseTy = (height - baseZoom * iHeight) / 2d;

				double totalScale = baseZoom * userScale;
				double totalTx = baseTx + userTranslateX;
				double totalTy = baseTy + userTranslateY;

				// 2. Calculate world point under mouse
				double mx = e.getX();
				double my = e.getY();
				double wx = (mx - totalTx) / totalScale;
				double wy = (my - totalTy) / totalScale;

				// 3. Update Scale
				double delta = e.getPreciseWheelRotation();
				if (delta < 0) {
					userScale *= 1.1;
				} else {
					userScale /= 1.1;
				}
				if (userScale < 0.1)
					userScale = 0.1;
				if (userScale > 50.0)
					userScale = 50.0;

				// 4. Calculate new translation to keep (wx, wy) at (mx, my) mx = wx *
				// newTotalScale + newTotalTx newTotalTx = mx - wx *
				// newTotalScale
				double newTotalScale = baseZoom * userScale;
				double newTotalTx = mx - wx * newTotalScale;
				double newTotalTy = my - wy * newTotalScale;

				// 5. Update user translation newTotalTx = baseTx + newUserTranslateX
				userTranslateX = newTotalTx - baseTx;
				userTranslateY = newTotalTy - baseTy;
			}
			repaint();
		});

		addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getClickCount() == 2) {
					userScale = 1.0;
					userTranslateX = 0;
					userTranslateY = 0;
					repaint();
				}
			}
		});
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			if (model.getImage() != null && g instanceof Graphics2D g2d) {
				AffineTransform transform = createAffineTransform();
				g2d.drawImage(model.getImage(), transform, null);

				AbstractAutoMowerModel mower = App.getApp().getMower();
				double cmPropix = model.getCalibration();
				if (cmPropix <= 0)
					cmPropix = 1.0;

				double mowingWidthPixels = mower.getMowingWidthInCm() / cmPropix;
				mowingWidthPixels *= transform.getScaleX();

				java.awt.Stroke oldStroke = g2d.getStroke();
				g2d.setStroke(new java.awt.BasicStroke((float) mowingWidthPixels, java.awt.BasicStroke.CAP_ROUND,
						java.awt.BasicStroke.JOIN_ROUND));

				if (line != null) {
					line.setDrawPoints(false);
					line.draw(g2d, transform);
				}

				// Draw current active segment in GREEN
				if (mower != null && mower.getSegmentStart() != null && mower.getCurrentPosition() != null) {
					g2d.setColor(java.awt.Color.GREEN);
					Point2D p1 = transform.transform(mower.getSegmentStart(), new Point2D.Double());
					Point2D p2 = transform.transform(mower.getCurrentPosition(), new Point2D.Double());
					g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
				}

				g2d.setStroke(oldStroke);
				model.draw(g2d, transform);

				// Draw Battery Level and Status
				mower.drawStats(g2d, new Point2D.Double(10, 10));
				drawScaleBar(g);

				if (mower.isCharging()) {
					g.setColor(java.awt.Color.YELLOW);
					g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 24f));
					g.drawString("CHARGING...", getWidth() / 2 - 50, getHeight() / 2);
				}

				// Draw transition target if applicable
				if (mower.currentState.name().equals("TRANSITIONING_TO_ZONE")
						&& mower.getTransitionTargetPoint() != null) {
					g2d.setColor(java.awt.Color.CYAN);
					Point2D tp = transform.transform(mower.getTransitionTargetPoint(), new Point2D.Double());
					g2d.fillOval((int) tp.getX() - 5, (int) tp.getY() - 5, 10, 10);
					g2d.drawString("TARGET ZONE", (int) tp.getX() + 7, (int) tp.getY() - 7);
				}
			}
		} catch (Exception e) {
			System.err.println("CRITICAL IN PAINT: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void drawScaleBar(Graphics g) {
		AbstractAutoMowerModel mower = App.getApp().getMower();
		if ((mower == null) || (model.getCalibration() <= 0))
			return;

		// Fix for zoom: scale bar should reflect zoom level
		AffineTransform t = createAffineTransform();
		double currentZoom = t.getScaleX();

		// 1 meter = 100 cm pixelsPerMeter = (100 cm / cmProPixel) * zoom
		double pixelsPerMeter = (100.0 / model.getCalibration()) * currentZoom;

		int barLength = (int) pixelsPerMeter;
		if (barLength < 10)
			return; // Too small to convert useful info

		int barX = getWidth() - barLength - 20;
		int barY = getHeight() - 40;
		int barHeight = 5;

		g.setColor(java.awt.Color.WHITE);
		g.fillRect(barX, barY, barLength, barHeight);
		g.drawLine(barX, barY - 3, barX, barY + barHeight + 3);

		g.drawString("1m", barX + (int) pixelsPerMeter / 2 - 10, barY - 5);
	}

	protected AffineTransform createAffineTransform() {
		double width = 1d * getWidth();
		double height = 1d * getHeight();
		double iWidth = (1d * model.getImage().getWidth());
		double iHeight = (1d * model.getImage().getHeight());
		double baseZoom = 1d / Math.max(iWidth / width, iHeight / height);

		double zoom = baseZoom * userScale;

		// Base centering
		double baseTx = (width - baseZoom * iWidth) / 2d;
		double baseTy = (height - baseZoom * iHeight) / 2d;

		// Apply user translation
		double totalTx = baseTx + userTranslateX;
		double totalTy = baseTy + userTranslateY;

		AffineTransform transform = new AffineTransform();
		transform.translate(totalTx, totalTy);
		transform.scale(zoom, zoom);

		return transform;
	}

	public void setModel(GroundModel model) {
		this.model = model;
		model.setChangeListener(e -> {
			onModelChanged();
			repaint();
		});
		onModelChanged();
		repaint();
	}

	protected void onModelChanged() {
		// Hook for subclasses
	}

	public MultiLine2D getLine() {
		return line;
	}

	public void setLine(MultiLine2D line) {
		this.line = line;
	}
}
