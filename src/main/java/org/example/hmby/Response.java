package org.example.hmby;

import lombok.Data;

@Data
public class Response<T> {
    private String message;
    private T data;
    private int status;
    
    public static <T> Response<T> success(T data) {
        Response<T> response = new Response<T>();
        response.setData(data);
        response.setStatus(200);
        return response;
    }

    public static <T> Response<T> success() {
        Response<T> response = new Response<T>();
        response.setStatus(200);
        return response;
    }

    public static <T> Response<T> fail() {
        Response<T> response = new Response<T>();
        response.setStatus(500);
        return response;
    }
    
    public static <T> Response<T> fail(String message) {
        Response<T> response = new Response<T>();
        response.setMessage(message);
        response.setStatus(500);
        return response;
    }

    public static <T> Response<T> fail(String message, int status) {
        Response<T> response = new Response<T>();
        response.setMessage(message);
        response.setStatus(status);
        return response;
    }
}
