package com.tare.controller;

import com.tare.model.InterfaceBinding;
import com.tare.store.MockDataStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BindingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockDataStore dataStore;

    @Test
    void list_returnsBindings() throws Exception {
        mockMvc.perform(get("/api/bindings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void execute_triggersExecutionAndCreatesLog() throws Exception {
        InterfaceBinding binding = dataStore.getBinding("bind-001");
        assertNotNull(binding);

        int logsBefore = dataStore.getExecutionLogs().size();

        mockMvc.perform(post("/api/bindings/bind-001/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("执行成功"));

        assertEquals(logsBefore + 1, dataStore.getExecutionLogs().size());
    }

    @Test
    void execute_notFound_returns404() throws Exception {
        mockMvc.perform(post("/api/bindings/non-existent/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
