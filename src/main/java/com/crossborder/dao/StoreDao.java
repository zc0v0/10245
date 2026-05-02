package com.crossborder.dao;

import com.crossborder.entity.Store;
import com.crossborder.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StoreDao {

    public Store save(Store store) {
        String sql = "INSERT INTO stores (name, platform, store_url, api_key, api_secret, active) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, store.getName());
            pstmt.setString(2, store.getPlatform());
            pstmt.setString(3, store.getStoreUrl());
            pstmt.setString(4, store.getApiKey());
            pstmt.setString(5, store.getApiSecret());
            pstmt.setBoolean(6, store.isActive());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    store.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return store;
    }

    public Store findById(Long id) {
        String sql = "SELECT * FROM stores WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToStore(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Store> findAll() {
        List<Store> stores = new ArrayList<>();
        String sql = "SELECT * FROM stores ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                stores.add(mapResultSetToStore(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stores;
    }

    public List<Store> findActive() {
        List<Store> stores = new ArrayList<>();
        String sql = "SELECT * FROM stores WHERE active = 1 ORDER BY created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                stores.add(mapResultSetToStore(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stores;
    }

    public void update(Store store) {
        String sql = "UPDATE stores SET name = ?, platform = ?, store_url = ?, api_key = ?, api_secret = ?, active = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, store.getName());
            pstmt.setString(2, store.getPlatform());
            pstmt.setString(3, store.getStoreUrl());
            pstmt.setString(4, store.getApiKey());
            pstmt.setString(5, store.getApiSecret());
            pstmt.setBoolean(6, store.isActive());
            pstmt.setLong(7, store.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(Long id) {
        String sql = "DELETE FROM stores WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Store mapResultSetToStore(ResultSet rs) throws SQLException {
        Store store = new Store();
        store.setId(rs.getLong("id"));
        store.setName(rs.getString("name"));
        store.setPlatform(rs.getString("platform"));
        store.setStoreUrl(rs.getString("store_url"));
        store.setApiKey(rs.getString("api_key"));
        store.setApiSecret(rs.getString("api_secret"));
        store.setActive(rs.getBoolean("active"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            store.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            store.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return store;
    }
}
