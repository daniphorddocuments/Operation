package com.daniphord.mahanga.Util;

public final class PasswordStrengthUtil {

    private PasswordStrengthUtil() {
    }

    public static String evaluate(String rawPassword, String username) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return "UNKNOWN";
        }

        int score = 0;
        String normalizedPassword = rawPassword.trim();
        String normalizedUsername = username == null ? "" : username.trim().toLowerCase();

        if (normalizedPassword.length() >= 8) {
            score += 1;
        }
        if (normalizedPassword.length() >= 12) {
            score += 1;
        }
        if (normalizedPassword.chars().anyMatch(Character::isUpperCase)) {
            score += 1;
        }
        if (normalizedPassword.chars().anyMatch(Character::isLowerCase)) {
            score += 1;
        }
        if (normalizedPassword.chars().anyMatch(Character::isDigit)) {
            score += 1;
        }
        if (normalizedPassword.chars().anyMatch(character -> !Character.isLetterOrDigit(character))) {
            score += 1;
        }
        if (!normalizedUsername.isBlank() && normalizedPassword.toLowerCase().contains(normalizedUsername)) {
            score -= 2;
        }

        if (score >= 5) {
            return "STRONG";
        }
        if (score >= 3) {
            return "MEDIUM";
        }
        return "WEAK";
    }
}
