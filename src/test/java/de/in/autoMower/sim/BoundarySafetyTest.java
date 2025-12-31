package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.fail;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class BoundarySafetyTest {

    @Test
    void testStayInsideFor100Collisions() {
        // Load tbs10.json as a complex test case
        File resourceFile = new File("src/test/resources/tbs10.json");
        if (!resourceFile.exists())
            resourceFile = new File("tbs10.json");

        ProjectData data = null;
        try {
            data = new ObjectMapper().readValue(resourceFile, ProjectData.class);
        } catch (Exception e) {
            fail("Failed to load generic json");
        }

        GroundModel groundModel = new GroundModel();
        groundModel.setCalibration(data.calibration);
        groundModel.setBorder(data.border.toMultiLine());
        groundModel.obstacles = data.obstacles.stream().map(ProjectData.MultiLineDTO::toMultiLine)
                .collect(Collectors.toList());
        if (data.chargingStation != null)
            groundModel.setChargingStation(data.chargingStation.toPoint());

        // Ensure station is inside
        groundModel.ensureChargingStationInside();

        AutoMowerModel mower = new AutoMowerModel();
        mower.groundModel = groundModel;

        mower.border = groundModel.getBorder();

        Point2D startPos = groundModel.getChargingStation();
        mower.setCurrentPosition(startPos);
        mower.currentState = AutoMowerModel.State.MOWING;

        // Trace for mower
        MultiLine2D trace = new MultiLine2D(java.awt.Color.RED);
        mower.line = trace;

        int collisions = 0;
        int maxSteps = 10000; // Safety break

        System.out.println("Starting Boundary Safety Test...");

        for (int i = 0; i < maxSteps && collisions < 500; i++) {
            mower.calculateNextSegment();

            if (mower.isStopped()) {
                fail("Mower stopped at step " + i + " after " + collisions + " collisions.");
            }

            // Move to end of segment
            Point2D nextPos = mower.currentLine.getP2();
            mower.setCurrentPosition(nextPos);

            // Check if inside
            if (!groundModel.isInside(nextPos)) {
                // Allow a tiny tolerance for points exactly on the line
                boolean nearStation = groundModel.getChargingStation() != null
                        && nextPos.distance(groundModel.getChargingStation()) < 2.0;

                if (!nearStation) {
                    double borderDist = groundModel.getBorder().ptSegDist(nextPos);
                    String msg = "Mower moved OUTSIDE at step " + i + " (Collisions: " + collisions + ") to " + nextPos
                            + ", dist to border: " + borderDist;
                    System.out.println(msg);
                    fail(msg);
                }
            }

            collisions++;
        }

        System.out.println("Boundary Safety Test passed with " + collisions + " collisions.");
    }
}
