package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.User;
import com.daniphord.mahanga.Repositories.UserRepository;
import com.daniphord.mahanga.Service.AiRouteService;
import com.daniphord.mahanga.Service.RoleAccessService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping
public class RouteController {

    private final AiRouteService aiRouteService;
    private final UserRepository userRepository;
    private final RoleAccessService roleAccessService;

    public RouteController(AiRouteService aiRouteService, UserRepository userRepository, RoleAccessService roleAccessService) {
        this.aiRouteService = aiRouteService;
        this.userRepository = userRepository;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping("/api/routes/calls/{callId}")
    @ResponseBody
    public ResponseEntity<?> callRoute(@PathVariable Long callId, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canViewMap(currentUser)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(aiRouteService.generateAndStoreForCall(callId));
    }

    @GetMapping("/api/routes/incidents/{incidentId}")
    @ResponseBody
    public ResponseEntity<?> incidentRoute(@PathVariable Long incidentId, HttpSession session) {
        User currentUser = currentUser(session);
        if (!roleAccessService.canViewMap(currentUser)) {
            return ResponseEntity.status(403).body(java.util.Map.of("error", "Action not allowed for your role"));
        }
        return ResponseEntity.ok(aiRouteService.generateAndStoreForIncident(incidentId));
    }

    @GetMapping("/routes/print/calls/{callId}")
    public String printCallRoute(@PathVariable Long callId, Model model, HttpSession session) {
        if (!roleAccessService.canViewMap(currentUser(session))) {
            throw new IllegalStateException("Action not allowed for your role");
        }
        model.addAttribute("pageTitle", "Printable Call Route");
        model.addAttribute("routeRecommendation", aiRouteService.generateAndStoreForCall(callId));
        return "route-print";
    }

    @GetMapping("/routes/print/incidents/{incidentId}")
    public String printIncidentRoute(@PathVariable Long incidentId, Model model, HttpSession session) {
        if (!roleAccessService.canViewMap(currentUser(session))) {
            throw new IllegalStateException("Action not allowed for your role");
        }
        model.addAttribute("pageTitle", "Printable Incident Route");
        model.addAttribute("routeRecommendation", aiRouteService.generateAndStoreForIncident(incidentId));
        return "route-print";
    }

    private User currentUser(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof Long id) {
            return userRepository.findById(id).orElse(null);
        }
        return null;
    }
}
