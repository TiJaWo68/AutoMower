package de.in.autoMower.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ModelVersionPersistenceTest {

    @Test
    public void testV2Persistence(@TempDir File tempDir) throws IOException {
        // Create V2 model
        AbstractAutoMowerModel v2 = new AutoMowerModelV2();
        v2.setSpeedInCmPerSec(42.0);

        // Serialize
        ProjectData.MowerDTO dto = new ProjectData.MowerDTO(v2);
        assertEquals(2, dto.version, "DTO should capture version 2");

        ProjectData project = new ProjectData();
        project.mower = dto;

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(tempDir, "project.json");
        mapper.writeValue(file, project);

        // Deserialize
        ProjectData loadedData = mapper.readValue(file, ProjectData.class);
        assertEquals(2, loadedData.mower.version, "Loaded version should be 2");

        // Simulate App logic
        AbstractAutoMowerModel loadedMower;
        if (loadedData.mower.version == 2) {
            loadedMower = new AutoMowerModelV2();
        } else {
            loadedMower = new AutoMowerModel();
        }

        assertTrue(loadedMower instanceof AutoMowerModelV2, "Should instantiate V2");
    }

    @Test
    public void testV1Persistence(@TempDir File tempDir) throws IOException {
        // Create V1 model
        AbstractAutoMowerModel v1 = new AutoMowerModel();

        // Serialize
        ProjectData.MowerDTO dto = new ProjectData.MowerDTO(v1);
        assertEquals(1, dto.version, "DTO should capture version 1 (default)");

        ProjectData project = new ProjectData();
        project.mower = dto;

        ObjectMapper mapper = new ObjectMapper();
        File file = new File(tempDir, "project_v1.json");
        mapper.writeValue(file, project);

        // Deserialize
        ProjectData loadedData = mapper.readValue(file, ProjectData.class);
        assertEquals(1, loadedData.mower.version, "Loaded version should be 1");
    }
}
