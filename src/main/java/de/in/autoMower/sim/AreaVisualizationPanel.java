package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

public class AreaVisualizationPanel extends SimulationPanel {

    private static final long serialVersionUID = 1L;
    private List<Shape> gridSquares = new ArrayList<>();
    private int drawnCount = 0;
    private Timer timer;

    public AreaVisualizationPanel(GroundModel model) {
        super(model);
        calculateGrid();
        startAnimation();
    }

    private void calculateGrid() {
        gridSquares.clear();
        if (model == null || model.getBorder() == null)
            return;

        double calibration = model.getCalibration();
        if (calibration <= 0)
            calibration = 1.0;
        double squareSizePx = 100.0 / calibration; // 1m in pixels

        // 1. Find Longest Edge Angle
        double angle = 0;
        MultiLine2D border = model.getBorder();
        double maxLenSq = -1;

        List<Point2D> points = border.getPoints();
        if (points.size() > 1) {
            for (int i = 0; i < points.size(); i++) {
                Point2D p1 = points.get(i);
                Point2D p2 = points.get((i + 1) % points.size());
                double dx = p2.getX() - p1.getX();
                double dy = p2.getY() - p1.getY();
                double lenSq = dx * dx + dy * dy;
                if (lenSq > maxLenSq) {
                    maxLenSq = lenSq;
                    angle = Math.atan2(dy, dx);
                }
            }
        }

        // 2. Setup Rotation
        AffineTransform toRotated = AffineTransform.getRotateInstance(-angle);
        AffineTransform toWorld = AffineTransform.getRotateInstance(angle);

        // 3. Find Bounds in Rotated Space
        double minU = Double.MAX_VALUE, minV = Double.MAX_VALUE;
        double maxU = -Double.MAX_VALUE, maxV = -Double.MAX_VALUE;

        for (Point2D p : points) {
            Point2D rotP = toRotated.transform(p, null);
            if (rotP.getX() < minU)
                minU = rotP.getX();
            if (rotP.getY() < minV)
                minV = rotP.getY();
            if (rotP.getX() > maxU)
                maxU = rotP.getX();
            if (rotP.getY() > maxV)
                maxV = rotP.getY();
        }

        // 4. Generate Grid
        for (double u = minU; u < maxU; u += squareSizePx) {
            for (double v = minV; v < maxV; v += squareSizePx) {
                // Center in rotated space
                double uc = u + squareSizePx / 2.0;
                double vc = v + squareSizePx / 2.0;

                // Transform center to world to check 'isInside'
                Point2D centerWorld = toWorld.transform(new Point2D.Double(uc, vc), null);

                if (model.isInside(centerWorld)) {
                    // Create square in world space
                    Path2D square = new Path2D.Double();
                    Point2D p0 = toWorld.transform(new Point2D.Double(u, v), null);
                    Point2D p1 = toWorld.transform(new Point2D.Double(u + squareSizePx, v), null);
                    Point2D p2 = toWorld.transform(new Point2D.Double(u + squareSizePx, v + squareSizePx), null);
                    Point2D p3 = toWorld.transform(new Point2D.Double(u, v + squareSizePx), null);

                    square.moveTo(p0.getX(), p0.getY());
                    square.lineTo(p1.getX(), p1.getY());
                    square.lineTo(p2.getX(), p2.getY());
                    square.lineTo(p3.getX(), p3.getY());
                    square.closePath();

                    gridSquares.add(square);
                }
            }
        }
    }

    private void startAnimation() {
        drawnCount = 0;
        if (timer != null && timer.isRunning())
            timer.stop();

        // Animate: Add ~10 squares every 50ms, or faster depending on total count
        int delay = 20;
        int step = Math.max(1, gridSquares.size() / 100); // Finish in ~2 seconds (100 * 20ms)

        timer = new Timer(delay, e -> {
            drawnCount += step;
            if (drawnCount >= gridSquares.size()) {
                drawnCount = gridSquares.size();
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        timer.start();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g); // Draws background, map, mower etc.

        Graphics2D g2 = (Graphics2D) g;
        // Use semi-transparent red
        g2.setColor(new Color(255, 0, 0, 100)); // R, G, B, Alpha

        // We need to apply transform if SimulationPanel uses one?
        // SimulationPanel draws using 'at' (AffineTransform) if overridden or if it
        // sets up G2.
        // Looking at SimulationPanel code (from memory/previous views):
        // It usually applies transform in paintComponent or draw methods.
        // The SetupGroundPanel/SimulationPanel logic usually handles Zoom/Pan.
        // If SimulationPanel does 'super.paintComponent(g)' and then transforms...
        // Let's assume SimulationPanel manages transform and we need to draw in WORLD
        // coordinates
        // OR SimulationPanel sets up the transform for us.

        // Wait, SetupGroundPanel uses `createAffineTransform()`. SimulationPanel likely
        // has it too.
        // If I draw raw rectangles, I need to transform them.

        java.awt.geom.AffineTransform at = createAffineTransform();
        java.awt.geom.AffineTransform saveAt = g2.getTransform();
        g2.transform(at);

        for (int i = 0; i < drawnCount; i++) {
            g2.fill(gridSquares.get(i));
            g2.setColor(Color.BLACK);
            g2.draw(gridSquares.get(i)); // Border
            g2.setColor(new Color(255, 0, 0, 100));
        }

        g2.setTransform(saveAt);

        // Draw info overlay (no transform)
        g2.setColor(Color.WHITE);
        g2.drawString("Visualizing " + gridSquares.size() + " m² blocks", 10, 120);
    }
}
