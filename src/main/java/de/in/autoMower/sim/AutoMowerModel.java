package de.in.autoMower.sim;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

public class AutoMowerModel implements Serializable {

	double speedInCmPerSec = 1000 / 36d;
	double mowingWidthInCm = 14;
	// akkulaufZeit
	// Ladezeit
	Point2D destination = null;
	Point2D currentPosition = null;
	boolean stopped = true;
	
	private Line2D currentLine ;

	public double getSpeedInCmPerSec() {
		return speedInCmPerSec;
	}

	public double getMowingWidthInCm() {
		return mowingWidthInCm;
	}

	public Point2D getDestination() {
		return destination;
	}

	public void setDestination(Point2D destination) {
		this.destination = destination;
	}

	public Point2D getCurrentPosition() {
		return currentPosition;
	}

	public void setCurrentPosition(Point2D currentPosition) {
		this.currentPosition = currentPosition;
	}

	public void setSpeedInCmPerSec(double speedInCmPerSec) {

		this.speedInCmPerSec = speedInCmPerSec;
	}

	public void setMowingWidthInCm(double mowingWidthInCm) {

		this.mowingWidthInCm = mowingWidthInCm;
	}

	private Random random;
	private long startTime;
	private Double cmProPixel;
	private MultiLine2D border;
	private List<Line2D> collisionLines;
	private MultiLine2D line;
	private GroundModel groundModel;
	
	public void start(MultiLine2D line, GroundModel groundModel) {
		try {
		    this.line = line;
		    this.groundModel = groundModel;
			random = new Random();
			startTime = System.currentTimeMillis();
			cmProPixel = groundModel.getCalibration();
			border = groundModel.getBorder();
			collisionLines = List.of(border.getLine(1));

			currentLine = new Line2D.Double(border.getPoint(0), border.getPoint(1));
			currentPosition = new Point2D.Double(border.getPoint(0).getX(), border.getPoint(0).getY());
			this.line.addPoint(border.getPoint(0));
			this.line.addPoint(currentPosition);
			
			runMower();

		} catch (NullPointerException ex) {
		    ex.printStackTrace();
			JOptionPane.showMessageDialog(null, "Please set a border line");
		}

	}

	
	public void runMower() {
	    stopped = false;

        while (!stopped) {

            if (currentPosition.equals(currentLine.getP2())) {
                Line2D cl = collisionLines.size() == 2
                        ? random.nextBoolean() ? collisionLines.get(0) : collisionLines.get(1)
                        : collisionLines.get(0);
                double angle = GeomUtil.getAngleDeg(currentLine.getP1(), currentPosition,
                        random.nextBoolean() ? cl.getP1() : cl.getP2());
                angle *= random.nextDouble(1);
                angle = angle * Math.PI / 180;
                double targetX = currentLine.getP1().getX() * Math.sin(angle);
                double targetY = currentLine.getP1().getY() * Math.cos(angle);

                currentLine = new Line2D.Double(currentPosition, new Point2D.Double(targetX, targetY));
                // Strecke mit 10km Länge erzeugen, diese Entferung wird nie erreicht
                Point2D p2 = GeomUtil.getColinearPointWithLength(currentLine.getP1(), currentLine.getP2(),
                        1000000d / cmProPixel);

                collisionLines = groundModel.getCollisionLines(currentPosition, p2);
                Point2D cop = GeomUtil.getIntersectPoint(collisionLines.get(0), currentLine);

                currentPosition = new Point2D.Double(currentPosition.getX(), currentPosition.getY());
                line.addPoint(currentPosition);
                currentLine = new Line2D.Double(currentPosition, cop);
                System.out.println(currentLine.getP2());
            }

            double diff = (System.currentTimeMillis() - startTime) / 1000d;
            double distanceInCm = speedInCmPerSec * diff;
            double pixelDistance = distanceInCm / cmProPixel;
            Point2D cop = GeomUtil.getColinearPointWithLength(currentLine.getP1(), currentLine.getP2(),
                    pixelDistance);
            if (currentLine.getP1().distance(cop) > currentLine.getP1().distance(currentLine.getP2()))
                currentPosition.setLocation(currentLine.getP2());
            else
                currentPosition.setLocation(cop);
            App.getApp().getPanel().repaint();

        }
	}
	
	public void stop() {
		stopped = true;
		currentPosition = new Point2D.Double(currentPosition.getX(), currentPosition.getY());

	}
	
	public void resume() {
		if (currentPosition.equals(currentLine.getP1())) {
		stopped = false;
		}
}



}
