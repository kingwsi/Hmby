package org.example.hmby.exception;


import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

}
