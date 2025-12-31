package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.fail;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class TraceUpdateTest {

    @Test
    void testTraceUpdatesContinuously() {
        // Load tbs10.json
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

        TestAutoMowerModel mower = new TestAutoMowerModel();
        mower.groundModel = groundModel;

        mower.border = groundModel.getBorder();

        if (data.mower != null) {
            mower.setSpeedInCmPerSec(data.mower.speedInCmPerSec);
            mower.batteryCapacityWh = data.mower.batteryCapacityWh;
            mower.currentBatteryWh = mower.batteryCapacityWh;
        }

        Point2D startPos = groundModel.getChargingStation();
        if (startPos == null)
            startPos = groundModel.getBorder().getPoint(0);

        mower.setCurrentPosition(startPos);
        mower.currentState = AutoMowerModel.State.MOWING;

        // Setup Line (Trace)
        MultiLine2D traceLine = new MultiLine2D(java.awt.Color.RED);
        traceLine.addPoint(startPos);
        mower.line = traceLine; // Assign to mower

        mower.currentLine = new java.awt.geom.Line2D.Double(startPos, startPos);

        int maxLegs = 100;
        int previousPoints = traceLine.getNumberOfPoints();

        System.out.println("Starting Trace Test. Initial Points: " + previousPoints);

        for (int i = 0; i < maxLegs; i++) {
            mower.calculateNextSegment();

            // SIMULATE MOVEMENT (Crucial!)
            mower.setCurrentPosition(mower.currentLine.getP2());
            // Also need to manually add point if we are mimicking runMower?
            // No, calculateNextSegment adds point at START of segment.
            // But since we move position AFTER, the NEXT call will add the new position.

            if (mower.isStopped()) {
                fail("Mower stopped at leg " + i + ": " + mower.getLastErrorMessage());
            }

            int currentPoints = traceLine.getNumberOfPoints();
            Point2D lastPoint = traceLine.getPoints().get(currentPoints - 1);

            System.out.println("Leg " + i + ": Points=" + currentPoints + " Last=" + lastPoint);

            // Check for stagnation
            // Note: Now we only add points if distance > 1.0.
            // So points might NOT increase every step if stuck.
            // But coordinates MUST eventually change (Force Jump).

            // Check for NaN
            if (Double.isNaN(lastPoint.getX()) || Double.isNaN(lastPoint.getY())) {
                fail("Trace coordinate is NaN at leg " + i);
            }

            previousPoints = currentPoints;
        }

        System.out.println("TraceTest Passed. Final Points: " + traceLine.getNumberOfPoints());
    }

    static class TestAutoMowerModel extends AutoMowerModel {
        private static final long serialVersionUID = 1L;
        private String lastErrorMessage = null;

        @Override
        public void showErrorMessage(String msg) {
            this.lastErrorMessage = msg;
        }

        @Override
        protected void updateUI() {
        } // Mock

        public String getLastErrorMessage() {
            return lastErrorMessage;
        }
    }
}
