package com.github.vadimmiheev.vectordocs.storageservice.service;

import com.github.vadimmiheev.vectordocs.storageservice.dto.DocumentResponse;
import com.github.vadimmiheev.vectordocs.storageservice.entity.Document;
import com.github.vadimmiheev.vectordocs.storageservice.event.DocumentDeletedEvent;
import com.github.vadimmiheev.vectordocs.storageservice.event.DocumentUploadedEvent;
import com.github.vadimmiheev.vectordocs.storageservice.exception.InvalidFileTypeException;
import com.github.vadimmiheev.vectordocs.storageservice.exception.ResourceNotFoundException;
import com.github.vadimmiheev.vectordocs.storageservice.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentStorageServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<Document> documentCaptor;

    @Captor
    private ArgumentCaptor<DocumentUploadedEvent> eventCaptor;

    private DocumentStorageService service;

    @BeforeEach
    void setUp() {
        service = new DocumentStorageService(documentRepository, eventPublisher);
        // Set @Value fields manually since they're not initialized in unit tests
        service.setRootStoragePath("/tmp/test-storage");
        service.setStorageServiceHost("http://localhost:8080");
        service.setInternalHost("http://storage-service");
    }

    @Test
    void save_ValidPdfFile_ShouldSaveDocumentAndPublishEvent() throws IOException {
        // Given
        String userId = "123";
        String originalFilename = "document.pdf";
        String contentType = "application/pdf";
        long fileSize = 1024L;
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(originalFilename);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getSize()).thenReturn(fileSize);
        when(file.isEmpty()).thenReturn(false);
        doNothing().when(file).transferTo(any(Path.class));

        // When
        DocumentResponse response = service.save(userId, file);

        // Then
        assertNotNull(response);
        assertEquals(originalFilename, response.getName());
        assertEquals(fileSize, response.getSize());
        assertEquals(userId, response.getUserId());
        assertEquals(contentType, response.getContentType());
        assertEquals("uploaded", response.getStatus());

        // Verify repository save
        verify(documentRepository).save(documentCaptor.capture());
        Document savedDocument = documentCaptor.getValue();
        assertNotNull(savedDocument.getId());
        assertEquals(originalFilename, savedDocument.getName());
        assertEquals(fileSize, savedDocument.getSize());
        assertEquals(userId, savedDocument.getUserId());
        assertEquals(contentType, savedDocument.getContentType());
        assertNotNull(savedDocument.getPath());
        String rootStoragePath = "/tmp/test-storage";
        assertTrue(savedDocument.getPath().contains(rootStoragePath));
        assertTrue(savedDocument.getPath().contains(userId));
        assertTrue(savedDocument.getPath().contains(savedDocument.getId()));
        assertEquals("uploaded", savedDocument.getStatus());

        // Verify event publication
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        DocumentUploadedEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertNotNull(event.document());
        assertEquals(response.getId(), event.document().getId());
        assertEquals(response.getName(), event.document().getName());

        // Verify file was transferred
        verify(file).transferTo(any(Path.class));
    }

    @Test
    void save_ValidTxtFile_ShouldSaveDocument() throws IOException {
        // Given
        String userId = "456";
        String originalFilename = "notes.txt";
        String contentType = "text/plain";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn(originalFilename);
        when(file.getContentType()).thenReturn(contentType);
        when(file.getSize()).thenReturn(2048L);
        when(file.isEmpty()).thenReturn(false);
        doNothing().when(file).transferTo(any(Path.class));

        // When
        DocumentResponse response = service.save(userId, file);

        // Then
        assertNotNull(response);
        assertEquals(originalFilename, response.getName());
        assertEquals("text/plain", response.getContentType());
        verify(documentRepository).save(any(Document.class));
        verify(eventPublisher).publishEvent(any(DocumentUploadedEvent.class));
    }

    @Test
    void save_InvalidFileType_ShouldThrowInvalidFileTypeException() {
        // Given
        String userId = "123";
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("image.png");
        when(file.getContentType()).thenReturn("image/png");
        when(file.isEmpty()).thenReturn(false);

        // When & Then
        assertThrows(InvalidFileTypeException.class, () -> service.save(userId, file));
        verify(documentRepository, never()).save(any(Document.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void save_EmptyFile_ShouldThrowIllegalArgumentException() {
        // Given
        String userId = "123";
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(userId, file));
        verify(documentRepository, never()).save(any(Document.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void save_NullUserId_ShouldThrowIllegalArgumentException() {
        // Given
        String userId = null;
        MultipartFile file = mock(MultipartFile.class);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(userId, file));
        verify(documentRepository, never()).save(any(Document.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void save_BlankUserId_ShouldThrowIllegalArgumentException() {
        // Given
        String userId = "   ";
        MultipartFile file = mock(MultipartFile.class);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> service.save(userId, file));
        verify(documentRepository, never()).save(any(Document.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void save_FileTransferIOException_ShouldThrowRuntimeException() throws IOException {
        // Given
        String userId = "123";
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn("doc.pdf");
        lenient().when(file.getContentType()).thenReturn("application/pdf");
        lenient().when(file.getSize()).thenReturn(1024L);
        lenient().when(file.isEmpty()).thenReturn(false);
        doThrow(new IOException("Disk full")).when(file).transferTo(any(Path.class));

        // When & Then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.save(userId, file));
        assertEquals("Failed to store file", ex.getMessage());
        assertInstanceOf(IOException.class, ex.getCause());
        verify(documentRepository, never()).save(any(Document.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void save_EventPublicationFails_ShouldLogErrorButNotFail() throws IOException {
        // Given
        String userId = "123";
        MultipartFile file = mock(MultipartFile.class);
        lenient().when(file.getOriginalFilename()).thenReturn("doc.pdf");
        lenient().when(file.getContentType()).thenReturn("application/pdf");
        lenient().when(file.getSize()).thenReturn(1024L);
        lenient().when(file.isEmpty()).thenReturn(false);
        doNothing().when(file).transferTo(any(Path.class));
        doThrow(new RuntimeException("Event bus down")).when(eventPublisher).publishEvent(any(DocumentUploadedEvent.class));

        // When
        DocumentResponse response = service.save(userId, file);

        // Then - should still succeed despite event publication failure
        assertNotNull(response);
        verify(documentRepository).save(any(Document.class));
        // Event publication was attempted
        verify(eventPublisher).publishEvent(any(DocumentUploadedEvent.class));
    }

    @Test
    void getUserDocuments_ShouldReturnOnlyUserDocuments() {
        // Given
        String userId = "user1";
        Document doc1 = new Document();
        doc1.setId("id1");
        doc1.setUserId(userId);
        doc1.setName("doc1.pdf");
        Document doc2 = new Document();
        doc2.setId("id2");
        doc2.setUserId(userId);
        doc2.setName("doc2.txt");
        List<Document> userDocuments = List.of(doc1, doc2);
        when(documentRepository.findAllByUserIdOrderByCreatedAtDesc(userId)).thenReturn(userDocuments);

        // When
        List<DocumentResponse> result = service.getUserDocuments(userId);

        // Then
        assertEquals(2, result.size());
        assertEquals("id1", result.get(0).getId());
        assertEquals("id2", result.get(1).getId());
        verify(documentRepository).findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getById_UserOwnsDocument_ShouldReturnDocument() {
        // Given
        String userId = "user1";
        String docId = "doc123";
        Document doc = new Document();
        doc.setId(docId);
        doc.setUserId(userId);
        doc.setName("test.pdf");
        when(documentRepository.findByIdAndUserId(docId, userId)).thenReturn(Optional.of(doc));

        // When
        DocumentResponse response = service.getById(userId, docId);

        // Then
        assertNotNull(response);
        assertEquals(docId, response.getId());
        assertEquals(userId, response.getUserId());
        verify(documentRepository).findByIdAndUserId(docId, userId);
    }

    @Test
    void getById_UserDoesNotOwnDocument_ShouldThrowResourceNotFoundException() {
        // Given
        String userId = "user1";
        String docId = "doc123";
        when(documentRepository.findByIdAndUserId(docId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> service.getById(userId, docId));
        verify(documentRepository).findByIdAndUserId(docId, userId);
    }

    @Test
    void delete_UserOwnsDocument_ShouldDeleteAndReturnCount() {
        // Given
        String userId = "user1";
        String docId = "doc123";
        Document doc = new Document();
        doc.setId(docId);
        doc.setUserId(userId);
        doc.setPath("/tmp/storage/user1/doc123");
        when(documentRepository.findByIdAndUserId(docId, userId)).thenReturn(Optional.of(doc));
        when(documentRepository.deleteByIdAndUserId(docId, userId)).thenReturn(1L);

        // When
        long deletedCount = service.delete(userId, docId);

        // Then
        assertEquals(1L, deletedCount);
        verify(documentRepository).findByIdAndUserId(docId, userId);
        verify(documentRepository).deleteByIdAndUserId(docId, userId);
        verify(eventPublisher).publishEvent(any(DocumentDeletedEvent.class));
    }

    @Test
    void delete_UserDoesNotOwnDocument_ShouldThrowResourceNotFoundException() {
        // Given
        String userId = "user1";
        String docId = "doc123";
        when(documentRepository.findByIdAndUserId(docId, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> service.delete(userId, docId));
        verify(documentRepository).findByIdAndUserId(docId, userId);
        verify(documentRepository, never()).deleteByIdAndUserId(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}