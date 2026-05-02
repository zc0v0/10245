package com.crossborder.controller;

import com.crossborder.dao.*;
import com.crossborder.service.*;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML
    private Label storesCountLabel;
    @FXML
    private Label ordersCountLabel;
    @FXML
    private Label productsCountLabel;
    @FXML
    private Label revenueLabel;
    @FXML
    private Label pendingOrdersLabel;
    @FXML
    private Label shippedOrdersLabel;
    @FXML
    private Label deliveredOrdersLabel;
    @FXML
    private Label unreadAlertsLabel;
    @FXML
    private Label pendingAlertsLabel;

    private ReportExportService reportExportService;
    private OrderPullService orderPullService;
    private ProductCategoryService productCategoryService;
    private AnomalyDetectionService anomalyDetectionService;
    private RefundInterceptionService refundInterceptionService;
    
    private StoreDao storeDao;
    private OrderDao orderDao;
    private ProductDao productDao;
    private AlertDao alertDao;

    public void setServices(ReportExportService reportExportService, 
                            OrderPullService orderPullService,
                            ProductCategoryService productCategoryService,
                            AnomalyDetectionService anomalyDetectionService,
                            RefundInterceptionService refundInterceptionService,
                            StoreDao storeDao,
                            OrderDao orderDao,
                            ProductDao productDao,
                            AlertDao alertDao) {
        this.reportExportService = reportExportService;
        this.orderPullService = orderPullService;
        this.productCategoryService = productCategoryService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.refundInterceptionService = refundInterceptionService;
        this.storeDao = storeDao;
        this.orderDao = orderDao;
        this.productDao = productDao;
        this.alertDao = alertDao;
    }

    @FXML
    public void initialize() {
    }

    public void refreshData() {
        Map<String, Object> stats = reportExportService.getDashboardStatistics();
        
        storesCountLabel.setText(String.valueOf(stats.get("totalStores")));
        ordersCountLabel.setText(String.valueOf(stats.get("totalOrders")));
        productsCountLabel.setText(String.valueOf(stats.get("totalProducts")));
        
        BigDecimal revenue = (BigDecimal) stats.get("totalRevenue");
        revenueLabel.setText(String.format("$%,.2f", revenue));
        
        pendingOrdersLabel.setText(String.valueOf(stats.get("pendingOrders")));
        shippedOrdersLabel.setText(String.valueOf(stats.get("shippedOrders")));
        deliveredOrdersLabel.setText(String.valueOf(stats.get("deliveredOrders")));
        
        List<com.crossborder.entity.Alert> unreadAlerts = alertDao.findUnread();
        unreadAlertsLabel.setText(String.valueOf(unreadAlerts.size()));
        
        List<com.crossborder.entity.Alert> allAlerts = alertDao.findAll();
        long pendingCount = allAlerts.stream()
            .filter(a -> "PENDING".equals(a.getStatus()))
            .count();
        pendingAlertsLabel.setText(String.valueOf(pendingCount));
    }

    @FXML
    private void handleSyncOrders() {
        int pulled = orderPullService.pullOrdersFromAllStores();
        showAlert(Alert.AlertType.INFORMATION, "同步完成", 
            "成功从各平台拉取 " + pulled + " 条新订单");
        refreshData();
    }

    @FXML
    private void handleClassifyProducts() {
        int classified = productCategoryService.classifyAllProducts();
        showAlert(Alert.AlertType.INFORMATION, "分类完成", 
            "成功智能分类 " + classified + " 个商品");
        refreshData();
    }

    @FXML
    private void handleDetectAnomalies() {
        int anomalies = anomalyDetectionService.runAllAnomalyChecks();
        showAlert(Alert.AlertType.INFORMATION, "检测完成", 
            "发现 " + anomalies + " 个异常项，已生成告警");
        refreshData();
    }

    @FXML
    private void handleProcessRefunds() {
        int processed = refundInterceptionService.processPendingRefunds();
        showAlert(Alert.AlertType.INFORMATION, "处理完成", 
            "成功处理 " + processed + " 条退款申请");
        refreshData();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
