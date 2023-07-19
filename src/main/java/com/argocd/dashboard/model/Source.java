package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class Source {
    private String repoURL;
    private String path;
    private String targetRevision;

    // Getters and setters
}
