package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class OperationState {
    private Operation operation;
    private String phase;
    private String message;
    private SyncResult syncResult;
    private String startedAt;
    private String finishedAt;

    // Getters and setters
}
