package org.example.hmby.enumerate;

import lombok.Getter;

/**
 * @author ws
 * @since 2025/8/16
 */
public enum PromptPlaceholder {
    
    TRANS("{{TRANSLATED}}"),
    DICTIONARY("{{DICTIONARY}}"),
    ;
    
    @Getter
    private final String placeholder;
    
    private PromptPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }
}
