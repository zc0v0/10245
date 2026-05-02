package com.crossborder.dao;

import com.crossborder.entity.Product;
import com.crossborder.util.DatabaseUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public Product save(Product product) {
        String sql = "INSERT INTO products (store_id, platform, product_id, sku, name, description, category_id, category_name, price, cost_price, stock_quantity, safety_stock, sales_volume, status, image_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setObject(1, product.getStoreId());
            pstmt.setString(2, product.getPlatform());
            pstmt.setString(3, product.getProductId());
            pstmt.setString(4, product.getSku());
            pstmt.setString(5, product.getName());
            pstmt.setString(6, product.getDescription());
            pstmt.setString(7, product.getCategoryId());
            pstmt.setString(8, product.getCategoryName());
            pstmt.setObject(9, product.getPrice());
            pstmt.setObject(10, product.getCostPrice());
            pstmt.setInt(11, product.getStockQuantity());
            pstmt.setInt(12, product.getSafetyStock());
            pstmt.setInt(13, product.getSalesVolume());
            pstmt.setString(14, product.getStatus());
            pstmt.setString(15, product.getImageUrl());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return product;
    }

    public Product findById(Long id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProduct(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY sales_volume DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<Product> findByStoreId(Long storeId) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE store_id = ? ORDER BY sales_volume DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, storeId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<Product> findBestSellers(int limit) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY sales_volume DESC LIMIT ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public List<Product> findLowStock() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE stock_quantity <= safety_stock ORDER BY stock_quantity ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public void update(Product product) {
        String sql = "UPDATE products SET price = ?, cost_price = ?, stock_quantity = ?, sales_volume = ?, status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setObject(1, product.getPrice());
            pstmt.setObject(2, product.getCostPrice());
            pstmt.setInt(3, product.getStockQuantity());
            pstmt.setInt(4, product.getSalesVolume());
            pstmt.setString(5, product.getStatus());
            pstmt.setLong(6, product.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateStock(Long productId, int quantity) {
        String sql = "UPDATE products SET stock_quantity = stock_quantity + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, quantity);
            pstmt.setLong(2, productId);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setId(rs.getLong("id"));
        product.setStoreId(rs.getObject("store_id") != null ? rs.getLong("store_id") : null);
        product.setPlatform(rs.getString("platform"));
        product.setProductId(rs.getString("product_id"));
        product.setSku(rs.getString("sku"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setCategoryId(rs.getString("category_id"));
        product.setCategoryName(rs.getString("category_name"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setCostPrice(rs.getBigDecimal("cost_price"));
        product.setStockQuantity(rs.getInt("stock_quantity"));
        product.setSafetyStock(rs.getInt("safety_stock"));
        product.setSalesVolume(rs.getInt("sales_volume"));
        product.setStatus(rs.getString("status"));
        product.setImageUrl(rs.getString("image_url"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            product.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            product.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        return product;
    }
}
