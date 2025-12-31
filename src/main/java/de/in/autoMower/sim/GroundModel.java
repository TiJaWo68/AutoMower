package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
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
	private final List<ZonePoint> zonePoints = new ArrayList<>();

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

		drawZonePoints(g2d, transform);

	}

	private void drawZonePoints(Graphics2D g, AffineTransform transform) {
		if (zonePoints.isEmpty())
			return;

		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 12f));

		for (int i = 0; i < zonePoints.size(); i++) {
			ZonePoint zp = zonePoints.get(i);
			Point2D worldP = zp.getPoint();
			Point2D viewP = transform.transform(worldP, new Point2D.Double());

			g.setColor(CHARGING_STATION_COLOR);
			int s = 6;
			g.fillRect((int) viewP.getX() - s / 2, (int) viewP.getY() - s / 2, s, s);
			g.setColor(Color.BLACK);
			g.drawRect((int) viewP.getX() - s / 2, (int) viewP.getY() - s / 2, s, s);

			String label = getZoneLabel(i) + " (" + zp.getPercentage() + "%)";
			int tx = (int) viewP.getX() + 8;
			int ty = (int) viewP.getY() + 4;

			// Shadow
			g.setColor(new Color(0, 0, 0, 180));
			g.drawString(label, tx + 1, ty + 1);
			// Text
			g.setColor(CHARGING_STATION_COLOR);
			g.drawString(label, tx, ty);
		}
	}

	public String getZoneLabel(int index) {
		StringBuilder sb = new StringBuilder();
		int i = index;
		do {
			sb.insert(0, (char) ('A' + (i % 26)));
			i = (i / 26) - 1;
		} while (i >= 0);
		return sb.toString();
	}

	public void mouseMoved(Point2D p) {
		Line2D bLine = highLightedLine;
		Point2D bPoint = highLightedPoint;
		highLightedLine = border.getLine2D(p);
		highLightedPoint = border.getPoint(p);
		if (testForHighLightedChanged(bLine, bPoint))
			return;
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
		if ((border != null && !border.contains(p)) && (border.ptSegDist(p) > eps)) {
			return false;
		}
		for (MultiLine2D obstacle : obstacles) {
			if (obstacle.contains(p) && (obstacle.ptSegDist(p) > eps)) {
				return false;
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

		// Snap to nearest vertex
		chargingStation.setLocation(pts.get(nearestIdx));

		Point2D v = border.getPoint(nearestIdx);
		int nextIdx = (nearestIdx + 1) % border.getNumberOfPoints();
		Point2D vNext = border.getPoint(nextIdx);

		double dx_f = v.getX() - chargingStation.getX();
		double dy_f = v.getY() - chargingStation.getY();
		double dx_v = vNext.getX() - v.getX();
		double dy_v = vNext.getY() - v.getY();

		double det = dx_f * dy_v - dy_f * dx_v;
		int direction = (det > 0) ? -1 : 1;

		border.setNumberingStartAndDirection(nearestIdx, direction);
		border.setShowIndices(true);

		sortZonePoints();
		listener.stateChanged(new ChangeEvent(this));
	}

	public List<ZonePoint> getZonePoints() {
		return zonePoints;
	}

	public void setZonePoints(List<ZonePoint> zones) {
		this.zonePoints.clear();
		this.zonePoints.addAll(zones);
		sortZonePoints();
		listener.stateChanged(new ChangeEvent(this));
	}

	public void removeZonePointNear(Point2D p, double threshold) {
		ZonePoint found = null;
		for (ZonePoint zp : zonePoints) {
			if (zp.getPoint().distance(p) < threshold) {
				found = zp;
				break;
			}
		}
		if (found != null) {
			zonePoints.remove(found);
			sortZonePoints();
			listener.stateChanged(new ChangeEvent(this));
		}
	}

	public void addZonePoint(Point2D p, int percentage) {
		if (border == null || border.getNumberOfPoints() < 2)
			return;

		Point2D projected = projectToBorder(p);
		if (projected != null) {
			zonePoints.add(new ZonePoint(projected, percentage));
			sortZonePoints();
			listener.stateChanged(new ChangeEvent(this));
		}
	}

	private Point2D projectToBorder(Point2D p) {
		List<Point2D> pts = border.getPoints();
		int n = pts.size();
		double minD = Double.MAX_VALUE;
		Point2D best = null;

		for (int i = 0; i < n; i++) {
			int next = (i + 1) % n;
			Line2D segment = new Line2D.Double(pts.get(i), pts.get(next));
			Point2D proj = GeomUtil.getPerpendicularPointToLine(segment, p);
			if (proj != null) {
				// Clamp to segment
				double dist = segment.ptSegDist(p);
				if (dist < minD) {
					minD = dist;
					// Ensure proj is on segment
					if (segment.ptSegDist(proj) > 0.001) {
						if (p.distance(pts.get(i)) < p.distance(pts.get(next))) {
							best = (Point2D) pts.get(i).clone();
						} else {
							best = (Point2D) pts.get(next).clone();
						}
					} else {
						best = proj;
					}
				}
			}
		}
		return best;
	}

	private void sortZonePoints() {
		if (border == null || border.getNumberOfPoints() == 0 || zonePoints.isEmpty())
			return;

		int startIdx = border.numberingStartIndex;
		int direction = border.numberingDirection;
		List<Point2D> pts = border.getPoints();

		zonePoints.sort(Comparator.comparingDouble(p -> getDistOnPerimeter(p.getPoint(), startIdx, direction, pts)));
	}

	public double getDistOnPerimeter(Point2D p) {
		if (border == null || border.getNumberOfPoints() == 0)
			return 0;
		return getDistOnPerimeter(p, border.numberingStartIndex, border.numberingDirection, border.getPoints());
	}

	private double getDistOnPerimeter(Point2D p, int startIdx, int direction, List<Point2D> pts) {
		int n = pts.size();
		// Find segment containing p
		double minDist = Double.MAX_VALUE;
		int segmentStart = -1;
		for (int i = 0; i < n; i++) {
			int next = (i + 1) % n;
			Line2D seg = new Line2D.Double(pts.get(i), pts.get(next));
			double d = seg.ptSegDist(p);
			if (d < minDist) {
				minDist = d;
				segmentStart = i;
			}
		}

		double totalDist = 0;
		int curr = startIdx;
		while (curr != segmentStart) {
			int next = (curr + direction + n) % n;
			totalDist += pts.get(curr).distance(pts.get(next));
			curr = next;
		}

		// distance from curr to p
		// Note: p is on segment (segmentStart, segmentStart+1)
		// If direction is 1, we add distance from segmentStart to p
		// If direction is -1, the segment relative to our path order is different.
		// Wait, the while loop brings us to segmentStart in the specific direction.
		// So we just need to add directed distance from segmentStart to p within the
		// segment.

		totalDist += pts.get(segmentStart).distance(p);
		return totalDist;
	}

	public double getPerimeterLength() {
		if (border == null || border.getNumberOfPoints() < 2)
			return 0;
		double total = 0;
		List<Point2D> pts = border.getPoints();
		for (int i = 0; i < pts.size(); i++) {
			total += pts.get(i).distance(pts.get((i + 1) % pts.size()));
		}
		return total;
	}

	public Point2D getPointAtDistOnPerimeter(double dist) {
		if (border == null || border.getNumberOfPoints() < 2)
			return null;

		List<Point2D> pts = border.getPoints();
		int n = pts.size();
		int startIdx = border.numberingStartIndex;
		int direction = border.numberingDirection;

		double currentDist = 0;
		int curr = startIdx;
		double maxLen = getPerimeterLength();
		dist = dist % maxLen;
		if (dist < 0)
			dist += maxLen;

		while (true) {
			int next = (curr + direction + n) % n;
			double segLen = pts.get(curr).distance(pts.get(next));
			if (currentDist + segLen >= dist) {
				// Point is on this segment
				double t = (dist - currentDist) / segLen;
				return new Point2D.Double(
						pts.get(curr).getX() + t * (pts.get(next).getX() - pts.get(curr).getX()),
						pts.get(curr).getY() + t * (pts.get(next).getY() - pts.get(curr).getY()));
			}
			currentDist += segLen;
			curr = next;
			if (curr == startIdx)
				break; // Wrapped around
		}
		return pts.get(startIdx);
	}

	public void clearZonePoints() {
		zonePoints.clear();
		listener.stateChanged(new ChangeEvent(this));
	}
}
