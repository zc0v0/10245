package com.crossborder.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {
    private static final String DB_URL = "jdbc:sqlite:store_management.db";
    private static boolean initialized = false;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            initializeDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void initializeDatabase() throws SQLException {
        if (initialized) return;
        
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            String createStoresTable = """
                CREATE TABLE IF NOT EXISTS stores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    platform TEXT NOT NULL,
                    store_url TEXT,
                    api_key TEXT,
                    api_secret TEXT,
                    active INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            String createOrdersTable = """
                CREATE TABLE IF NOT EXISTS orders (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_no TEXT NOT NULL UNIQUE,
                    store_id INTEGER,
                    platform TEXT,
                    buyer_name TEXT,
                    buyer_email TEXT,
                    shipping_address TEXT,
                    phone TEXT,
                    total_amount DECIMAL(10,2),
                    shipping_fee DECIMAL(10,2),
                    discount DECIMAL(10,2),
                    tax DECIMAL(10,2),
                    status TEXT,
                    payment_method TEXT,
                    currency TEXT,
                    order_time TIMESTAMP,
                    payment_time TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            String createOrderItemsTable = """
                CREATE TABLE IF NOT EXISTS order_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_id INTEGER,
                    product_id INTEGER,
                    product_name TEXT,
                    sku TEXT,
                    quantity INTEGER,
                    unit_price DECIMAL(10,2),
                    total_price DECIMAL(10,2),
                    status TEXT
                )
                """;

            String createProductsTable = """
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    store_id INTEGER,
                    platform TEXT,
                    product_id TEXT,
                    sku TEXT,
                    name TEXT,
                    description TEXT,
                    category_id TEXT,
                    category_name TEXT,
                    price DECIMAL(10,2),
                    cost_price DECIMAL(10,2),
                    stock_quantity INTEGER,
                    safety_stock INTEGER DEFAULT 10,
                    sales_volume INTEGER DEFAULT 0,
                    status TEXT,
                    image_url TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            String createCategoriesTable = """
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_id TEXT,
                    name TEXT,
                    parent_id TEXT,
                    platform TEXT,
                    product_count INTEGER DEFAULT 0
                )
                """;

            String createShipmentsTable = """
                CREATE TABLE IF NOT EXISTS shipments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    order_no TEXT,
                    tracking_number TEXT,
                    carrier TEXT,
                    status TEXT,
                    shipped_time TIMESTAMP,
                    estimated_delivery_time TIMESTAMP,
                    actual_delivery_time TIMESTAMP,
                    current_location TEXT,
                    last_update TEXT,
                    days_in_transit INTEGER,
                    is_delayed INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            String createRefundRequestsTable = """
                CREATE TABLE IF NOT EXISTS refund_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    refund_no TEXT UNIQUE,
                    order_no TEXT,
                    store_id INTEGER,
                    platform TEXT,
                    buyer_name TEXT,
                    refund_amount DECIMAL(10,2),
                    reason TEXT,
                    status TEXT,
                    is_malicious INTEGER DEFAULT 0,
                    risk_reason TEXT,
                    risk_score DECIMAL(5,2),
                    request_time TIMESTAMP,
                    processed_time TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            String createAlertsTable = """
                CREATE TABLE IF NOT EXISTS alerts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    alert_type TEXT,
                    title TEXT,
                    content TEXT,
                    store_id INTEGER,
                    platform TEXT,
                    related_id TEXT,
                    priority TEXT,
                    status TEXT,
                    is_read INTEGER DEFAULT 0,
                    alert_time TIMESTAMP,
                    processed_time TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """;

            stmt.execute(createStoresTable);
            stmt.execute(createOrdersTable);
            stmt.execute(createOrderItemsTable);
            stmt.execute(createProductsTable);
            stmt.execute(createCategoriesTable);
            stmt.execute(createShipmentsTable);
            stmt.execute(createRefundRequestsTable);
            stmt.execute(createAlertsTable);
            
            initialized = true;
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }
}
