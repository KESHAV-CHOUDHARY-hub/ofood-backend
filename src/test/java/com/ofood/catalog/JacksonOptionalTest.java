package com.ofood.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@JsonTest
public class JacksonOptionalTest {

    @Autowired
    private ObjectMapper objectMapper;

    static class Dto {
        public Optional<String> field;
    }

    @Test
    public void testOptionalDeserialization() throws Exception {
        // Case 1: Field absent
        String jsonAbsent = "{}";
        Dto dtoAbsent = objectMapper.readValue(jsonAbsent, Dto.class);
        assertNull(dtoAbsent.field, "Absent field must remain a null reference");

        // Case 2: Explicit null
        String jsonExplicitNull = "{\"field\": null}";
        Dto dtoExplicitNull = objectMapper.readValue(jsonExplicitNull, Dto.class);
        assertNotNull(dtoExplicitNull.field, "Explicit null must not be a null reference");
        assertFalse(dtoExplicitNull.field.isPresent(), "Explicit null must be Optional.empty()");

        // Case 3: Supplied value
        String jsonSupplied = "{\"field\": \"value\"}";
        Dto dtoSupplied = objectMapper.readValue(jsonSupplied, Dto.class);
        assertNotNull(dtoSupplied.field, "Supplied value must not be a null reference");
        assertTrue(dtoSupplied.field.isPresent(), "Supplied value must be present");
        assertEquals("value", dtoSupplied.field.get(), "Supplied value must match");
    }
}
