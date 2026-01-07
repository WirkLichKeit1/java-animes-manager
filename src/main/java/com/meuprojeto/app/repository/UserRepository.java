package com.meuprojeto.app.repository;

import com.meuprojeto.app.config.DatabaseConfig;
import com.meuprojeto.app.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {
    public UserRepository() {
        createTable();
    }

    private void createTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                email TEXT NOT NULL
            )
        """;

        try (Connection conn  = DatabaseConfig.getConnection();
            Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace(e);
        }
    }

    public void save(User user) {
        String sql = "INSERT INTO users (nome, email) VALUES (?,?)";

        try (Connection conn = DatabaseConfig.getConnection();
            PreparedStatement stmt = conn.preparedStatement(sql)) {

            stmt.setStrint(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.executeUpdate();
        } catch (SQLException e ) {
            e.printStackTrace(e);
        }
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection conn = DatabaseConfig.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getStrint("name"),
                        rs.getString("email")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace(e);
        }

        return users;
    }
}