package com.crossborder.controller;

import com.crossborder.dao.*;
import com.crossborder.entity.Store;
import com.crossborder.service.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML
    private StackPane contentArea;
    
    @FXML
    private Label lastSyncLabel;
    
    @FXML
    private ToggleButton dashboardBtn;
    @FXML
    private ToggleButton ordersBtn;
    @FXML
    private ToggleButton productsBtn;
    @FXML
    private ToggleButton storesBtn;
    @FXML
    private ToggleButton anomalyBtn;
    @FXML
    private ToggleButton alertsBtn;
    @FXML
    private ToggleButton reportsBtn;

    private OrderPullService orderPullService;
    private ProductCategoryService productCategoryService;
    private AnomalyDetectionService anomalyDetectionService;
    private RefundInterceptionService refundInterceptionService;
    private ReportExportService reportExportService;

    private StoreDao storeDao;
    private OrderDao orderDao;
    private ProductDao productDao;
    private ShipmentDao shipmentDao;
    private RefundRequestDao refundRequestDao;
    private AlertDao alertDao;

    @FXML
    public void initialize() {
        storeDao = new StoreDao();
        orderDao = new OrderDao();
        productDao = new ProductDao();
        shipmentDao = new ShipmentDao();
        refundRequestDao = new RefundRequestDao();
        alertDao = new AlertDao();

        orderPullService = new OrderPullService(storeDao, orderDao);
        productCategoryService = new ProductCategoryService(productDao);
        anomalyDetectionService = new AnomalyDetectionService(productDao, shipmentDao, alertDao);
        refundInterceptionService = new RefundInterceptionService(refundRequestDao, alertDao);
        reportExportService = new ReportExportService(orderDao, productDao, storeDao);

        initializeSampleData();

        dashboardBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadDashboardView();
        });
        ordersBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadOrdersView();
        });
        productsBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadProductsView();
        });
        storesBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadStoresView();
        });
        anomalyBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadAnomalyView();
        });
        alertsBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadAlertsView();
        });
        reportsBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadReportsView();
        });

        loadDashboardView();
    }

    private void initializeSampleData() {
        if (storeDao.findAll().isEmpty()) {
            String[] platforms = {"Amazon", "eBay", "AliExpress", "Shopify"};
            String[] storeNames = {"优品跨境店", "全球精选", "海外仓直发", "跨境优品汇"};
            
            for (int i = 0; i < platforms.length; i++) {
                Store store = new Store();
                store.setName(storeNames[i] + " (" + platforms[i] + ")");
                store.setPlatform(platforms[i]);
                store.setActive(true);
                storeDao.save(store);
            }
        }
    }

    @FXML
    private void handleRefresh() {
        if (dashboardBtn.isSelected()) {
            loadDashboardView();
        } else if (ordersBtn.isSelected()) {
            loadOrdersView();
        } else if (productsBtn.isSelected()) {
            loadProductsView();
        } else if (storesBtn.isSelected()) {
            loadStoresView();
        } else if (anomalyBtn.isSelected()) {
            loadAnomalyView();
        } else if (alertsBtn.isSelected()) {
            loadAlertsView();
        } else if (reportsBtn.isSelected()) {
            loadReportsView();
        }
    }

    @FXML
    private void handleSyncOrders() {
        int pulled = orderPullService.pullOrdersFromAllStores();
        lastSyncLabel.setText("上次同步: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        showAlert(Alert.AlertType.INFORMATION, "同步完成", 
            "成功从各平台拉取 " + pulled + " 条新订单");
        
        handleRefresh();
    }

    private void loadDashboardView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DashboardView.fxml"));
            Parent view = loader.load();
            DashboardController controller = loader.getController();
            controller.setServices(reportExportService, orderPullService, productCategoryService, 
                                   anomalyDetectionService, refundInterceptionService,
                                   storeDao, orderDao, productDao, alertDao);
            controller.refreshData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载仪表板视图: " + e.getMessage());
        }
    }

    private void loadOrdersView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OrdersView.fxml"));
            Parent view = loader.load();
            OrdersController controller = loader.getController();
            controller.setServices(orderDao, storeDao);
            controller.refreshData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载订单管理视图: " + e.getMessage());
        }
    }

    private void loadProductsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductsView.fxml"));
            Parent view = loader.load();
            ProductsController controller = loader.getController();
            controller.setServices(productDao, productCategoryService, storeDao);
            controller.refreshData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载商品管理视图: " + e.getMessage());
        }
    }

    private void loadStoresView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StoresView.fxml"));
            Parent view = loader.load();
            StoresController controller = loader.getController();
            controller.setServices(storeDao, orderPullService);
            controller.refreshData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载店铺管理视图: " + e.getMessage());
        }
    }

    private void loadAnomalyView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AnomalyView.fxml"));
            Parent view = loader.load();
            AnomalyController controller = loader.getController();
            controller.setServices(anomalyDetectionService, productDao, shipmentDao);
            controller.refreshData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载异常监控视图: " + e.getMessage());
        }
    }

    private void loadAlertsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AlertsView.fxml"));
            Parent view = loader.load();
            AlertsController controller = loader.getController();
            controller.setServices(alertDao);
            controller.refreshData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载告警中心视图: " + e.getMessage());
        }
    }

    private void loadReportsView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReportsView.fxml"));
            Parent view = loader.load();
            ReportsController controller = loader.getController();
            controller.setServices(reportExportService, storeDao, productDao);
            controller.refreshData();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "错误", "无法加载报表分析视图: " + e.getMessage());
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
