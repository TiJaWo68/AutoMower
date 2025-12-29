package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GroundModel implements Serializable {

	static final long serialVersionUID = 123456789014L;

	public static final Color BORDER_COLOR = Color.ORANGE;
	public static final Color OBSTACLE_COLOR = Color.CYAN;
	public static final Color CHARGING_STATION_COLOR = Color.GREEN;
	protected Point2D highLightedPoint = null;
	protected Line2D highLightedLine = null;

	transient ChangeListener listener = e -> {
		// TODO Auto-generated method stub

	};

	MultiLine2D border = new MultiLine2D(BORDER_COLOR);

	List<MultiLine2D> obstacles = new LinkedList<>();

	/**
	 * cm pro pixel
	 */
	Double calibration = 10d;

	private transient BufferedImage image;
	private Point2D chargingStation;

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
		if (listener != null)
			listener.stateChanged(new ChangeEvent(image));
	}

	public GroundModel() {
		border.setShowIndices(true);
	}

	public MultiLine2D getBorder() {
		return border;
	}

	public void setBorder(MultiLine2D border) {
		this.border = border;
		updateNumbering();
		listener.stateChanged(new ChangeEvent(this));
	}

	public List<MultiLine2D> getObstacles() {
		return obstacles;
	}

	public void addObstacle(MultiLine2D obstacle) {
		obstacles.add(obstacle);
		listener.stateChanged(new ChangeEvent(this));
	}

	public void removeObstacle(MultiLine2D obstacle) {
		obstacles.remove(obstacle);
		highLightedLine = null;
		highLightedPoint = null;
		listener.stateChanged(new ChangeEvent(this));
	}

	public void setChangeListener(ChangeListener listener) {
		this.listener = listener;
	}

	public void draw(Graphics2D g2d, AffineTransform transform) {
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		border.draw(g2d, transform);
		for (MultiLine2D p : obstacles)
			p.draw(g2d, transform);
		g2d.setColor(Color.RED);
		if (highLightedLine != null) {
			Point2D p1 = transform.transform(highLightedLine.getP1(), new Point2D.Double());
			Point2D p2 = transform.transform(highLightedLine.getP2(), new Point2D.Double());
			g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
		} else if (highLightedPoint != null) {
			Point2D p = transform.transform(highLightedPoint, new Point2D.Double());
			g2d.drawOval((int) (p.getX() - MultiLine2D.ED), (int) (p.getY() - MultiLine2D.ED), 2 * MultiLine2D.ED,
					2 * MultiLine2D.ED);
		}

		if (chargingStation != null) {
			g2d.setColor(CHARGING_STATION_COLOR);
			Point2D p = transform.transform(chargingStation, new Point2D.Double());
			g2d.fillOval((int) (p.getX() - 5), (int) (p.getY() - 5), 10, 10);
			g2d.drawOval((int) (p.getX() - 7), (int) (p.getY() - 7), 14, 14);
		}

	}

	public void mouseMoved(Point2D p) {
		Line2D bLine = highLightedLine;
		Point2D bPoint = highLightedPoint;
		highLightedLine = border.getLine2D(p);
		highLightedPoint = border.getPoint(p);
		if (testForHighLightedChanged(bLine, bPoint))
			return;
		else
			for (MultiLine2D line : obstacles) {
				highLightedLine = line.getLine2D(p);
				highLightedPoint = line.getPoint(p);
				if (testForHighLightedChanged(bLine, bPoint))
					return;
			}
		if (highLightedLine == null && bLine != null || highLightedPoint == null && bPoint != null)
			listener.stateChanged(new ChangeEvent(this));
	}

	protected boolean testForHighLightedChanged(Line2D bLine, Point2D bPoint) {
		if ((highLightedPoint != null && highLightedPoint != bPoint)
				|| (highLightedLine != null && highLightedLine != bLine))
			listener.stateChanged(new ChangeEvent(this));
		return highLightedLine != null || highLightedPoint != null;
	}

	public MultiLine2D getObstacle(Point2D tp) {
		for (MultiLine2D line : obstacles)
			if (line.getLine2D(tp) != null || line.getPoint(tp) != null)
				return line;
		return null;
	}

	public void setCalibration(Line2D line, int lengthInCM) {
		double x = line.getX1() - line.getX2();
		double y = line.getY1() - line.getY2();
		calibration = lengthInCM / Math.sqrt(x * x + y * y);
	}

	public void setCalibration(Double calibration) {
		this.calibration = calibration;
	}

	public Point2D getChargingStation() {
		return chargingStation;
	}

	public void setChargingStation(Point2D chargingStation) {
		this.chargingStation = chargingStation;
		updateNumbering();
		listener.stateChanged(new ChangeEvent(this));
	}

	public List<Line2D> getCollisionLines(Point2D p1, Point2D p2) {
		Line2D given = new Line2D.Double(p1, p2);
		List<Line2D> result = new LinkedList<>();
		double distanceToP1 = Double.MAX_VALUE;
		List<MultiLine2D> lines = new LinkedList<>(obstacles);
		lines.add(border);
		for (MultiLine2D line : lines)
			for (int i = 0; i < line.getNumberOfPoints(); i++) {
				Line2D l = line.getLine(i);
				Point2D intersectPoint = GeomUtil.getIntersectPoint(l, given);
				if (intersectPoint != null && l.ptSegDist(intersectPoint) < 0.05
						&& given.ptSegDist(intersectPoint) < 0.05) {
					double distance = p1.distance(intersectPoint);
					if (distance > 0.05)
						if (distance < distanceToP1) {
							distanceToP1 = distance;
							result = List.of(l);
						} else if (distance == distanceToP1) {
							result = new LinkedList<>(result);
							result.add(l);
						}
				}
			}
		return result;
	}

	public Point2D getCollisionPoint(Point2D p1, Point2D p2) {
		Line2D given = new Line2D.Double(p1, p2);
		Point2D found = null;
		double distanceToP1 = Double.MAX_VALUE;
		List<MultiLine2D> lines = new LinkedList<>(obstacles);
		lines.add(border);
		for (MultiLine2D line : lines)
			for (int i = 0; i < line.getNumberOfPoints(); i++) {
				Line2D l = line.getLine(i);
				Point2D intersectPoint = GeomUtil.getIntersectPoint(l, given);
				if (intersectPoint != null && l.ptSegDist(intersectPoint) < 0.05
						&& given.ptSegDist(intersectPoint) < 0.05) {
					double distance = p1.distance(intersectPoint);
					if (distance < distanceToP1) {
						distanceToP1 = distance;
						found = intersectPoint;
					}
				}
			}
		return found;
	}

	public Double getCalibration() {
		return calibration;
	}

	public double getNetArea() {
		double netAreaPx = border.getArea();
		for (MultiLine2D obstacle : obstacles) {
			netAreaPx -= obstacle.getArea();
		}
		return netAreaPx * calibration * calibration;
	}

	public boolean isInside(Point2D p) {
		return isInside(p, 1.0);
	}

	public boolean isInside(Point2D p, double eps) {
		if (border != null && !border.contains(p)) {
			if (border.ptSegDist(p) > eps) {
				return false;
			}
		}
		for (MultiLine2D obstacle : obstacles) {
			if (obstacle.contains(p)) {
				if (obstacle.ptSegDist(p) > eps) {
					return false;
				}
			}
		}
		return true;
	}

	public Line2D getCollidingLine(Line2D check) {
		List<Line2D> closest = getCollisionLines(check.getP1(), check.getP2());
		if (closest.isEmpty())
			return null;
		return closest.get(0);
	}

	public void ensureChargingStationInside() {
		if (chargingStation == null || isInside(chargingStation))
			return;

		// Try to nudge it inside in 8 directions (up to 10px each)
		for (int dist = 1; dist <= 10; dist++) {
			for (int ang = 0; ang < 360; ang += 45) {
				double rad = Math.toRadians(ang);
				Point2D test = new Point2D.Double(chargingStation.getX() + Math.cos(rad) * dist,
						chargingStation.getY() + Math.sin(rad) * dist);
				if (isInside(test)) {
					chargingStation.setLocation(test);
					updateNumbering();
					return;
				}
			}
		}
	}

	public void updateNumbering() {
		if (chargingStation == null || border == null || border.getNumberOfPoints() == 0)
			return;

		int nearestIdx = 0;
		double minDist = Double.MAX_VALUE;
		List<Point2D> pts = border.getPoints();
		for (int i = 0; i < pts.size(); i++) {
			double d = pts.get(i).distance(chargingStation);
			if (d < minDist) {
				minDist = d;
				nearestIdx = i;
			}
		}

		// Determine "left" direction from dock's perspective (to match AutoMowerModel)
		int direction = 1;
		Point2D v = border.getPoint(nearestIdx);
		int nextIdx = (nearestIdx + 1) % border.getNumberOfPoints();
		Point2D vNext = border.getPoint(nextIdx);

		double dx_f = v.getX() - chargingStation.getX();
		double dy_f = v.getY() - chargingStation.getY();
		double dx_v = vNext.getX() - v.getX();
		double dy_v = vNext.getY() - v.getY();

		double det = dx_f * dy_v - dy_f * dx_v;
		direction = (det > 0) ? -1 : 1;

		border.setNumberingStartAndDirection(nearestIdx, direction);
		border.setShowIndices(true);
	}
}
