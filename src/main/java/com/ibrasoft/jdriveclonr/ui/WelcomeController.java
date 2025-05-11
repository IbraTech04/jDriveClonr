package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

public class WelcomeController {
    @FXML
    public ImageView logoImageView;

    @FXML
    protected void onStartClicked(ActionEvent event) {
        App.navigateTo("auth.fxml");
    }
}