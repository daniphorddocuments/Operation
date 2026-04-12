package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.AuditService;
import com.daniphord.mahanga.Service.PdfBrandingService;
import com.daniphord.mahanga.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/branding")
public class BrandingController {

    private final PdfBrandingService pdfBrandingService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public BrandingController(PdfBrandingService pdfBrandingService, AuditService auditService, UserRepository userRepository) {
        this.pdfBrandingService = pdfBrandingService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public Map<String, String> currentBranding() {
        return pdfBrandingService.currentBranding();
    }

    @PutMapping("/signature")
    public ResponseEntity<Map<String, String>> updateSignature(@RequestBody Map<String, String> requestBody, HttpSession session, HttpServletRequest request) {
        String signatureText = requestBody.getOrDefault("signatureText", "");
        if (signatureText == null || signatureText.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "signatureText is required"));
        }
        pdfBrandingService.updateSignature(signatureText);
        auditService.logAction(currentUser(session), "BRANDING_SIGNATURE_UPDATED", "Updated PDF digital signature", "Branding", null, RequestUtil.getClientIpAddress(request));
        return ResponseEntity.ok(Map.of("signatureText", signatureText.trim()));
    }

    @PutMapping("/signature-footer")
    public ResponseEntity<Map<String, String>> updateSignatureFooter(@RequestBody Map<String, String> requestBody, HttpSession session, HttpServletRequest request) {
        String signatureFooterBase64 = requestBody.getOrDefault("signatureFooterBase64", "");
        if (signatureFooterBase64 == null || signatureFooterBase64.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "signatureFooterBase64 is required"));
        }
        pdfBrandingService.updateSignatureFooter(signatureFooterBase64);
        auditService.logAction(currentUser(session), "BRANDING_SIGNATURE_FOOTER_UPDATED", "Updated PDF signature footer block", "Branding", null, RequestUtil.getClientIpAddress(request));
        return ResponseEntity.ok(pdfBrandingService.currentBranding());
    }

    @PutMapping("/logos")
    public ResponseEntity<Map<String, String>> updateLogos(@RequestBody Map<String, String> requestBody, HttpSession session, HttpServletRequest request) {
        String fireLogoBase64 = requestBody.get("fireLogoBase64");
        String tanzaniaLogoBase64 = requestBody.get("tanzaniaLogoBase64");
        if ((fireLogoBase64 == null || fireLogoBase64.isBlank()) && (tanzaniaLogoBase64 == null || tanzaniaLogoBase64.isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Provide fireLogoBase64 or tanzaniaLogoBase64"));
        }
        pdfBrandingService.updateLogos(fireLogoBase64, tanzaniaLogoBase64);
        auditService.logAction(currentUser(session), "BRANDING_LOGO_UPDATED", "Updated PDF branding logos", "Branding", null, RequestUtil.getClientIpAddress(request));
        return ResponseEntity.ok(pdfBrandingService.currentBranding());
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }
}
