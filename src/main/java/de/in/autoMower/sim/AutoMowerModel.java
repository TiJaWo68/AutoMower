package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

/**
 * Simplified AutoMowerModel based on specific geometric rules.
 */
public class AutoMowerModel implements Serializable {

	private static final long serialVersionUID = 1L;

	// --- Simulation Constants & Config ---
	double speedInCmPerSec = 1000 / 36d;
	double mowingWidthInCm = 14;
	double timeScale = 1.0;

	// Battery
	double batteryCapacityWh = 50.0;
	double currentBatteryWh = 50.0;
	double energyConsumptionWhPerCm = 0.00045;
	double chargeRateWhPerSec = 0.5;

	// --- State ---
	enum State {
		MOWING,
		SEEKING_BORDER,
		FOLLOWING_BORDER,
		CHARGING,
		EDGE_CUTTING,
		STOPPED
	}

	State currentState = State.MOWING;

	public boolean isCharging = false;
	public boolean isReturningToDock = false;
	public boolean stopped = false;
	public Double cmProPixel = 1.0;
	public double simulatedRuntimeSeconds = 0;

	Point2D currentPosition = null;
	protected Point2D segmentStart = null;
	protected Line2D currentLine = null;

	// References
	MultiLine2D border;
	protected GroundModel groundModel;
	protected MultiLine2D line = new MultiLine2D(Color.RED); // Trace

	// Helper
	Random random = new Random();
	List<Line2D> collisionLines = new ArrayList<>();
	private long lastUIUpdate = 0;
	private int currentBorderIndex = -1;
	private int edgeCuttingPointsLeft = -1;
	private int edgeCuttingDirection = 1;

	// --- Getters/Setters ---
	public double getTimeScale() {
		return timeScale;
	}

	public void setTimeScale(double t) {
		this.timeScale = t;
	}

	public double getSpeedInCmPerSec() {
		return speedInCmPerSec;
	}

	public void setSpeedInCmPerSec(double s) {
		this.speedInCmPerSec = s;
	}

	public Point2D getCurrentPosition() {
		return currentPosition;
	}

	public Point2D getSegmentStart() {
		return segmentStart;
	}

	public Line2D getCurrentLine() {
		return currentLine;
	}

	public void setCurrentPosition(Point2D pos) {
		if (pos == null)
			this.currentPosition = null;
		else
			this.currentPosition = new Point2D.Double(pos.getX(), pos.getY());
	}

	public double getMowingWidthInCm() {
		return mowingWidthInCm;
	}

	public void setMowingWidthInCm(double mw) {
		this.mowingWidthInCm = mw;
	}

	// --- Core Logic ---

	public void start(MultiLine2D visualTrace, GroundModel gm) {
		startInternal(visualTrace, gm, State.MOWING);
	}

	public void startEdgeCutting(MultiLine2D visualTrace, GroundModel gm) {
		startInternal(visualTrace, gm, State.EDGE_CUTTING);
	}

