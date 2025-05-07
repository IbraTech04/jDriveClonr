package com.ibrasoft.jdriveclonr;

import com.ibrasoft.jdriveclonr.model.ConfigModel;
import com.ibrasoft.jdriveclonr.service.DriveAPIService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

public class App extends Application {
    private static Stage primaryStage;
    private static ConfigModel configModel;
    @Getter
    @Setter
    private static DriveAPIService driveService;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        configModel = new ConfigModel();
        navigateTo("welcome.fxml");
        primaryStage.setTitle("DriveClonr");
        
        // Set application icon
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/DriveClonrLogo.ico")));
        primaryStage.getIcons().add(icon);
        
        primaryStage.show();
    }

    public static void navigateTo(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/" + fxml));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(Objects.requireNonNull(App.class.getResource("/styles/main.css")).toExternalForm());
            primaryStage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setScene(Scene scene) {
        primaryStage.setScene(scene);
    }

//    public static DriveAPIService getDriveService(){
//        return driveService;
//    }

    public static ConfigModel getConfig() {
        return configModel;
    }

    public static void main(String[] args) {
        launch(args);
    }
}