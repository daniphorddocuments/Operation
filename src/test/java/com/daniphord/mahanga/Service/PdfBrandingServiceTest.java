package com.daniphord.mahanga.Service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PdfBrandingServiceTest {

    @Test
    void brandingStateExposesNoSignatureArtifacts() {
        PdfBrandingService pdfBrandingService = new PdfBrandingService();

        Map<String, String> branding = pdfBrandingService.currentBranding();

        assertEquals("", branding.get("signatureText"));
        assertEquals("", branding.get("signatureFooterDataUri"));
        assertFalse(branding.get("fireLogoDataUri").isBlank());
        assertFalse(branding.get("tanzaniaLogoDataUri").isBlank());
    }

    @Test
    void generatePdfStillProducesOutputWithoutSignatureFooter() {
        PdfBrandingService pdfBrandingService = new PdfBrandingService();

        byte[] pdf = pdfBrandingService.generatePdf("Test Document", "<p>Body content</p>", "en");

        assertFalse(pdf.length == 0);
    }
}