	private void startInternal(MultiLine2D visualTrace, GroundModel gm, State startState) {
		this.line = visualTrace;
		this.groundModel = gm;
		this.border = gm.getBorder();
		this.cmProPixel = gm.getCalibration();

		Point2D station = gm.getChargingStation();
		if (station != null) {
			this.currentPosition = new Point2D.Double(station.getX(), station.getY());
		} else {
			station = border.getPoint(0);
			if (this.currentPosition == null || !gm.isInside(this.currentPosition)) {
				this.currentPosition = new Point2D.Double(station.getX(), station.getY());
			}
		}

		this.line.addPoint(new Point2D.Double(currentPosition.getX(), currentPosition.getY()));

		if (!gm.isInside(this.currentPosition)) {
			boolean atDock = gm.getChargingStation() != null && currentPosition.distance(gm.getChargingStation()) < 2.0;
			if (!atDock) {
				showErrorMessage("Safety Error: Robot spawned outside!");
				return;
			}
		}

		this.currentBatteryWh = this.batteryCapacityWh;
		this.currentState = startState;
		this.stopped = false;

		if (currentState == State.EDGE_CUTTING) {
			this.currentBorderIndex = getNearestBorderIndex(currentPosition);
			Point2D startPt = border.getPoint(currentBorderIndex);
			this.currentPosition = new Point2D.Double(startPt.getX(), startPt.getY());
			this.edgeCuttingPointsLeft = border.getNumberOfPoints();

			// Determine "left" direction from dock's perspective
			Point2D dock = groundModel.getChargingStation();
			if (dock != null) {
				Point2D v = border.getPoint(currentBorderIndex);
				int nextIdx = (currentBorderIndex + 1) % border.getNumberOfPoints();
				Point2D vNext = border.getPoint(nextIdx);

				double dx_f = v.getX() - dock.getX();
				double dy_f = v.getY() - dock.getY();
				double dx_v = vNext.getX() - v.getX();
				double dy_v = vNext.getY() - v.getY();

				double det = dx_f * dy_v - dy_f * dx_v;
				this.edgeCuttingDirection = (det > 0) ? -1 : 1;
			} else {
				this.edgeCuttingDirection = 1;
			}
			showInfoMessage("Starting edge cutting at vertex " + currentBorderIndex + " at " + currentPosition
					+ " direction: " + edgeCuttingDirection);
		}

		calculateNextSegment();
		new Thread(this::runMower).start();
	}

	private double chargingStateTime = 0;

	public void runMower() {
		stopped = false;
		long lastTime = System.currentTimeMillis();

		try {
			while (!stopped) {
				long now = System.currentTimeMillis();
				double dtReal = (now - lastTime) / 1000d;
				lastTime = now;
				double dtSim = dtReal * timeScale;
				simulatedRuntimeSeconds += dtSim;

				if (currentState == State.CHARGING) {
					// Fast Charging: Wait 3 seconds simulation time
					chargingStateTime += dtSim;
					if (chargingStateTime >= 3.0) {
						currentBatteryWh = batteryCapacityWh;
						isCharging = false;
						currentState = State.MOWING;
						chargingStateTime = 0;
						calculateNextSegment();
					}
				} else {
					double moveDist = speedInCmPerSec * dtSim;
					double pixelDist = moveDist / cmProPixel;
					currentBatteryWh -= moveDist * energyConsumptionWhPerCm;

					if (currentState == State.MOWING && currentBatteryWh < batteryCapacityWh * 0.10) {
						currentState = State.SEEKING_BORDER;
						isReturningToDock = true;
						calculateNextSegment();
					}

					if (currentBatteryWh <= 0) {
						currentBatteryWh = 0;
						// Teleport to dock
						Point2D dock = groundModel.getChargingStation();
						if (dock != null) {
							currentPosition.setLocation(dock);
							currentState = State.CHARGING;
							isCharging = true;
							isReturningToDock = false;
							chargingStateTime = 0; // Reset timer
							showInfoMessage("Battery Empty! Teleported to dock.");
							// Prevent further movement in this loop iteration (which would overwrite
							// teleport)
							continue;
						} else {
							stopped = true;
							showInfoMessage("Battery Empty! No Dock found.");
							break;
						}
					}

					if (currentLine == null) {
						calculateNextSegment();
					}

					double distToTarget = currentPosition.distance(currentLine.getP2());

					if (pixelDist >= distToTarget) {
						currentPosition.setLocation(currentLine.getP2());
						// Segment completed - update trace
						line.addPoint(new Point2D.Double(currentPosition.getX(), currentPosition.getY()));
						if (currentState == State.EDGE_CUTTING) {
							edgeCuttingPointsLeft--;
						}
						calculateNextSegment();
					} else if (pixelDist > 0) {
						Point2D newPos = GeomUtil.getColinearPointWithLength(currentPosition, currentLine.getP2(),
								pixelDist);
						currentPosition.setLocation(newPos);
						currentLine.setLine(currentPosition, currentLine.getP2());
					}
				}

				if (now - lastUIUpdate > 50) {
					updateUI();
					lastUIUpdate = now;
				}
				Thread.sleep(10);
			}
		} catch (Exception e) {
			e.printStackTrace();
			showErrorMessage("Crash: " + e.getMessage());
			stopped = true;
		}
	}

