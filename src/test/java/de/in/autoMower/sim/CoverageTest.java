package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class CoverageTest {

    @Test
    void testCoverageBasics() {
        // 1. Setup Ground
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(Color.ORANGE);
        // 100x100 pixel box
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();

        gm.setBorder(border);
        gm.setCalibration(1.0); // 1 cm/pixel

        AutoMowerModel mower = new AutoMowerModel();

        mower.setMowingWidthInCm(20); // 10px radius

        // 2. Init Coverage (Manually for test, usually done in start())
        // We need to inject the gm into mower first?
        // start() does that. Let's assume we can set it up via startInternal or just
        // hack fields?
        // initCoverage() checks this.groundModel.

        // We can use start() but it starts a thread.
        // Let's subclass to avoid thread or just stop it immediately.
        // Or just set the field if possible. 'groundModel' is protected.

        TestMower testMower = new TestMower();
        testMower.setGroundModel(gm);
        testMower.setCalibration(1.0);
        testMower.setMowingWidthInCm(20);

        testMower.initCoverage();

        double initial = testMower.getCoveragePercentage();
        assertEquals(0.0, initial, 0.0001, "Initial coverage should be 0");

        // 3. Move Mower to center (50, 50) and update
        // Mower logic usually updates coverage in runMower loop.
        // We can expose updateCoverage for testing or simulate movement.
        // Since updateCoverage is private, we might need reflection or a test subclass
        // helper.

        testMower.callUpdateCoverage(new Point2D.Double(50, 50));

        double afterOneSpot = testMower.getCoveragePercentage();
        assertTrue(afterOneSpot > 0.0, "Coverage should increase after mowing one spot");
        System.out.println("Coverage after 1 spot: " + afterOneSpot);

        // Area of circle radius 10 = pi * 100 ~ 314 pixels.
        // Total area ~ 10000 pixels.
        // Percentage ~ 3.14% = 0.0314

        assertTrue(afterOneSpot > 0.02 && afterOneSpot < 0.04, "Coverage should be roughly 3%");

        // 4. Move to same spot -> Should not increase significantly
        testMower.callUpdateCoverage(new Point2D.Double(50, 50));
        assertEquals(afterOneSpot, testMower.getCoveragePercentage(), 0.0001,
                "Coverage shouldn't change for same spot");

    }

    static class TestMower extends AutoMowerModel {
        private static final long serialVersionUID = 1L;

        public void setGroundModel(GroundModel gm) {
            this.groundModel = gm;
        }

        public void setCalibration(double d) {
            if (this.groundModel != null)
                this.groundModel.setCalibration(d);
        }

        // Since updateCoverage is private, we need a way to call it.
        // Wait, I made it private in the implementation.
        // I can change it to protected or package-private for testing, OR use
        // reflection.
        // I'll assume for this test I can access it if I put the test in the same
        // package (de.in.autoMower.sim)
        // Oh wait, updateCoverage is private. Same package class can't access private.
        // I should have made it package protected or protected.
        // I will use Reflection for now to avoid editing the main file again just for
        // this test visibility.

        public void callUpdateCoverage(Point2D pos) {
            try {
                java.lang.reflect.Method method = AutoMowerModel.class.getDeclaredMethod("updateCoverage",
                        Point2D.class);
                method.setAccessible(true);
                method.invoke(this, pos);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
