package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class GroundModel implements Serializable {

	static final long serialVersionUID = 123456789014L;

	public static final Color BORDER_COLOR = Color.ORANGE;
	public static final Color OBSTACLE_COLOR = Color.CYAN;
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

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
		if (listener != null)
			listener.stateChanged(new ChangeEvent(image));
	}

	public GroundModel() {
		try {
			image = ImageIO.read(new File("ground.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public MultiLine2D getBorder() {
		return border;
	}

	public void setBorder(MultiLine2D border) {
		this.border = border;
		listener.stateChanged(new ChangeEvent(this));
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
				if (intersectPoint != null) {
					double distance = p1.distance(intersectPoint);
					if (distance > 0)
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
				if (intersectPoint != null) {
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
}
