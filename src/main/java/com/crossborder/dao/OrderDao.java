package com.crossborder.dao;

import com.crossborder.entity.Order;
import com.crossborder.entity.OrderItem;
import com.crossborder.util.DatabaseUtil;
import com.crossborder.util.TimestampUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    public Order save(Order order) {
        String sql = "INSERT INTO orders (order_no, store_id, platform, buyer_name, buyer_email, shipping_address, phone, total_amount, shipping_fee, discount, tax, status, payment_method, currency, order_time, payment_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, order.getOrderNo());
            pstmt.setObject(2, order.getStoreId());
            pstmt.setString(3, order.getPlatform());
            pstmt.setString(4, order.getBuyerName());
            pstmt.setString(5, order.getBuyerEmail());
            pstmt.setString(6, order.getShippingAddress());
            pstmt.setString(7, order.getPhone());
            pstmt.setObject(8, order.getTotalAmount());
            pstmt.setObject(9, order.getShippingFee());
            pstmt.setObject(10, order.getDiscount());
            pstmt.setObject(11, order.getTax());
            pstmt.setString(12, order.getStatus());
            pstmt.setString(13, order.getPaymentMethod());
            pstmt.setString(14, order.getCurrency());
            pstmt.setString(15, TimestampUtil.formatForSqlite(order.getOrderTime()));
            pstmt.setString(16, TimestampUtil.formatForSqlite(order.getPaymentTime()));
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    order.setId(generatedKeys.getLong(1));
                }
            }
            
            if (order.getOrderItems() != null) {
                for (OrderItem item : order.getOrderItems()) {
                    item.setOrderId(order.getId());
                    saveOrderItem(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return order;
    }

    private void saveOrderItem(OrderItem item) {
        String sql = "INSERT INTO order_items (order_id, product_id, product_name, sku, quantity, unit_price, total_price, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setObject(1, item.getOrderId());
            pstmt.setObject(2, item.getProductId());
            pstmt.setString(3, item.getProductName());
            pstmt.setString(4, item.getSku());
            pstmt.setInt(5, item.getQuantity());
            pstmt.setObject(6, item.getUnitPrice());
            pstmt.setObject(7, item.getTotalPrice());
            pstmt.setString(8, item.getStatus());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Order findById(Long id) {
        String sql = "SELECT * FROM orders WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setOrderItems(findOrderItemsByOrderId(order.getId()));
                return order;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Order findByOrderNo(String orderNo) {
        String sql = "SELECT * FROM orders WHERE order_no = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, orderNo);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setOrderItems(findOrderItemsByOrderId(order.getId()));
                return order;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders ORDER BY order_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setOrderItems(findOrderItemsByOrderId(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public List<Order> findByStoreId(Long storeId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT * FROM orders WHERE store_id = ? ORDER BY order_time DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, storeId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setOrderItems(findOrderItemsByOrderId(order.getId()));
                orders.add(order);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public void update(Order order) {
        String sql = "UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, order.getStatus());
            pstmt.setLong(2, order.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private List<OrderItem> findOrderItemsByOrderId(Long orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setId(rs.getLong("id"));
                item.setOrderId(rs.getLong("order_id"));
                item.setProductId(rs.getObject("product_id") != null ? rs.getLong("product_id") : null);
                item.setProductName(rs.getString("product_name"));
                item.setSku(rs.getString("sku"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitPrice(rs.getBigDecimal("unit_price"));
                item.setTotalPrice(rs.getBigDecimal("total_price"));
                item.setStatus(rs.getString("status"));
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setId(rs.getLong("id"));
        order.setOrderNo(rs.getString("order_no"));
        order.setStoreId(rs.getObject("store_id") != null ? rs.getLong("store_id") : null);
        order.setPlatform(rs.getString("platform"));
        order.setBuyerName(rs.getString("buyer_name"));
        order.setBuyerEmail(rs.getString("buyer_email"));
        order.setShippingAddress(rs.getString("shipping_address"));
        order.setPhone(rs.getString("phone"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setShippingFee(rs.getBigDecimal("shipping_fee"));
        order.setDiscount(rs.getBigDecimal("discount"));
        order.setTax(rs.getBigDecimal("tax"));
        order.setStatus(rs.getString("status"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setCurrency(rs.getString("currency"));
        
        order.setOrderTime(TimestampUtil.getLocalDateTime(rs, "order_time"));
        order.setPaymentTime(TimestampUtil.getLocalDateTime(rs, "payment_time"));
        order.setCreatedAt(TimestampUtil.getLocalDateTime(rs, "created_at"));
        order.setUpdatedAt(TimestampUtil.getLocalDateTime(rs, "updated_at"));
        
        return order;
    }
}
