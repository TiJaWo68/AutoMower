package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Color;
import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class GridAlignmentTest {

    @Test
    void testGridGenerationWithRotation() {
        // 1. Setup Ground with Slanted Border
        GroundModel gm = new GroundModel();
        MultiLine2D border = new MultiLine2D(Color.ORANGE);

        // Create a slanted rectangle (longer edge at 45 degrees)
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 100)); // Length ~141
        border.addPoint(new Point2D.Double(90, 110)); // Short edge
        border.addPoint(new Point2D.Double(-10, 10));
        border.closePath();

        gm.setBorder(border);
        gm.setCalibration(10.0); // 1px = 10cm => 1m = 10px squares

        // 2. Instantiate Panel (triggers calculation)
        AreaVisualizationPanel panel = new AreaVisualizationPanel(gm);

        // We can't easily inspect private gridSquares list without reflection or
        // exposure.
        // But we can ensure it didn't crash.

        // Let's assert that it runs.
        assertNotNull(panel);
    }
}
