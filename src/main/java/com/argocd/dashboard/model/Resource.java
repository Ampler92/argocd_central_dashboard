package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class Resource {
    private String version;
    private String kind;
    private String namespace;
    private String name;
    private String status;
    private Health health;

    // Getters and setters
}
