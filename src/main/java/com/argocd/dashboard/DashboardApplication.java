package com.argocd.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.argocd.dashboard", "com.argocd.dashboard.config"})
public class DashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
    }
}

