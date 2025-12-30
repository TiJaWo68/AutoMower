package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class CollisionLogicTest {

    @RepeatedTest(20)
    void testBounceDirectionValid() {
        AutoMowerModel model = new AutoMowerModel();

        // Wall from (0,0) to (10,0) -> Horizontal wall
        Line2D wall = new Line2D.Double(0, 0, 10, 0);
        Point2D hit = new Point2D.Double(5, 0);
        Point2D prev = new Point2D.Double(5, 5); // Coming from "Above" / "Inside"

        // Normal should be (0, 1) to point back to (5,5)

        Point2D result = model.calculateBounceDirection(wall, hit, prev);

        // Vector from hit to result
        double dx = result.getX() - hit.getX();
        double dy = result.getY() - hit.getY();

        // Must point somewhat "Up" (y > 0)
        assertTrue(dy > 0, "Result vector should point back inward (positive Y). Got " + dy);

        // Must be within 0 and 90 degrees from normal (0,1)
        // Since wall is horizontal, Normal is (0,1).
        // Angle 0 -> (0, 100) -> dy=100, dx=0
        // Angle 90 Left -> (-100, 0) -> dy=0, dx=-100
        // Angle 90 Right -> (100, 0) -> dy=0, dx=100

        // So dy should be >= 0 (strictly positive expected usually, but 0 is parallel
        // limit)
        // But in my implementation, I check dot product with Normal.
        // Normal is (0,1). Result dot Normal = 0*dx + 1*dy = dy.

        // Wait, if angle is 90 deg, cos(90)=0. So it moves parallel.
        // Implementation detail: I used random * 90.0.
        // If it's exactly 90, it might be parallel. But usually it's < 90.

        System.out.println("Result: " + dx + ", " + dy);
    }

    @Test
    void testVerticalWall() {
        AutoMowerModel model = new AutoMowerModel();
        // Wall (0,0) to (0,10)
        Line2D wall = new Line2D.Double(0, 0, 0, 10);
        Point2D hit = new Point2D.Double(0, 5);
        Point2D prev = new Point2D.Double(5, 5); // Right side

        Point2D result = model.calculateBounceDirection(wall, hit, prev);

        double dx = result.getX() - hit.getX();

        // Must point "Right" (x > 0)
        assertTrue(dx > 0, "Result should point right (positive X). Got " + dx);
    }
}
