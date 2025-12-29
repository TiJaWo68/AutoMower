/**
 * 
 */
package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Till
 */
public class MultiLine2D implements Serializable {

	static final long serialVersionUID = 123456789015L;

	public static final int ED = 4;

	protected List<Point2D> points = Collections.synchronizedList(new LinkedList<>());
	protected Color color;
	protected boolean closed = false;
	protected Point2D highLightedPoint = null;
	protected Line2D highLightedLine = null;
	protected boolean drawPoints = true;
	protected boolean showIndices = false;
	protected int numberingStartIndex = 0;
	protected int numberingDirection = 1;

	public MultiLine2D() {
		this(Color.BLACK);
	}

	public MultiLine2D(Color color) {
		this.color = color;
	}

	public void setDrawPoints(boolean drawPoints) {
		this.drawPoints = drawPoints;
	}

	public void setShowIndices(boolean showIndices) {
		this.showIndices = showIndices;
	}

	public void setNumberingStartAndDirection(int start, int direction) {
		this.numberingStartIndex = start;
		this.numberingDirection = direction;
	}

	public List<Point2D> getPoints() {
		return points;
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

		List<Point2D> safePoints;
		synchronized (points) {
			safePoints = new java.util.ArrayList<>(points);
		}

		if (showIndices) {
			g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 14f));
		}

		int index = 0;
		for (Point2D p : safePoints) {
			Point2D p2 = at.transform(p, new Point2D.Double());
			if (first == null)
				first = p2;
			if (drawPoints) {
				g.drawOval((int) (p2.getX() - MultiLine2D.ED / 2), (int) (p2.getY() - MultiLine2D.ED / 2), ED, ED);
			}
			if (showIndices) {
				int size = safePoints.size();
				int num = ((index - numberingStartIndex) * numberingDirection + size) % size + 1;
				String s = String.valueOf(num);
				int tx = (int) p2.getX() + 5;
				int ty = (int) p2.getY() - 5;
				// Draw shadow
				g.setColor(Color.BLACK);
				g.drawString(s, tx + 1, ty + 1);
				// Draw label
				g.setColor(Color.WHITE);
				g.drawString(s, tx, ty);
				g.setColor(color);
			}
			if (p1 != null)
				g.drawLine((int) (p1.getX()), (int) (p1.getY()), (int) (p2.getX()), (int) (p2.getY()));
			p1 = p2;
			index++;
		}

		if (closed && p1 != null)
			g.drawLine((int) (p1.getX()), (int) (p1.getY()), (int) (first.getX()), (int) (first.getY()));
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
		synchronized (points) {
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
		}
		if (closed && p1 != null) {
			Line2D.Double line = new Line2D.Double(p1, first);
			if (line.ptSegDist(tp) < ED)
				return line;
		}
		return null;
	}

	public Point2D getPoint(Point2D tp) {
		for (Point2D p : points)
			if (Math.abs(p.getX() - tp.getX()) < ED && Math.abs(p.getY() - tp.getY()) < ED)
				return p;
		return null;
	}

	public Line2D getLine(int number) {
		if (points.size() > number + 1)
			return new Line2D.Double(points.get(number), points.get(number + 1));
		if (points.size() == number + 1)
			return new Line2D.Double(points.get(number), points.get(0));
		return null;
	}

	public Point2D getPoint(int number) {
		if (points.size() > number)
			return points.get(number);
		return null;
	}

	public boolean contains(Point2D p) {
		if (!closed || points.size() < 3)
			return false;
		Path2D path = new Path2D.Double();
		synchronized (points) {
			path.moveTo(points.get(0).getX(), points.get(0).getY());
			for (int i = 1; i < points.size(); i++) {
				path.lineTo(points.get(i).getX(), points.get(i).getY());
			}
			path.closePath();
		}
		return path.contains(p);
	}

	/**
	 * Returns the area of the polygon using the shoelace formula.
	 * 
	 * @return area in square pixels
	 */
	public double getArea() {
		if (!closed || points.size() < 3)
			return 0;
		double area = 0;
		synchronized (points) {
			int j = points.size() - 1;
			for (int i = 0; i < points.size(); i++) {
				area += (points.get(j).getX() + points.get(i).getX()) * (points.get(j).getY() - points.get(i).getY());
				j = i;
			}
		}
		return Math.abs(area / 2.0);
	}

	public double ptSegDist(Point2D p) {
		double minDist = Double.MAX_VALUE;
		Point2D p1 = null;
		Point2D first = null;
		synchronized (points) {
			if (points.isEmpty())
				return Double.MAX_VALUE;
			for (Point2D p2 : points) {
				if (first == null)
					first = p2;
				if (p1 != null) {
					minDist = Math.min(minDist, new Line2D.Double(p1, p2).ptSegDist(p));
				}
				p1 = p2;
			}
			if (closed && p1 != null && first != null) {
				minDist = Math.min(minDist, new Line2D.Double(p1, first).ptSegDist(p));
			}
		}
		return minDist;
	}

	public Line2D getCollidingLine(Line2D checkLine) {
		Point2D p1 = null;
		Point2D first = null;
		synchronized (points) {
			for (Point2D p2 : points) {
				if (first == null)
					first = p2;
				if (p1 != null) {
					Line2D.Double l = new Line2D.Double(p1, p2);
					if (l.intersectsLine(checkLine)) {
						return l;
					}
				}
				p1 = p2;
			}
			if (closed && p1 != null) {
				Line2D.Double l = new Line2D.Double(p1, first);
				if (l.intersectsLine(checkLine)) {
					return l;
				}
			}
		}
		return null;
	}
}