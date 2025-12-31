package de.in.autoMower.sim;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JOptionPane;

/**
 * Common base for all AutoMower models.
 */
public abstract class AbstractAutoMowerModel implements Serializable {

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
        MOWING("MOWING"), SEEKING_BORDER("SEEKING"), FOLLOWING_BORDER("FOLLOWING"), CHARGING("CHARGING"),
        EDGE_CUTTING("EDGE-CUT"),
        STOPPED("STOPPED"), TRANSITIONING_TO_ZONE("TRANSIT");

        String description;

        State(String description) {
            this.description = description;
        }
    }

    State currentState = State.STOPPED;

    public double simulatedRuntimeSeconds = 0;

    Point2D currentPosition = null;
    protected Point2D segmentStart = null;
    protected Point2D transitionTargetPoint = null;
    protected Line2D currentLine = null;

    // References
    MultiLine2D border;
    protected GroundModel groundModel;
    protected MultiLine2D line = new MultiLine2D(Color.RED); // Trace

    // Helper
    Random random = new Random();
    List<Line2D> collisionLines = new ArrayList<>();
    private long lastUIUpdate = 0;
    protected int currentBorderIndex = 0;
    protected double transitionDistanceCm = 0;
    private int edgeCuttingPointsLeft = -1;
    private int edgeCuttingDirection = 1;

    // --- Model Info ---
    public abstract String getModelName();

    public abstract int getModelVersion();

    public abstract AbstractAutoMowerModel createNewInstance();

    @Override
    public String toString() {
        return getModelName();
    }

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

    public Point2D getTransitionTargetPoint() {
        return transitionTargetPoint;
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

        initCoverage();

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
            showInfoMessage(
                    "Starting edge cutting at vertex " + currentBorderIndex + " at " + currentPosition + " direction: "
                            + edgeCuttingDirection);
        }

        calculateNextSegment();
        new Thread(this::runMower).start();
    }

    private double chargingStateTime = 0;

    public void runMower() {
        long lastTime = System.currentTimeMillis();

        try {
            while (!isStopped()) {
                long now = System.currentTimeMillis();
                double dtReal = (now - lastTime) / 1000d;
                lastTime = now;
                double dtSim = dtReal * timeScale;
                simulatedRuntimeSeconds += dtSim;

                Point2D prevPos = new Point2D.Double(currentPosition.getX(), currentPosition.getY());

                if (currentState == State.CHARGING) {
                    // Fast Charging: Wait 3 seconds simulation time
                    chargingStateTime += dtSim;
                    if (chargingStateTime >= 3.0) {
                        currentBatteryWh = batteryCapacityWh;
                        currentState = State.MOWING;
                        chargingStateTime = 0;
                        calculateNextSegment();
                    }
                } else {
                    double moveDist = speedInCmPerSec * dtSim;
                    double pixelDist = moveDist / groundModel.getCalibration();
                    currentBatteryWh -= moveDist * energyConsumptionWhPerCm;

                    if (currentState == State.TRANSITIONING_TO_ZONE) {
                        transitionDistanceCm += moveDist;
                    }

                    if (currentState == State.MOWING && currentBatteryWh < batteryCapacityWh * 0.10) {
                        currentState = State.SEEKING_BORDER;
                        calculateNextSegment();
                    }

                    if (currentBatteryWh <= 0) {
                        currentBatteryWh = 0;
                        // Teleport to dock
                        Point2D dock = groundModel.getChargingStation();
                        if (dock != null) {
                            currentPosition.setLocation(dock);
                            currentState = State.CHARGING;
                            chargingStateTime = 0; // Reset timer
                            showInfoMessage("Battery Empty! Teleported to dock.");
                            // Prevent further movement in this loop iteration (which would overwrite
                            // teleport)
                            continue;
                        }
                        currentState = State.STOPPED;
                        showInfoMessage("Battery Empty! No Dock found.");
                        break;
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
                        updateCoverage(currentPosition);
                    }
                    onMove(moveDist, prevPos, currentPosition);
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
            currentState = State.STOPPED;
        }
    }

    public void calculateNextSegment() {
        if (isStopped() || groundModel == null)
            return;

        if (currentState == State.CHARGING) {
            currentLine = new Line2D.Double(currentPosition, currentPosition);
            return;
        }

        if (currentState == State.SEEKING_BORDER) {
            Point2D target = findNearestBorderPoint(currentPosition);
            if (target == null) {
                currentState = State.STOPPED;
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
                currentState = State.STOPPED;
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
                            currentBorderIndex = (currentBorderIndex - 1 + pts.size()) % pts.size();
                        }
                    }
                }
            }

            currentLine = new Line2D.Double(currentPosition, next);
            segmentStart = new Point2D.Double(currentPosition.getX(), currentPosition.getY());
            return;
        }

        if (currentState == State.TRANSITIONING_TO_ZONE) {
            if (transitionTargetPoint == null || currentPosition.distance(transitionTargetPoint) < 5.0) {
                currentState = State.MOWING;
                transitionTargetPoint = null;
                calculateNextSegment();
                return;
            }

            // Follow border logic
            List<Point2D> pts = border.getPoints();
            currentBorderIndex = (currentBorderIndex + 1) % pts.size();
            Point2D next = pts.get(currentBorderIndex);

            // Check if transitionTargetPoint is on this segment
            double dx = next.getX() - currentPosition.getX();
            double dy = next.getY() - currentPosition.getY();
            double len2 = dx * dx + dy * dy;
            if (len2 > 0.001) {
                double apx = transitionTargetPoint.getX() - currentPosition.getX();
                double apy = transitionTargetPoint.getY() - currentPosition.getY();
                double t = (apx * dx + apy * dy) / len2;

                if (t > 0 && t < 1) {
                    Point2D proj = new Point2D.Double(currentPosition.getX() + t * dx, currentPosition.getY() + t * dy);
                    if (proj.distance(transitionTargetPoint) < 5.0) {
                        next = proj;
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
                double ang = random.nextDouble() * 2 * Math.PI;
                dirEnd = new Point2D.Double(currentPosition.getX() + Math.cos(ang),
                        currentPosition.getY() + Math.sin(ang));
            } else if (touching.size() == 1) {
                incrementCollisionCount(currentPosition);
                Line2D wall = touching.get(0);
                dirEnd = calculateBounceDirection(wall, currentPosition, currentLine.getP1());

            } else {
                // Vertex/Corner - Robust Systematic Search
                boolean found = false;
                double[] testDists = { 0.2, 0.5, 0.05, 1.0 };
                double[] testSteps = { 5.0, 2.0, 1.0 };

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

                // Emergency recovery: Nudge away from nearest touching line
                if (!found && !touching.isEmpty()) {
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
                    onCollision(currentPosition);
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
                        Point2D mid = new Point2D.Double((currentPosition.getX() + hit.getX()) / 2.0,
                                (currentPosition.getY() + hit.getY()) / 2.0);
                        if (groundModel.isInside(mid, NAV_EPS)) {
                            incrementCollisionCount(hit);
                            currentLine = new Line2D.Double(currentPosition, hit);
                            segmentStart = new Point2D.Double(currentPosition.getX(), currentPosition.getY());
                            return;
                        }
                    }
                }
                double ang = Math.atan2(dirEnd.getY() - currentPosition.getY(), dirEnd.getX() - currentPosition.getX());
                ang += (random.nextDouble() - 0.5) * (0.5 + i * 0.05);
                dirEnd = new Point2D.Double(currentPosition.getX() + Math.cos(ang),
                        currentPosition.getY() + Math.sin(ang));
            }

            navigationErrorCount++;
            System.err.println("Navigation Error: Stuck at " + currentPosition);

            Point2D dock = groundModel.getChargingStation();
            if (dock != null) {
                currentPosition.setLocation(dock);
                currentState = State.CHARGING;
                chargingStateTime = 0;
                currentLine = new Line2D.Double(currentPosition, currentPosition);
                showInfoMessage("Recovered from stuck: Teleported to dock.");
                return;
            }
            showErrorMessage("Navigation Error: Stuck at " + currentPosition + " (No Dock found)");
            currentLine = new Line2D.Double(currentPosition, currentPosition);
            currentState = State.STOPPED;
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

    protected int getNearestBorderIndex(Point2D p) {
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
        State oldState = currentState;
        incrementCollisionCount(hit);
        currentLine = new Line2D.Double(currentPosition, hit);
        if (currentState == oldState) {
            currentState = State.MOWING;
        }
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
        currentState = State.STOPPED;
    }

    public boolean isStopped() {
        return currentState == State.STOPPED;
    }

    public boolean isCharging() {
        return currentState == State.CHARGING;
    }

    public void cancel() {
        currentState = State.STOPPED;
    }

    public boolean isReturningToDock() {
        return currentState == State.SEEKING_BORDER || currentState == State.FOLLOWING_BORDER;
    }

    public void resume() {
        currentState = State.MOWING;
        new Thread(this::runMower).start();
    }

    public Point2D calculateBounceDirection(Line2D wall, Point2D hitPoint, Point2D prevPoint) {
        double wx = wall.getX2() - wall.getX1();
        double wy = wall.getY2() - wall.getY1();
        double len = Math.sqrt(wx * wx + wy * wy);
        if (len < 0.0001)
            return new Point2D.Double(hitPoint.getX(), hitPoint.getY());
        wx /= len;
        wy /= len;
        double nx = -wy;
        double ny = wx;
        double ix = prevPoint.getX() - hitPoint.getX();
        double iy = prevPoint.getY() - hitPoint.getY();
        if (nx * ix + ny * iy < 0) {
            nx = -nx;
            ny = -ny;
        }
        double angleDeg = random.nextDouble() * 90.0;
        double angleRad = Math.toRadians(angleDeg);
        if (random.nextBoolean()) {
            angleRad = -angleRad;
        }
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);
        double rx = nx * cosA - ny * sinA;
        double ry = nx * sinA + ny * cosA;
        return new Point2D.Double(hitPoint.getX() + rx * 100.0, hitPoint.getY() + ry * 100.0);
    }

    private int navigationErrorCount = 0;

    public int getNavigationErrorCount() {
        return navigationErrorCount;
    }

    protected int collisionCount = 0;

    public int getCollisionCount() {
        return collisionCount;
    }

    protected void incrementCollisionCount(Point2D pos) {
        collisionCount++;
        onCollision(pos);
    }

    public double getTransitionDistanceMeters() {
        return transitionDistanceCm / 100.0;
    }

    protected void onCollision(Point2D pos) {
    }

    protected void onMove(double distCm, Point2D p1, Point2D p2) {
    }

    public void drawStats(Graphics g, Point2D pos) {
        int x = (int) pos.getX();
        int y = (int) pos.getY();
        int width = 100;
        int height = 15;

        double batteryPercent = currentBatteryWh / batteryCapacityWh;

        g.setColor(java.awt.Color.GRAY);
        g.drawRect(x, y, width, height);
        g.drawRect(x + width, y + height / 4, 4, height / 2);

        if (batteryPercent > 0.5)
            g.setColor(java.awt.Color.GREEN);
        else if (batteryPercent > 0.15)
            g.setColor(java.awt.Color.ORANGE);
        else
            g.setColor(java.awt.Color.RED);

        g.fillRect(x + 1, y + 1, (int) ((width - 1) * batteryPercent), height - 1);

        g.setColor(java.awt.Color.WHITE);
        g.drawString(String.format("%s (%.0f%%)", currentState.description, batteryPercent * 100), x, y + height + 15);

        int hours = (int) (simulatedRuntimeSeconds / 3600);
        int minutes = (int) ((simulatedRuntimeSeconds % 3600) / 60);
        int seconds = (int) (simulatedRuntimeSeconds % 60);
        String runtimeStr = String.format("Runtime: %02d:%02d:%02d", hours, minutes, seconds);
        g.drawString(runtimeStr, x, y + height + 30);

        String coverageStr = String.format("Coverage: %.1f%%", getCoveragePercentage() * 100.0);
        g.drawString(coverageStr, x, y + height + 45);

        g.drawString("Collisions: " + getCollisionCount(), x, y + height + 60);

        int errs = getNavigationErrorCount();
        if (errs > 0) {
            g.setColor(java.awt.Color.RED);
        }
        g.drawString("Nav Errors: " + errs, x, y + height + 75);
    }

    private byte[][] coverageGrid;
    private int gridMinX, gridMinY;
    private int gridWidth, gridHeight;
    private long totalMowablePixels = 0;
    private long mowedPixels = 0;
    private boolean coverageInitialized = false;

    public void initCoverage() {
        if (groundModel == null || groundModel.getBorder() == null)
            return;

        java.awt.Rectangle bounds = groundModel.getBorder().getBounds();
        gridMinX = bounds.x;
        gridMinY = bounds.y;
        gridWidth = bounds.width + 1;
        gridHeight = bounds.height + 1;

        coverageGrid = new byte[gridWidth][gridHeight];
        totalMowablePixels = 0;
        mowedPixels = 0;

        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Point2D p = new Point2D.Double(gridMinX + x, gridMinY + y);
                if (groundModel.isInside(p)) {
                    coverageGrid[x][y] = 1;
                    totalMowablePixels++;
                } else {
                    coverageGrid[x][y] = 0;
                }
            }
        }
        coverageInitialized = true;
    }

    protected void updateCoverage(Point2D pos) {
        if (!coverageInitialized || coverageGrid == null)
            return;

        double pixelRadius = (mowingWidthInCm / 2) / groundModel.getCalibration();
        int r = (int) Math.ceil(pixelRadius);

        int cx = (int) pos.getX() - gridMinX;
        int cy = (int) pos.getY() - gridMinY;

        int minX = Math.max(0, cx - r);
        int maxX = Math.min(gridWidth - 1, cx + r);
        int minY = Math.max(0, cy - r);
        int maxY = Math.min(gridHeight - 1, cy + r);

        double rSq = pixelRadius * pixelRadius;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (coverageGrid[x][y] == 1) {
                    double dx = x - cx;
                    double dy = y - cy;
                    if (dx * dx + dy * dy <= rSq) {
                        coverageGrid[x][y] = 2;
                        mowedPixels++;
                    }
                }
            }
        }
    }

    public double getCoveragePercentage() {
        if (totalMowablePixels == 0)
            return 0;
        return (double) mowedPixels / totalMowablePixels;
    }

}
