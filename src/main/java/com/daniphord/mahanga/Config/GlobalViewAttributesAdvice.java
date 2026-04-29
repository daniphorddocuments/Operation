package com.daniphord.mahanga.Config;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewAttributesAdvice {

    @ModelAttribute("currentLang")
    public String currentLang(HttpSession session) {
        Object lang = session.getAttribute("currentLang");
        return lang == null || lang.toString().isBlank() ? "sw" : lang.toString();
    }

    @ModelAttribute("showLangSwitch")
    public boolean showLangSwitch() {
        return true;
    }

    @ModelAttribute("_csrf")
    public CsrfToken csrfToken(CsrfToken csrfToken) {
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        return csrfToken;
    }

    @ModelAttribute("supportPhone")
    public String supportPhone() {
        return "114";
    }

    @ModelAttribute("supportEmail")
    public String supportEmail() {
        return "operations@fire.go.tz";
    }
}
