package dorsu.jareth.queue;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.net.InetSocketAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
            String newTicket = "Ticket-" + (getLastTicketNumber() + 1);
            String sql = "INSERT INTO tickets (ticket_number) VALUES (?)";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, newTicket);
            stmt.executeUpdate();
            conn.send("New ticket issued: " + newTicket);
            broadcastQueueStatus();
        } catch (SQLException e) {
            conn.send("Error issuing a new ticket: " + e.getMessage());
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
        InetSocketAddress address = new InetSocketAddress("localhost", 9090);
        QueueManagementServer server = new QueueManagementServer(address);
        try {
            server.start();
            System.out.println("Queue Management WebSocket server started on port 9090");

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
