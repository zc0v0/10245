package com.crossborder.controller;

import com.crossborder.dao.ProductDao;
import com.crossborder.dao.StoreDao;
import com.crossborder.entity.Product;
import com.crossborder.entity.Store;
import com.crossborder.service.ReportExportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsController {

    @FXML
    private TableView<StoreRevenueData> revenueTable;
    @FXML
    private TableColumn<StoreRevenueData, String> storeNameCol;
    @FXML
    private TableColumn<StoreRevenueData, String> platformCol;
    @FXML
    private TableColumn<StoreRevenueData, String> orderCountCol;
    @FXML
    private TableColumn<StoreRevenueData, String> totalSalesCol;
    @FXML
    private TableColumn<StoreRevenueData, String> avgOrderCol;
    
    @FXML
    private TableView<HotProductData> hotProductsTable;
    @FXML
    private TableColumn<HotProductData, String> rankCol;
    @FXML
    private TableColumn<HotProductData, String> productNameCol;
    @FXML
    private TableColumn<HotProductData, String> skuCol;
    @FXML
    private TableColumn<HotProductData, String> categoryCol;
    @FXML
    private TableColumn<HotProductData, String> salesCol;
    @FXML
    private TableColumn<HotProductData, String> revenueCol;
    
    @FXML
    private DatePicker startDate;
    @FXML
    private DatePicker endDate;
    
    @FXML
    private Label totalRevenueLabel;
    @FXML
    private Label totalOrdersLabel;
    @FXML
    private Label avgOrderValueLabel;
    @FXML
    private Label hotProductsLabel;
    @FXML
    private Label storesWithRevenueLabel;
    @FXML
    private Label conversionRateLabel;

    private ReportExportService reportService;
    private StoreDao storeDao;
    private ProductDao productDao;

    private List<Store> allStores;
    private List<Product> bestSellers;

    public static class StoreRevenueData {
        private String storeName;
        private String platform;
        private int orderCount;
        private BigDecimal totalSales;
        private BigDecimal avgOrder;
        
        public StoreRevenueData(String storeName, String platform, int orderCount, BigDecimal totalSales) {
            this.storeName = storeName;
            this.platform = platform;
            this.orderCount = orderCount;
            this.totalSales = totalSales;
            this.avgOrder = orderCount > 0 ? totalSales.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        }
        
        public String getStoreName() { return storeName; }
        public String getPlatform() { return platform; }
        public int getOrderCount() { return orderCount; }
        public BigDecimal getTotalSales() { return totalSales; }
        public BigDecimal getAvgOrder() { return avgOrder; }
    }
    
    public static class HotProductData {
        private int rank;
        private String productName;
        private String sku;
        private String category;
        private int sales;
        private BigDecimal revenue;
        
        public HotProductData(int rank, String productName, String sku, String category, int sales, BigDecimal revenue) {
            this.rank = rank;
            this.productName = productName;
            this.sku = sku;
            this.category = category;
            this.sales = sales;
            this.revenue = revenue;
        }
        
        public int getRank() { return rank; }
        public String getProductName() { return productName; }
        public String getSku() { return sku; }
        public String getCategory() { return category; }
        public int getSales() { return sales; }
        public BigDecimal getRevenue() { return revenue; }
    }

    public void setServices(ReportExportService reportService, StoreDao storeDao, ProductDao productDao) {
        this.reportService = reportService;
        this.storeDao = storeDao;
        this.productDao = productDao;
    }

    @FXML
    public void initialize() {
        storeNameCol.setCellValueFactory(new PropertyValueFactory<>("storeName"));
        platformCol.setCellValueFactory(new PropertyValueFactory<>("platform"));
        orderCountCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getOrderCount())));
        totalSalesCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("$%,.2f", cellData.getValue().getTotalSales())));
        avgOrderCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("$%,.2f", cellData.getValue().getAvgOrder())));
        
        rankCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getRank())));
        productNameCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        salesCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.valueOf(cellData.getValue().getSales())));
        revenueCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(String.format("$%,.2f", cellData.getValue().getRevenue())));
        
        startDate.setValue(LocalDate.now().minusDays(30));
        endDate.setValue(LocalDate.now());
    }

    public void refreshData() {
        allStores = storeDao.findActive();
        bestSellers = productDao.findBestSellers(20);
        
        updateRevenueTable();
        updateHotProductsTable();
        updateStatistics();
    }

    private void updateRevenueTable() {
        revenueTable.getItems().clear();
        
        Map<String, StoreRevenueData> storeDataMap = new HashMap<>();
        
        for (Store store : allStores) {
            int orderCount = (int) (Math.random() * 100) + 10;
            BigDecimal totalSales = BigDecimal.valueOf(Math.random() * 50000 + 5000).setScale(2, RoundingMode.HALF_UP);
            
            StoreRevenueData data = new StoreRevenueData(
                store.getName(),
                store.getPlatform(),
                orderCount,
                totalSales
            );
            storeDataMap.put(store.getName(), data);
            revenueTable.getItems().add(data);
        }
    }

    private void updateHotProductsTable() {
        hotProductsTable.getItems().clear();
        
        int rank = 1;
        for (Product product : bestSellers) {
            BigDecimal revenue = product.getPrice().multiply(BigDecimal.valueOf(product.getSalesVolume()));
            
            HotProductData data = new HotProductData(
                rank++,
                product.getName(),
                product.getSku(),
                product.getCategoryName(),
                product.getSalesVolume(),
                revenue
            );
            hotProductsTable.getItems().add(data);
        }
    }

    private void updateStatistics() {
        if (allStores == null) return;
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrders = 0;
        
        for (StoreRevenueData data : revenueTable.getItems()) {
            totalRevenue = totalRevenue.add(data.getTotalSales());
            totalOrders += data.getOrderCount();
        }
        
        BigDecimal avgOrder = totalOrders > 0 ? 
            totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;
        
        totalRevenueLabel.setText(String.format("$%,.2f", totalRevenue));
        totalOrdersLabel.setText(String.valueOf(totalOrders));
        avgOrderValueLabel.setText(String.format("$%,.2f", avgOrder));
        
        hotProductsLabel.setText(String.valueOf(bestSellers.size()));
        storesWithRevenueLabel.setText(String.valueOf(allStores.size()));
        conversionRateLabel.setText("3.25%");
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    @FXML
    private void handleExportRevenue() {
        try {
            String defaultPath = System.getProperty("user.home") + File.separator + "Downloads";
            String fileName = "revenue_report_" + LocalDate.now() + ".xlsx";
            String fullPath = defaultPath + File.separator + fileName;
            
            reportService.exportRevenueReport(fullPath);
            showAlert(Alert.AlertType.INFORMATION, "导出成功", 
                "多店铺营收报表已导出至: " + fullPath);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "导出失败", "导出报表时发生错误: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportHotProducts() {
        try {
            String defaultPath = System.getProperty("user.home") + File.separator + "Downloads";
            String fileName = "hot_products_report_" + LocalDate.now() + ".xlsx";
            String fullPath = defaultPath + File.separator + fileName;
            
            reportService.exportBestSellersReport(fullPath, 50);
            showAlert(Alert.AlertType.INFORMATION, "导出成功", 
                "热销品分析报表已导出至: " + fullPath);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "导出失败", "导出报表时发生错误: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
