package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class ApplicationSyncResponse {
    private Metadata metadata;
    private ApplicationSpec spec;
    private ApplicationStatus status;
    private Operation operation;
    private String message;
    private String error;
}
