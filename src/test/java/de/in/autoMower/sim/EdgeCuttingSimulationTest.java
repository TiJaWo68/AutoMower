package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Color;
import java.io.File;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class EdgeCuttingSimulationTest {

    @Test
    void testEdgeCuttingCompletesLoop() throws InterruptedException {
        // Load tbs10.json
        File resourceFile = new File("tbs10.json");
        if (!resourceFile.exists()) {
            resourceFile = new File("src/test/resources/tbs10.json");
        }

        ProjectData data = null;
        try {
            data = new ObjectMapper().readValue(resourceFile, ProjectData.class);
        } catch (Exception e) {
            fail("Failed to load generic json: " + e.getMessage());
        }

        GroundModel groundModel = new GroundModel();
        groundModel.setCalibration(data.calibration);
        groundModel.setBorder(data.border.toMultiLine());
        groundModel.obstacles = data.obstacles.stream().map(ProjectData.MultiLineDTO::toMultiLine)
                .collect(Collectors.toList());
        if (data.chargingStation != null)
            groundModel.setChargingStation(data.chargingStation.toPoint());

        AutoMowerModel mower = new AutoMowerModel() {
            @Override
            protected void updateUI() {
            }

            @Override
            protected void showErrorMessage(String msg) {
                System.err.println("Mower Error: " + msg);
            }

            @Override
            protected void showInfoMessage(String msg) {
                System.out.println("Mower Info: " + msg);
            }
        };

        mower.setTimeScale(50.0); // Fast simulation

        MultiLine2D trace = new MultiLine2D(Color.RED);
        mower.startEdgeCutting(trace, groundModel);

        System.out.println("Mower started edge cutting at " + mower.getCurrentPosition());
        System.out.println("Dock is at " + groundModel.getChargingStation());

        // Wait for completion (it should stop at charging station)
        long start = System.currentTimeMillis();
        int lastPoints = 0;
        while (!mower.stopped && (System.currentTimeMillis() - start) < 60000) {
            Thread.sleep(1000);
            int currentPoints = trace.getNumberOfPoints();
            System.out.println("Time: " + (System.currentTimeMillis() - start) / 1000 + "s, Points: " + currentPoints +
                    ", Pos: " + mower.getCurrentPosition() + ", State: " + mower.currentState);
            lastPoints = currentPoints;
        }

        assertTrue(mower.stopped, "Mower did not stop within timeout. Final Pos: " + mower.getCurrentPosition()
                + ", State: " + mower.currentState);
        assertTrue(mower.isCharging, "Mower should be charging at the end. State: " + mower.currentState);

        // Ensure it followed the border (trace points should be roughly equal to border
        // points + 1)
        assertTrue(trace.getNumberOfPoints() >= groundModel.getBorder().getNumberOfPoints(),
                "Trace too short: " + trace.getNumberOfPoints() + " vs " + groundModel.getBorder().getNumberOfPoints());
    }
}
