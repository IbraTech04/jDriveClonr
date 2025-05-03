package com.ibrasoft.jdriveclonr.ui;

import com.ibrasoft.jdriveclonr.App;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class WelcomeController {
    @FXML
    protected void onStartClicked(ActionEvent event) {
        App.navigateTo("auth.fxml");
    }
}