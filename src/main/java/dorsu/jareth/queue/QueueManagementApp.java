package dorsu.jareth.queue;

import java.io.File;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class QueueManagementApp extends Application {

    private WebSocketClient webSocketClient;
    private TextArea queueStatusArea;
    private Label currentlyServingLabel;
    private Label nextInQueueLabel;
    private Label totalQueueLabel;  // New label to display the number of tickets in the queue
    private Stage infoStage;
    private MediaPlayer mediaPlayer;  // MediaPlayer to control video playback
    private static boolean appLaunched = false;  // To track if the app has already been launched

    @Override
    public void start(Stage primaryStage) throws URISyntaxException {

        // Prevent multiple launches
        if (appLaunched) {
            return;  // Avoid relaunching
        }
        appLaunched = true;

        initializeWebSocketClient();

        queueStatusArea = new TextArea();
        queueStatusArea.setEditable(false);
        queueStatusArea.setWrapText(true);
        queueStatusArea.setFont(Font.font("Arial", 18));  // Larger font for queue status area
        queueStatusArea.setPrefHeight(300);  // Adjusted height

        // Buttons with larger font
        Button issueTicketButton = createStyledButton("Issue New Ticket", "#4CAF50");
        Button callNextTicketButton = createStyledButton("Call Next Ticket", "#FF9800");
        Button refreshQueueButton = createStyledButton("Refresh Queue Status", "#2196F3");

        issueTicketButton.setPrefWidth(200);
        callNextTicketButton.setPrefWidth(200);
        refreshQueueButton.setPrefWidth(200);

        issueTicketButton.setOnAction(e -> sendWebSocketMessage("newTicket"));
        callNextTicketButton.setOnAction(e -> sendWebSocketMessage("nextTicket"));
        refreshQueueButton.setOnAction(e -> sendWebSocketMessage("queueStatus"));

        HBox buttonBox = new HBox(15, issueTicketButton, callNextTicketButton, refreshQueueButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(10));

        VBox layout = new VBox(20, queueStatusArea, buttonBox);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: #F0F0F0; -fx-border-color: #333; -fx-border-radius: 8;");

        Scene scene = new Scene(layout, 900, 700); // Larger main window size
        primaryStage.setScene(scene);
        primaryStage.setTitle("Queue Management System");
        primaryStage.show();

        createInfoWindow();
    }

    // Inside the createInfoWindow method:
    private void createInfoWindow() {
        infoStage = new Stage();
        infoStage.setTitle("Queue Info");

        // Labels for displaying queue information
        currentlyServingLabel = new Label("Currently Serving: None");
        nextInQueueLabel = new Label("Next in Queue: None");
        totalQueueLabel = new Label("Total Tickets in Queue: 0");

        // Set modern fonts and adjust sizes
        currentlyServingLabel.setFont(Font.font("Segoe UI", 35));
        nextInQueueLabel.setFont(Font.font("Segoe UI", 35));
        totalQueueLabel.setFont(Font.font("Segoe UI", 35));

        currentlyServingLabel.setTextFill(Color.web("#00796B"));
        nextInQueueLabel.setTextFill(Color.web("#00796B"));
        totalQueueLabel.setTextFill(Color.web("#00796B"));

        // Create a Media object for the video
        Media media = null;
        MediaView mediaView = new MediaView();
        File defaultVideoFile = new File("ad/QueueManagementSystem.mp4");
        if (defaultVideoFile.exists()) {
            URI videoURI = defaultVideoFile.toURI();
            media = new Media(videoURI.toString());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaPlayer.setAutoPlay(true);

            // Set the media to loop
            mediaPlayer.setOnEndOfMedia(() -> mediaPlayer.seek(Duration.ZERO));
        }

        // Button to select a new video file
        Button selectFileButton = createStyledButton("Select Video File", "#4CAF50");
        selectFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Video File");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mkv", "*.avi")
            );
            File selectedFile = fileChooser.showOpenDialog(infoStage);
            if (selectedFile != null) {
                URI selectedFileURI = selectedFile.toURI();
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                Media newMedia = new Media(selectedFileURI.toString());
                mediaPlayer = new MediaPlayer(newMedia);
                mediaView.setMediaPlayer(mediaPlayer);
                mediaPlayer.setAutoPlay(true);
            }
        });

        // VBox for the info section
        VBox infoLayout = new VBox(40, currentlyServingLabel, nextInQueueLabel, totalQueueLabel, selectFileButton);
        infoLayout.setPadding(new Insets(20));
        infoLayout.setAlignment(Pos.CENTER);
        infoLayout.setStyle("-fx-background-color: #F9F9F9; -fx-background-radius: 12; -fx-border-color: #00796B; -fx-border-width: 2;");
        VBox.setVgrow(infoLayout, Priority.ALWAYS);

        // VBox for the video section
        VBox videoLayout = new VBox(mediaView);
        videoLayout.setStyle("-fx-background-color: #F0F0F0;");
        VBox.setVgrow(videoLayout, Priority.ALWAYS);
        videoLayout.setAlignment(Pos.CENTER); // Centering the video inside the VBox

        // Add responsiveness to the video size
        mediaView.setFitWidth(1024); // Maximum width
        mediaView.setPreserveRatio(true); // Maintain aspect ratio

        // Main layout
        VBox mainLayout = new VBox(10, infoLayout, videoLayout);
        mainLayout.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 20;");

        Scene infoScene = new Scene(mainLayout);
        infoStage.setScene(infoScene);

        // Make the window fullscreen
        infoStage.setFullScreen(true);
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

    private void initializeWebSocketClient() throws URISyntaxException {
        URI serverUri = new URI("ws://localhost:9090");

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
                    updateInfoWindow(message);
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

    public static void main(String[] args) {
        launch(args);
    }
}
