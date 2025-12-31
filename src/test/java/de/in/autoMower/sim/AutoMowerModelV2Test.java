package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.in.autoMower.sim.AbstractAutoMowerModel.State;

class AutoMowerModelV2Test {

    @Test
    void testTransitionToZoneAndBackToMowing() {
        // 1. Setup Ground Model with Border and Zones
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
        zones.add(new ZonePoint(new Point2D.Double(0, 0), 50)); // Zone A
        zones.add(new ZonePoint(new Point2D.Double(100, 100), 50)); // Zone B
        gm.setZonePoints(zones);

        // 2. Setup Mower V2
        AutoMowerModelV2 mower = new AutoMowerModelV2();
        mower.groundModel = gm;
        mower.border = border;
        mower.setCurrentPosition(new Point2D.Double(50, 0)); // On top edge
        mower.line = new MultiLine2D(Color.RED);
        mower.currentState = State.MOWING;
        mower.setTimeScale(1.0);

        // 3. Manually trigger TRANSITIONING_TO_ZONE
        // We want to go to midpoint between Zone A (0,0) and Zone B (100,100).
        // Midpoint on perimeter:
        // Perimeter points: (0,0) -> (100,0) -> (100,100) -> (0,100) -> (0,0)
        // Distances: (0,0) [0] -> (100,0) [100] -> (100,100) [200] -> (0,100) [300] ->
        // (0,0) [400]
        // Zone A point at 0. Zone B point at 200.
        // Midpoint between Zone A and Zone B (the other way around or forward?)
        // In AutoMowerModelV2: dMid = (dPrev + dTarget) / 2.0;
        // If target is Zone B (200) and Prev is Zone A (0), dMid = 100.
        // Point at 100 is (100,0).

        mower.transitionTargetPoint = new Point2D.Double(100, 100); // Let's set target directly for test
        mower.currentState = State.TRANSITIONING_TO_ZONE;

        System.out
                .println("Starting transit from " + mower.getCurrentPosition() + " to " + mower.transitionTargetPoint);

        // 4. Simulate movement until it should reach the target
        // We call calculateNextSegment and update currentPosition
        int maxSteps = 1000;
        int steps = 0;
        while (mower.currentState == State.TRANSITIONING_TO_ZONE && steps < maxSteps) {
            mower.calculateNextSegment();
            assertNotNull(mower.getCurrentLine(), "Current line should not be null during transit");
            mower.setCurrentPosition(mower.getCurrentLine().getP2());
            steps++;
            System.out
                    .println("Step " + steps + ": Pos=" + mower.getCurrentPosition() + ", State=" + mower.currentState);
        }

        // 5. Verify it reached MOWING state and continue to see if it moves
        assertTrue(steps < maxSteps, "Mower got stuck in TRANSITIONING_TO_ZONE state for " + maxSteps + " steps");
        assertEquals(State.MOWING, mower.currentState, "Mower should be in MOWING state after reaching target");

        Point2D posAtMowingStart = mower.getCurrentPosition();
        mower.calculateNextSegment();
        mower.setCurrentPosition(mower.getCurrentLine().getP2());
        assertTrue(mower.getCurrentPosition().distance(posAtMowingStart) > 1.0, "Mower should move after transition");
        System.out.println("Final state reached and mower is moving.");
    }

    @Test
    void testComplexTransitAcrossMultipleVertices() {
        // Setup a more complex polygon
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(Color.ORANGE);
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 50));
        border.addPoint(new Point2D.Double(200, 50));
        border.addPoint(new Point2D.Double(200, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();
        gm.setBorder(border);
        gm.setCalibration(1.0);

        List<ZonePoint> zones = new ArrayList<>();
        zones.add(new ZonePoint(new Point2D.Double(0, 0), 50));
        zones.add(new ZonePoint(new Point2D.Double(200, 100), 50));
        gm.setZonePoints(zones);

        AutoMowerModelV2 mower = new AutoMowerModelV2();
        mower.groundModel = gm;
        mower.border = border;
        mower.setCurrentPosition(new Point2D.Double(50, 0));
        mower.line = new MultiLine2D(Color.RED);
        mower.currentState = State.MOWING;

        // Target: Midpoint between (0,0) and (200,100) on perimeter
        // Perimeter: 0,0 -> 100,0 [100] -> 100,50 [150] -> 200,50 [250] -> 200,100
        // [300] -> 0,100 [500] -> 0,0 [600]
        // Target dist: (0 + 300) / 2 = 150.
        // Point at dist 150 is (100, 50).
        mower.transitionTargetPoint = new Point2D.Double(100, 50);
        mower.currentState = State.TRANSITIONING_TO_ZONE;
        mower.currentBorderIndex = 0; // Starting at top edge

        int maxSteps = 100;
        int steps = 0;
        while (mower.currentState == State.TRANSITIONING_TO_ZONE && steps < maxSteps) {
            Point2D prevPos = mower.getCurrentPosition();
            mower.calculateNextSegment();
            mower.setCurrentPosition(mower.getCurrentLine().getP2());
            steps++;
            System.out
                    .println("Step " + steps + ": Pos=" + mower.getCurrentPosition() + ", State=" + mower.currentState);

            if (mower.currentState == State.MOWING) {
                // Transition just happened. Mower current position is the target of the first
                // mowing segment.
                // We should check if the PREVIOUS position was near the target.
                assertTrue(prevPos.distance(100, 50) < 5.0,
                        "Mower should have been near target before switching to MOWING. Dist: "
                                + prevPos.distance(100, 50));
            }
        }

        assertEquals(State.MOWING, mower.currentState, "Mower should reach MOWING state eventually");
    }
}
