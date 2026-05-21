package com.dataflow.ai.business.permission.processor;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class DefaultMaskProcessor implements MaskProcessor {

    private static final Pattern EMAIL = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

    @Override
    public Object mask(Object value, String rule) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        if (rule == null || rule.isBlank()) {
            return partialMask(s);
        }
        return switch (rule.toUpperCase()) {
            case "PHONE" -> maskPhone(s);
            case "ID_CARD" -> maskIdCard(s);
            case "EMAIL" -> maskEmail(s);
            case "FULL", "HIDE" -> "***";
            default -> partialMask(s);
        };
    }

    @Override
    public boolean supports(String rule) {
        return true;
    }

    @Override
    public List<String> getSupportedRules() {
        return List.of("PHONE", "ID_CARD", "EMAIL", "PARTIAL", "FULL", "CUSTOM");
    }

    private String maskPhone(String s) {
        if (s.length() < 7) {
            return partialMask(s);
        }
        return s.substring(0, 3) + "****" + s.substring(s.length() - 4);
    }

    private String maskIdCard(String s) {
        if (s.length() < 8) {
            return partialMask(s);
        }
        return s.substring(0, 4) + "**********" + s.substring(s.length() - 4);
    }

    private String maskEmail(String s) {
        if (!EMAIL.matcher(s).matches()) {
            return partialMask(s);
        }
        int at = s.indexOf('@');
        return s.charAt(0) + "***" + s.substring(at);
    }

    private String partialMask(String s) {
        if (s.length() <= 2) {
            return "**";
        }
        return s.charAt(0) + "***" + s.charAt(s.length() - 1);
    }
}
