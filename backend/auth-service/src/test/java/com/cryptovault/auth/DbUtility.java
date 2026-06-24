package com.cryptovault.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;

public class DbUtility {
    @Test
    public void printUsers() throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cryptovault", "postgres", "@tharv23")) {
            System.out.println("PG_USERS_START");
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT id, email, role, name FROM users")) {
                    while (rs.next()) {
                        System.out.println("USER: " + rs.getString("email") + " | ROLE: " + rs.getString("role") + " | NAME: " + rs.getString("name") + " | ID: " + rs.getString("id"));
                    }
                }
            }
            System.out.println("PG_USERS_END");
        }
    }

    @Test
    public void printAllTables() throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cryptovault", "postgres", "@tharv23")) {
            System.out.println("ALL_TABLES_START");
            try (Statement stmt = conn.createStatement()) {
                String[] types = {"TABLE"};
                try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", types)) {
                    while (rs.next()) {
                        String tableName = rs.getString("TABLE_NAME");
                        System.out.println("TABLE: " + tableName);
                    }
                }
            }
            System.out.println("ALL_TABLES_END");
        }
    }


    @Test
    public void printWallets() throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cryptovault", "postgres", "@tharv23")) {
            System.out.println("WALLETS_START");
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT w.id, w.user_id, w.currency, w.balance, u.email FROM wallets w JOIN users u ON w.user_id = u.id")) {
                    while (rs.next()) {
                        System.out.println("WALLET: " + rs.getString("id") + " | USER: " + rs.getString("email") + " | CURRENCY: " + rs.getString("currency") + " | BALANCE: " + rs.getString("balance"));
                    }
                }
            }
            System.out.println("WALLETS_END");
        }
    }

    @Test
    public void printAuditLogs() throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cryptovault", "postgres", "@tharv23")) {
            System.out.println("AUDIT_LOGS_START");
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT id, user_id, event_type, service_name, action, description FROM audit_logs")) {
                    while (rs.next()) {
                        System.out.println("AUDIT: " + rs.getString("id") + " | USER: " + rs.getString("user_id") + " | TYPE: " + rs.getString("event_type") + " | DESC: " + rs.getString("description"));
                    }
                }
            }
            System.out.println("AUDIT_LOGS_END");
        }
    }

    @Test
    public void seedWalletsAndAuditLogs() throws Exception {
        Class.forName("org.postgresql.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cryptovault", "postgres", "@tharv23")) {
            conn.setAutoCommit(false);
            try {
                // Get all users
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id, email FROM users")) {
                    while (rs.next()) {
                        UUID userId = (UUID) rs.getObject("id");
                        String email = rs.getString("email");
                        
                        // Seed BTC, ETH, USDT wallets
                        seedUserWallet(conn, userId, email, "BTC", 5.50);
                        seedUserWallet(conn, userId, email, "ETH", 25.00);
                        seedUserWallet(conn, userId, email, "USDT", 500.00);
                    }
                }
                conn.commit();
                System.out.println("SEED_SUCCESS");
            } catch (Exception ex) {
                conn.rollback();
                ex.printStackTrace();
                throw ex;
            }
        }
    }

    private void seedUserWallet(Connection conn, UUID userId, String email, String currency, double initialBalance) throws Exception {
        UUID walletId = null;
        boolean exists = false;
        
        // Check if wallet exists
        String checkSql = "SELECT id FROM wallets WHERE user_id = ? AND currency = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setObject(1, userId);
            checkStmt.setString(2, currency);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    walletId = (UUID) rs.getObject("id");
                    exists = true;
                }
            }
        }
        
        if (exists) {
            // Update balance to make sure they have funds for testing
            String updateSql = "UPDATE wallets SET balance = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setBigDecimal(1, java.math.BigDecimal.valueOf(initialBalance));
                updateStmt.setObject(2, walletId);
                updateStmt.executeUpdate();
            }
        } else {
            // Insert wallet
            walletId = UUID.randomUUID();
            String insertSql = "INSERT INTO wallets (id, user_id, currency, balance, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setObject(1, walletId);
                insertStmt.setObject(2, userId);
                insertStmt.setString(3, currency);
                insertStmt.setBigDecimal(4, java.math.BigDecimal.valueOf(initialBalance));
                insertStmt.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                insertStmt.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                insertStmt.executeUpdate();
            }
        }
        
        // Check if WALLET_CREATED audit log already exists
        boolean logExists = false;
        String checkLogSql = "SELECT id FROM audit_logs WHERE description LIKE ?";
        try (PreparedStatement checkLogStmt = conn.prepareStatement(checkLogSql)) {
            checkLogStmt.setString(1, "%Wallet ID: " + walletId + "%");
            try (ResultSet rs = checkLogStmt.executeQuery()) {
                if (rs.next()) {
                    logExists = true;
                }
            }
        }
        
        if (!logExists) {
            // Insert audit log
            String insertLogSql = "INSERT INTO audit_logs (id, user_id, event_type, service_name, action, description, ip_address, performed_by, event_timestamp, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertLogStmt = conn.prepareStatement(insertLogSql)) {
                insertLogStmt.setObject(1, UUID.randomUUID());
                insertLogStmt.setObject(2, userId);
                insertLogStmt.setString(3, "WALLET_CREATED");
                insertLogStmt.setString(4, "wallet-service");
                insertLogStmt.setString(5, "WALLET_CREATION");
                insertLogStmt.setString(6, String.format("Wallet ID: %s, User: %s, Currency: %s", walletId, email, currency));
                insertLogStmt.setString(7, "127.0.0.1");
                insertLogStmt.setString(8, "SYSTEM");
                insertLogStmt.setTimestamp(9, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                insertLogStmt.setTimestamp(10, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                insertLogStmt.executeUpdate();
            }
        }
    }

    @Test
    public void createAdminUser() throws Exception {
        Class.forName("org.postgresql.Driver");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("AdminPassword123");
        
        try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/cryptovault", "postgres", "@tharv23")) {
            // Delete if already exists
            try (PreparedStatement deleteStmt = conn.prepareStatement("DELETE FROM users WHERE email = ?")) {
                deleteStmt.setString(1, "admin@cryptovault.com");
                deleteStmt.executeUpdate();
            }
            
            // Insert admin
            UUID id = UUID.randomUUID();
            String sql = "INSERT INTO users (id, name, email, password, role, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(sql)) {
                insertStmt.setObject(1, id);
                insertStmt.setString(2, "Admin User");
                insertStmt.setString(3, "admin@cryptovault.com");
                insertStmt.setString(4, hashedPassword);
                insertStmt.setString(5, "ADMIN");
                insertStmt.setObject(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                insertStmt.setObject(7, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                insertStmt.executeUpdate();
                System.out.println("ADMIN_USER_CREATED: admin@cryptovault.com / AdminPassword123");
            }
        }
    }
}
