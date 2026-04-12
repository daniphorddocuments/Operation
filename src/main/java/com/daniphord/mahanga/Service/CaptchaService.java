package com.daniphord.mahanga.Service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;

@Service
public class CaptchaService {

    private static final String CAPTCHA_ANSWER = "publicCaptchaAnswer";
    private static final String CAPTCHA_QUESTION = "publicCaptchaQuestion";
    private final SecureRandom random = new SecureRandom();

    public Map<String, String> issueChallenge(HttpSession session) {
        int left = random.nextInt(8) + 1;
        int right = random.nextInt(8) + 1;
        session.setAttribute(CAPTCHA_ANSWER, String.valueOf(left + right));
        String question = "What is " + left + " + " + right + "?";
        session.setAttribute(CAPTCHA_QUESTION, question);
        return Map.of("question", question);
    }

    public boolean verify(HttpSession session, String answer) {
        Object expected = session.getAttribute(CAPTCHA_ANSWER);
        session.removeAttribute(CAPTCHA_ANSWER);
        session.removeAttribute(CAPTCHA_QUESTION);
        return expected != null && expected.toString().equals(answer == null ? "" : answer.trim());
    }

    public String currentQuestion(HttpSession session) {
        if (session.getAttribute(CAPTCHA_ANSWER) == null) {
            return issueChallenge(session).get("question");
        }
        Object question = session.getAttribute(CAPTCHA_QUESTION);
        return question == null ? issueChallenge(session).get("question") : question.toString();
    }
}
