package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.LoginCarouselSlide;
import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.AuditService;
import com.daniphord.mahanga.Service.LoginCarouselSlideService;
import com.daniphord.mahanga.Service.RoleAccessService;
import com.daniphord.mahanga.Util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class LoginCarouselAdminController {

    private final LoginCarouselSlideService loginCarouselSlideService;
    private final AuditService auditService;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;

    public LoginCarouselAdminController(
            LoginCarouselSlideService loginCarouselSlideService,
            AuditService auditService,
            UserRepository userRepository,
            RoleAccessService roleAccessService
    ) {
        this.loginCarouselSlideService = loginCarouselSlideService;
        this.auditService = auditService;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping("/api/admin/login-carousel")
    public ResponseEntity<?> slides(HttpSession session) {
        if (!canManageLoginCarousel(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(loginCarouselSlideService.adminSlides());
    }

    @PostMapping("/api/admin/login-carousel")
    public ResponseEntity<?> uploadSlide(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "subtitle", required = false) String subtitle,
            @RequestParam(value = "displayOrder", required = false) Integer displayOrder,
            @RequestParam(value = "targetPage", required = false) String targetPage,
            @RequestParam(value = "active", defaultValue = "true") Boolean active,
            HttpSession session,
            HttpServletRequest request
    ) {
        if (!canManageLoginCarousel(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            LoginCarouselSlide slide = loginCarouselSlideService.createSlide(title, subtitle, displayOrder, active, targetPage, image);
            auditService.logAction(
                    currentUser(session),
                    "LOGIN_CAROUSEL_SLIDE_CREATED",
                    "Uploaded showcase image slide for " + (slide.getTargetPage() == null ? "LOGIN" : slide.getTargetPage()),
                    "LoginCarousel",
                    slide.getId(),
                    RequestUtil.getClientIpAddress(request)
            );
            return ResponseEntity.ok(Map.of("status", "ok", "id", slide.getId()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        } catch (Exception exception) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Unable to upload showcase image"));
        }
    }

    @DeleteMapping("/api/admin/login-carousel/{slideId}")
    public ResponseEntity<?> deleteSlide(@PathVariable Long slideId, HttpSession session, HttpServletRequest request) {
        if (!canManageLoginCarousel(session)) {
            return ResponseEntity.status(403).body(Map.of("error", "Action not allowed for your role"));
        }
        try {
            loginCarouselSlideService.deleteSlide(slideId);
            auditService.logAction(
                    currentUser(session),
                    "LOGIN_CAROUSEL_SLIDE_DELETED",
                    "Deleted showcase image slide",
                    "LoginCarousel",
                    slideId,
                    RequestUtil.getClientIpAddress(request)
            );
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/media/login-carousel/{slideId}")
    public ResponseEntity<Resource> image(@PathVariable Long slideId) {
        Resource resource = loginCarouselSlideService.imageResource(slideId);
        String contentType = loginCarouselSlideService.imageContentType(slideId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }

    private boolean canManageLoginCarousel(HttpSession session) {
        User user = currentUser(session);
        return roleAccessService.canManageSystemSettings(user) || roleAccessService.canManageRolePermissions(user);
    }
}
