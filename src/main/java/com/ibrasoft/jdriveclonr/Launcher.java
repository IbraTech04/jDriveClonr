package com.ibrasoft.jdriveclonr;

import javafx.application.Application;

/**
 * Launcher class for the DriveClonr application.
 * This class serves as the entry point and is responsible for launching the JavaFX Application.
 * Separating the launcher from the main Application class follows JavaFX best practices
 * and helps avoid potential module path issues in some deployment scenarios.
 */
public class Launcher {
    
    /**
     * Main method that serves as the application entry point.
     * Launches the JavaFX Application.
     * 
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
