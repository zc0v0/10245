package com.crossborder.dao;

import com.crossborder.entity.Shipment;
import com.crossborder.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ShipmentDao {

    public Shipment save(Shipment shipment) {
        String sql = "INSERT INTO shipments (order_no, tracking_number, carrier, status, shipped_time, estimated_delivery_time, current_location, last_update, days_in_transit, is_delayed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, shipment.getOrderNo());
            pstmt.setString(2, shipment.getTrackingNumber());
            pstmt.setString(3, shipment.getCarrier());
            pstmt.setString(4, shipment.getStatus());
            pstmt.setObject(5, shipment.getShippedTime());
            pstmt.setObject(6, shipment.getEstimatedDeliveryTime());
            pstmt.setString(7, shipment.getCurrentLocation());
            pstmt.setString(8, shipment.getLastUpdate());
            pstmt.setInt(9, shipment.getDaysInTransit());
            pstmt.setBoolean(10, shipment.isDelayed());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    shipment.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shipment;
    }

    public Shipment findById(Long id) {
        String sql = "SELECT * FROM shipments WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToShipment(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Shipment findByOrderNo(String orderNo) {
        String sql = "SELECT * FROM shipments WHERE order_no = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, orderNo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToShipment(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Shipment> findDelayed() {
        List<Shipment> shipments = new ArrayList<>();
        String sql = "SELECT * FROM shipments WHERE is_delayed = 1 AND status != 'DELIVERED' ORDER BY days_in_transit DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                shipments.add(mapResultSetToShipment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return shipments;
    }

    public void update(Shipment shipment) {
        String sql = "UPDATE shipments SET status = ?, current_location = ?, last_update = ?, days_in_transit = ?, is_delayed = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, shipment.getStatus());
            pstmt.setString(2, shipment.getCurrentLocation());
            pstmt.setString(3, shipment.getLastUpdate());
            pstmt.setInt(4, shipment.getDaysInTransit());
            pstmt.setBoolean(5, shipment.isDelayed());
            pstmt.setLong(6, shipment.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Shipment mapResultSetToShipment(ResultSet rs) throws SQLException {
        Shipment shipment = new Shipment();
        shipment.setId(rs.getLong("id"));
        shipment.setOrderNo(rs.getString("order_no"));
        shipment.setTrackingNumber(rs.getString("tracking_number"));
        shipment.setCarrier(rs.getString("carrier"));
        shipment.setStatus(rs.getString("status"));
        shipment.setCurrentLocation(rs.getString("current_location"));
        shipment.setLastUpdate(rs.getString("last_update"));
        shipment.setDaysInTransit(rs.getInt("days_in_transit"));
        shipment.setDelayed(rs.getBoolean("is_delayed"));
        
        Timestamp shippedTime = rs.getTimestamp("shipped_time");
        if (shippedTime != null) {
            shipment.setShippedTime(shippedTime.toLocalDateTime());
        }
        Timestamp estimatedDeliveryTime = rs.getTimestamp("estimated_delivery_time");
        if (estimatedDeliveryTime != null) {
            shipment.setEstimatedDeliveryTime(estimatedDeliveryTime.toLocalDateTime());
        }
        Timestamp actualDeliveryTime = rs.getTimestamp("actual_delivery_time");
        if (actualDeliveryTime != null) {
            shipment.setActualDeliveryTime(actualDeliveryTime.toLocalDateTime());
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            shipment.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            shipment.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return shipment;
    }
}
