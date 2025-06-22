package org.example.hmby.exception;

import org.example.hmby.enumerate.ConfigKey;

/**
 * @author ws </br>
 * 2025/6/22
 */
public class ConfigNotFoundException extends RuntimeException {
    public ConfigNotFoundException(ConfigKey configKey) {
        super("Config not found: " + configKey);
    }
}