	public void calculateNextSegment() {
		if (currentState == State.STOPPED || groundModel == null)
			return;

		if (currentState == State.CHARGING) {
			currentLine = new Line2D.Double(currentPosition, currentPosition);
			return;
		}

		if (currentState == State.SEEKING_BORDER) {
			Point2D target = findNearestBorderPoint(currentPosition);
			if (target == null) {
				stopped = true;
				return;
			}
			Line2D path = new Line2D.Double(currentPosition, target);
			Line2D colPath = getFirstCollision(path);
			if (colPath != null) {
				handleCollision(colPath);
				return;
			}
			currentLine = path;
			segmentStart = new Point2D.Double(currentPosition.getX(), currentPosition.getY());
			if (currentPosition.distance(target) < 1.0) {
				currentState = State.FOLLOWING_BORDER;
				currentBorderIndex = getNearestBorderIndex(currentPosition);
				calculateNextSegment();
			}
			return;
		}

		if (currentState == State.EDGE_CUTTING) {
			if (edgeCuttingPointsLeft <= 0) {
				currentState = State.CHARGING;
				isCharging = true;
				isReturningToDock = false;
				stopped = true;
				currentLine = new Line2D.Double(currentPosition, currentPosition);
				showInfoMessage("Edge cutting finished at " + currentPosition);
				return;
			}

			List<Point2D> pts = border.getPoints();
			currentBorderIndex = (currentBorderIndex + edgeCuttingDirection + pts.size()) % pts.size();
			Point2D next = pts.get(currentBorderIndex);
			currentLine = new Line2D.Double(currentPosition, next);
			segmentStart = new Point2D.Double(currentPosition.getX(), currentPosition.getY());
			return;
		}

		if (currentState == State.FOLLOWING_BORDER) {
			Point2D dock = groundModel.getChargingStation();
			if (dock != null && currentPosition.distance(dock) < 12.0) {
				currentState = State.CHARGING;
				isCharging = true;
				isReturningToDock = false;
				currentLine = new Line2D.Double(currentPosition, currentPosition);
				return;
			}

			List<Point2D> pts = border.getPoints();
			currentBorderIndex = (currentBorderIndex + 1) % pts.size();
			Point2D next = pts.get(currentBorderIndex);

			// Check if we pass the dock on this segment
			if (dock != null) {
				double dx = next.getX() - currentPosition.getX();
				double dy = next.getY() - currentPosition.getY();
				double len2 = dx * dx + dy * dy;
				if (len2 > 0.001) {
					double apx = dock.getX() - currentPosition.getX();
					double apy = dock.getY() - currentPosition.getY();
					double t = (apx * dx + apy * dy) / len2;

					if (t > 0 && t < 1) {
						Point2D proj = new Point2D.Double(currentPosition.getX() + t * dx,
								currentPosition.getY() + t * dy);
						// If closest point is close enough to dock, stop there
						if (proj.distance(dock) < 12.0) { // Increased tolerance
							next = proj;
							// Decrement index so we resume from correct segment later?
							// Actually we will transition to charging next step, so index matters less?
							// But we should correct index if we stop mid-segment.
							// For now, let's just stop at proj.
							// To ensure we don't skip the next point logically if we resume,
							// we probably want to stay on currentBorderIndex - 1 (the one we just left)
							// but "currentBorderIndex" tracks "next target".
							// So we should revert the increment of currentBorderIndex?
							currentBorderIndex = (currentBorderIndex - 1 + pts.size()) % pts.size();
						}
					}
				}
			}

			currentLine = new Line2D.Double(currentPosition, next);
			segmentStart = new Point2D.Double(currentPosition.getX(), currentPosition.getY());
			return;
		}

		if (currentState == State.MOWING) {
			if (currentLine != null && !currentPosition.equals(currentLine.getP2())
					&& currentPosition.distance(currentLine.getP2()) > 0.001) {
				return; // Continuing
			}

			double NAV_EPS = 0.05;
			Point2D dirEnd = null;
			List<Line2D> touching = findTouchingLines(currentPosition);

			if (currentLine == null || currentPosition.distance(currentLine.getP1()) < 0.05 || touching.isEmpty()) {
				// No valid previous movement or not touching walls - pick a random direction
				// and let the scan handle it
				double ang = random.nextDouble() * 2 * Math.PI;
				dirEnd = new Point2D.Double(currentPosition.getX() + Math.cos(ang),
						currentPosition.getY() + Math.sin(ang));
			} else if (touching.size() == 1) {
				Line2D wall = touching.get(0);
				Point2D p1 = currentLine.getP1();
				double angIn = GeomUtil.getAngleRad(p1, currentPosition);
				double angWall = GeomUtil.getAngleRad(wall.getP1(), wall.getP2());
				double angOut = 2 * angWall - angIn;
				angOut += (random.nextDouble() - 0.5) * 0.1;
				dirEnd = new Point2D.Double(currentPosition.getX() + Math.cos(angOut) * 100,
						currentPosition.getY() + Math.sin(angOut) * 100);
				if (!groundModel.isInside(GeomUtil.getColinearPointWithLength(currentPosition, dirEnd, 1.0), NAV_EPS)) {
					angOut += Math.PI;
					dirEnd = new Point2D.Double(currentPosition.getX() + Math.cos(angOut) * 100,
							currentPosition.getY() + Math.sin(angOut) * 100);
				}
			} else {
				// Vertex/Corner - Robust Systematic Search
				boolean found = false;
				double[] testDists = { 0.2, 0.5, 0.05, 1.0 }; // Try different distances
				double[] testSteps = { 5.0, 2.0, 1.0 }; // Finer and finer

				outer: for (double dist : testDists) {
					for (double step : testSteps) {
						for (double d = 0; d < 360; d += step) {
							double ang = Math.toRadians(d);
							Point2D test = new Point2D.Double(currentPosition.getX() + Math.cos(ang),
									currentPosition.getY() + Math.sin(ang));
							if (groundModel.isInside(GeomUtil.getColinearPointWithLength(currentPosition, test, dist),
									NAV_EPS)) {
								dirEnd = test;
								found = true;
								break outer;
							}
						}
					}
				}

				if (!found) {
					// Emergency recovery: Nudge away from nearest touching line
					if (!touching.isEmpty()) {
						Line2D wall = touching.get(0);
						Point2D p1 = wall.getP1();
						Point2D p2 = wall.getP2();
						double wallAng = GeomUtil.getAngleRad(p1, p2);
						double perpAng = wallAng + Math.PI / 2.0;
						Point2D test = new Point2D.Double(currentPosition.getX() + Math.cos(perpAng),
								currentPosition.getY() + Math.sin(perpAng));
						if (!groundModel.isInside(GeomUtil.getColinearPointWithLength(currentPosition, test, 0.1),
								NAV_EPS)) {
							perpAng -= Math.PI; // Try other side
							test = new Point2D.Double(currentPosition.getX() + Math.cos(perpAng),
									currentPosition.getY() + Math.sin(perpAng));
						}
						dirEnd = test;
					}
				}
			}

			if (dirEnd == null) {
				double ang = random.nextDouble() * 2 * Math.PI;
				dirEnd = new Point2D.Double(currentPosition.getX() + Math.cos(ang),
						currentPosition.getY() + Math.sin(ang));
			}

			// Raycast with Nudge
			double rayLen = 10000;
			for (int i = 0; i < 200; i++) {
				Point2D testPoint = GeomUtil.getColinearPointWithLength(currentPosition, dirEnd, 1.0);
				if (!groundModel.isInside(testPoint, NAV_EPS)) {
					// This direction goes outside, try another one
					double ang = Math.atan2(dirEnd.getY() - currentPosition.getY(),
							dirEnd.getX() - currentPosition.getX());
					ang += (random.nextDouble() - 0.5) * (0.5 + i * 0.05);
					dirEnd = new Point2D.Double(currentPosition.getX() + Math.cos(ang),
							currentPosition.getY() + Math.sin(ang));
					continue;
				}

				Point2D startPoint = GeomUtil.getColinearPointWithLength(currentPosition, dirEnd, 0.1);
				Point2D far = GeomUtil.getColinearPointWithLength(startPoint, dirEnd, rayLen);
				Line2D ray = new Line2D.Double(startPoint, far);
				Line2D hitWall = groundModel.getCollidingLine(ray);
				if (hitWall != null) {
					Point2D hit = GeomUtil.getIntersectPoint(hitWall, ray);
					if (hit != null && currentPosition.distance(hit) > 0.05) {
						// Verify that the segment is mostly inside (check midpoint)
						Point2D mid = new Point2D.Double((currentPosition.getX() + hit.getX()) / 2.0,
								(currentPosition.getY() + hit.getY()) / 2.0);
						if (groundModel.isInside(mid, NAV_EPS)) {
							currentLine = new Line2D.Double(currentPosition, hit);
							segmentStart = new Point2D.Double(currentPosition.getX(), currentPosition.getY());
							return;
						}
					}
				}
				double ang = Math.atan2(dirEnd.getY() - currentPosition.getY(),
						dirEnd.getX() - currentPosition.getX());
				ang += (random.nextDouble() - 0.5) * (0.5 + i * 0.05);
				dirEnd = new Point2D.Double(currentPosition.getX() + Math.cos(ang),
						currentPosition.getY() + Math.sin(ang));
			}

			showErrorMessage("Navigation Error: Stuck at " + currentPosition);
			currentLine = new Line2D.Double(currentPosition, currentPosition);
			stopped = true;
		}
	}

