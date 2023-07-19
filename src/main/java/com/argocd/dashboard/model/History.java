package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class History {
    private String revision;
    private String deployedAt;
    // Other fields as needed

    // Getters and setters
}
