package org.example.lab6networkfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.lab6networkfx.controller.MainController;
import org.example.lab6networkfx.domain.Friendship;
import org.example.lab6networkfx.domain.Tuple;
import org.example.lab6networkfx.domain.User;
import org.example.lab6networkfx.domain.friendships.FriendshipRequest;
import org.example.lab6networkfx.domain.validators.Validator;
import org.example.lab6networkfx.domain.validators.ValidatorFactory;
import org.example.lab6networkfx.domain.validators.ValidatorStrategy;
import org.example.lab6networkfx.repository.database.factory.DataBaseRepoFactory;
import org.example.lab6networkfx.repository.database.factory.DataBaseStrategy;
import org.example.lab6networkfx.repository.database.utils.AbstractDataBaseRepo;
import org.example.lab6networkfx.repository.database.utils.DataBaseAcces;
import org.example.lab6networkfx.service.NetworkService;

import java.io.IOException;
import java.sql.SQLException;

public class App extends Application {
    private DataBaseAcces data;
    private AbstractDataBaseRepo<Integer, User> userRepo;
    private AbstractDataBaseRepo<Tuple<Integer,Integer>, Friendship> friendshipRepo;
    private AbstractDataBaseRepo<Tuple<Integer, Integer>, FriendshipRequest> friendshipRequestRepo;
    public NetworkService service;

    @Override
    public void start(Stage stage) throws Exception {
        NetworkService service;
        ValidatorFactory factory = ValidatorFactory.getInstance();
        Validator userValidator = factory.createValidator(ValidatorStrategy.User);
        Validator friendValidator = factory.createValidator(ValidatorStrategy.Friendship);
        Validator requestValidator = factory.createValidator(ValidatorStrategy.FriendshipRequest);

        String url = "jdbc:postgresql://localhost:5432/postgres";
        String username = "postgres";
        String password = "ivona2004";

        this.data = new DataBaseAcces(url, username, password);
        try {
            data.createConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        DataBaseRepoFactory repoFactory = new DataBaseRepoFactory(data);
        this.userRepo = repoFactory.createRepo(DataBaseStrategy.User, userValidator);
        this.friendshipRepo = repoFactory.createRepo(DataBaseStrategy.Friendship, friendValidator);
        this.friendshipRequestRepo = repoFactory.createRepo(DataBaseStrategy.FriendshipRequest, requestValidator);
        this.service = new NetworkService(userRepo, friendshipRepo, friendshipRequestRepo, "database");
        initView(stage);
        stage.setMinHeight(400);
        stage.setMinWidth(700);
        stage.show();
    }

    private void initView(Stage primaryStage) throws IOException {
        FXMLLoader stageLoader = new FXMLLoader();
        stageLoader.setLocation(getClass().getResource("main-view.fxml"));
        Scene scene = new Scene(stageLoader.load());
        scene.getStylesheets().add(getClass().getResource("/org/example/lab6networkfx/styles/main-view.css").toExternalForm());
        primaryStage.setTitle("Network Application");
        primaryStage.setScene(scene);

        MainController controller = stageLoader.getController();
        controller.setNetworkService(service);
    }

    public static void main(String[] args) {
        launch(args);
    }
}