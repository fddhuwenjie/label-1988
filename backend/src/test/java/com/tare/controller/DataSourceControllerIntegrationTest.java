package com.tare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tare.dto.DataSourceDTO;
import com.tare.model.DataSourceInterface;
import com.tare.store.MockDataStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DataSourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockDataStore dataStore;

    @Test
    void list_returnsDataSources() throws Exception {
        mockMvc.perform(get("/api/data-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void create_andUpdate_preservesCreatedAt() throws Exception {
        DataSourceDTO dto = new DataSourceDTO();
        dto.setName("集成测试数据源");
        dto.setUrl("https://api.example.com/test");
        dto.setMethod("GET");

        String createResponse = mockMvc.perform(post("/api/data-sources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var createResult = objectMapper.readTree(createResponse);
        String id = createResult.get("data").get("id").asText();
        String createdAt = createResult.get("data").get("createdAt").asText();

        dto.setName("更新后的名称");
        mockMvc.perform(put("/api/data-sources/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createdAt").value(createdAt));

        DataSourceInterface updated = dataStore.getDataSourceInterface(id);
        assertNotNull(updated.getCreatedAt());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/data-sources/non-existent-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }
}
