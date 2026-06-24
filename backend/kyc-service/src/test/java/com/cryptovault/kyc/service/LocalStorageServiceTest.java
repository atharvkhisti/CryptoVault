package com.cryptovault.kyc.service;

import com.cryptovault.kyc.service.impl.LocalStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying LocalStorageService storage strategy logic.
 */
class LocalStorageServiceTest {

    private LocalStorageService storageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString());
    }

    @Test
    void testStoreAndRetrieveSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan_card.jpg",
                "image/jpeg",
                "Mock JPG File Data".getBytes()
        );

        String path = storageService.store(file, "user-uuid-1", "pan_card.jpg");
        assertNotNull(path);
        assertTrue(path.contains("user-uuid-1"));
        assertTrue(path.contains("pan_card.jpg"));

        byte[] retrieved = storageService.retrieve(path);
        assertArrayEquals("Mock JPG File Data".getBytes(), retrieved);
    }

    @Test
    void testDeleteSuccess() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "aadhaar.pdf",
                "application/pdf",
                "Mock PDF File Data".getBytes()
        );

        String path = storageService.store(file, "user-uuid-2", "aadhaar.pdf");
        assertNotNull(path);

        storageService.delete(path);

        assertThrows(NoSuchFileException.class, () -> storageService.retrieve(path));
    }

    @Test
    void testTraversalAttackSecurity() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "attack.pdf",
                "application/pdf",
                "Attack Data".getBytes()
        );

        assertThrows(SecurityException.class, () -> 
            storageService.store(file, "../outside", "attack.pdf")
        );
    }
}
