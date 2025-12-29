package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import org.junit.jupiter.api.Test;

class StationSpawnTest {

    @Test
    void testSpawnAtStationOnBorder() {
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(java.awt.Color.BLACK);
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();
        gm.setBorder(border);

        // Station exactly on the border
        Point2D station = new Point2D.Double(50, 0);
        gm.setChargingStation(station);

        AutoMowerModel mower = new AutoMowerModel();
        mower.setCurrentPosition(station);
        MultiLine2D trace = new MultiLine2D(java.awt.Color.RED);
        mower.start(trace, gm);

        assertFalse(mower.stopped, "Mower should NOT be stopped at startup if at charging station");
    }

    @Test
    void testSpawnAtStationOutsideNudge() {
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(java.awt.Color.BLACK);
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();
        gm.setBorder(border);

        // Station 1px OUTSIDE (y = -1)
        Point2D station = new Point2D.Double(50, -1);
        gm.setChargingStation(new Point2D.Double(station.getX(), station.getY()));

        // Before nudge it is outside
        assertFalse(gm.isInside(station), "Station should be outside initially");

        // Execute nudge
        gm.ensureChargingStationInside();

        // After nudge it should be inside
        assertTrue(gm.isInside(gm.getChargingStation()), "Station should be inside after nudge");
        assertTrue(station.distance(gm.getChargingStation()) >= 1.0, "Station should have moved at least 1px");

        AutoMowerModel mower = new AutoMowerModel();
        // Robot starts at the OLD station position
        mower.setCurrentPosition(station);

        MultiLine2D trace = new MultiLine2D(java.awt.Color.RED);
        mower.start(trace, gm);

        assertFalse(mower.stopped, "Mower should have recovered to the nudged station");
        assertTrue(mower.getCurrentPosition().distance(gm.getChargingStation()) < 0.1,
                "Mower should be at the NEW station");
    }
}
