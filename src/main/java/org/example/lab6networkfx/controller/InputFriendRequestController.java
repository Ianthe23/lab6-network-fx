package org.example.lab6networkfx.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.example.lab6networkfx.domain.User;
import org.example.lab6networkfx.service.NetworkService;

import java.util.ArrayList;
import java.util.List;

public class InputFriendRequestController {
    @FXML
    private ComboBox<String> user1Field;

    @FXML
    private ComboBox<String> user2Field;

    @FXML
    private Button btnSendRequest;

    @FXML
    private Button btnExit;

    NetworkService service;
    Stage inputStage;
    User user;

    private void clearFields() {
        user1Field.setValue(null);
        user2Field.setValue(null);
    }

    public void setService(NetworkService service, Stage stage, User user) {
        this.service = service;
        this.inputStage = stage;
        this.user = user;

        setFields(user);
    }

    private void setFields(User user) {
        //add the users to the combo boxes
        List<String> users = new ArrayList<>();
        service.getAllUsers().forEach(user1 -> users.add(((User) user1).getUsername()));

        if (user == null) {
            user1Field.setValue(null);
        } else {
            user1Field.setValue(user.getUsername());
        }
        user1Field.getItems().addAll(users);
        user2Field.getItems().addAll(users);
    }

    private void handleRequest() {
        try {
            String user1 = user1Field.getValue().toString();
            String user2 = user2Field.getValue().toString();
            service.pendingFriendshipRequest(user1, user2);
            AlertMessages.showMessage(null, Alert.AlertType.CONFIRMATION, "Friend Request sent", "Friend Request sent successfully");
            clearFields();
            inputStage.close();
        } catch (Exception e) {
            AlertMessages.showMessage(null, Alert.AlertType.ERROR, "Add Friendship", e.getMessage());
        }
        inputStage.close();
    }

    public void handleExit() {
        inputStage.close();
    }

    @FXML
    public void handleClicks(ActionEvent event) {
        if (event.getSource() == btnSendRequest) {
            handleRequest();
        } else if (event.getSource() == btnExit) {
            handleExit();
        }
    }
}