package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import de.in.autoMower.sim.AutoMowerModel.State;

class NavigationErrorRecoveryTest {

    @Test
    void testTeleportOnStuck() throws InterruptedException {
        // 1. Setup Ground with Trap
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(Color.ORANGE);
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();

        gm.setBorder(border);
        gm.setCalibration(1.0);
        Point2D dock = new Point2D.Double(10, 10);
        gm.setChargingStation(dock);

        // 2. Setup Mower
        // Use a subclass to force a navigation error scenario or just expose the method
        TestMower mower = new TestMower();
        mower.groundModel = gm;
        mower.border = border;
        mower.setCurrentPosition(new Point2D.Double(50, 50));
        mower.setSpeedInCmPerSec(100);
        mower.setTimeScale(100);

        // 3. Force "Stuck" condition
        // We can't easily reproduce the physics trap without complex geometry.
        // Instead, we will simulate the behavior of "calculateNextSegment" failing 200
        // times.
        // BUT that logic is inside private calculateNextSegment.

        // Alternative: Subclass override to force calling the error block?
        // No, the error block is deep inside.

        // Let's create a scenario we know fails?
        // A point surrounded by obstacles with NO escape?
        // e.g. a tiny triangle hole.

        // Or simpler: Mock the 'dirEnd' finding to always fail?
        // That requires overriding 'findTouchingLines' or something?

        // Let's try to inject a scenario where raycast always fails.
        // This is hard.

        // Let's rely on the method 'ensureChargingStationInside' logic? No.

        // Given the constraints, let's just manually trigger the failure condition via
        // reflection or subclass hook?
        // I'll add a 'triggerNavigationError()' method to my TestMower subclass that
        // simulates what happens.
        // Wait, I can't modify the state exactly as the method does unless I copy-paste
        // code.

        // Better: Create a degenerate ground model where 'isInside' always returns
        // false EXCEPT for start pos.
        // Then it will fail to find a valid direction.

        // Let's try that.

        GroundModel trapGm = new GroundModel() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isInside(Point2D p, double eps) {
                // Only (50,50) is inside, everything else is outside.
                if (p.distance(50, 50) < 0.1)
                    return true;
                return false;
            }

            @Override
            public Point2D getChargingStation() {
                return new Point2D.Double(10, 10); // Dock is "outside" logically but exists
            }
        };
        // We need border for initialization
        trapGm.setBorder(border);

        mower.groundModel = trapGm;
        mower.setCurrentPosition(new Point2D.Double(50, 50));

        // Now call calculateNextSegment
        // It should try 200 times to find a direction, fail, then trigger error
        // handling.

        mower.calculateNextSegment(); // calculates...

        // Assertions
        assertEquals(1, mower.getNavigationErrorCount(), "Should have 1 error");
        assertEquals(State.CHARGING, mower.currentState, "Should be charging");
        assertEquals(10.0, mower.getCurrentPosition().getX(), 0.001);
        assertEquals(10.0, mower.getCurrentPosition().getY(), 0.001);
        assertFalse(mower.stopped, "Should NOT be stopped");
    }

    static class TestMower extends AutoMowerModel {
        private static final long serialVersionUID = 1L;
        // Helper to access protected method if needed, but it is public in base class
    }
}
