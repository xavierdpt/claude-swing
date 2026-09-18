package com.xavierdpt.tasks;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    private final String jdbcUrl;

    public TaskRepository(Path databaseFile) {
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
        initSchema();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private void initSchema() {
        String createTable = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    content TEXT,
                    summary TEXT
                )
                """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
            stmt.execute("ALTER TABLE tasks ADD COLUMN summary TEXT");
        } catch (SQLException e) {
            if (!isDuplicateColumn(e)) {
                throw new RuntimeException("Failed to initialize database schema", e);
            }
        }
    }

    private boolean isDuplicateColumn(SQLException e) {
        String message = e.getMessage();
        return message != null && message.toLowerCase().contains("duplicate column name");
    }

    public List<Task> findAll() {
        String sql = "SELECT id, title, content, summary FROM tasks ORDER BY id";
        List<Task> tasks = new ArrayList<>();
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(new Task(rs.getInt("id"), rs.getString("title"), rs.getString("content"), rs.getString("summary")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load tasks", e);
        }
        return tasks;
    }

    public Task insert(String title, String content) {
        String sql = "INSERT INTO tasks (title, content) VALUES (?, ?)";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Task(keys.getInt(1), title, content, null);
                }
            }
            throw new SQLException("No generated key returned for inserted task");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert task", e);
        }
    }

    public void update(int id, String title, String content) {
        String sql = "UPDATE tasks SET title = ?, content = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update task " + id, e);
        }
    }

    public void updateSummary(int id, String summary) {
        String sql = "UPDATE tasks SET summary = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, summary);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update summary for task " + id, e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete task " + id, e);
        }
    }
}
