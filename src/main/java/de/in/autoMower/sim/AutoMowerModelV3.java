package de.in.autoMower.sim;

import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Version 3 of the AutoMowerModel.
 * Optimized to keep target percentages while minimizing border driving.
 * Logic:
 * 1. Tracks collisions per zone.
 * 2. On EVERY collision, it checks if the current zone has a significant
 * surplus.
 * 3. If surplus is high AND another zone has a significant deficit, it
 * transitions.
 * 4. To minimize border driving, it prioritizes "opportunistic" transitions
 * when
 * the robot is already near a hungry zone, or when the deficit is critical.
 */
public class AutoMowerModelV3 extends AbstractAutoMowerModel {

    private static final long serialVersionUID = 1L;
    private static final double CRITICAL_DEFICIT_THRESHOLD = 0.08; // 8% absolute deficit
    private static final double SURPLUS_LEAVE_THRESHOLD = 0.05; // 5% surplus triggers exit check
    private static final int MIN_COLLISIONS_PER_STAY = 15;
    private static final double OPPORTUNISTIC_DIST_RATIO = 0.25; // 25% of perimeter

    private final Map<Integer, Integer> zoneCollisionStats = new TreeMap<>();
    private int lastTransitionCollisionCount = 0;

    public AutoMowerModelV3() {
    }

    public Map<Integer, Integer> getZoneCollisionStats() {
        return zoneCollisionStats;
    }

    @Override
    protected void onCollision(Point2D pos) {
        if (groundModel == null)
            return;

        List<ZonePoint> zones = groundModel.getZonePoints();
        if (zones.isEmpty())
            return;

        double d = groundModel.getDistOnPerimeter(pos);
        int n = zones.size();
        int zoneIdx = 0;

        // Identify current zone
        boolean found = false;
        for (int i = 0; i < n; i++) {
            double ziDist = groundModel.getDistOnPerimeter(zones.get(i).getPoint());
            if (i == 0) {
                if (d <= ziDist) {
                    zoneIdx = 0;
                    found = true;
                    break;
                }
            } else {
                double prevZiDist = groundModel.getDistOnPerimeter(zones.get(i - 1).getPoint());
                if (d > prevZiDist && d <= ziDist) {
                    zoneIdx = i;
                    found = true;
                    break;
                }
            }
        }
        if (!found)
            zoneIdx = 0;

        zoneCollisionStats.merge(zoneIdx, 1, Integer::sum);

        int totalCollisions = zoneCollisionStats.values().stream().mapToInt(Integer::intValue).sum();
        double perimeterLen = groundModel.getPerimeterLength();

        // Check for imbalance
        int bestTargetZone = -1;
        double maxDeficitValue = 0;

        double currentZoneActualPct = zoneCollisionStats.getOrDefault(zoneIdx, 0) / (double) totalCollisions;
        double currentZoneTargetPct = zones.get(zoneIdx).getPercentage() / 100.0;
        double currentZoneSurplus = currentZoneActualPct - currentZoneTargetPct;

        // Find the most starved zone
        for (int i = 0; i < n; i++) {
            double actualPct = zoneCollisionStats.getOrDefault(i, 0) / (double) totalCollisions;
            double targetPct = zones.get(i).getPercentage() / 100.0;
            double deficit = targetPct - actualPct;
            if (deficit > maxDeficitValue) {
                maxDeficitValue = deficit;
                bestTargetZone = i;
            }
        }

        // Decision logic for V3:
        // Transition if:
        // 1. We stayed long enough in the current zone
        // 2. Current zone has a surplus
        // 3. Target zone has a deficit
        // 4. Either the deficit is critical OR the target is "close" (opportunistic)

        boolean shouldTransition = false;
        if (totalCollisions > 20 && bestTargetZone != -1 && bestTargetZone != zoneIdx) {
            int stayLength = totalCollisions - lastTransitionCollisionCount;

            if (stayLength >= MIN_COLLISIONS_PER_STAY && currentZoneSurplus > 0) {
                double dTarget = groundModel.getDistOnPerimeter(zones.get(bestTargetZone).getPoint());
                double dCurrent = groundModel.getDistOnPerimeter(pos);
                double distToTarget = (dTarget - dCurrent + perimeterLen) % perimeterLen;
                boolean opportunistic = distToTarget < (perimeterLen * OPPORTUNISTIC_DIST_RATIO);

                if (maxDeficitValue > CRITICAL_DEFICIT_THRESHOLD) {
                    shouldTransition = true;
                } else if (opportunistic && currentZoneSurplus > SURPLUS_LEAVE_THRESHOLD) {
                    shouldTransition = true;
                }
            }
        }

        if (shouldTransition) {
            lastTransitionCollisionCount = totalCollisions;
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

                zoneCollisionStats.merge(bestTargetZone, 1, Integer::sum);
                collisionCount++;
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

        int totalCollisions = zoneCollisionStats.values().stream().mapToInt(Integer::intValue).sum();

        List<ZonePoint> zones = groundModel.getZonePoints();
        g.setColor(java.awt.Color.WHITE);
        for (Map.Entry<Integer, Integer> entry : zoneCollisionStats.entrySet()) {
            int idx = entry.getKey();
            String label = groundModel.getZoneLabel(idx);
            double pct = totalCollisions > 0 ? (entry.getValue() * 100.0 / totalCollisions) : 0.0;
            int targetPct = (idx >= 0 && idx < zones.size()) ? zones.get(idx).getPercentage() : 0;
            g.drawString(String.format("Zone %s: %d (%.1f%% / Target: %d%%)", label, entry.getValue(), pct, targetPct),
                    startX,
                    startY + (row * 15));
            row++;
        }
        g.drawString(String.format("Transition distance: %.1f m", getTransitionDistanceMeters()), startX,
                startY + (row * 15));
    }

    @Override
    public String getModelName() {
        return "Version 3 (Smart Balancing)";
    }

    @Override
    public int getModelVersion() {
        return 3;
    }

    @Override
    public AbstractAutoMowerModel createNewInstance() {
        return new AutoMowerModelV3();
    }
}
