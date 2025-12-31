package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Test;

class CalibrationRegressionTest {

	@Test
	void testCalibrationChangeUpdatesArea() throws Exception {
		// 1. Load tbs10.json
		File file = new File("src/test/resources/tbs10.json");
		assertTrue(file.exists(), "tbs10.json should exist");

		App app = new App();
		// Note: App constructor initializes UI, might need Headless handling or just
		// test logic if possible. App.loadProject is what we
		// want to test.

		app.loadProject(file);

		GroundModel model = app.getGroundModel();
		AbstractAutoMowerModel mower = app.getMower();

		assertNotNull(model);
		assertNotNull(mower);

		// 2. Determine Initial Area
		double initialArea = model.getNetArea();
		double initialCalibration = model.getCalibration();
		System.out.println("current Calib: " + initialCalibration);
		System.out.println("current Area: " + initialArea);
		// AND fix: model.setCalibration automatically affects logic (since mower reads
		// from model)

		// Let's assume we change calibration factor directly for test simplified 1px =
		// 10cm -> 1px = 1cm (Factor 10 change)
		double newCalibration = initialCalibration / 10.0;

		model.setCalibration(newCalibration);

		// CRITICAL: This is what my UI fix does manually. behavior should be tested. If
		// the bug is "grid doesn't update", it implies
		// AreaVisualizationPanel isn't seeing the new value. AreaVisualizationPanel
		// reads from 'model'.

		// Verify Model Area changed
		double newArea = model.getNetArea();
		System.out.println("New Calib: " + newCalibration);
		System.out.println("New Area: " + newArea);

		// Since NetArea = px * cal * cal, reducing cal by 10 => Area reduces by 100.
		assertNotEquals(initialArea, newArea, 0.1, "Area should change after calibration update");

		// 4. Verify Mower Scale Update (Simulating the fix propagation) If we only call
		// model.setCalibration, mower is NOT updated
		// automatically (unless we fixed GroundModel). The user says "It is still the
		// same raster". AreaVisualizationPanel uses
		// 'model.getCalibration()'. If model.calibration is updated,
		// AreaVisualizationPanel (which is created NEW every time "Visualize" is
		// clicked) should see it.

		// Wait, does 'getNetArea' use correct calibration? Yes: return netAreaPx *
		// calibration * calibration;

		// Why does user say "Raster is same"? Maybe 'tbs10.json' loads differently? Or
		// maybe AreaVisualizationPanel uses a cached model?

	}
}
