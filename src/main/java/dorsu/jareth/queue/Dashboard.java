package dorsu.jareth.queue;

import dorsu.jareth.auth.Authentication;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class Dashboard extends Application {

    private TextArea queueStatusArea;
    private Button issueTicketButton;
    private Button callNextTicketButton;
    private Button refreshQueueStatusButton;
    private Button logoutButton;
    private Button ticketChartsButton;
    private Button queueHistoryButton;
    private Button infoWindowButton;
    private ImageView dorsuLogo;

    private Stage dashboardStage;

    private WebSocketClient webSocketClient;
    private QueueManagementServer server;

    private Label currentlyServingLabel;
    private Label nextInQueueLabel;
    private Label totalQueueLabel;  // New label to display the number of tickets in the queue
    private Stage infoStage;
    private MediaPlayer mediaPlayer;  // MediaPlayer to control video playback

    @Override
    public void start(Stage primaryStage) throws URISyntaxException {
        primaryStage.setOnCloseRequest(event -> {
            // Perform any necessary cleanup or actions before exiting
            System.out.println("Application closing..."); // Optional: Log the event
            // You might add code here to save data, close resources, etc.
            Platform.exit(); //Ensure the application exits cleanly
            System.exit(0); // Ensure the JVM exits
        });
        dashboardStage = primaryStage;
        startQueueManagementServer();
        initializeWebSocketClient();
        // Create and set up the scene
        Scene scene = createDashboardScene(primaryStage);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Queue Management System - Main Window");
        primaryStage.show();
    }

    public Scene createDashboardScene(Stage primaryStage) {
        // Initialize root layout
        AnchorPane root = new AnchorPane();
        root.setPrefSize(788, 543);
        root.setStyle("-fx-background-color: #404040;"); // Set background color for root

        // Initialize TextArea
        queueStatusArea = new TextArea();
        // Style the TextArea with a shade of gray
        queueStatusArea.setStyle("-fx-background-color: #555; -fx-text-fill: black;");
        queueStatusArea.setEditable(false);
        queueStatusArea.setLayoutX(232);
        queueStatusArea.setLayoutY(14);
        queueStatusArea.setPrefSize(542, 436);

        // Initialize Buttons
        issueTicketButton = new Button("Issue New Ticket");
        issueTicketButton.setLayoutX(232);
        issueTicketButton.setLayoutY(461);
        issueTicketButton.setPrefSize(138, 34);
        issueTicketButton.setOnAction(event -> issueNewTicket());

        callNextTicketButton = new Button("Call Next Ticket");
        callNextTicketButton.setLayoutX(394);
        callNextTicketButton.setLayoutY(461);
        callNextTicketButton.setPrefSize(138, 34);
        callNextTicketButton.setOnAction(event -> callNextTicket());

        refreshQueueStatusButton = new Button("Refresh Queue Status");
        refreshQueueStatusButton.setLayoutX(556);
        refreshQueueStatusButton.setLayoutY(461);
        refreshQueueStatusButton.setPrefSize(138, 34);
        refreshQueueStatusButton.setOnAction(event -> refreshQueueStatus());

        logoutButton = new Button("Logout");
        logoutButton.setLayoutX(27);
        logoutButton.setLayoutY(495);
        logoutButton.setOnAction(event -> {
            try {
                logout();
            } catch (InterruptedException ex) {

            }
        });

        ticketChartsButton = new Button("Ticket Dahsboard");
        ticketChartsButton.setLayoutX(53);
        ticketChartsButton.setLayoutY(182);
        ticketChartsButton.setPrefSize(121, 25);
        ticketChartsButton.setOnAction(event -> {
            try {
                showDashboardReports();
            } catch (Exception ex) {
            }
        });

        queueHistoryButton = new Button("Queue History");
        queueHistoryButton.setLayoutX(53);
        queueHistoryButton.setLayoutY(220);
        queueHistoryButton.setPrefSize(121, 25);
        queueHistoryButton.setOnAction(event -> {
            try {
                showQueueHistory();
            } catch (Exception ex) {

            }
        });

        infoWindowButton = new Button("Info Window");
        infoWindowButton.setLayoutX(53);
        infoWindowButton.setLayoutY(259);
        infoWindowButton.setPrefSize(121, 25);
        infoWindowButton.setOnAction(event -> openInformationWindow());

        // Initialize ImageView
        dorsuLogo = new ImageView(new Image(getClass().getResourceAsStream("/dorsu/jareth/auth/DOrSU_logo.png")));
        dorsuLogo.setLayoutX(53);
        dorsuLogo.setLayoutY(26);
        dorsuLogo.setFitHeight(133);
        dorsuLogo.setFitWidth(133);
        dorsuLogo.setPreserveRatio(true);

        // Style the buttons with a shade of gray
        issueTicketButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        callNextTicketButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        refreshQueueStatusButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        logoutButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        ticketChartsButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        queueHistoryButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        infoWindowButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        // Add all nodes to the root layout
        root.getChildren().addAll(
                queueStatusArea,
                issueTicketButton,
                callNextTicketButton,
                refreshQueueStatusButton,
                logoutButton,
                ticketChartsButton,
                queueHistoryButton,
                infoWindowButton,
                dorsuLogo
        );

        // Create the Scene
        return new Scene(root, 788, 543);
    }

    private void initializeWebSocketClient() throws URISyntaxException {
        URI serverUri = new URI("ws://localhost:8080");

        webSocketClient = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                System.out.println("Connected to WebSocket Server");
                send("queueStatus");
            }

            @Override
            public void onMessage(String message) {
                Platform.runLater(() -> {
                    queueStatusArea.appendText(message + "\n");
                    if (nextInQueueLabel != null && totalQueueLabel != null && currentlyServingLabel != null) {
                        updateInfoWindow(message);
                    }
                });
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("Connection closed: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("WebSocket Error: " + ex.getMessage());
            }
        };

        webSocketClient.connect();
    }

    private void sendWebSocketMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
        } else {
            System.err.println("WebSocket is not connected.");
        }
    }

    // Inside the createInfoWindow method:
    private void createInfoWindow() {
        // Ensure the file path is correct
        File defaultVideoFile = new File("ad/QueueManagementSystem.mp4");
        if (!defaultVideoFile.exists()) {
            System.err.println("Error: Video file not found at: " + defaultVideoFile.getAbsolutePath());
            return; // Exit early if video file is not found
        }

        infoStage = new Stage();
        infoStage.setTitle("Queue Info");

        AnchorPane infoPane = new AnchorPane();
        // Set styles for the infoPane
        infoPane.setStyle("-fx-background-color: #404040;"); // Dark gray background

        infoPane.setPrefWidth(1308); // Match the width from FXML
        infoPane.setPrefHeight(918); // Match the height from FXML

        // Container for information labels
        AnchorPane containerForTheInfo = new AnchorPane();
        containerForTheInfo.setPrefHeight(919); // Match the height from FXML
        containerForTheInfo.setPrefWidth(442); // Match the width from FXML
        AnchorPane.setLeftAnchor(containerForTheInfo, 0.0);  // Anchor to the left
        AnchorPane.setTopAnchor(containerForTheInfo, 0.0);  // Anchor to the top

        //Labels
        currentlyServingLabel = new Label("Currently Serving: None");
        currentlyServingLabel.setStyle("-fx-text-fill: white;");
        currentlyServingLabel.setFont(new Font(28));
        AnchorPane.setTopAnchor(currentlyServingLabel, 166.0);
        AnchorPane.setLeftAnchor(currentlyServingLabel, 55.0);
        containerForTheInfo.getChildren().add(currentlyServingLabel);

        nextInQueueLabel = new Label("Next in Queue: None");
        nextInQueueLabel.setStyle("-fx-text-fill: white;");
        nextInQueueLabel.setFont(new Font(28));
        AnchorPane.setTopAnchor(nextInQueueLabel, 374.0);
        AnchorPane.setLeftAnchor(nextInQueueLabel, 55.0);
        containerForTheInfo.getChildren().add(nextInQueueLabel);

        totalQueueLabel = new Label("Total Tickets in Queue: 0");
        totalQueueLabel.setStyle("-fx-text-fill: white;");
        totalQueueLabel.setFont(new Font(28));
        AnchorPane.setTopAnchor(totalQueueLabel, 270.0);
        AnchorPane.setLeftAnchor(totalQueueLabel, 55.0);
        containerForTheInfo.getChildren().add(totalQueueLabel);

        // Button
        Button selectFileButton = new Button("Select File");
        // Style the button
        selectFileButton.setStyle("-fx-background-color: #555; -fx-text-fill: white;");
        AnchorPane.setBottomAnchor(selectFileButton, 12.0);
        AnchorPane.setLeftAnchor(selectFileButton, 14.0);
        containerForTheInfo.getChildren().add(selectFileButton);

        // Container for MediaView
        StackPane containerForTheMedia = new StackPane();
        AnchorPane.setRightAnchor(containerForTheMedia, 0.0);
        AnchorPane.setTopAnchor(containerForTheMedia, 0.0);
        AnchorPane.setBottomAnchor(containerForTheMedia, 0.0); // Anchor to the bottom
        AnchorPane.setLeftAnchor(containerForTheMedia, 442.0);    // Anchor to the left

        // Wrapper Pane for MediaView
        Pane mediaWrapper = new Pane();
        StackPane.setAlignment(mediaWrapper, Pos.CENTER);
        containerForTheMedia.getChildren().add(mediaWrapper);

        MediaView mediaView = new MediaView();
        mediaWrapper.getChildren().add(mediaView);
        mediaView.setPreserveRatio(false);

        // Bind the wrapper's size to the StackPane's size using a simple bind (one-way)
        mediaWrapper.prefWidthProperty().bind(containerForTheMedia.widthProperty());
        mediaWrapper.prefHeightProperty().bind(containerForTheMedia.heightProperty());

        // Bind the MediaView's fitWidth and fitHeight to the wrapper's size
        mediaView.fitWidthProperty().bind(mediaWrapper.prefWidthProperty());
        mediaView.fitHeightProperty().bind(mediaWrapper.prefHeightProperty());

        // Media setup
        Media media = new Media(defaultVideoFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.setAutoPlay(true);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Loop the video
        mediaPlayer.setOnEndOfMedia(() -> mediaPlayer.seek(Duration.ZERO));

        selectFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Video File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mkv", "*.avi")
            );
            File selectedFile = fileChooser.showOpenDialog(infoStage);
            if (selectedFile != null) {
                try {
                    URI selectedFileURI = selectedFile.toURI();
                    if (mediaPlayer != null) {
                        mediaPlayer.stop();
                    }
                    Media newMedia = new Media(selectedFileURI.toString());
                    mediaPlayer = new MediaPlayer(newMedia);
                    mediaView.setMediaPlayer(mediaPlayer);
                    mediaPlayer.setAutoPlay(true);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                } catch (Exception ex) {
                    System.err.println("Error creating media from selected file: " + ex.getMessage());
                }
            }
        });

        infoPane.getChildren().addAll(containerForTheInfo, containerForTheMedia);
        Scene infoScene = new Scene(infoPane);
        infoStage.setScene(infoScene);
        infoStage.show();
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setTextFill(Color.WHITE);
        button.setFont(Font.font("Arial", 16));  // Larger font for buttons
        button.setPrefWidth(180);
        button.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 20; -fx-font-weight: bold;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: derive(" + color + ", -20%); -fx-background-radius: 20;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 20;"));
        return button;
    }

    private void updateInfoWindow(String message) {
        if (message.startsWith("Serving:")) {
            currentlyServingLabel.setText("Currently Serving: " + message.replace("Serving: ", ""));
        } else if (message.startsWith("Queue Status:")) {
            String queueStatus = message.replace("Queue Status: ", "").trim();

            // Check if the queueStatus contains any actual tickets
            if (queueStatus.isEmpty()) {
                // If no queue data exists, treat it as empty
                nextInQueueLabel.setText("Next in Queue: None");
                totalQueueLabel.setText("Total Tickets in Queue: 0");
            } else {
                // Split the queue data into individual tickets
                String[] queueParts = queueStatus.split(", ");

                // Fix: Check for the correct number of items
                String nextTicket = queueParts.length > 0 ? queueParts[0] : "None";
                nextInQueueLabel.setText("Next in Queue: " + nextTicket);

                // Total tickets
                totalQueueLabel.setText("Total Tickets in Queue: " + queueParts.length);
            }
        }
    }

    private void issueNewTicket() {
        // Logic for issuing a new ticket, e.g., send request to server via WebSocket
        System.out.println("Issuing new ticket...");
        sendWebSocketMessage("newticket");
    }

    private void callNextTicket() {
        // Logic for calling the next ticket in the queue
        System.out.println("Calling next ticket...");
        sendWebSocketMessage("nextticket");
    }

    private void refreshQueueStatus() {
        // Logic for refreshing the queue status from the server
        System.out.println("Refreshing queue status...");
        sendWebSocketMessage("queuestatus");
    }

    private void logout() throws InterruptedException {
        // Confirmation dialog
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Logout Confirmation");
        alert.setHeaderText(null);
        alert.setContentText("Are you sure you want to logout?");

        // Set the buttons for the confirmation dialog
        ButtonType buttonTypeYes = new ButtonType("Yes");
        ButtonType buttonTypeNo = new ButtonType("No");
        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo);

        // Show the dialog and wait for the user's response
        javafx.scene.control.ButtonType result = alert.showAndWait().orElse(buttonTypeNo); // buttonTypeNo is default if closed

        if (result == buttonTypeYes) {
            // Logic for logging out, e.g., close the application or clear user session
            Authentication app = new Authentication();
            Stage primaryStage = new Stage();
            app.start(primaryStage);
            primaryStage.show();
            server.stop(); // Assuming 'server' is defined elsewhere
            this.dashboardStage.close();
            System.out.println("Logging out...");
        } else {
            System.out.println("Logout cancelled."); // Optional: Indicate that logout was cancelled
        }
    }

    private void showDashboardReports() throws Exception {
        TicketsCharts app = new TicketsCharts();
        Stage primaryStage = new Stage();
        app.start(primaryStage);
        primaryStage.show();
        System.out.println("Displaying dashboard...");
    }

    private void showQueueHistory() throws Exception {
        TicketsTable app = new TicketsTable();
        Stage primaryStage = new Stage();
        app.start(primaryStage);
        primaryStage.show();
        System.out.println("Displaying queue history...");
    }

    private void openInformationWindow() {
        createInfoWindow();
    }

    private void startQueueManagementServer() {
        // Define the IP address and port for the server
        InetSocketAddress serverAddress = new InetSocketAddress("localhost", 8080); // Change as needed

        // Initialize and start the QueueManagementServer with the address
        server = new QueueManagementServer(serverAddress);
        new Thread(() -> {
            try {
                server.start(); // Assuming the startServer() method starts the server
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Error: " + e.getMessage());
                showAlert("Server Error", "Could not start the server.", Alert.AlertType.ERROR);
            }
        }).start();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
