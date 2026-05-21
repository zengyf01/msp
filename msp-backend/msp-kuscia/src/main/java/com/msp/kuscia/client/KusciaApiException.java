package com.msp.kuscia.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Kuscia API调用异常
 */
public class KusciaApiException extends RuntimeException {

    private final String taskId;
    private final int httpStatus;

    public KusciaApiException(String message) {
        super(message);
        this.taskId = null;
        this.httpStatus = 0;
    }

    public KusciaApiException(String message, Throwable cause) {
        super(message, cause);
        this.taskId = null;
        this.httpStatus = 0;
    }

    public KusciaApiException(String message, String taskId, int httpStatus) {
        super(message);
        this.taskId = taskId;
        this.httpStatus = httpStatus;
    }

    public KusciaApiException(String message, Throwable cause, String taskId, int httpStatus) {
        super(message, cause);
        this.taskId = taskId;
        this.httpStatus = httpStatus;
    }

    public static KusciaApiException fromWebClientException(WebClientResponseException e) {
        return new KusciaApiException(
            "Kuscia API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
            e,
            extractTaskId(e),
            e.getStatusCode().value()
        );
    }

    private static String extractTaskId(WebClientResponseException e) {
        String location = e.getHeaders().getFirst("Location");
        if (location != null && location.contains("/tasks/")) {
            return location.substring(location.lastIndexOf("/") + 1);
        }
        return null;
    }

    public String getTaskId() {
        return taskId;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isNotFound() {
        return httpStatus == HttpStatus.NOT_FOUND.value();
    }

    public boolean isRetryable() {
        return httpStatus == 0 || httpStatus >= 500;
    }
}