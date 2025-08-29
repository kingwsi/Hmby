package org.example.hmby.exception;

/**
 * @author ws </br>
 * 2025/6/22
 */
public class ConfigNotFoundException extends RuntimeException {
    public ConfigNotFoundException(String message) {
        super("Config not found: " + message);
    }
}
