package dorsu.jareth.queue;

import java.io.File;
import java.io.IOException;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class QueueManagementServer extends WebSocketServer {

    private Connection connection;

    public QueueManagementServer(InetSocketAddress address) {
        super(address);
        connectToDatabase();
    }

    private void connectToDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/queue_management";
            String user = "root";
            String password = ""; // Replace with your MySQL password
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to the database.");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection from: " + conn.getRemoteSocketAddress());
        conn.send("Connected to Queue Management System.");
        sendQueueStatus(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress() + " - Reason: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Message from client (" + conn.getRemoteSocketAddress() + "): " + message);
        switch (message.toLowerCase()) {
            case "newticket":
                issueNewTicket(conn);
                break;
            case "nextticket":
                callNextTicket(conn);
                break;
            case "queuestatus":
                sendQueueStatus(conn);
                break;
            default:
                conn.send("Unknown command: " + message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error occurred with client: "
                + (conn != null ? conn.getRemoteSocketAddress() : "unknown")
                + " - " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Queue Management WebSocket server started!");
    }

    private void issueNewTicket(WebSocket conn) {
        try {
            int newTicketNumber = getLastTicketNumber() + 1;
            String newTicket = "Ticket-" + newTicketNumber;
            String sql = "INSERT INTO tickets (ticket_number, status, created_at) VALUES (?, 'PENDING', NOW())";  //Added status and timestamp
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, newTicket);
            stmt.executeUpdate();
            conn.send("New ticket issued: " + newTicket);
            broadcastQueueStatus();

            // Generate PDF after successful ticket creation
            try {
                generateTicketPDF(newTicket);
                conn.send("Ticket PDF generated: ticket_" + newTicketNumber + ".pdf"); //Inform client
            } catch (IOException e) {
                conn.send("Error generating PDF: " + e.getMessage()); //Inform client of PDF error
                System.err.println("Error generating PDF: " + e.getMessage());  //Log the error
            }

        } catch (SQLException e) {
            conn.send("Error issuing a new ticket: " + e.getMessage());
            System.err.println("SQL Error: " + e.getMessage()); //Log the SQL error
        }
    }

    private void generateTicketPDF(String ticketNumber) throws IOException {
        // Create the "tickets" directory if it doesn't exist
        Path ticketsDirectory = Paths.get("tickets");
        if (!Files.exists(ticketsDirectory)) {
            Files.createDirectories(ticketsDirectory);
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDFont pdfFont = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(pdfFont, 12);

                contentStream.setLeading(15); // Set leading (space between lines)

                float yOffset = 750; // Starting y-coordinate
                contentStream.newLineAtOffset(50, yOffset);
                contentStream.showText("Ticket Number: " + ticketNumber);
                yOffset -= 20; // Adjust y-coordinate for next line

                contentStream.newLineAtOffset(0, -20); // Move to the next line
                contentStream.showText("Status: PENDING");
                yOffset -= 20; // Adjust y-coordinate for next line

                contentStream.newLineAtOffset(0, -20); // Move to the next line
                contentStream.showText("Issued on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy hh:mm a")));
                contentStream.endText();
                contentStream.close();
                String filePath = ticketsDirectory.resolve("ticket_" + ticketNumber.substring(7) + ".pdf").toString();
                document.save(new File(filePath));
            }
            // Save the PDF in the "tickets" directory
            String filePath = ticketsDirectory.resolve("ticket_" + ticketNumber.substring(7) + ".pdf").toString();
            document.save(new File(filePath));
        }
    }

    private void callNextTicket(WebSocket conn) {
        try {
            String sql = "SELECT id, ticket_number FROM tickets WHERE status = 'PENDING' ORDER BY id LIMIT 1";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                int id = rs.getInt("id");
                String nextTicket = rs.getString("ticket_number");
                conn.send("Serving: " + nextTicket);

                sql = "UPDATE tickets SET status = 'SERVED' WHERE id = ?";
                PreparedStatement updateStmt = connection.prepareStatement(sql);
                updateStmt.setInt(1, id);
                updateStmt.executeUpdate();

                broadcast("Serving: " + nextTicket);
                broadcastQueueStatus();
            } else {
                conn.send("No tickets in the queue.");
            }
        } catch (SQLException e) {
            conn.send("Error calling the next ticket: " + e.getMessage());
        }
    }

    private void sendQueueStatus(WebSocket conn) {
        try {
            String sql = "SELECT ticket_number FROM tickets WHERE status = 'PENDING' ORDER BY id";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            List<String> queue = new ArrayList<>();
            while (rs.next()) {
                queue.add(rs.getString("ticket_number"));
            }

            String queueStatus = queue.isEmpty()
                    ? "Queue Status: The queue is empty."
                    : "Queue Status: " + queue.toString();
            conn.send(queueStatus);
        } catch (SQLException e) {
            conn.send("Error fetching queue status: " + e.getMessage());
        }
    }

    private void broadcastQueueStatus() {
        try {
            String sql = "SELECT ticket_number FROM tickets WHERE status = 'PENDING' ORDER BY id";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            List<String> queue = new ArrayList<>();
            while (rs.next()) {
                queue.add(rs.getString("ticket_number"));
            }

            String queueStatus = queue.isEmpty()
                    ? "Queue Status: The queue is empty."
                    : "Queue Status: " + queue.toString();
            broadcast(queueStatus);
        } catch (SQLException e) {
            System.err.println("Error broadcasting queue status: " + e.getMessage());
        }
    }

    private int getLastTicketNumber() throws SQLException {
        String sql = "SELECT MAX(id) AS max_id FROM tickets";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            return rs.getInt("max_id");
        }
        return 0;
    }

    public static void main(String[] args) {
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        QueueManagementServer server = new QueueManagementServer(address);
        try {
            server.start();
            System.out.println("Queue Management WebSocket server started on port 8080");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                    System.out.println("Server stopped successfully.");
                } catch (InterruptedException e) {
                    System.err.println("Error while stopping the server: " + e.getMessage());
                }
            }));
        } catch (Exception e) {
            System.err.println("Error starting the server: " + e.getMessage());
        }
    }
}
