package com.crossborder.service;

import com.crossborder.dao.AlertDao;
import com.crossborder.dao.ProductDao;
import com.crossborder.dao.ShipmentDao;
import com.crossborder.entity.Alert;
import com.crossborder.entity.Product;
import com.crossborder.entity.Shipment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AnomalyDetectionService {
    private final ProductDao productDao;
    private final ShipmentDao shipmentDao;
    private final AlertDao alertDao;
    
    private static final int BEST_SELLER_THRESHOLD = 100;
    private static final int DELAYED_DAYS_THRESHOLD = 7;

    public AnomalyDetectionService(ProductDao productDao, ShipmentDao shipmentDao, AlertDao alertDao) {
        this.productDao = productDao;
        this.shipmentDao = shipmentDao;
        this.alertDao = alertDao;
    }

    public List<Product> detectOutOfStockBestSellers() {
        List<Product> lowStockProducts = productDao.findLowStock();
        List<Product> outOfStockBestSellers = new ArrayList<>();
        
        for (Product product : lowStockProducts) {
            if (product.getSalesVolume() >= BEST_SELLER_THRESHOLD) {
                outOfStockBestSellers.add(product);
                createOutOfStockAlert(product);
            }
        }
        
        return outOfStockBestSellers;
    }

    public List<Shipment> detectDelayedShipments() {
        List<Shipment> delayedShipments = new ArrayList<>();
        List<Shipment> allDelayed = shipmentDao.findDelayed();
        
        for (Shipment shipment : allDelayed) {
            if (shipment.getDaysInTransit() >= DELAYED_DAYS_THRESHOLD) {
                delayedShipments.add(shipment);
                createDelayAlert(shipment);
            }
        }
        
        return delayedShipments;
    }

    public int runAllAnomalyChecks() {
        int totalAnomalies = 0;
        
        List<Product> outOfStock = detectOutOfStockBestSellers();
        totalAnomalies += outOfStock.size();
        
        List<Shipment> delayed = detectDelayedShipments();
        totalAnomalies += delayed.size();
        
        return totalAnomalies;
    }

    private void createOutOfStockAlert(Product product) {
        Alert alert = new Alert();
        alert.setAlertType("OUT_OF_STOCK");
        alert.setTitle("爆款断货预警 - " + product.getName());
        alert.setContent(String.format(
            "商品【%s】销量已达%d件，但当前库存仅为%d件，低于安全库存%d件。请及时补货！",
            product.getName(), product.getSalesVolume(), 
            product.getStockQuantity(), product.getSafetyStock()
        ));
        alert.setStoreId(product.getStoreId());
        alert.setPlatform(product.getPlatform());
        alert.setRelatedId(product.getSku());
        alert.setPriority(product.getStockQuantity() == 0 ? "CRITICAL" : "HIGH");
        alert.setStatus("PENDING");
        alert.setRead(false);
        alert.setAlertTime(LocalDateTime.now());
        
        alertDao.save(alert);
    }

    private void createDelayAlert(Shipment shipment) {
        Alert alert = new Alert();
        alert.setAlertType("SHIPMENT_DELAY");
        alert.setTitle("物流滞留预警 - 订单 " + shipment.getOrderNo());
        alert.setContent(String.format(
            "订单【%s】物流单号【%s】已在途%d天，超过正常配送时间阈值%d天。当前状态：%s，位置：%s。请联系物流公司核实情况。",
            shipment.getOrderNo(), shipment.getTrackingNumber(),
            shipment.getDaysInTransit(), DELAYED_DAYS_THRESHOLD,
            shipment.getStatus(), shipment.getCurrentLocation()
        ));
        alert.setRelatedId(shipment.getOrderNo());
        alert.setPriority(shipment.getDaysInTransit() > 14 ? "CRITICAL" : "HIGH");
        alert.setStatus("PENDING");
        alert.setRead(false);
        alert.setAlertTime(LocalDateTime.now());
        
        alertDao.save(alert);
    }

    public static class DetectionResult {
        private List<Product> outOfStockBestSellers;
        private List<Shipment> delayedShipments;

        public DetectionResult(List<Product> outOfStockBestSellers, List<Shipment> delayedShipments) {
            this.outOfStockBestSellers = outOfStockBestSellers;
            this.delayedShipments = delayedShipments;
        }

        public List<Product> getOutOfStockBestSellers() {
            return outOfStockBestSellers;
        }

        public List<Shipment> getDelayedShipments() {
            return delayedShipments;
        }

        public int getTotalAnomalies() {
            return outOfStockBestSellers.size() + delayedShipments.size();
        }
    }
}
