module com.ibrasoft.jdriveclonr {
    // JavaFX modules
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires javafx.graphics;

    requires jdk.httpserver;

    // Google API Client Libraries
    requires transitive google.api.client; // Changed from 'requires' to 'requires transitive'
    requires com.google.gson;
    requires com.google.common;
    requires com.fasterxml.jackson.core;
    requires com.google.api.client.auth;
    requires com.google.api.services.drive;
    requires com.google.api.client.json.gson;
    requires com.google.api.client.extensions.java6.auth;
    requires com.google.api.client.extensions.jetty.auth;
    requires com.google.api.client.json.jackson2;
    requires lombok;
    requires com.google.auth.oauth2;
    requires java.net.http;
    requires gax;
    requires google.photos.library.client;
    requires com.google.api.client;

    // Allow FXML loader to access controllers
    opens com.ibrasoft.jdriveclonr to javafx.fxml;
    opens com.ibrasoft.jdriveclonr.ui to javafx.fxml;
    opens com.ibrasoft.jdriveclonr.auth to javafx.fxml;
    
    // Export packages for use by other modules
    exports com.ibrasoft.jdriveclonr;
    exports com.ibrasoft.jdriveclonr.ui;
    exports com.ibrasoft.jdriveclonr.auth;
    exports com.ibrasoft.jdriveclonr.service;
    exports com.ibrasoft.jdriveclonr.model;
    opens com.ibrasoft.jdriveclonr.model to javafx.fxml;
}