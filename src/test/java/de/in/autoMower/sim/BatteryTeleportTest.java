package de.in.autoMower.sim;

import java.awt.geom.Point2D;
import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BatteryTeleportTest {

    @Test
    public void testBatteryEmptyTeleport() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProjectData data = mapper.readValue(new File("tbs10.json"), ProjectData.class);

        GroundModel gm = new GroundModel();
        gm.calibration = data.calibration;
        gm.border = data.border.toMultiLine();
        if (data.chargingStation != null) {
            gm.setChargingStation(data.chargingStation.toPoint());
            gm.ensureChargingStationInside();
        }

        AutoMowerModel model = new AutoMowerModel();
        model.groundModel = gm;
        model.border = gm.getBorder();
        model.setTimeScale(5.0);

        // Start far from dock with very low battery
        Point2D startPos = gm.getBorder().getPoints().get(10); // Some random point far away
        Point2D dock = gm.getChargingStation();

        // Ensure startPos is far from dock
        if (startPos.distance(dock) < 50) {
            startPos = gm.getBorder().getPoints().get(5);
        }

        model.setCurrentPosition(new Point2D.Double(startPos.getX(), startPos.getY()));
        model.currentState = AutoMowerModel.State.MOWING;
        model.currentBatteryWh = 0.001; // Almost empty

        // Run simulation briefly
        Thread mowerThread = new Thread(model::runMower);
        mowerThread.start();

        long start = System.currentTimeMillis();
        boolean teleported = false;

        while (System.currentTimeMillis() - start < 5000) {
            if (model.currentState == AutoMowerModel.State.CHARGING) {
                // Check position
                if (model.getCurrentPosition().distance(dock) < 1.0) {
                    teleported = true;
                    break;
                }
            }
            Thread.sleep(50);
        }

        Assertions.assertTrue(teleported, "Mower should have teleported to dock on battery empty");

        // Now test Fast Charging Time
        // Current state is CHARGING. We need to wait for it to become MOWING.
        // The simulation runs at 100x speed.
        // 3 seconds sim time = 0.03 seconds real time. That's too fast to measure
        // reliably with Thread.sleep(50).
        // Let's reduce timeScale for accurate timing or just check that it eventually
        // leaves.

        model.setTimeScale(10.0); // 10x speed. 3s sim = 0.3s real. Only charging timer counts up.
        // Wait for charging to finish
        start = System.currentTimeMillis();
        boolean charged = false;
        while (System.currentTimeMillis() - start < 2000) {
            if (model.currentState == AutoMowerModel.State.MOWING
                    && model.currentBatteryWh >= model.batteryCapacityWh - 1.0) {
                charged = true;
                break;
            }
            Thread.sleep(50);
        }

        model.stop();
        Assertions.assertTrue(charged, "Mower should have recharged and resumed mowing");
    }
}
