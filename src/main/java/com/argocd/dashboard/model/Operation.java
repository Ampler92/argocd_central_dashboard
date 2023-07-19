package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class Operation {
    private Sync sync;
    private InitiatedBy initiatedBy;
    private Retry retry;

    // Getters and setters
}
