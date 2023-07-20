package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class Metadata {
    private String name;
    private String namespace;
    private String instance;
    private String clusterName;
}
