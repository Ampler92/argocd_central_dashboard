package com.argocd.dashboard.model;

import lombok.Data;

import java.util.List;

@Data
public class SyncResult {
    private List<Resource> resources;
    private String revision;
    private Source source;

    // Getters and setters
}
