package de.in.autoMower.sim;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ChargingResumptionTest {

    @Test
    public void testChargingResumption() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProjectData data = mapper.readValue(new File("tbs10.json"), ProjectData.class);

        GroundModel gm = new GroundModel();
        gm.calibration = data.calibration;
        gm.border = data.border.toMultiLine();
        gm.obstacles = data.obstacles.stream().map(ProjectData.MultiLineDTO::toMultiLine)
                .collect(java.util.stream.Collectors.toList());
        if (data.chargingStation != null) {
            gm.setChargingStation(data.chargingStation.toPoint());
            gm.ensureChargingStationInside();
        }

        Point2D dock = gm.getChargingStation();
        System.out.println("Dock position: " + dock);

        AutoMowerModel model = new AutoMowerModel();
        model.groundModel = gm;
        model.border = gm.getBorder();
        model.setTimeScale(100.0); // Faster simulation

        // Set battery to 5% to force seeking border -> charging
        if (data.mower != null) {
            model.batteryCapacityWh = data.mower.batteryCapacityWh;
        }
        model.currentBatteryWh = model.batteryCapacityWh * 0.05;

        // Manually force into CHARGING state to test resumption
        model.currentState = AutoMowerModel.State.CHARGING;
        model.isCharging = true;
        model.currentBatteryWh = model.batteryCapacityWh * 0.95; // Near full to charge quickly
        model.currentPosition = new Point2D.Double(395, 313);
        model.currentLine = new Line2D.Double(model.currentPosition, model.currentPosition);

        Thread mowerThread = new Thread(model::runMower);
        mowerThread.start();

        System.out.println("Forced into CHARGING state. Waiting for resumption...");
        boolean reachedCharging = true;

        Assertions.assertTrue(reachedCharging, "Should have reached CHARGING state");
        System.out.println("Reached CHARGING state as expected.");

        // Wait for it to charge and resume MOWING
        long start = System.currentTimeMillis();
        boolean resumedMowing = false;
        while (System.currentTimeMillis() - start < 20000) {
            if (model.currentState == AutoMowerModel.State.MOWING && !model.isCharging) {
                resumedMowing = true;
                break;
            }
            if (model.stopped) {
                System.out.println("Mower STOPPED during charging/resumption.");
                break;
            }
            Thread.sleep(100);
        }

        Assertions.assertTrue(resumedMowing, "Should have resumed MOWING state after charging");

        // Check if it's moving (trace growth)
        int initialPoints = model.line.getNumberOfPoints();
        Thread.sleep(2000);
        int finalPoints = model.line.getNumberOfPoints();

        Assertions.assertTrue(finalPoints > initialPoints, "Trace should grow after resumption");

        model.stop();
    }
}