	private List<Line2D> findTouchingLines(Point2D p) {
		List<Line2D> res = new ArrayList<>();
		double eps = 1.0;
		if (border != null)
			checkMultiLine(border, p, res, eps);
		if (groundModel != null && groundModel.getObstacles() != null) {
			for (MultiLine2D obs : groundModel.getObstacles()) {
				checkMultiLine(obs, p, res, eps);
			}
		}
		return res;
	}

	private void checkMultiLine(MultiLine2D ml, Point2D p, List<Line2D> res, double eps) {
		for (int i = 0; i < ml.getNumberOfPoints(); i++) {
			Line2D l = ml.getLine(i);
			if (l != null && l.ptSegDist(p) < eps)
				res.add(l);
		}
	}

	private Point2D findNearestBorderPoint(Point2D p) {
		if (border == null)
			return null;
		Point2D best = null;
		double minD = Double.MAX_VALUE;
		for (Point2D bp : border.getPoints()) {
			double d = p.distance(bp);
			if (d < minD) {
				minD = d;
				best = bp;
			}
		}
		return best;
	}

	private int getNearestBorderIndex(Point2D p) {
		if (border == null)
			return -1;
		int best = 0;
		double minD = Double.MAX_VALUE;
		List<Point2D> pts = border.getPoints();
		for (int i = 0; i < pts.size(); i++) {
			double d = p.distance(pts.get(i));
			if (d < minD) {
				minD = d;
				best = i;
			}
		}
		return best;
	}

	private Line2D getFirstCollision(Line2D trajectory) {
		return groundModel == null ? null : groundModel.getCollidingLine(trajectory);
	}

	private void handleCollision(Line2D wall) {
		Point2D hit = GeomUtil.getIntersectPoint(wall, currentLine);
		if (hit == null)
			hit = wall.getP1();
		currentLine = new Line2D.Double(currentPosition, hit);
		currentState = State.MOWING;
	}

	protected void updateUI() {
		EventQueue.invokeLater(() -> {
			if (App.getApp() != null && App.getApp().getPanel() != null)
				App.getApp().getPanel().repaint();
		});
	}

	protected void showErrorMessage(String message) {
		EventQueue.invokeLater(() -> {
			if (App.getApp() != null)
				JOptionPane.showMessageDialog(App.getApp(), message, "Error", JOptionPane.ERROR_MESSAGE);
		});
	}

	protected void showInfoMessage(String message) {
		System.out.println("Info: " + message);
	}

	public void stop() {
		stopped = true;
	}

	public void resume() {
		stopped = false;
		new Thread(this::runMower).start();
	}

	public void cancel() {
		stopped = true;
	}
}