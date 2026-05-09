package com.tare.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PostProcessorServiceTest {

    @Autowired
    private PostProcessorService postProcessorService;

    @Test
    void process_emptyScript_returnsOriginal() {
        String input = "{\"code\":200,\"data\":{\"id\":\"1\"}}";
        String result = postProcessorService.process(input, "");
        assertEquals(input, result);
    }

    @Test
    void process_nullScript_returnsOriginal() {
        String input = "{\"code\":200,\"data\":{\"id\":\"1\"}}";
        String result = postProcessorService.process(input, null);
        assertEquals(input, result);
    }

    @Test
    void process_extractPath_returnsExtracted() {
        String input = "{\"code\":200,\"data\":{\"items\":[{\"id\":\"a\"}]}}";
        String result = postProcessorService.process(input, "extract:data.items");
        assertNotNull(result);
        assertTrue(result.contains("\"id\":\"a\""));
    }

    @Test
    void process_mapInstruction_renamesField() {
        String input = "{\"oldName\":\"value\"}";
        String result = postProcessorService.process(input, "map:oldName->newName");
        assertNotNull(result);
        assertTrue(result.contains("\"newName\":\"value\""));
    }
}
