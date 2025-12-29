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
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
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

		addMouseWheelListener(new MouseWheelListener() {

			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
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

					// 4. Calculate new translation to keep (wx, wy) at (mx, my)
					// mx = wx * newTotalScale + newTotalTx
					// newTotalTx = mx - wx * newTotalScale
					double newTotalScale = baseZoom * userScale;
					double newTotalTx = mx - wx * newTotalScale;
					double newTotalTy = my - wy * newTotalScale;

					// 5. Update user translation
					// newTotalTx = baseTx + newUserTranslateX
					userTranslateX = newTotalTx - baseTx;
					userTranslateY = newTotalTy - baseTy;

					repaint();
				} else {
					repaint();
				}
			}
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

	private int paintCounter = 0;

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		try {
			if (model.getImage() != null && g instanceof Graphics2D g2d) {
				AffineTransform transform = createAffineTransform();
				g2d.drawImage(model.getImage(), transform, null);

				AutoMowerModel mower = App.getApp().getMower();
				// Use the actual total scale for consistency with rendering?
				// mower.cmProPixel is physical calibration.
				// The view scale is transform.getScaleX().
				// But drawing logic below might assume model space coordinates if using
				// transform.

				// Wait, the drawing logic below:
				// line.draw(g2d, transform) -> transforms points.
				// stroke width needs to be in model pixels or screen pixels?
				// If we set stroke width, it's affected by transform if we draw in model space?
				// No, setStroke is affected by transform ONLY if we transform the Graphics
				// context.
				// But here we are passing 'transform' to 'line.draw(g2d, transform)'.
				// So 'line.draw' probably manually transforms points?
				// Let's assume existing logic works if 'transform' maps Model -> Screen.

				double cmPropix = (mower.cmProPixel != null) ? mower.cmProPixel : model.getCalibration();
				if (cmPropix <= 0)
					cmPropix = 1.0;

				// mowingWidth is in cm.
				// mowingWidthPixels = cm / (cm/pixel) = pixels (model space).
				// If we draw in screen space (transforming points), the line width should be
				// scaled?
				// Original code:
				// double mowingWidthPixels = mower.getMowingWidthInCm() / cmPropix;
				// g2d.setStroke(..., mowingWidthPixels, ...)

				// Check line.draw implementation... assuming it uses the transform to verify.
				// Assuming standard implementation:
				// If we draw transformed points, we are drawing in Screen Space.
				// So stroke width 'mowingWidthPixels' (Model Space) needs to be scaled by
				// 'zoom'?
				// Original code didn't scale stroke?
				// "double mowingWidthPixels = mower.getMowingWidthInCm() / cmPropix;"
				// This is width in Image Pixels.
				// If we draw on Screen, and Image is zoomed 2x, the mower path should be 2x
				// wider?
				// If 'line.draw' transforms points to Screen Space, then we are drawing in
				// Screen Space.
				// So a fixed stroke width of 'mowingWidthPixels' would look THIN when zoomed
				// in.
				// We should probably scale the stroke width by 'transform.getScaleX()'.

				// However, if 'line.draw' applies the transform to the Graphics object state?
				// "line.draw(g2d, transform)"

				// Let's stick to modifying 'createAffineTransform' and 'userTranslate' for now
				// and assume the existing drawing logic works with the returned transform.

				double mowingWidthPixels = mower.getMowingWidthInCm() / cmPropix;
				// Adjust stroke width by zoom so it scales with image?
				// Original code: did not. It used 'mowingWidthPixels' which is constant.
				// If I zoom in, the stroke stays constant width on screen -> looks thinner
				// relative to image.
				// If user wants zoom, they probably want to see details. Scaling stroke is
				// usually desired.
				// Let's multiply by transform.getScaleX() to keep physical size correct on
				// screen.
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
				drawBatteryStatus(g);
				drawScaleBar(g);

				if (mower.isCharging) {
					g.setColor(java.awt.Color.YELLOW);
					g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 24f));
					g.drawString("CHARGING...", getWidth() / 2 - 50, getHeight() / 2);
				}

				paintCounter++;
				if (paintCounter % 100 == 0) {
					// System.out.println("Repainted 100 frames. Line points: " + (line != null ?
					// line.getNumberOfPoints() : "null"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("CRITICAL IN PAINT: " + e.getMessage());
		}
	}

	private void drawBatteryStatus(Graphics g) {
		AutoMowerModel mower = App.getApp().getMower();
		if (mower == null)
			return;

		int x = 10;
		int y = 10;
		int width = 100;
		int height = 15;

		double batteryPercent = mower.currentBatteryWh / mower.batteryCapacityWh;

		// Draw battery frame
		g.setColor(java.awt.Color.GRAY);
		g.drawRect(x, y, width, height);
		g.drawRect(x + width, y + height / 4, 4, height / 2); // Battery tip

		// Draw battery fill
		if (batteryPercent > 0.5)
			g.setColor(java.awt.Color.GREEN);
		else if (batteryPercent > 0.15)
			g.setColor(java.awt.Color.ORANGE);
		else
			g.setColor(java.awt.Color.RED);

		g.fillRect(x + 1, y + 1, (int) ((width - 1) * batteryPercent), height - 1);

		// Draw status text
		g.setColor(java.awt.Color.WHITE);
		String status = "Mowing";
		if (mower.isCharging)
			status = "Charging";
		else if (mower.isReturningToDock)
			status = "Returning home";

		g.drawString(String.format("%s (%.0f%%)", status, batteryPercent * 100), x, y + height + 15);

		// Draw runtime
		int hours = (int) (mower.simulatedRuntimeSeconds / 3600);
		int minutes = (int) ((mower.simulatedRuntimeSeconds % 3600) / 60);
		int seconds = (int) (mower.simulatedRuntimeSeconds % 60);
		String runtimeStr = String.format("Runtime: %02d:%02d:%02d", hours, minutes, seconds);
		g.drawString(runtimeStr, x, y + height + 30);
	}

	private void drawScaleBar(Graphics g) {
		AutoMowerModel mower = App.getApp().getMower();
		if (mower == null || mower.cmProPixel == null)
			return;

		// Fix for zoom: scale bar should reflect zoom level
		AffineTransform t = createAffineTransform();
		double currentZoom = t.getScaleX();

		// 1 meter = 100 cm
		// pixelsPerMeter = (100 cm / cmProPixel) * zoom
		double pixelsPerMeter = (100.0 / mower.cmProPixel) * currentZoom;

		int barX = getWidth() - (int) pixelsPerMeter - 20;
		int barY = getHeight() - 40;
		int barHeight = 5;

		g.setColor(java.awt.Color.WHITE);
		g.fillRect(barX, barY, (int) pixelsPerMeter, barHeight);
		g.drawLine(barX, barY - 3, barX, barY + barHeight + 3);
		g.drawLine(barX + (int) pixelsPerMeter, barY - 3, barX + (int) pixelsPerMeter, barY + barHeight + 3);

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
