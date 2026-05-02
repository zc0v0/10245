package com.crossborder.controller;

import com.crossborder.dao.OrderDao;
import com.crossborder.dao.StoreDao;
import com.crossborder.entity.Order;
import com.crossborder.service.OrderPullService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class OrdersController {

    @FXML
    private TableView<Order> ordersTable;
    @FXML
    private TableColumn<Order, String> orderNoCol;
    @FXML
    private TableColumn<Order, String> platformCol;
    @FXML
    private TableColumn<Order, String> buyerCol;
    @FXML
    private TableColumn<Order, String> amountCol;
    @FXML
    private TableColumn<Order, String> shippingCol;
    @FXML
    private TableColumn<Order, String> taxCol;
    @FXML
    private TableColumn<Order, String> statusCol;
    @FXML
    private TableColumn<Order, String> orderTimeCol;
    
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private ComboBox<String> platformCombo;
    
    @FXML
    private Label totalOrdersLabel;
    @FXML
    private Label pendingOrdersLabel;
    @FXML
    private Label shippedOrdersLabel;
    @FXML
    private Label deliveredOrdersLabel;

    private OrderDao orderDao;
    private StoreDao storeDao;
    private OrderPullService orderPullService;

    private List<Order> allOrders;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setServices(OrderDao orderDao, StoreDao storeDao) {
        this.orderDao = orderDao;
        this.storeDao = storeDao;
        this.orderPullService = new OrderPullService(storeDao, orderDao);
    }

    @FXML
    public void initialize() {
        orderNoCol.setCellValueFactory(new PropertyValueFactory<>("orderNo"));
        platformCol.setCellValueFactory(new PropertyValueFactory<>("platform"));
        buyerCol.setCellValueFactory(new PropertyValueFactory<>("buyerName"));
        
        amountCol.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getTotalAmount();
            return new SimpleStringProperty(amount != null ? String.format("$%,.2f", amount) : "$0.00");
        });
        
        shippingCol.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getShippingFee();
            return new SimpleStringProperty(amount != null ? String.format("$%,.2f", amount) : "$0.00");
        });
        
        taxCol.setCellValueFactory(cellData -> {
            BigDecimal amount = cellData.getValue().getTax();
            return new SimpleStringProperty(amount != null ? String.format("$%,.2f", amount) : "$0.00");
        });
        
        statusCol.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            String displayStatus = switch (status) {
                case "PENDING" -> "待处理";
                case "PAID" -> "已付款";
                case "SHIPPED" -> "已发货";
                case "DELIVERED" -> "已完成";
                default -> status;
            };
            return new SimpleStringProperty(displayStatus);
        });
        
        orderTimeCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getOrderTime() != null) {
                return new SimpleStringProperty(cellData.getValue().getOrderTime().format(dateFormatter));
            }
            return new SimpleStringProperty("--");
        });
        
        statusCombo.setValue("全部");
        platformCombo.setValue("全部");
    }

    public void refreshData() {
        allOrders = orderDao.findAll();
        updateTable(allOrders);
        updateStatistics();
    }

    private void updateTable(List<Order> orders) {
        ordersTable.getItems().clear();
        ordersTable.getItems().addAll(orders);
    }

    private void updateStatistics() {
        if (allOrders == null) return;
        
        totalOrdersLabel.setText(String.valueOf(allOrders.size()));
        
        long pendingCount = allOrders.stream()
            .filter(o -> "PENDING".equals(o.getStatus()) || "PAID".equals(o.getStatus()))
            .count();
        pendingOrdersLabel.setText(String.valueOf(pendingCount));
        
        long shippedCount = allOrders.stream()
            .filter(o -> "SHIPPED".equals(o.getStatus()))
            .count();
        shippedOrdersLabel.setText(String.valueOf(shippedCount));
        
        long deliveredCount = allOrders.stream()
            .filter(o -> "DELIVERED".equals(o.getStatus()))
            .count();
        deliveredOrdersLabel.setText(String.valueOf(deliveredCount));
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        String statusFilter = statusCombo.getValue();
        String platformFilter = platformCombo.getValue();
        
        List<Order> filtered = allOrders.stream()
            .filter(o -> {
                boolean matchesKeyword = keyword.isEmpty() ||
                    o.getOrderNo().toLowerCase().contains(keyword) ||
                    (o.getBuyerName() != null && o.getBuyerName().toLowerCase().contains(keyword));
                
                boolean matchesStatus = "全部".equals(statusFilter) ||
                    statusFilter.equals(o.getStatus());
                
                boolean matchesPlatform = "全部".equals(platformFilter) ||
                    platformFilter.equals(o.getPlatform());
                
                return matchesKeyword && matchesStatus && matchesPlatform;
            })
            .collect(Collectors.toList());
        
        updateTable(filtered);
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        statusCombo.setValue("全部");
        platformCombo.setValue("全部");
        refreshData();
    }

    @FXML
    private void handleSyncOrders() {
        int pulled = orderPullService.pullOrdersFromAllStores();
        showAlert(Alert.AlertType.INFORMATION, "同步完成", 
            "成功从各平台拉取 " + pulled + " 条新订单");
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
