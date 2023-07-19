package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class Automated {
    private boolean prune;
    private boolean selfHeal;

    // Getters and setters
}
