package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.LoginCarouselSlide;
import com.daniphord.mahanga.Repositories.LoginCarouselSlideRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LoginCarouselSlideService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/png", "image/jpeg", "image/webp");

    private final LoginCarouselSlideRepository repository;
    private final Path storagePath;

    public LoginCarouselSlideService(
            LoginCarouselSlideRepository repository,
            @Value("${froms.storage.login-carousel-dir:login-carousel}") String storageDirectory
    ) {
        this.repository = repository;
        this.storagePath = Paths.get(storageDirectory).toAbsolutePath().normalize();
    }

    public List<LoginCarouselSlide> activeSlides() {
        return repository.findByActiveTrueOrderByDisplayOrderAscIdAsc();
    }

    public List<Map<String, Object>> adminSlides() {
        return repository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(this::toAdminPayload)
                .collect(Collectors.toList());
    }

    public LoginCarouselSlide createSlide(String title, String subtitle, Integer displayOrder, Boolean active, String targetPage, MultipartFile image) {
        validateImage(image);
        try {
            Files.createDirectories(storagePath);
            String extension = resolveExtension(image.getOriginalFilename(), image.getContentType());
            String storedFilename = System.currentTimeMillis() + "-" + UUID.randomUUID() + extension;
            Path target = storagePath.resolve(storedFilename).normalize();
            Files.copy(image.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            LoginCarouselSlide slide = new LoginCarouselSlide();
            slide.setTitle(normalizeTitle(title));
            slide.setSubtitle(normalizeSubtitle(subtitle));
            slide.setDisplayOrder(displayOrder == null ? 0 : displayOrder);
            slide.setActive(active == null || active);
            slide.setTargetPage(normalizeTargetPage(targetPage));
            slide.setImageFilename(storedFilename);
            slide.setContentType(normalizeContentType(image.getContentType()));
            slide.setCreatedAt(LocalDateTime.now());
            return repository.save(slide);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to store carousel image", exception);
        }
    }

    public void deleteSlide(Long slideId) {
        LoginCarouselSlide slide = repository.findById(slideId)
                .orElseThrow(() -> new IllegalArgumentException("Carousel slide not found"));
        repository.delete(slide);
        try {
            Files.deleteIfExists(storagePath.resolve(slide.getImageFilename()).normalize());
        } catch (IOException ignored) {
            // Preserve database operation even if file cleanup fails.
        }
    }

    public Resource imageResource(Long slideId) {
        LoginCarouselSlide slide = repository.findById(slideId)
                .orElseThrow(() -> new IllegalArgumentException("Carousel slide not found"));
        Path imagePath = storagePath.resolve(slide.getImageFilename()).normalize();
        if (!Files.exists(imagePath)) {
            throw new IllegalArgumentException("Carousel image file not found");
        }
        return new FileSystemResource(imagePath);
    }

    public String imageContentType(Long slideId) {
        return repository.findById(slideId)
                .map(LoginCarouselSlide::getContentType)
                .orElse("application/octet-stream");
    }

    public List<Map<String, Object>> loginSlides() {
        return slidesForPage("LOGIN").stream()
                .map(slide -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", slide.getId());
                    payload.put("title", slide.getTitle());
                    payload.put("subtitle", slide.getSubtitle() == null ? "" : slide.getSubtitle());
                    payload.put("imageUrl", "/media/login-carousel/" + slide.getId());
                    return payload;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> landingSlides() {
        return slidesForPage("LANDING").stream()
                .map(slide -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", slide.getId());
                    payload.put("title", slide.getTitle());
                    payload.put("subtitle", slide.getSubtitle() == null ? "" : slide.getSubtitle());
                    payload.put("imageUrl", "/media/login-carousel/" + slide.getId());
                    return payload;
                })
                .collect(Collectors.toList());
    }

    public List<LoginCarouselSlide> slidesForPage(String targetPage) {
        return repository.findByActiveTrueAndTargetPageOrderByDisplayOrderAscIdAsc(normalizeTargetPage(targetPage));
    }

    private Map<String, Object> toAdminPayload(LoginCarouselSlide slide) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", slide.getId());
        payload.put("title", slide.getTitle());
        payload.put("subtitle", slide.getSubtitle() == null ? "" : slide.getSubtitle());
        payload.put("displayOrder", slide.getDisplayOrder());
        payload.put("active", Boolean.TRUE.equals(slide.getActive()));
        payload.put("targetPage", slide.getTargetPage() == null ? "LOGIN" : slide.getTargetPage());
        payload.put("createdAt", slide.getCreatedAt() == null ? "" : slide.getCreatedAt().toString());
        payload.put("imageUrl", "/media/login-carousel/" + slide.getId());
        return payload;
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }
        if (image.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Image file is too large");
        }
        String contentType = normalizeContentType(image.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only PNG, JPG, and WEBP images are allowed");
        }
    }

    private String normalizeTitle(String title) {
        String value = title == null ? "" : title.trim();
        if (value.isBlank()) {
            return "System Showcase";
        }
        return value.length() > 160 ? value.substring(0, 160) : value;
    }

    private String normalizeSubtitle(String subtitle) {
        if (subtitle == null) {
            return "";
        }
        String value = subtitle.trim();
        return value.length() > 600 ? value.substring(0, 600) : value;
    }

    private String normalizeContentType(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "application/octet-stream" : normalized;
    }

    private String normalizeTargetPage(String targetPage) {
        String normalized = targetPage == null ? "" : targetPage.trim().toUpperCase(Locale.ROOT);
        return "LANDING".equals(normalized) ? "LANDING" : "LOGIN";
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String cleanedName = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int extensionIndex = cleanedName.lastIndexOf('.');
        if (extensionIndex >= 0) {
            String extension = cleanedName.substring(extensionIndex).toLowerCase(Locale.ROOT);
            if (extension.matches("\\.(png|jpg|jpeg|webp)")) {
                return extension;
            }
        }
        return switch (normalizeContentType(contentType)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
