package de.in.autoMower.sim;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.awt.Color;
import java.io.File;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConcavePathTest {

    @Test
    public void testConcavePathAtPoint9() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProjectData data = mapper.readValue(new File("tbs10.json"), ProjectData.class);

        GroundModel gm = new GroundModel();
        gm.calibration = data.calibration;
        gm.border = data.border.toMultiLine();
        gm.obstacles = new java.util.LinkedList<>(); // Simplify

        AutoMowerModel model = new AutoMowerModel();
        model.groundModel = gm;
        model.border = gm.getBorder();

        // Point 9 from tbs10.json
        Point2D p9 = gm.getBorder().getPoints().get(9);
        model.setCurrentPosition(p9);

        // Incoming from point 8
        Point2D p8 = gm.getBorder().getPoints().get(8);
        model.currentLine = new Line2D.Double(p8, p9);

        model.currentState = AutoMowerModel.State.MOWING;

        // Trigger calculation
        model.calculateNextSegment();

        Line2D nextLine = model.getCurrentLine();
        Assertions.assertNotNull(nextLine, "Should have calculated a next line");

        System.out.println("Current Position (P9): " + p9);
        System.out.println("Next Line P2: " + nextLine.getP2());

        // Check if midpoint of nextLine is inside
        Point2D midPoint = new Point2D.Double(
                (nextLine.getX1() + nextLine.getX2()) / 2.0,
                (nextLine.getY1() + nextLine.getY2()) / 2.0);

        boolean inside = gm.isInside(midPoint, 0.05);
        System.out.println("Is midpoint of next segment inside? " + inside);

        Assertions.assertTrue(inside, "The segment should be entirely within the allowed area");
    }
}
