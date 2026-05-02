package com.crossborder.controller;

import com.crossborder.dao.AlertDao;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AlertsController {

    @FXML
    private TableView<com.crossborder.entity.Alert> alertsTable;
    @FXML
    private TableColumn<com.crossborder.entity.Alert, String> priorityCol;
    @FXML
    private TableColumn<com.crossborder.entity.Alert, String> typeCol;
    @FXML
    private TableColumn<com.crossborder.entity.Alert, String> titleCol;
    @FXML
    private TableColumn<com.crossborder.entity.Alert, String> contentCol;
    @FXML
    private TableColumn<com.crossborder.entity.Alert, String> statusCol;
    @FXML
    private TableColumn<com.crossborder.entity.Alert, String> createdAtCol;
    @FXML
    private TableColumn<com.crossborder.entity.Alert, Void> actionCol;
    
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private ComboBox<String> statusCombo;
    
    @FXML
    private Label unreadBadge;
    @FXML
    private Label totalAlertsLabel;
    @FXML
    private Label unreadAlertsLabel;
    @FXML
    private Label highPriorityLabel;

    private AlertDao alertDao;

    private List<com.crossborder.entity.Alert> allAlerts;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setService(AlertDao alertDao) {
        this.alertDao = alertDao;
    }

    public void setServices(AlertDao alertDao) {
        this.alertDao = alertDao;
    }

    @FXML
    public void initialize() {
        priorityCol.setCellValueFactory(cellData -> {
            String priority = cellData.getValue().getPriority();
            String display = priority != null ? priority : "NORMAL";
            return new SimpleStringProperty(display);
        });
        
        typeCol.setCellValueFactory(new PropertyValueFactory<>("alertType"));
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        contentCol.setCellValueFactory(new PropertyValueFactory<>("content"));
        
        statusCol.setCellValueFactory(cellData -> {
            boolean isRead = cellData.getValue().isRead();
            String display = isRead ? "已读" : "未读";
            return new SimpleStringProperty(display);
        });
        
        createdAtCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(dateFormatter));
            }
            return new SimpleStringProperty("--");
        });
        
        setupActionColumn();
        
        typeCombo.setValue("全部");
        statusCombo.setValue("全部");
    }

    private void setupActionColumn() {
        Callback<TableColumn<com.crossborder.entity.Alert, Void>, TableCell<com.crossborder.entity.Alert, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<com.crossborder.entity.Alert, Void> call(final TableColumn<com.crossborder.entity.Alert, Void> param) {
                return new TableCell<>() {
                    private final Button markReadBtn = new Button("标记已读");
                    
                    {
                        markReadBtn.setOnAction(event -> {
                            com.crossborder.entity.Alert alert = getTableView().getItems().get(getIndex());
                            handleMarkAsRead(alert);
                        });
                        
                        markReadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11;");
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            com.crossborder.entity.Alert alert = getTableView().getItems().get(getIndex());
                            if (!alert.isRead()) {
                                setGraphic(markReadBtn);
                            } else {
                                setGraphic(null);
                            }
                        }
                    }
                };
            }
        };
        actionCol.setCellFactory(cellFactory);
    }

    public void refreshData() {
        allAlerts = alertDao.findAll();
        updateTable(allAlerts);
        updateStatistics();
    }

    private void updateTable(List<com.crossborder.entity.Alert> alerts) {
        alertsTable.getItems().clear();
        alertsTable.getItems().addAll(alerts);
    }

    private void updateStatistics() {
        if (allAlerts == null) return;
        
        long unreadCount = allAlerts.stream()
            .filter(a -> !a.isRead())
            .count();
        
        long highPriorityCount = allAlerts.stream()
            .filter(a -> "HIGH".equals(a.getPriority()) || "CRITICAL".equals(a.getPriority()))
            .count();
        
        totalAlertsLabel.setText(String.valueOf(allAlerts.size()));
        unreadAlertsLabel.setText(String.valueOf(unreadCount));
        unreadBadge.setText(String.valueOf(unreadCount));
        highPriorityLabel.setText(String.valueOf(highPriorityCount));
    }

    @FXML
    private void handleMarkAllRead() {
        alertDao.markAllAsRead();
        showAlert(Alert.AlertType.INFORMATION, "操作完成", 
            "所有告警已标记为已读");
        refreshData();
    }

    @FXML
    private void handleRefresh() {
        typeCombo.setValue("全部");
        statusCombo.setValue("全部");
        refreshData();
    }

    private void handleMarkAsRead(com.crossborder.entity.Alert alert) {
        alertDao.markAsRead(alert.getId());
        refreshData();
    }

    @FXML
    private void handleFilter() {
        String typeFilter = typeCombo.getValue();
        String statusFilter = statusCombo.getValue();
        
        List<com.crossborder.entity.Alert> filtered = allAlerts.stream()
            .filter(a -> {
                boolean matchesType = "全部".equals(typeFilter) ||
                    (a.getAlertType() != null && a.getAlertType().equals(typeFilter));
                
                boolean matchesStatus = "全部".equals(statusFilter) ||
                    ("未读".equals(statusFilter) && !a.isRead()) ||
                    ("已读".equals(statusFilter) && a.isRead());
                
                return matchesType && matchesStatus;
            })
            .collect(Collectors.toList());
        
        updateTable(filtered);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
