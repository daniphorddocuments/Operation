package com.daniphord.mahanga.Config;

import com.daniphord.mahanga.Service.DashboardDefinitionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.time.LocalDateTime;

@ControllerAdvice(annotations = Controller.class)
public class WebExceptionHandler {

    private final DashboardDefinitionService dashboardDefinitionService;

    public WebExceptionHandler(DashboardDefinitionService dashboardDefinitionService) {
        this.dashboardDefinitionService = dashboardDefinitionService;
    }

    @ExceptionHandler(Exception.class)
    public String handleDashboardException(
            Exception exception,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session
    ) {
        String message = friendlyMessage(exception);
        String target = fallbackTarget(request.getRequestURI(), session);
        HttpStatus status = resolveStatus(exception);
        if (target.equals(request.getRequestURI())) {
            response.setStatus(status.value());
            request.setAttribute("status", status.value());
            request.setAttribute("error", status.getReasonPhrase());
            request.setAttribute("message", message);
            request.setAttribute("path", request.getRequestURI());
            request.setAttribute("timestamp", LocalDateTime.now());
            return "error";
        }
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("error", message);
        RequestContextUtils.saveOutputFlashMap(target, request, response);
        return "redirect:" + target;
    }

    private HttpStatus resolveStatus(Exception exception) {
        if (exception instanceof MissingServletRequestParameterException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (exception instanceof AccessDeniedException) {
            return HttpStatus.FORBIDDEN;
        }
        if (exception instanceof ResponseStatusException responseStatusException) {
            return HttpStatus.valueOf(responseStatusException.getStatusCode().value());
        }
        if (exception instanceof IllegalStateException illegalStateException
                && "Action not allowed for your role".equalsIgnoreCase(illegalStateException.getMessage())) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String fallbackTarget(String path, HttpSession session) {
        if (path == null || path.isBlank()) {
            return dashboardForSession(session);
        }
        if (path.startsWith("/control-room")) {
            return "/control-room/dashboard";
        }
        if (path.startsWith("/operations") || path.startsWith("/api/incidents") || path.startsWith("/api/equipment")) {
            return "/operations/dashboard";
        }
        return dashboardForSession(session);
    }

    private String dashboardForSession(HttpSession session) {
        Object roleValue = session.getAttribute("role");
        if (roleValue == null) {
            return "/login";
        }
        return dashboardDefinitionService.routeFor(roleValue.toString());
    }

    private String friendlyMessage(Exception exception) {
        if (exception instanceof MissingServletRequestParameterException missingParameterException) {
            return "Required form field is missing: " + missingParameterException.getParameterName();
        }
        if (exception instanceof DataIntegrityViolationException) {
            return "This action cannot be completed because the record is already linked to existing system data.";
        }
        if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            String message = exception.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }
        }
        return "The action could not be completed. Please try again from the dashboard.";
    }
}
