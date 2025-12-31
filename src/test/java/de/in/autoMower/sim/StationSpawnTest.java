package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class StationSpawnTest {

    @Test
    void testSnapToNearestVertex() {
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(java.awt.Color.BLACK);
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();
        gm.setBorder(border);

        // Station near (0,0)
        Point2D station = new Point2D.Double(10, 0);
        gm.setChargingStation(station); // Should snap to (0,0)

        Point2D snapped = gm.getChargingStation();
        assertTrue(snapped.distance(new Point2D.Double(0, 0)) < 0.01, "Station should snap to (0,0)");

        // Station near (100,0)
        station = new Point2D.Double(90, 0);
        gm.setChargingStation(station); // Should snap to (100,0)
        snapped = gm.getChargingStation();
        assertTrue(snapped.distance(new Point2D.Double(100, 0)) < 0.01, "Station should snap to (100,0)");
    }

    @Test
    void testMowerStartsAtSnappedStation() {
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(java.awt.Color.BLACK);
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();
        gm.setBorder(border);

        // Station near (0,0)
        Point2D station = new Point2D.Double(10, -5);
        gm.setChargingStation(station); // Snaps to (0,0)

        AutoMowerModel mower = new AutoMowerModel();
        mower.setCurrentPosition(gm.getChargingStation()); // Spawn at snapped pos

        MultiLine2D trace = new MultiLine2D(java.awt.Color.RED);
        mower.start(trace, gm);

        assertFalse(mower.isStopped(), "Mower should run");
        assertTrue(mower.getCurrentPosition().distance(new Point2D.Double(0, 0)) < 0.1, "Mower should be at (0,0)");
    }
}
