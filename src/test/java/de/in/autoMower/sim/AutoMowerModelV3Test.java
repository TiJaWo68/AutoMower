package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.in.autoMower.sim.AbstractAutoMowerModel.State;

class AutoMowerModelV3Test {

    @Test
    void testTransitionToZoneAndBackToMowing() {
        // 1. Setup Ground
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(Color.ORANGE);
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();

        gm.setBorder(border);
        gm.setCalibration(1.0);

        List<ZonePoint> zones = new ArrayList<>();
        zones.add(new ZonePoint(new Point2D.Double(50, 0), 50)); // Zone A ends at top mid
        zones.add(new ZonePoint(new Point2D.Double(100, 100), 50)); // Zone B ends at bottom right
        gm.setZonePoints(zones);

        // 2. Setup Mower V3
        AutoMowerModelV3 mower = new AutoMowerModelV3();
        mower.groundModel = gm;
        mower.border = border;
        mower.setCurrentPosition(new Point2D.Double(25, 0)); // Inside Zone A
        mower.line = new MultiLine2D(Color.RED);
        mower.currentState = State.MOWING;

        // 3. Simulate collisions to create imbalance
        // We want Zone A to have a surplus and Zone B to be empty (100% deficit)
        // Force 30 collisions in Zone A (to pass the >20 threshold)
        for (int i = 0; i < 30; i++) {
            mower.onCollision(new Point2D.Double(20, 0)); // Explicitly in Zone A
        }

        // At this point, totalCollisions = 30.
        // Zone A: 30 (100%), Target: 15. Surplus: 50%
        // Zone B: 0 (0%), Target: 15. Deficit: 50%
        // This should trigger a transition on the next collision in Zone A

        mower.onCollision(new Point2D.Double(25, 0));

        // 4. Verify transition started
        assertEquals(State.TRANSITIONING_TO_ZONE, mower.currentState, "Mower should start transition due to imbalance");

        // Target should be midpoint of Zone B (between (50,0) and (100,100) on
        // perimeter)
        // Perimeter dist (50,0) = 50. (100,100) = 200. Mid = 125.
        // Point at dist 125 is (100, 25).
        assertTrue(mower.transitionTargetPoint.distance(100, 25) < 1.0,
                "Target point should be midpoint of Zone B. Pos: " + mower.transitionTargetPoint);

        // 5. Run simulation to reach target
        int maxSteps = 100;
        int steps = 0;
        while (mower.currentState == State.TRANSITIONING_TO_ZONE && steps < maxSteps) {
            Point2D prevPos = mower.getCurrentPosition();
            mower.calculateNextSegment();
            mower.setCurrentPosition(mower.getCurrentLine().getP2());
            steps++;
            if (mower.currentState == State.MOWING) {
                // Done
                assertTrue(prevPos.distance(100, 25) < 5.0);
            }
        }

        assertEquals(State.MOWING, mower.currentState, "Mower should reach MOWING state after transition");
    }

    @Test
    void testNoTransitionWhenBalanced() {
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(Color.ORANGE);
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();
        gm.setBorder(border);
        gm.setCalibration(1.0);

        List<ZonePoint> zones = new ArrayList<>();
        zones.add(new ZonePoint(new Point2D.Double(50, 0), 50));
        zones.add(new ZonePoint(new Point2D.Double(100, 100), 50));
        gm.setZonePoints(zones);

        AutoMowerModelV3 mower = new AutoMowerModelV3();
        mower.groundModel = gm;
        mower.border = border;
        mower.setCurrentPosition(new Point2D.Double(25, 0));
        mower.currentState = State.MOWING;

        // Balanced: Alternate collisions
        for (int i = 0; i < 15; i++) {
            mower.onCollision(new Point2D.Double(20, 0)); // Zone A
            mower.onCollision(new Point2D.Double(100, 50)); // Zone B
        }

        // Zone A: 15 (50%), Zone B: 15 (50%). Total 30.
        mower.onCollision(new Point2D.Double(25, 0));

        assertEquals(State.MOWING, mower.currentState, "Should NOT transition when balanced");
    }
}
