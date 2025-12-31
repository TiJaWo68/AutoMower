package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ZonePointTest {

    @Test
    public void testZonePointSorting() {
        GroundModel model = new GroundModel();
        MultiLine2D border = model.getBorder();
        // Square 0,0 to 100,100
        border.addPoint(new Point2D.Double(0, 0));
        border.addPoint(new Point2D.Double(100, 0));
        border.addPoint(new Point2D.Double(100, 100));
        border.addPoint(new Point2D.Double(0, 100));
        border.closePath();

        // Start at index 0 (0,0), direction 1 (CW if 0,0 100,0 100,100... check det
        // logic in updateNumbering)
        model.setChargingStation(new Point2D.Double(0, 0));
        // updateNumbering sets direction based on det.
        // 0,0 -> 100,0 is dx=100. 100,0 -> 100,100 is dy=100. det = 100*100 = 10000.
        // direction = (10000 > 0) ? -1 : 1 => -1.
        // Wait, numbering direction in MultiLine2D: ((index - startIndex) * direction +
        // size) % size + 1
        // If direction is -1, it goes 0, n-1, n-2...

        // Add points on segments
        model.addZonePoint(new Point2D.Double(50, 0), 20); // Path pos: 50 if dir 1, or 350 if dir -1?
        model.addZonePoint(new Point2D.Double(0, 50), 30); // Path pos: 350 if dir 1, or 50 if dir -1?

        List<ZonePoint> zps = model.getZonePoints();
        assertEquals(2, zps.size());

        // With vertices (0,0) (100,0) (100,100) (0,100) and charger at 0,0:
        // direction is 1. Segments are: 0-1 (y=0, x:0-100), 1-2 (x=100, y:0-100), 2-3
        // (y=100, x:100-0), 3-0 (x=0, y:100-0)
        // (50,0) is on segment 0-1. Perimeter distance: 50.
        // (0,50) is on segment 3-0. Segment starts at index 3.
        // Path distance to index 3: 0->1(100) + 1->2(100) + 2->3(100) = 300.
        // Plus dist(pts[3], p) = dist((0,100), (0,50)) = 50. Total 350.

        // Sorting should yield (50,0) then (0,50)
        assertEquals(50, zps.get(0).getPoint().getX());
        assertEquals(0, zps.get(0).getPoint().getY());

        assertEquals(0, zps.get(1).getPoint().getX());
        assertEquals(50, zps.get(1).getPoint().getY());
    }

    @Test
    public void testZonePointLabels() {
        // Test the label logic locally as it's private in GroundModel
        assertEquals("A", getLabel(0));
        assertEquals("Z", getLabel(25));
        assertEquals("AA", getLabel(26));
        assertEquals("AZ", getLabel(51));
        assertEquals("BA", getLabel(52));
    }

    private String getLabel(int index) {
        StringBuilder sb = new StringBuilder();
        int i = index;
        do {
            sb.insert(0, (char) ('A' + (i % 26)));
            i = (i / 26) - 1;
        } while (i >= 0);
        return sb.toString();
    }

    @Test
    public void testRemoval() {
        GroundModel model = new GroundModel();
        model.getBorder().addPoint(new Point2D.Double(0, 0));
        model.getBorder().addPoint(new Point2D.Double(100, 0));
        model.getBorder().closePath();

        Point2D p = new Point2D.Double(50, 0);
        model.addZonePoint(p, 20);
        assertEquals(1, model.getZonePoints().size());

        // Remove
        model.removeZonePointNear(new Point2D.Double(51, 0), 5.0);
        assertEquals(0, model.getZonePoints().size());
    }

    @Test
    public void testPersistence(@TempDir File tempDir) throws IOException {
        GroundModel model = new GroundModel();
        model.getBorder().addPoint(new Point2D.Double(0, 0));
        model.getBorder().addPoint(new Point2D.Double(10, 0));
        model.getBorder().closePath();
        model.setChargingStation(new Point2D.Double(0, 0));
        model.addZonePoint(new Point2D.Double(5, 0), 25);

        ProjectData data = new ProjectData();
        data.zonePoints = List.of(new ProjectData.ZonePointDTO(model.getZonePoints().get(0)));

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(tempDir, "zones.json");
        mapper.writeValue(file, data);

        ProjectData loaded = mapper.readValue(file, ProjectData.class);
        assertEquals(1, loaded.zonePoints.size());
        assertEquals(5, loaded.zonePoints.get(0).x);
        assertEquals(25, loaded.zonePoints.get(0).percentage);
    }
}
