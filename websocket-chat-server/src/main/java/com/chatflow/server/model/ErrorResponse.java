 package com.chatflow.server.model;

import java.util.List;

public class ErrorResponse {
    private String status;
    private List<String> errors;
    private String serverTimestamp;

    public ErrorResponse(String status, List<String> errors, String serverTimestamp) {
        this.status = status;
        this.errors = errors;
        this.serverTimestamp = serverTimestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getServerTimestamp() {
        return serverTimestamp;
    }

    public void setServerTimestamp(String serverTimestamp) {
        this.serverTimestamp = serverTimestamp;
    }
}