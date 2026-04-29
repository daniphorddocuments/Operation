package com.daniphord.mahanga.Config;

import com.daniphord.mahanga.Service.SecurityIntelligenceService;
import com.daniphord.mahanga.Util.OperationRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Enhanced Security Configuration with:
 * - CSRF protection
 * - Role-based access control
 * - CORS configuration
 * - Security headers
 * - Session management
 */
@Configuration
public class SecurityConfig {

    private final List<String> allowedOrigins;
    private final boolean csrfEnabled;

    public SecurityConfig(
            @Value("${froms.security.allowed-origins:http://localhost,http://localhost:8080,http://localhost:3000}") String allowedOrigins,
            @Value("${froms.security.csrf-enabled:true}") boolean csrfEnabled
    ) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        this.csrfEnabled = csrfEnabled;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityIntelligenceService securityIntelligenceService) throws Exception {
        http
                // Enable CSRF protection with secure cookie storage
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                        .ignoringRequestMatchers("/change-language")
                );

        if (!csrfEnabled) {
            http.csrf(csrf -> csrf.disable());
        }

        http
                // Security headers for enhanced protection
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; connect-src 'self' ws: wss:; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; font-src 'self' https://cdnjs.cloudflare.com https://fonts.gstatic.com data:; img-src 'self' data: blob:; media-src 'self' blob: data:; frame-src 'self' https://www.openstreetmap.org https://www.google.com https://maps.google.com;"))
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                )
                // Authorize requests based on role
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/logout", "/error", "/change-language", "/ui/**", "/css/**", "/js/**", "/images/**", "/webjars/**", "/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/geography/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/signal").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/reports/*", "/api/public/reports/*/messages").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/reports/*/messages", "/api/video/public/reports/*/sessions/start", "/api/video/public/reports/*/sessions/*/end").permitAll()
                        .requestMatchers("/control-room/**", "/api/control-room/**").authenticated()
                        .requestMatchers("/operations/**", "/api/incidents/**", "/api/equipment/**").authenticated()
                        .requestMatchers("/api/investigations/**").authenticated()
                        .requestMatchers("/dashboard", "/api/video/**", "/signal").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/system-tests/**").authenticated()
                        .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/documents/**").authenticated()
                        .requestMatchers("/api/admin/branding/**").authenticated()
                        .requestMatchers("/api/admin/login-carousel/**").authenticated()
                        .requestMatchers("/api/admin/role-permissions/**").authenticated()
                        .requestMatchers("/api/admin/audit/**").authenticated()
                        .requestMatchers("/api/admin/security/**").authenticated()
                        .requestMatchers("/media/login-carousel/**").permitAll()
                        .requestMatchers("/api/reports/**").authenticated()
                        .anyRequest().authenticated()
                )
                // Session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/login?session=expired")
                        .sessionFixation(Customizer.withDefaults())
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            ErrorDescriptor descriptor = describeAuthenticationFailure(request);
                            renderError(
                                    request,
                                    response,
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    descriptor.error(),
                                    descriptor.message(),
                                    request.getRequestURI(),
                                    descriptor.reasonCode(),
                                    descriptor.nextStep()
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            ErrorDescriptor descriptor = describeAccessDenied(request, accessDeniedException instanceof CsrfException);
                            renderError(
                                    request,
                                    response,
                                    HttpServletResponse.SC_FORBIDDEN,
                                    descriptor.error(),
                                    descriptor.message(),
                                    request.getRequestURI(),
                                    descriptor.reasonCode(),
                                    descriptor.nextStep()
                            );
                        })
                )
                // Disable form login and basic auth (using custom login controller)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // CORS configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .addFilterBefore(sessionAuthenticationFilter(securityIntelligenceService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    OncePerRequestFilter sessionAuthenticationFilter(SecurityIntelligenceService securityIntelligenceService) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {
                var httpSession = request.getSession(false);
                Object userId = httpSession != null ? httpSession.getAttribute("userId") : null;
                Object username = httpSession != null ? httpSession.getAttribute("username") : null;
                Object role = httpSession != null ? httpSession.getAttribute("role") : null;

                if (userId != null && username != null && role != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    String normalizedRole = normalizeRole(role.toString());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username.toString(),
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

                if (httpSession != null && securityIntelligenceService != null && userId != null) {
                    Object refreshToken = httpSession.getAttribute(SecurityIntelligenceService.SESSION_REFRESH_TOKEN);
                    if (refreshToken != null) {
                        securityIntelligenceService.keepAlive(httpSession, String.valueOf(refreshToken));
                    }
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    private String normalizeRole(String role) {
        return OperationRole.normalizeRole(role);
    }

    private void renderError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String error,
            String message,
            String path,
            String reasonCode,
            String nextStep
    ) throws IOException, ServletException {
        response.setStatus(status);
        if (wantsJsonResponse(request)) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(jsonError(status, error, message, path, reasonCode, nextStep));
            return;
        }
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, message);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, path);
        request.setAttribute("status", status);
        request.setAttribute("error", error);
        request.setAttribute("message", message);
        request.setAttribute("path", path);
        request.setAttribute("reasonCode", reasonCode);
        request.setAttribute("nextStep", nextStep);
        request.setAttribute("timestamp", LocalDateTime.now());
        request.getRequestDispatcher("/error").forward(request, response);
    }

    private ErrorDescriptor describeAuthenticationFailure(HttpServletRequest request) {
        if (request.getRequestedSessionId() != null && !request.isRequestedSessionIdValid()) {
            return new ErrorDescriptor(
                    "Session Expired",
                    "The session identifier sent with this request is no longer valid. The session expired or was invalidated before accessing " + request.getRequestURI() + ".",
                    "SESSION_EXPIRED",
                    "Sign in again, then retry the page or action."
            );
        }
        if (request.getSession(false) == null) {
            return new ErrorDescriptor(
                    "No Active Session",
                    "No authenticated session was found for this protected request. You opened a protected page without signing in first.",
                    "NO_ACTIVE_SESSION",
                    "Sign in first, then open the requested page again."
            );
        }
        if (request.getSession(false).getAttribute("userId") == null) {
            return new ErrorDescriptor(
                    "Incomplete Session",
                    "A session exists, but the required user identity is missing from it. The authentication state is incomplete.",
                    "MISSING_SESSION_IDENTITY",
                    "Sign out if needed, then sign in again to create a fresh session."
            );
        }
        return new ErrorDescriptor(
                "Authentication Required",
                "This request reached a protected resource without a valid authenticated security context.",
                "AUTHENTICATION_REQUIRED",
                "Sign in again and retry the requested page."
        );
    }

    private ErrorDescriptor describeAccessDenied(HttpServletRequest request, boolean csrfFailure) {
        if (csrfFailure) {
            return new ErrorDescriptor(
                    "Security Token Invalid",
                    "The request was rejected because the CSRF/security token was missing, expired, or invalid.",
                    "CSRF_VALIDATION_FAILED",
                    "Refresh the page and submit the action again."
            );
        }
        Object role = request.getSession(false) != null ? request.getSession(false).getAttribute("role") : null;
        String currentRole = role == null ? "UNKNOWN" : role.toString();
        String authorities = SecurityContextHolder.getContext().getAuthentication() == null
                ? "NONE"
                : SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return new ErrorDescriptor(
                "Access Denied",
                "Your current role '" + currentRole + "' does not have permission to access " + request.getRequestURI() + ". Active authorities: " + authorities + ".",
                "INSUFFICIENT_PERMISSIONS",
                "Use a page allowed for your role or sign in with an account that has the required permission."
        );
    }

    private boolean wantsJsonResponse(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return (uri != null && uri.startsWith("/api/"))
                || "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private String jsonError(int status, String error, String message, String path, String reasonCode, String nextStep) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("error", error);
        payload.put("message", message);
        payload.put("path", path);
        payload.put("reasonCode", reasonCode);
        payload.put("nextStep", nextStep);

        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append('"').append(escapeJson(String.valueOf(value))).append('"');
            }
        }
        json.append('}');
        return json.toString();
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private record ErrorDescriptor(String error, String message, String reasonCode, String nextStep) {
    }

    /**
     * Supports CSRF tokens rendered into server-side HTML and sent back by JS clients via header.
     * The XOR handler keeps BREACH protection for views, while header-based requests use the plain token.
     */
    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            this.xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            if (headerValue != null && !headerValue.isBlank()) {
                return this.plain.resolveCsrfTokenValue(request, csrfToken);
            }
            return this.xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength 12 for better security
    }
}
