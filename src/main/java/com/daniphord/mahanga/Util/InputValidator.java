package com.daniphord.mahanga.Util;

import java.util.regex.Pattern;

/**
 * Input Validation Utility
 * Prevents XSS, SQL Injection, and other input-based attacks
 */
public class InputValidator {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,50}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{7,15}$");
    private static final Pattern CURRENCY_PATTERN = Pattern.compile("^[0-9]+(\\.[0-9]{2})?$");
    private static final Pattern XSS_PATTERN = Pattern.compile("[<>\"'%;()&+]");

    /**
     * Validate username format
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    /**
     * Validate password strength (min 8 chars, uppercase, lowercase, digit, special)
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUppercase = Pattern.compile("[A-Z]").matcher(password).find();
        boolean hasLowercase = Pattern.compile("[a-z]").matcher(password).find();
        boolean hasDigit = Pattern.compile("[0-9]").matcher(password).find();
        boolean hasSpecial = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};:'\",.<>?/\\\\|`~]").matcher(password).find();

        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validate currency amount
     */
    public static boolean isValidCurrency(String amount) {
        return amount != null && CURRENCY_PATTERN.matcher(amount).matches();
    }

    /**
     * Sanitize input to prevent XSS
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return null;
        }
        return XSS_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Validate string length
     */
    public static boolean isValidLength(String input, int minLength, int maxLength) {
        return input != null && input.length() >= minLength && input.length() <= maxLength;
    }

    /**
     * Check if string contains only alphanumeric and basic punctuation
     */
    public static boolean isAlphanumericSafe(String input) {
        return input != null && input.matches("^[a-zA-Z0-9\\s.,_-]*$");
    }

    /**
     * Validate role is one of allowed values
     */
    public static boolean isValidRole(String role) {
        return role != null && (role.equals("MANAGER") || role.equals("ACCOUNTANT") || role.equals("SUPER_ADMIN"));
    }
}

