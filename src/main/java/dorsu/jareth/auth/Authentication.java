package dorsu.jareth.auth;

import dorsu.jareth.queue.QueueManagementApp;
import dorsu.jareth.queue.QueueManagementServer;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class Authentication extends Application {

    private Scene loginScene;
    private Scene registerScene;

    @Override
    public void start(Stage primaryStage) {
        // Create the Login Scene
        primaryStage.setTitle("Authentication");

        // Left panel for login
        AnchorPane leftPaneLogin = new AnchorPane();
        leftPaneLogin.setPrefSize(300, 400);
        leftPaneLogin.setStyle("-fx-background-color: #404040;");

        // Replace companyTextLogin with an ImageView
        Image logoImage = new Image(getClass().getResource("/dorsu/jareth/auth/DOrSU_logo.png").toExternalForm());
        ImageView logoImageView = new ImageView(logoImage);
        logoImageView.setFitWidth(150);  // Set the desired width
        logoImageView.setPreserveRatio(true);  // Preserve the aspect ratio
        logoImageView.setLayoutX(78);  // Adjust the X position as needed
        logoImageView.setLayoutY(50);  // Adjust the Y position as needed

        // Add "DOrSU" and "Queue Management System" as separate Text elements
        Text dorsuText = new Text("DOrSU");
        dorsuText.setFill(Color.WHITE);
        dorsuText.setFont(Font.font(24)); // Adjust font size as needed
        dorsuText.setLayoutX(115); // Center alignment below the logo
        dorsuText.setLayoutY(225);

        Text queueManagementText = new Text("Queue Management System");
        queueManagementText.setFill(Color.WHITE);
        queueManagementText.setFont(Font.font(18)); // Slightly smaller font for the second line
        queueManagementText.setLayoutX(45); // Center alignment below "DOrSU"
        queueManagementText.setLayoutY(255);

        leftPaneLogin.getChildren().addAll(logoImageView, dorsuText, queueManagementText);

        // Right panel for login
        AnchorPane rightPaneLogin = new AnchorPane();
        rightPaneLogin.setPrefSize(300, 400);

        Text userLoginText = new Text("User Login");
        userLoginText.setFill(Color.web("#404040"));
        userLoginText.setFont(Font.font("Wingdings 3", 27));
        userLoginText.setLayoutX(81);
        userLoginText.setLayoutY(143);

        TextField usernameFieldLogin = new TextField();
        usernameFieldLogin.setPromptText("Username");
        usernameFieldLogin.setLayoutX(49);
        usernameFieldLogin.setLayoutY(163);
        usernameFieldLogin.setPrefSize(202, 25);
        usernameFieldLogin
                .setStyle("-fx-background-color: transparent; -fx-border-color: #404040; -fx-border-width: 0 0 2 0;");

        PasswordField passwordFieldLogin = new PasswordField();
        passwordFieldLogin.setPromptText("Password");
        passwordFieldLogin.setLayoutX(49);
        passwordFieldLogin.setLayoutY(202);
        passwordFieldLogin.setPrefSize(202, 25);
        passwordFieldLogin
                .setStyle("-fx-background-color: transparent; -fx-border-color: #404040; -fx-border-width: 0 0 2 0;");

        Button loginButton = new Button("Login");
        loginButton.setLayoutX(112);
        loginButton.setLayoutY(255);
        loginButton.setPrefSize(76, 32);
        loginButton.setStyle("-fx-background-color: #404040; -fx-background-radius: 0;");
        loginButton.setTextFill(Color.WHITE);
        // Inside the registration form button (submitButton) event handler
        loginButton.setOnAction((ActionEvent event) -> {
            String username = usernameFieldLogin.getText();
            String password = passwordFieldLogin.getText();

            // Validate fields
            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Input Error", "Please enter both username and password.", Alert.AlertType.WARNING);
                return;
            }

            // Call the login method to validate user credentials
            handleLogin(username, password);
        });

        Text registerText = new Text("Don't have an account? ");
        Text registerLink = new Text("Register");
        registerLink.setFill(Color.GREEN); // Set color for "Register" text
        registerLink.setUnderline(true); // Add underline to "Register"
        // Change cursor to hand pointer on hover
        registerLink.setStyle("-fx-cursor: hand;");

        registerText.setFill(Color.web("#404040")); // Default color for other text

        registerLink.setOnMouseClicked(event -> primaryStage.setScene(registerScene)); // Switch to registration scene

        TextFlow registerTextFlow = new TextFlow(registerText, registerLink);
        registerTextFlow.setLayoutX(65);
        registerTextFlow.setLayoutY(313);
        rightPaneLogin.getChildren().addAll(userLoginText, usernameFieldLogin, passwordFieldLogin, loginButton,
                registerTextFlow);

        // BorderPane layout for login
        BorderPane rootLogin = new BorderPane();
        rootLogin.setLeft(leftPaneLogin);
        rootLogin.setRight(rightPaneLogin);

        loginScene = new Scene(rootLogin, 600, 400);

        // Create the Register Scene
        // Left panel for registration
        AnchorPane leftPaneRegister = new AnchorPane();
        leftPaneRegister.setPrefSize(300, 400);
        leftPaneRegister.setStyle("-fx-background-color: #404040;");

        // Replace companyTextLogin with an ImageView
        Image logoImage2 = new Image(getClass().getResource("/dorsu/jareth/auth/DOrSU_logo.png").toExternalForm());
        ImageView logoImageView2 = new ImageView(logoImage);
        logoImageView2.setFitWidth(150);  // Set the desired width
        logoImageView2.setPreserveRatio(true);  // Preserve the aspect ratio
        logoImageView2.setLayoutX(78);  // Adjust the X position as needed
        logoImageView2.setLayoutY(50);  // Adjust the Y position as needed

        // Add "DOrSU" and "Queue Management System" as separate Text elements
        Text dorsuText2 = new Text("DOrSU");
        dorsuText2.setFill(Color.WHITE);
        dorsuText2.setFont(Font.font(24)); // Adjust font size as needed
        dorsuText2.setLayoutX(115); // Center alignment below the logo
        dorsuText2.setLayoutY(225);

        Text queueManagementText2 = new Text("Queue Management System");
        queueManagementText2.setFill(Color.WHITE);
        queueManagementText2.setFont(Font.font(18)); // Slightly smaller font for the second line
        queueManagementText2.setLayoutX(45); // Center alignment below "DOrSU"
        queueManagementText2.setLayoutY(255);

        leftPaneRegister.getChildren().addAll(logoImageView2, dorsuText2, queueManagementText2);

        // Right panel for registration
        AnchorPane rightPaneRegister = new AnchorPane();
        rightPaneRegister.setPrefSize(300, 400);

        Text userRegisterText = new Text("User Registration");
        userRegisterText.setFill(Color.web("#404040"));
        userRegisterText.setFont(Font.font("Wingdings 3", 27));
        userRegisterText.setLayoutX(47);
        userRegisterText.setLayoutY(53);

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        firstNameField.setLayoutX(49);
        firstNameField.setLayoutY(76);
        firstNameField.setPrefSize(202, 25);
        firstNameField.setStyle("-fx-background-color: transparent; -fx-border-color: #404040; -fx-border-width: 0 0 2 0;");

        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        lastNameField.setLayoutX(49);
        lastNameField.setLayoutY(113);
        lastNameField.setPrefSize(202, 25);
        lastNameField.setStyle("-fx-background-color: transparent; -fx-border-color: #404040; -fx-border-width: 0 0 2 0;");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setLayoutX(49);
        emailField.setLayoutY(147);
        emailField.setPrefSize(202, 25);
        emailField.setStyle("-fx-background-color: transparent; -fx-border-color: #404040; -fx-border-width: 0 0 2 0;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setLayoutX(49);
        usernameField.setLayoutY(183);
        usernameField.setPrefSize(202, 25);
        usernameField.setStyle("-fx-background-color: transparent; -fx-border-color: #404040; -fx-border-width: 0 0 2 0;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setLayoutX(49);
        passwordField.setLayoutY(219);
        passwordField.setPrefSize(202, 25);
        passwordField.setStyle("-fx-background-color: transparent; -fx-border-color: #404040; -fx-border-width: 0 0 2 0;");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm Password");
        confirmPasswordField.setLayoutX(49);
        confirmPasswordField.setLayoutY(254);
        confirmPasswordField.setPrefSize(202, 25);
        confirmPasswordField
                .setStyle("-fx-background-color: transparent; -fx-border-color: #404040; -fx-border-width: 0 0 2 0;");

        Button submitButton = new Button("Submit");
        submitButton.setLayoutX(112);
        submitButton.setLayoutY(302);
        submitButton.setPrefSize(76, 32);
        submitButton.setStyle("-fx-background-color: #404040; -fx-background-radius: 0;");
        submitButton.setTextFill(Color.WHITE);
        // Inside the registration form button (submitButton) event handler
        submitButton.setOnAction((ActionEvent event) -> {
            String firstName = firstNameField.getText();
            String lastName = lastNameField.getText();
            String email = emailField.getText();
            String username = usernameField.getText();
            String password = passwordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // Validate fields
            if (!password.equals(confirmPassword)) {
                showAlert("Password Mismatch", "Passwords do not match. Please try again.", AlertType.WARNING);
                return;
            }

            // Call the registration method
            handleRegistration(firstName, lastName, email, username, password);
        });

        Text loginText = new Text("Already have an account? ");
        Text loginLink = new Text("Login");
        loginLink.setFill(Color.BLUE); // Set color for "Login" text
        loginLink.setUnderline(true); // Add underline to "Login"
        loginLink.setStyle("-fx-cursor: hand;");

        loginText.setFill(Color.web("#404040")); // Default color for other text
        loginLink.setOnMouseClicked(event -> primaryStage.setScene(loginScene)); // Switch to login scene

        TextFlow loginTextFlow = new TextFlow(loginText, loginLink);
        loginTextFlow.setLayoutX(66);
        loginTextFlow.setLayoutY(358);

        rightPaneRegister.getChildren().addAll(userRegisterText, firstNameField, lastNameField, emailField, usernameField, passwordField,
                confirmPasswordField, submitButton, loginTextFlow);

        // BorderPane layout for registration
        BorderPane rootRegister = new BorderPane();
        rootRegister.setLeft(leftPaneRegister);
        rootRegister.setRight(rightPaneRegister);

        registerScene = new Scene(rootRegister, 600, 400);

        // Set the initial scene to login
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    // Registration functionality
    private void handleRegistration(String firstName, String lastName, String email, String username, String password) {
        String query = "INSERT INTO users (first_name, last_name, email, username, password) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, email);
            stmt.setString(4, username);
            stmt.setString(5, password); // In a real-world scenario, hash the password

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert("Registration Successful", "You have been successfully registered.", AlertType.INFORMATION);
            } else {
                showAlert("Registration Failed", "There was an error with the registration. Please try again.", AlertType.ERROR);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "There was a problem connecting to the database.", AlertType.ERROR);
        }
    }
// Handle Login

    private void handleLogin(String username, String password) {
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // Note: In a real-world scenario, hash and compare the password securely.

            ResultSet resultSet = stmt.executeQuery();

            if (resultSet.next()) {
                showAlert("Login Successful", "Welcome, " + username, AlertType.INFORMATION);
                // Redirect to another scene, for example, a dashboard

                // Start the QueueManagementServer
                startQueueManagementServer();

                // Switch to the main scene (QueueManagementApp)
                switchToDashboardScene();

                // primaryStage.setScene(dashboardScene); 
            } else {
                showAlert("Login Failed", "Invalid username or password.", AlertType.ERROR);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "There was a problem connecting to the database.", AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startQueueManagementServer() {
        // Define the IP address and port for the server
        InetSocketAddress serverAddress = new InetSocketAddress("localhost", 9090); // Change as needed

        // Initialize and start the QueueManagementServer with the address
        QueueManagementServer server = new QueueManagementServer(serverAddress);
        new Thread(() -> {
            try {
                server.start(); // Assuming the startServer() method starts the server
            } catch (Exception e) {
                e.printStackTrace();
                showAlert("Server Error", "Could not start the server.", Alert.AlertType.ERROR);
            }
        }).start();
    }

    private void switchToDashboardScene() {
        // Create a new thread to run the QueueManagementApp without blocking the main thread
        new Thread(() -> {
            // Launch the QueueManagementApp (which is a JavaFX Application)
            QueueManagementApp.launch(QueueManagementApp.class);
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
