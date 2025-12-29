package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class NavigationErrorReproductionTest {

    @Test
    void testNavigationStuckReproduction() throws InterruptedException {
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
        mower.cmProPixel = groundModel.getCalibration();
        mower.border = groundModel.getBorder();

        // Move mower to a known sharp corner from tbs10.json
        // Corner 0: (130.27, 202.28), Corner 1: (143.71, 217.65)
        // Let's place it very close to Corner 0
        mower.setCurrentPosition(new Point2D.Double(130.3, 202.3));

        if (data.mower != null) {
            mower.setSpeedInCmPerSec(data.mower.speedInCmPerSec);
            mower.batteryCapacityWh = data.mower.batteryCapacityWh;
            mower.currentBatteryWh = mower.batteryCapacityWh;
            if (data.mower.currentPosition != null) {
                mower.setCurrentPosition(data.mower.currentPosition.toPoint());
                if (!groundModel.isInside(mower.getCurrentPosition())) {
                    // Fallback if outside
                    if (groundModel.getChargingStation() != null)
                        mower.setCurrentPosition(groundModel.getChargingStation());
                    else
                        mower.setCurrentPosition(groundModel.getBorder().getPoint(0));
                }
            }
        } else {
            // A more problematic corner/pocket if possible.
            // Let's try near the vertex (378.65, 307.91) which is part of a small
            // indentation.
            mower.setCurrentPosition(new Point2D.Double(378.66, 307.92));
        }

        // Ensure it's in MOWING state and pointing towards the indentation
        mower.currentState = AutoMowerModel.State.MOWING;
        mower.currentLine = new Line2D.Double(mower.getCurrentPosition(), new Point2D.Double(378, 307)); // Pointing
                                                                                                         // towards
                                                                                                         // boundary
                                                                                                         // outside

        // High speed to trigger many calculations
        mower.setTimeScale(100.0);

        MultiLine2D trace = new MultiLine2D(Color.RED);
        trace.addPoint(mower.getCurrentPosition());

        // Important: Start runs in a thread.
        // We will start it, let it run for 2 seconds (simulated time)
        // Check trace points.

        mower.start(trace, groundModel);

        // Wait for potential stuck state
        Thread.sleep(5000);

        mower.stop();

        int points = trace.getNumberOfPoints();
        System.out.println("Points after 2s at speed 41: " + points);

        if (points < 2) {
            fail("Trace did not grow. Mower might be stuck immediately.");
        }

        assertFalse(mower.crashed, "Mower crashed: " + mower.getLastErrorMessage());
    }

    static class TestAutoMowerModel extends AutoMowerModel {
        private static final long serialVersionUID = 1L;
        String lastErrorMessage = null;
        boolean crashed = false;

        @Override
        public void showErrorMessage(String msg) {
            this.lastErrorMessage = msg;
            this.crashed = true;
            System.err.println("Mower Error: " + msg);
        }

        @Override
        protected void updateUI() {
            // Mock UI update - maybe verify thread safety here?
        }

        @Override
        protected void showInfoMessage(String msg) {
        }

        public String getLastErrorMessage() {
            return lastErrorMessage;
        }
    }
}
