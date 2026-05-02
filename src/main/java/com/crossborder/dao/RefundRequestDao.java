package com.crossborder.dao;

import com.crossborder.entity.RefundRequest;
import com.crossborder.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RefundRequestDao {

    public RefundRequest save(RefundRequest refund) {
        String sql = "INSERT INTO refund_requests (refund_no, order_no, store_id, platform, buyer_name, refund_amount, reason, status, is_malicious, risk_reason, risk_score, request_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, refund.getRefundNo());
            pstmt.setString(2, refund.getOrderNo());
            pstmt.setObject(3, refund.getStoreId());
            pstmt.setString(4, refund.getPlatform());
            pstmt.setString(5, refund.getBuyerName());
            pstmt.setObject(6, refund.getRefundAmount());
            pstmt.setString(7, refund.getReason());
            pstmt.setString(8, refund.getStatus());
            pstmt.setBoolean(9, refund.isMalicious());
            pstmt.setString(10, refund.getRiskReason());
            pstmt.setObject(11, refund.getRiskScore());
            pstmt.setObject(12, refund.getRequestTime());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    refund.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return refund;
    }

    public RefundRequest findById(Long id) {
        String sql = "SELECT * FROM refund_requests WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToRefundRequest(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<RefundRequest> findAll() {
        List<RefundRequest> refunds = new ArrayList<>();
        String sql = "SELECT * FROM refund_requests ORDER BY request_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                refunds.add(mapResultSetToRefundRequest(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return refunds;
    }

    public List<RefundRequest> findPending() {
        List<RefundRequest> refunds = new ArrayList<>();
        String sql = "SELECT * FROM refund_requests WHERE status = 'PENDING' ORDER BY request_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                refunds.add(mapResultSetToRefundRequest(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return refunds;
    }

    public List<RefundRequest> findMalicious() {
        List<RefundRequest> refunds = new ArrayList<>();
        String sql = "SELECT * FROM refund_requests WHERE is_malicious = 1 ORDER BY request_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                refunds.add(mapResultSetToRefundRequest(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return refunds;
    }

    public void update(RefundRequest refund) {
        String sql = "UPDATE refund_requests SET status = ?, is_malicious = ?, risk_reason = ?, risk_score = ?, processed_time = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, refund.getStatus());
            pstmt.setBoolean(2, refund.isMalicious());
            pstmt.setString(3, refund.getRiskReason());
            pstmt.setObject(4, refund.getRiskScore());
            pstmt.setLong(5, refund.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private RefundRequest mapResultSetToRefundRequest(ResultSet rs) throws SQLException {
        RefundRequest refund = new RefundRequest();
        refund.setId(rs.getLong("id"));
        refund.setRefundNo(rs.getString("refund_no"));
        refund.setOrderNo(rs.getString("order_no"));
        refund.setStoreId(rs.getObject("store_id") != null ? rs.getLong("store_id") : null);
        refund.setPlatform(rs.getString("platform"));
        refund.setBuyerName(rs.getString("buyer_name"));
        refund.setRefundAmount(rs.getBigDecimal("refund_amount"));
        refund.setReason(rs.getString("reason"));
        refund.setStatus(rs.getString("status"));
        refund.setMalicious(rs.getBoolean("is_malicious"));
        refund.setRiskReason(rs.getString("risk_reason"));
        refund.setRiskScore(rs.getDouble("risk_score"));
        
        Timestamp requestTime = rs.getTimestamp("request_time");
        if (requestTime != null) {
            refund.setRequestTime(requestTime.toLocalDateTime());
        }
        Timestamp processedTime = rs.getTimestamp("processed_time");
        if (processedTime != null) {
            refund.setProcessedTime(processedTime.toLocalDateTime());
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            refund.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            refund.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return refund;
    }
}
