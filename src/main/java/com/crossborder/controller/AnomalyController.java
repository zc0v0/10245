package com.crossborder.controller;

import com.crossborder.dao.ProductDao;
import com.crossborder.dao.ShipmentDao;
import com.crossborder.entity.Product;
import com.crossborder.entity.Shipment;
import com.crossborder.service.AnomalyDetectionService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class AnomalyController {

    @FXML
    private TableView<Product> outOfStockTable;
    @FXML
    private TableColumn<Product, String> oosNameCol;
    @FXML
    private TableColumn<Product, String> oosSkuCol;
    @FXML
    private TableColumn<Product, String> oosSalesCol;
    @FXML
    private TableColumn<Product, String> oosStockCol;
    @FXML
    private TableColumn<Product, String> oosSafetyCol;
    
    @FXML
    private TableView<Shipment> delayedShipmentsTable;
    @FXML
    private TableColumn<Shipment, String> dsOrderNoCol;
    @FXML
    private TableColumn<Shipment, String> dsTrackingCol;
    @FXML
    private TableColumn<Shipment, String> dsCarrierCol;
    @FXML
    private TableColumn<Shipment, String> dsDaysCol;
    @FXML
    private TableColumn<Shipment, String> dsStatusCol;
    
    @FXML
    private ComboBox<String> typeCombo;
    
    @FXML
    private Label totalAnomaliesLabel;
    @FXML
    private Label outOfStockLabel;
    @FXML
    private Label delayedShipmentsLabel;

    private AnomalyDetectionService anomalyService;
    private ProductDao productDao;
    private ShipmentDao shipmentDao;

    private List<Product> outOfStockProducts;
    private List<Shipment> delayedShipments;

    public void setServices(AnomalyDetectionService anomalyService, ProductDao productDao, ShipmentDao shipmentDao) {
        this.anomalyService = anomalyService;
        this.productDao = productDao;
        this.shipmentDao = shipmentDao;
    }

    @FXML
    public void initialize() {
        oosNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        oosSkuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        oosSalesCol.setCellValueFactory(new PropertyValueFactory<>("salesVolume"));
        oosStockCol.setCellValueFactory(cellData -> {
            int stock = cellData.getValue().getStockQuantity();
            String style = stock <= 5 ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "";
            return new SimpleStringProperty(String.valueOf(stock));
        });
        oosSafetyCol.setCellValueFactory(new PropertyValueFactory<>("safetyStock"));
        
        dsOrderNoCol.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        dsTrackingCol.setCellValueFactory(new PropertyValueFactory<>("trackingNumber"));
        dsCarrierCol.setCellValueFactory(new PropertyValueFactory<>("carrier"));
        dsDaysCol.setCellValueFactory(cellData -> {
            int days = cellData.getValue().getDaysInTransit();
            String style = days >= 7 ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "";
            return new SimpleStringProperty(String.valueOf(days));
        });
        dsStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        typeCombo.setValue("全部");
    }

    public void refreshData() {
        outOfStockProducts = productDao.findLowStock();
        delayedShipments = shipmentDao.findDelayed();
        
        outOfStockTable.getItems().clear();
        outOfStockTable.getItems().addAll(outOfStockProducts);
        
        delayedShipmentsTable.getItems().clear();
        delayedShipmentsTable.getItems().addAll(delayedShipments);
        
        updateStatistics();
    }

    private void updateStatistics() {
        int outOfStockCount = outOfStockProducts != null ? outOfStockProducts.size() : 0;
        int delayedCount = delayedShipments != null ? delayedShipments.size() : 0;
        int total = outOfStockCount + delayedCount;
        
        totalAnomaliesLabel.setText(String.valueOf(total));
        outOfStockLabel.setText(String.valueOf(outOfStockCount));
        delayedShipmentsLabel.setText(String.valueOf(delayedCount));
    }

    @FXML
    private void handleDetect() {
        int anomalies = anomalyService.runAllAnomalyChecks();
        showAlert(Alert.AlertType.INFORMATION, "检测完成", 
            "发现 " + anomalies + " 个异常项，已生成告警");
        refreshData();
    }

    @FXML
    private void handleRefresh() {
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
