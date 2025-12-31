package de.in.autoMower.sim;

import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Version 2 of the AutoMowerModel. Tracks per-zone collision statistics.
 */
public class AutoMowerModelV2 extends AbstractAutoMowerModel {

	private static final long serialVersionUID = 1L;

	private final Map<Integer, Integer> zoneCollisionStats = new TreeMap<>();

	public AutoMowerModelV2() {
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

		// Zones are intervals: [0, Z0] -> Zone 0 (Z0, Z1] -> Zone 1 ... (Zn-1,
		// Perimeter] -> Zone 0

		int n = zones.size();
		int zoneIdx = 0; // Default to Zone A (index 0)

		// Check if it falls in (Zi-1, Zi]
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

		if (!found) {
			// Must be in (Zn-1, Perimeter]
			zoneIdx = 0;
		}

		zoneCollisionStats.merge(zoneIdx, 1, Integer::sum);

		int totalCollisions = zoneCollisionStats.values().stream().mapToInt(Integer::intValue).sum();
		if (totalCollisions % 20 == 19) {
			double perimeterLen = groundModel.getPerimeterLength();

			int bestTargetZone = -1;
			double maxDeficit = 0;

			for (int i = 0; i < n; i++) {
				int actual = zoneCollisionStats.getOrDefault(i, 0);
				double ideal = totalCollisions * (zones.get(i).getPercentage() / 100.0);
				double deficit = ideal - actual;
				if (deficit > maxDeficit) {
					maxDeficit = deficit;
					bestTargetZone = i;
				}
			}

			if (bestTargetZone != -1 && bestTargetZone != zoneIdx) {
				// Transition to the midpoint between current zone boundary and target zone
				// boundary? User: "halben Weg zwischen dem
				// vorhergehenden Zonenpunkt und dem benachteiligten Zonenpunkt"
				double dTarget = groundModel.getDistOnPerimeter(zones.get(bestTargetZone).getPoint());
				double dPrev = groundModel.getDistOnPerimeter(zones.get((bestTargetZone - 1 + n) % n).getPoint());

				double dMid;
				if (dTarget < dPrev) {
					// Wraps around
					dMid = (dPrev + dTarget + perimeterLen) / 2.0;
				} else {
					dMid = (dPrev + dTarget) / 2.0;
				}

				transitionTargetPoint = groundModel.getPointAtDistOnPerimeter(dMid);
				if (transitionTargetPoint != null) {
					currentState = State.TRANSITIONING_TO_ZONE;
					// Snap to border to start following
					double dColl = groundModel.getDistOnPerimeter(pos);
					currentPosition = groundModel.getPointAtDistOnPerimeter(dColl);
					currentBorderIndex = getNearestBorderIndex(currentPosition);

					// User: "zählt eine Kollision für C"
					zoneCollisionStats.merge(bestTargetZone, 1, Integer::sum);
					collisionCount++; // Also increment global count for the "artificial" collision
				}
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
		return "Version 2 (Zone Statistics)";
	}

	@Override
	public int getModelVersion() {
		return 2;
	}

	@Override
	public AbstractAutoMowerModel createNewInstance() {
		return new AutoMowerModelV2();
	}
}
