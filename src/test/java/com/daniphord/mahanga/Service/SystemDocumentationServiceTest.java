package com.daniphord.mahanga.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemDocumentationServiceTest {

    private final SystemDocumentationService systemDocumentationService =
            new SystemDocumentationService(new PdfBrandingService());

    @Test
    void adminDocumentsIncludeArchitectureDesignAndSrs() {
        var documents = systemDocumentationService.adminDocuments();

        assertTrue(documents.stream().anyMatch(item -> "system-architecture".equals(item.get("key"))));
        assertTrue(documents.stream().anyMatch(item -> "system-design-document".equals(item.get("key"))));
        assertTrue(documents.stream().anyMatch(item -> "system-requirements-specification".equals(item.get("key"))));
    }

    @Test
    void systemDesignDocumentPdfCanBeGenerated() {
        byte[] pdf = systemDocumentationService.generatePdf("system-design-document", "en");

        assertFalse(pdf.length == 0);
    }
}
