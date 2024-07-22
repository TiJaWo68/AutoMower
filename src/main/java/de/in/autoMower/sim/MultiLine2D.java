/**
 * 
 */
package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Till
 */
public class MultiLine2D {

	public static final int ED = 4;

	protected List<Point2D> points = new LinkedList<>();
	protected Color color;
	protected boolean closed = false;
	protected Point2D highLightedPoint = null;
	protected Line2D highLightedLine = null;

	public MultiLine2D(Color color) {
		this.color = color;
	}

	public void addPoint(Point2D p) {
		points.add(p);
	}

	public Point2D getPoint(MouseEvent me, AffineTransform at) {
		Point2D tp = at.transform(new Point2D.Double(me.getX(), me.getY()), new Point2D.Double());
		return getPoint(tp);
	}

	public void draw(Graphics2D g, AffineTransform at) {
		g.setColor(color);
		Point2D p1 = null;
		Point2D first = null;
		for (Point2D p : points) {
			Point2D p2 = at.transform(p, new Point2D.Double());
			if (first == null)
				first = p2;
			g.drawOval((int) (p2.getX() - MultiLine2D.ED / 2), (int) (p2.getY() - MultiLine2D.ED / 2), ED, ED);
			if (p1 != null)
				g.drawLine((int) (p1.getX()), (int) (p1.getY()), (int) (p2.getX()), (int) (p2.getY()));
			p1 = p2;
		}
		if (closed && p1 != null) {
			g.drawLine((int) (p1.getX()), (int) (p1.getY()), (int) (first.getX()), (int) (first.getY()));
		}
	}

	public int getNumberOfPoints() {
		return points.size();
	}

	public void closePath() {
		closed = true;
	}

	public Line2D getLine2D(Point2D tp) {
		Point2D p1 = null;
		Point2D first = null;
		for (Point2D p2 : points) {
			if (first == null)
				first = p2;
			if (Math.abs(p2.getX() - tp.getX()) < ED && Math.abs(p2.getY() - tp.getY()) < ED)
				return null;
			if (p1 != null) {
				Line2D.Double line = new Line2D.Double(p1, p2);
				if (line.ptSegDist(tp) < ED)
					return line;
			}
			p1 = p2;
		}
		if (closed && p1 != null) {
			Line2D.Double line = new Line2D.Double(p1, first);
			if (line.ptSegDist(tp) < ED)
				return line;
		}
		return null;
	}

	public Point2D getPoint(Point2D tp) {
		for (Point2D p : points) {
			if (Math.abs(p.getX() - tp.getX()) < ED && Math.abs(p.getY() - tp.getY()) < ED) {
				return p;
			}
		}
		return null;
	}
}
