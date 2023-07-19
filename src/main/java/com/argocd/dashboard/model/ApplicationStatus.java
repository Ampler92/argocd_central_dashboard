package com.argocd.dashboard.model;

import lombok.Data;

import java.util.List;

@Data
public class ApplicationStatus {
    private List<Resource> resources;
    private Sync sync;
    private Health health;
    private List<History> history;
    private String reconciledAt;
    private OperationState operationState;
    private String sourceType;
    private Summary summary;

    // Getters and setters
}
