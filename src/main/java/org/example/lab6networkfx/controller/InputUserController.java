package org.example.lab6networkfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.example.lab6networkfx.domain.User;
import org.example.lab6networkfx.service.NetworkService;
import javafx.event.ActionEvent;

import javafx.scene.control.TextField;

public class InputUserController {
    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button btnAdd;

    @FXML
    private Button btnExit;

    NetworkService service;
    Stage inputStage;
    User user;

    private void clearFields() {
        firstNameField.setText("");
        lastNameField.setText("");
        usernameField.setText("");
        passwordField.setText("");
    }

    public void setService(NetworkService service, Stage stage, User user) {
        this.service = service;
        this.inputStage = stage;
        this.user = user;

        if (user != null) {
            setFields(user);
        }
    }

    private void setFields(User user) {
        firstNameField.setText(user.getFirstName());
        lastNameField.setText(user.getLastName());
        usernameField.setText(user.getUsername());
        passwordField.setText(user.getPassword());
    }

    private void handleAdd() {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();

        User user = new User(firstName, lastName, username, password);
        saveUser(user);

        clearFields();
    }

    public void handleExit() {
        inputStage.close();
    }

    private void saveUser(User user) {
        try {
            service.addUser(user.getFirstName(), user.getLastName(), user.getUsername(), user.getPassword());
            AlertMessages.showMessage(inputStage, Alert.AlertType.CONFIRMATION, "Add User", "User added successfully!");
            inputStage.close();
        } catch (Exception e) {
            AlertMessages.showMessage(inputStage, Alert.AlertType.ERROR, "Add User", e.getMessage());
        }
        inputStage.close();
    }

    @FXML
    public void handleClicks(ActionEvent event) {
        if (event.getSource() == btnAdd) {
            handleAdd();
        } else {
            handleExit();
        }
    }



}
