package com.github.vadimmiheev.vectordocs.storageservice.controller;

import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;
import com.github.vadimmiheev.vectordocs.storageservice.service.DocumentStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(controllers = DocumentController.class, properties = "DEMO_USER_ID=999")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentStorageService storageService;

    private final String regularUserId = "123";
    private final String demoUserId = "999"; // must match property

    @Test
    void listDocuments_RegularUser_ShouldReturnDocuments() throws Exception {
        DocumentResponse doc1 = new DocumentResponse("id1", "doc1.pdf", 1024L, regularUserId,
                "application/pdf", Instant.now(), "http://example.com/download", "uploaded");
        DocumentResponse doc2 = new DocumentResponse("id2", "doc2.txt", 2048L, regularUserId,
                "text/plain", Instant.now(), "http://example.com/download", "uploaded");
        when(storageService.getUserDocuments(regularUserId)).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/documents")
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("id1"))
                .andExpect(jsonPath("$[1].id").value("id2"));
    }

    @Test
    void listDocuments_DemoUser_ShouldReturnDocuments() throws Exception {
        DocumentResponse doc = new DocumentResponse("id3", "demo.pdf", 1024L, demoUserId,
                "application/pdf", Instant.now(), "http://example.com/download", "uploaded");
        when(storageService.getUserDocuments(demoUserId)).thenReturn(List.of(doc));

        mockMvc.perform(get("/documents")
                        .header("X-User-Id", demoUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("id3"));
    }

    @Test
    void getById_RegularUser_ShouldReturnDocument() throws Exception {
        DocumentResponse doc = new DocumentResponse("id1", "doc.pdf", 1024L, regularUserId,
                "application/pdf", Instant.now(), "http://example.com/download", "uploaded");
        when(storageService.getById(regularUserId, "id1")).thenReturn(doc);

        mockMvc.perform(get("/documents/id1")
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("id1"))
                .andExpect(jsonPath("$.name").value("doc.pdf"));
    }

    @Test
    void upload_RegularUser_ShouldSuccess() throws Exception {
        DocumentResponse response = new DocumentResponse("newId", "test.pdf", 1024L, regularUserId,
                "application/pdf", Instant.now(), "http://example.com/download", "uploaded");
        when(storageService.save(eq(regularUserId), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                MediaType.APPLICATION_PDF_VALUE, "test content".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/documents")
                        .file(file)
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("newId"));
    }

    @Test
    void upload_DemoUser_ShouldReturnForbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf",
                MediaType.APPLICATION_PDF_VALUE, "test content".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/documents")
                        .file(file)
                        .header("X-User-Id", demoUserId))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden())
                .andDo(result -> verify(storageService, never()).save(any(), any()));
    }

    @Test
    void delete_RegularUser_ShouldSuccess() throws Exception {
        when(storageService.delete(regularUserId, "id1")).thenReturn(1L);

        mockMvc.perform(delete("/documents/id1")
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_DemoUser_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/documents/id1")
                        .header("X-User-Id", demoUserId))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("Demo user is not allowed to delete documents")));
    }

    @Test
    void delete_RegularUser_NotFound_ShouldReturnInternalServerError() throws Exception {
        when(storageService.delete(regularUserId, "id1")).thenReturn(0L);

        mockMvc.perform(delete("/documents/id1")
                        .header("X-User-Id", regularUserId))
                .andExpect(status().isInternalServerError());
    }

    // Note: Download endpoint is not restricted for demo users, but requires file system mocking.
}