package com.crossborder.controller;

import com.crossborder.dao.*;
import com.crossborder.entity.Store;
import com.crossborder.service.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainController {

    @FXML
    private StackPane contentArea;
    
    @FXML
    private Label lastSyncLabel;
    
    @FXML
    private Label currentTimeLabel;
    
    @FXML
    private Label alertBadge;
    
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
    
    private Timeline clockTimeline;

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
        
        updateAlertBadge();
        
        startClock();

        dashboardBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadViewSafely("仪表板", this::loadDashboardView);
        });
        ordersBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadViewSafely("订单管理", this::loadOrdersView);
        });
        productsBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadViewSafely("商品管理", this::loadProductsView);
        });
        storesBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadViewSafely("店铺管理", this::loadStoresView);
        });
        anomalyBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadViewSafely("异常监控", this::loadAnomalyView);
        });
        alertsBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadViewSafely("告警中心", this::loadAlertsView);
        });
        reportsBtn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) loadViewSafely("报表分析", this::loadReportsView);
        });

        loadViewSafely("仪表板", this::loadDashboardView);
    }
    
    private void startClock() {
        clockTimeline = new Timeline(
            new KeyFrame(Duration.seconds(1), event -> {
                currentTimeLabel.setText(LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                ));
            })
        );
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }
    
    private void updateAlertBadge() {
        try {
            long unreadCount = alertDao.findUnread().size();
            if (unreadCount > 0) {
                alertBadge.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
                alertBadge.setVisible(true);
            } else {
                alertBadge.setVisible(false);
            }
        } catch (Exception e) {
            alertBadge.setVisible(false);
        }
    }
    
    @FunctionalInterface
    private interface ViewLoader {
        void load() throws IOException;
    }
    
    private void loadViewSafely(String viewName, ViewLoader loader) {
        try {
            loader.load();
            updateAlertBadge();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorView(viewName, e);
        }
    }
    
    private void showErrorView(String viewName, Exception e) {
        VBox errorView = new VBox();
        errorView.setAlignment(Pos.CENTER);
        errorView.setSpacing(20);
        errorView.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 40;");
        
        Label iconLabel = new Label("❌");
        iconLabel.setStyle("-fx-font-size: 48;");
        
        Label titleLabel = new Label("加载视图失败");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label nameLabel = new Label("视图: " + viewName);
        nameLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #7f8c8d;");
        
        Label messageLabel = new Label("错误: " + e.getMessage());
        messageLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #e74c3c; -fx-wrap-text: true;");
        messageLabel.setMaxWidth(500);
        
        Button retryBtn = new Button("重试");
        retryBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 10 25; -fx-background-radius: 6;");
        retryBtn.setOnAction(event -> handleRefresh());
        
        errorView.getChildren().addAll(iconLabel, titleLabel, nameLabel, messageLabel, retryBtn);
        contentArea.getChildren().setAll(errorView);
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
            loadViewSafely("仪表板", this::loadDashboardView);
        } else if (ordersBtn.isSelected()) {
            loadViewSafely("订单管理", this::loadOrdersView);
        } else if (productsBtn.isSelected()) {
            loadViewSafely("商品管理", this::loadProductsView);
        } else if (storesBtn.isSelected()) {
            loadViewSafely("店铺管理", this::loadStoresView);
        } else if (anomalyBtn.isSelected()) {
            loadViewSafely("异常监控", this::loadAnomalyView);
        } else if (alertsBtn.isSelected()) {
            loadViewSafely("告警中心", this::loadAlertsView);
        } else if (reportsBtn.isSelected()) {
            loadViewSafely("报表分析", this::loadReportsView);
        }
    }

    @FXML
    private void handleSyncOrders() {
        try {
            int pulled = orderPullService.pullOrdersFromAllStores();
            lastSyncLabel.setText("上次同步: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            showAlert(Alert.AlertType.INFORMATION, "同步完成", 
                "成功从各平台拉取 " + pulled + " 条新订单");
            
            handleRefresh();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "同步失败", 
                "同步订单时发生错误: " + e.getMessage());
        }
    }

    private void loadDashboardView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DashboardView.fxml"));
        Parent view = loader.load();
        DashboardController controller = loader.getController();
        controller.setServices(reportExportService, orderPullService, productCategoryService, 
                               anomalyDetectionService, refundInterceptionService,
                               storeDao, orderDao, productDao, alertDao);
        controller.refreshData();
        contentArea.getChildren().setAll(view);
    }

    private void loadOrdersView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/OrdersView.fxml"));
        Parent view = loader.load();
        OrdersController controller = loader.getController();
        controller.setServices(orderDao, storeDao);
        controller.refreshData();
        contentArea.getChildren().setAll(view);
    }

    private void loadProductsView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProductsView.fxml"));
        Parent view = loader.load();
        ProductsController controller = loader.getController();
        controller.setServices(productDao, productCategoryService, storeDao);
        controller.refreshData();
        contentArea.getChildren().setAll(view);
    }

    private void loadStoresView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/StoresView.fxml"));
        Parent view = loader.load();
        StoresController controller = loader.getController();
        controller.setServices(storeDao, orderPullService);
        controller.refreshData();
        contentArea.getChildren().setAll(view);
    }

    private void loadAnomalyView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AnomalyView.fxml"));
        Parent view = loader.load();
        AnomalyController controller = loader.getController();
        controller.setServices(anomalyDetectionService, productDao, shipmentDao);
        controller.refreshData();
        contentArea.getChildren().setAll(view);
    }

    private void loadAlertsView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AlertsView.fxml"));
        Parent view = loader.load();
        AlertsController controller = loader.getController();
        controller.setServices(alertDao);
        controller.refreshData();
        contentArea.getChildren().setAll(view);
    }

    private void loadReportsView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ReportsView.fxml"));
        Parent view = loader.load();
        ReportsController controller = loader.getController();
        controller.setServices(reportExportService, storeDao, productDao);
        controller.refreshData();
        contentArea.getChildren().setAll(view);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
