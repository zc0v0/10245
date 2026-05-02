package com.crossborder.controller;

import com.crossborder.dao.ProductDao;
import com.crossborder.dao.StoreDao;
import com.crossborder.entity.Product;
import com.crossborder.service.ProductCategoryService;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductsController {

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> nameCol;
    @FXML
    private TableColumn<Product, String> skuCol;
    @FXML
    private TableColumn<Product, String> categoryCol;
    @FXML
    private TableColumn<Product, String> priceCol;
    @FXML
    private TableColumn<Product, String> costPriceCol;
    @FXML
    private TableColumn<Product, String> stockCol;
    @FXML
    private TableColumn<Product, String> safetyStockCol;
    @FXML
    private TableColumn<Product, String> salesCol;
    @FXML
    private TableColumn<Product, String> statusCol;
    
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryCombo;
    @FXML
    private ComboBox<String> stockCombo;
    
    @FXML
    private Label totalProductsLabel;
    @FXML
    private Label lowStockLabel;
    @FXML
    private Label hotProductsLabel;

    private ProductDao productDao;
    private StoreDao storeDao;
    private ProductCategoryService categoryService;

    private List<Product> allProducts;

    public void setServices(ProductDao productDao, ProductCategoryService categoryService, StoreDao storeDao) {
        this.productDao = productDao;
        this.storeDao = storeDao;
        this.categoryService = categoryService;
    }

    @FXML
    public void initialize() {
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        skuCol.setCellValueFactory(new PropertyValueFactory<>("sku"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        
        priceCol.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getPrice();
            return new SimpleStringProperty(price != null ? String.format("$%,.2f", price) : "--");
        });
        
        costPriceCol.setCellValueFactory(cellData -> {
            BigDecimal price = cellData.getValue().getCostPrice();
            return new SimpleStringProperty(price != null ? String.format("$%,.2f", price) : "--");
        });
        
        stockCol.setCellValueFactory(cellData -> {
            int stock = cellData.getValue().getStockQuantity();
            int safetyStock = cellData.getValue().getSafetyStock();
            String style = stock <= safetyStock ? "-fx-text-fill: #e74c3c;" : "";
            return new SimpleStringProperty(String.valueOf(stock));
        });
        
        safetyStockCol.setCellValueFactory(new PropertyValueFactory<>("safetyStock"));
        salesCol.setCellValueFactory(new PropertyValueFactory<>("salesVolume"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        stockCombo.setValue("全部");
    }

    public void refreshData() {
        allProducts = productDao.findAll();
        updateCategoryCombo();
        updateTable(allProducts);
        updateStatistics();
    }

    private void updateCategoryCombo() {
        String currentValue = categoryCombo.getValue();
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add("全部");
        
        Map<String, Integer> stats = categoryService.getCategoryStatistics();
        for (String category : stats.keySet()) {
            if (!"未分类".equals(category)) {
                categoryCombo.getItems().add(category);
            }
        }
        
        if (currentValue != null && categoryCombo.getItems().contains(currentValue)) {
            categoryCombo.setValue(currentValue);
        } else {
            categoryCombo.setValue("全部");
        }
    }

    private void updateTable(List<Product> products) {
        productsTable.getItems().clear();
        productsTable.getItems().addAll(products);
    }

    private void updateStatistics() {
        if (allProducts == null) return;
        
        totalProductsLabel.setText(String.valueOf(allProducts.size()));
        
        long lowStockCount = allProducts.stream()
            .filter(p -> p.getStockQuantity() <= p.getSafetyStock())
            .count();
        lowStockLabel.setText(String.valueOf(lowStockCount));
        
        long hotCount = allProducts.stream()
            .filter(p -> p.getSalesVolume() >= 50)
            .count();
        hotProductsLabel.setText(String.valueOf(hotCount));
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().toLowerCase().trim();
        String categoryFilter = categoryCombo.getValue();
        String stockFilter = stockCombo.getValue();
        
        List<Product> filtered = allProducts.stream()
            .filter(p -> {
                boolean matchesKeyword = keyword.isEmpty() ||
                    (p.getName() != null && p.getName().toLowerCase().contains(keyword)) ||
                    (p.getSku() != null && p.getSku().toLowerCase().contains(keyword));
                
                boolean matchesCategory = "全部".equals(categoryFilter) ||
                    categoryFilter.equals(p.getCategoryName());
                
                boolean matchesStock = true;
                if ("库存不足".equals(stockFilter)) {
                    matchesStock = p.getStockQuantity() <= p.getSafetyStock();
                } else if ("正常".equals(stockFilter)) {
                    matchesStock = p.getStockQuantity() > p.getSafetyStock();
                }
                
                return matchesKeyword && matchesCategory && matchesStock;
            })
            .collect(Collectors.toList());
        
        updateTable(filtered);
    }

    @FXML
    private void handleRefresh() {
        searchField.clear();
        categoryCombo.setValue("全部");
        stockCombo.setValue("全部");
        refreshData();
    }

    @FXML
    private void handleClassify() {
        int classified = categoryService.classifyAllProducts();
        showAlert(Alert.AlertType.INFORMATION, "分类完成", 
            "成功智能分类 " + classified + " 个商品");
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
