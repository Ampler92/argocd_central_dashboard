package com.argocd.dashboard.model;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class ArgoCDInstance {

    private String name;
    private String url;
    private String username;
    private String password;

    // Getters and Setters
}
