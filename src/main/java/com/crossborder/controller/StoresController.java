package com.crossborder.controller;

import com.crossborder.dao.StoreDao;
import com.crossborder.entity.Store;
import com.crossborder.service.OrderPullService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class StoresController {

    @FXML
    private TableView<Store> storesTable;
    @FXML
    private TableColumn<Store, String> nameCol;
    @FXML
    private TableColumn<Store, String> platformCol;
    @FXML
    private TableColumn<Store, String> storeUrlCol;
    @FXML
    private TableColumn<Store, String> activeCol;
    @FXML
    private TableColumn<Store, String> createdAtCol;
    @FXML
    private TableColumn<Store, Void> actionCol;
    
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusCombo;
    
    @FXML
    private Label totalStoresLabel;
    @FXML
    private Label activeStoresLabel;
    @FXML
    private Label inactiveStoresLabel;

    private StoreDao storeDao;
    private OrderPullService orderPullService;

    private List<Store> allStores;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setServices(StoreDao storeDao, OrderPullService orderPullService) {
        this.storeDao = storeDao;
        this.orderPullService = orderPullService;
    }

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        platformCol.setCellValueFactory(new PropertyValueFactory<>("platform"));
        storeUrlCol.setCellValueFactory(new PropertyValueFactory<>("storeUrl"));
        
        activeCol.setCellValueFactory(cellData -> {
            boolean active = cellData.getValue().isActive();
            String display = active ? "启用" : "禁用";
            String style = active ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #95a5a6;";
            return new SimpleStringProperty(display);
        });
        
        createdAtCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(dateFormatter));
            }
            return new SimpleStringProperty("--");
        });
        
        setupActionColumn();
        
        statusCombo.setValue("全部");
    }

    private void setupActionColumn() {
        Callback<TableColumn<Store, Void>, TableCell<Store, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Store, Void> call(final TableColumn<Store, Void> param) {
                return new TableCell<>() {
                    private final Button syncBtn = new Button("同步订单");
                    private final Button toggleBtn = new Button("切换状态");
                    
                    {
                        syncBtn.setOnAction(event -> {
                            Store store = getTableView().getItems().get(getIndex());
                            handleSyncStoreOrders(store);
                        });
                        
                        toggleBtn.setOnAction(event -> {
                            Store store = getTableView().getItems().get(getIndex());
                            handleToggleStoreStatus(store);
                        });
                        
                        syncBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 11;");
                        toggleBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 11;");
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox hbox = new HBox(5, syncBtn, toggleBtn);
                            setGraphic(hbox);
                        }
                    }
                };
            }
        };
        actionCol.setCellFactory(cellFactory);
    }

    public void refreshData() {
        allStores = storeDao.findAll();
        updateTable(allStores);
        updateStatistics();
    }

    private void updateTable(List<Store> stores) {
        storesTable.getItems().clear();
        storesTable.getItems().addAll(stores);
    }

    private void updateStatistics() {
        if (allStores == null) return;
        
        totalStoresLabel.setText(String.valueOf(allStores.size()));
        
        long activeCount = allStores.stream()
            .filter(Store::isActive)
            .count();
        activeStoresLabel.setText(String.valueOf(activeCount));
        
        long inactiveCount = allStores.size() - activeCount;
        inactiveStoresLabel.setText(String.valueOf(inactiveCount));
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        String statusFilter = statusCombo.getValue();
        
        List<Store> filtered = allStores.stream()
            .filter(s -> {
                boolean matchesKeyword = keyword.isEmpty() ||
                    s.getName().toLowerCase().contains(keyword) ||
                    s.getPlatform().toLowerCase().contains(keyword);
                
                boolean matchesStatus = "全部".equals(statusFilter) ||
                    ("启用".equals(statusFilter) && s.isActive()) ||
                    ("禁用".equals(statusFilter) && !s.isActive());
                
                return matchesKeyword && matchesStatus;
            })
            .collect(Collectors.toList());
        
        updateTable(filtered);
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        statusCombo.setValue("全部");
        refreshData();
    }

    @FXML
    private void handleAddStore() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("添加店铺");
        dialog.setHeaderText("请输入店铺名称");
        dialog.setContentText("店铺名称:");
        
        dialog.showAndWait().ifPresent(name -> {
            ChoiceDialog<String> platformDialog = new ChoiceDialog<>(
                "Amazon", "Amazon", "eBay", "AliExpress", "Shopify", "Wish"
            );
            platformDialog.setTitle("选择平台");
            platformDialog.setHeaderText("请选择店铺所属平台");
            platformDialog.setContentText("平台:");
            
            platformDialog.showAndWait().ifPresent(platform -> {
                Store store = new Store();
                store.setName(name);
                store.setPlatform(platform);
                store.setActive(true);
                storeDao.save(store);
                
                showAlert(Alert.AlertType.INFORMATION, "添加成功", 
                    "店铺 " + name + " 添加成功！");
                refreshData();
            });
        });
    }

    private void handleSyncStoreOrders(Store store) {
        int pulled = orderPullService.pullOrdersFromStore(store);
        showAlert(Alert.AlertType.INFORMATION, "同步完成", 
            "从店铺 " + store.getName() + " 拉取了 " + pulled + " 条新订单");
    }

    private void handleToggleStoreStatus(Store store) {
        store.setActive(!store.isActive());
        storeDao.update(store);
        refreshData();
        
        String status = store.isActive() ? "已启用" : "已禁用";
        showAlert(Alert.AlertType.INFORMATION, "状态已更新", 
            "店铺 " + store.getName() + " " + status);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
