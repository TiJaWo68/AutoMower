package de.in.autoMower.sim;

import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Version 4 of the AutoMowerModel.
 * Tracks distance covered per zone instead of collision counts.
 * 1. A segment completely within Zone A is fully assigned to Zone A.
 * 2. A segment transitioning from Zone A to Zone B is divided 50/50.
 * 3. Uses smart transition logic based on distance deficits.
 */
public class AutoMowerModelV4 extends AbstractAutoMowerModel {

    private static final long serialVersionUID = 1L;
    private static final double CRITICAL_DEFICIT_THRESHOLD = 0.08; // 8% absolute deficit
    private static final double SURPLUS_LEAVE_THRESHOLD = 0.05; // 5% surplus triggers exit check
    private static final int MIN_DISTANCE_M_PER_STAY = 50; // Require 50m per stay
    private static final double OPPORTUNISTIC_DIST_RATIO = 0.25; // 25% of perimeter

    private final Map<Integer, Double> zoneDistanceStats = new TreeMap<>();
    private double lastTransitionTotalDistanceM = 0;

    public AutoMowerModelV4() {
    }

    public Map<Integer, Double> getZoneDistanceStats() {
        return zoneDistanceStats;
    }

    @Override
    protected void onMove(double distCm, Point2D p1, Point2D p2) {
        if (groundModel == null || distCm <= 0)
            return;

        int z1 = getZoneAt(p1);
        int z2 = getZoneAt(p2);

        if (z1 == z2) {
            zoneDistanceStats.merge(z1, distCm, Double::sum);
        } else {
            // Split 50/50
            zoneDistanceStats.merge(z1, distCm / 2.0, Double::sum);
            zoneDistanceStats.merge(z2, distCm / 2.0, Double::sum);
        }
    }

    private int getZoneAt(Point2D p) {
        if (groundModel == null)
            return 0;

        // Find nearest point on border to determine zone
        double dPerim = groundModel.getDistOnPerimeter(p);
        List<ZonePoint> zones = groundModel.getZonePoints();
        if (zones.isEmpty())
            return 0;

        int n = zones.size();
        for (int i = 0; i < n; i++) {
            double ziDist = groundModel.getDistOnPerimeter(zones.get(i).getPoint());
            if (i == 0) {
                if (dPerim <= ziDist)
                    return 0;
            } else {
                double prevZiDist = groundModel.getDistOnPerimeter(zones.get(i - 1).getPoint());
                if (dPerim > prevZiDist && dPerim <= ziDist)
                    return i;
            }
        }
        return 0;
    }

    @Override
    protected void onCollision(Point2D pos) {
        if (groundModel == null)
            return;

        List<ZonePoint> zones = groundModel.getZonePoints();
        if (zones.isEmpty())
            return;

        double totalDistCm = zoneDistanceStats.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalDistCm < 1000)
            return; // Wait for at least 10m total before balancing

        double totalDistM = totalDistCm / 100.0;
        int n = zones.size();
        int currentZone = getZoneAt(pos);

        // Check for imbalance
        int bestTargetZone = -1;
        double maxDeficitPct = 0;

        double currentZoneActualPct = zoneDistanceStats.getOrDefault(currentZone, 0.0) / totalDistCm;
        double currentZoneTargetPct = zones.get(currentZone).getPercentage() / 100.0;
        double currentZoneSurplusPct = currentZoneActualPct - currentZoneTargetPct;

        for (int i = 0; i < n; i++) {
            double actualPct = zoneDistanceStats.getOrDefault(i, 0.0) / totalDistCm;
            double targetPct = zones.get(i).getPercentage() / 100.0;
            double deficitPct = targetPct - actualPct;
            if (deficitPct > maxDeficitPct) {
                maxDeficitPct = deficitPct;
                bestTargetZone = i;
            }
        }

        boolean shouldTransition = false;
        if (bestTargetZone != -1 && bestTargetZone != currentZone) {
            double stayLengthM = totalDistM - lastTransitionTotalDistanceM;

            if (stayLengthM >= MIN_DISTANCE_M_PER_STAY && currentZoneSurplusPct > 0) {
                double perimeterLen = groundModel.getPerimeterLength();
                double dTarget = groundModel.getDistOnPerimeter(zones.get(bestTargetZone).getPoint());
                double dCurrent = groundModel.getDistOnPerimeter(pos);
                double distToTargetPerim = (dTarget - dCurrent + perimeterLen) % perimeterLen;
                boolean opportunistic = distToTargetPerim < (perimeterLen * OPPORTUNISTIC_DIST_RATIO);

                if (maxDeficitPct > CRITICAL_DEFICIT_THRESHOLD) {
                    shouldTransition = true;
                } else if (opportunistic && currentZoneSurplusPct > SURPLUS_LEAVE_THRESHOLD) {
                    shouldTransition = true;
                }
            }
        }

        if (shouldTransition) {
            lastTransitionTotalDistanceM = totalDistM;
            double perimeterLen = groundModel.getPerimeterLength();
            double dTarget = groundModel.getDistOnPerimeter(zones.get(bestTargetZone).getPoint());
            double dPrev = groundModel.getDistOnPerimeter(zones.get((bestTargetZone - 1 + n) % n).getPoint());

            double dMid;
            if (dTarget < dPrev) {
                dMid = (dPrev + dTarget + perimeterLen) / 2.0;
                if (dMid >= perimeterLen)
                    dMid -= perimeterLen;
            } else {
                dMid = (dPrev + dTarget) / 2.0;
            }

            transitionTargetPoint = groundModel.getPointAtDistOnPerimeter(dMid);
            if (transitionTargetPoint != null) {
                currentState = State.TRANSITIONING_TO_ZONE;
                double dColl = groundModel.getDistOnPerimeter(pos);
                currentPosition = groundModel.getPointAtDistOnPerimeter(dColl);
                currentBorderIndex = getNearestBorderIndex(currentPosition);

                // Add artificial distance to target zone?
                // User: "zählt eine Kollision für C" in V2 context.
                // For V4, we don't have collisions but distance.
                // We'll just start moving there.
            }
        }
    }

    @Override
    public void drawStats(Graphics g, Point2D pos) {
        super.drawStats(g, pos);

        int row = 0;
        int startX = (int) pos.getX();
        int startY = (int) pos.getY() + 110;

        if (groundModel == null)
            return;

        double totalDistCm = zoneDistanceStats.values().stream().mapToDouble(Double::doubleValue).sum();
        List<ZonePoint> zones = groundModel.getZonePoints();
        g.setColor(java.awt.Color.WHITE);

        for (int i = 0; i < zones.size(); i++) {
            double actualCm = zoneDistanceStats.getOrDefault(i, 0.0);
            double pct = totalDistCm > 0 ? (actualCm * 100.0 / totalDistCm) : 0.0;
            int targetPct = zones.get(i).getPercentage();
            String label = groundModel.getZoneLabel(i);
            g.drawString(String.format("Zone %s: %.1f m (%.1f%% / Target: %d%%)",
                    label, actualCm / 100.0, pct, targetPct),
                    startX, startY + (row * 15));
            row++;
        }
        g.drawString(String.format("Transition distance: %.1f m", getTransitionDistanceMeters()), startX,
                startY + (row * 15));
    }

    @Override
    public String getModelName() {
        return "Version 4 (Distance Based)";
    }

    @Override
    public int getModelVersion() {
        return 4;
    }

    @Override
    public AbstractAutoMowerModel createNewInstance() {
        return new AutoMowerModelV4();
    }
}
