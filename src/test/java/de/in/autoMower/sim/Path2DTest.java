package de.in.autoMower.sim;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import org.junit.jupiter.api.Test;

class Path2DTest {
    @Test
    void testBoundaryPoint() {
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(100, 0);
        path.lineTo(100, 100);
        path.lineTo(0, 100);
        path.closePath();

        System.out.println("Top (50,0): " + path.contains(new Point2D.Double(50, 0)));
        System.out.println("Bottom (50,100): " + path.contains(new Point2D.Double(50, 100)));
        System.out.println("Left (0,50): " + path.contains(new Point2D.Double(0, 50)));
        System.out.println("Right (100,50): " + path.contains(new Point2D.Double(100, 50)));

        System.out.println("TL (0,0): " + path.contains(new Point2D.Double(0, 0)));
        System.out.println("TR (100,0): " + path.contains(new Point2D.Double(100, 0)));
        System.out.println("BR (100,100): " + path.contains(new Point2D.Double(100, 100)));
        System.out.println("BL (0,100): " + path.contains(new Point2D.Double(0, 100)));
    }
}
