package com.crossborder.dao;

import com.crossborder.entity.Alert;
import com.crossborder.util.DatabaseUtil;
import com.crossborder.util.TimestampUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertDao {

    public Alert save(Alert alert) {
        String sql = "INSERT INTO alerts (alert_type, title, content, store_id, platform, related_id, priority, status, is_read, alert_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, alert.getAlertType());
            pstmt.setString(2, alert.getTitle());
            pstmt.setString(3, alert.getContent());
            pstmt.setObject(4, alert.getStoreId());
            pstmt.setString(5, alert.getPlatform());
            pstmt.setString(6, alert.getRelatedId());
            pstmt.setString(7, alert.getPriority());
            pstmt.setString(8, alert.getStatus());
            pstmt.setInt(9, alert.isRead() ? 1 : 0);
            pstmt.setString(10, TimestampUtil.formatForSqlite(alert.getAlertTime()));
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    alert.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alert;
    }

    public Alert findById(Long id) {
        String sql = "SELECT * FROM alerts WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToAlert(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Alert> findAll() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts ORDER BY alert_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public List<Alert> findUnread() {
        List<Alert> alerts = new ArrayList<>();
        String sql = "SELECT * FROM alerts WHERE is_read = 0 ORDER BY alert_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                alerts.add(mapResultSetToAlert(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return alerts;
    }

    public void markAsRead(Long id) {
        String sql = "UPDATE alerts SET is_read = 1 WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void markAllAsRead() {
        String sql = "UPDATE alerts SET is_read = 1 WHERE is_read = 0";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Alert mapResultSetToAlert(ResultSet rs) throws SQLException {
        Alert alert = new Alert();
        alert.setId(rs.getLong("id"));
        alert.setAlertType(rs.getString("alert_type"));
        alert.setTitle(rs.getString("title"));
        alert.setContent(rs.getString("content"));
        alert.setStoreId(rs.getObject("store_id") != null ? rs.getLong("store_id") : null);
        alert.setPlatform(rs.getString("platform"));
        alert.setRelatedId(rs.getString("related_id"));
        alert.setPriority(rs.getString("priority"));
        alert.setStatus(rs.getString("status"));
        alert.setRead(rs.getBoolean("is_read"));
        
        alert.setAlertTime(TimestampUtil.getLocalDateTime(rs, "alert_time"));
        alert.setProcessedTime(TimestampUtil.getLocalDateTime(rs, "processed_time"));
        alert.setCreatedAt(TimestampUtil.getLocalDateTime(rs, "created_at"));
        
        return alert;
    }
}
