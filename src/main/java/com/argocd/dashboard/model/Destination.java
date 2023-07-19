package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class Destination {
    private String server;
    private String namespace;

    // Getters and setters
}
