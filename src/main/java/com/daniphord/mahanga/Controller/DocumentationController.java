package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Service.SystemDocumentationService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/documents")
public class DocumentationController {

    private final SystemDocumentationService systemDocumentationService;

    public DocumentationController(SystemDocumentationService systemDocumentationService) {
        this.systemDocumentationService = systemDocumentationService;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(systemDocumentationService.adminDocuments());
    }

    @GetMapping("/{key}/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable String key, @RequestParam(name = "lang", defaultValue = "en") String lang) {
        try {
            byte[] pdf = systemDocumentationService.generatePdf(key, lang);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + key + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(new ByteArrayResource(pdf));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<?> metadata(@PathVariable String key) {
        return ResponseEntity.ok(systemDocumentationService.adminDocuments().stream()
                .filter(item -> key.equals(item.get("key")))
                .findFirst()
                .orElse(Map.of("error", "Document not found")));
    }
}
