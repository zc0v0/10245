package com.crossborder.service;

import com.crossborder.dao.OrderDao;
import com.crossborder.dao.ProductDao;
import com.crossborder.dao.StoreDao;
import com.crossborder.entity.Order;
import com.crossborder.entity.Product;
import com.crossborder.entity.Store;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportExportService {
    private final OrderDao orderDao;
    private final ProductDao productDao;
    private final StoreDao storeDao;

    public ReportExportService(OrderDao orderDao, ProductDao productDao, StoreDao storeDao) {
        this.orderDao = orderDao;
        this.productDao = productDao;
        this.storeDao = storeDao;
    }

    public String exportRevenueReport(String filePath) throws IOException {
        List<Store> stores = storeDao.findAll();
        List<Order> allOrders = orderDao.findAll();
        
        Map<Long, List<Order>> ordersByStore = new HashMap<>();
        for (Order order : allOrders) {
            if (order.getStoreId() != null) {
                ordersByStore.computeIfAbsent(order.getStoreId(), k -> new ArrayList<>()).add(order);
            }
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("营收报表");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("多店铺营收报表");
            titleCell.setCellStyle(createTitleStyle(workbook));
            
            Row dateRow = sheet.createRow(1);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            int rowNum = 3;
            
            String[] headers = {"店铺名称", "平台", "订单数量", "总销售额", "平均订单金额", "运费收入", "税费"};
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            BigDecimal grandTotal = BigDecimal.ZERO;
            int totalOrders = 0;
            
            for (Store store : stores) {
                List<Order> storeOrders = ordersByStore.getOrDefault(store.getId(), new ArrayList<>());
                
                BigDecimal totalAmount = BigDecimal.ZERO;
                BigDecimal totalShipping = BigDecimal.ZERO;
                BigDecimal totalTax = BigDecimal.ZERO;
                
                for (Order order : storeOrders) {
                    if (order.getTotalAmount() != null) {
                        totalAmount = totalAmount.add(order.getTotalAmount());
                    }
                    if (order.getShippingFee() != null) {
                        totalShipping = totalShipping.add(order.getShippingFee());
                    }
                    if (order.getTax() != null) {
                        totalTax = totalTax.add(order.getTax());
                    }
                }
                
                grandTotal = grandTotal.add(totalAmount);
                totalOrders += storeOrders.size();
                
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(store.getName());
                row.createCell(1).setCellValue(store.getPlatform());
                row.createCell(2).setCellValue(storeOrders.size());
                
                Cell amountCell = row.createCell(3);
                amountCell.setCellValue(totalAmount.doubleValue());
                amountCell.setCellStyle(currencyStyle);
                
                Cell avgCell = row.createCell(4);
                if (storeOrders.size() > 0) {
                    avgCell.setCellValue(totalAmount.divide(BigDecimal.valueOf(storeOrders.size()), 2, BigDecimal.ROUND_HALF_UP).doubleValue());
                }
                avgCell.setCellStyle(currencyStyle);
                
                Cell shippingCell = row.createCell(5);
                shippingCell.setCellValue(totalShipping.doubleValue());
                shippingCell.setCellStyle(currencyStyle);
                
                Cell taxCell = row.createCell(6);
                taxCell.setCellValue(totalTax.doubleValue());
                taxCell.setCellStyle(currencyStyle);
            }
            
            rowNum++;
            Row summaryRow = sheet.createRow(rowNum);
            summaryRow.createCell(0).setCellValue("总计");
            summaryRow.createCell(2).setCellValue(totalOrders);
            
            Cell grandTotalCell = summaryRow.createCell(3);
            grandTotalCell.setCellValue(grandTotal.doubleValue());
            grandTotalCell.setCellStyle(currencyStyle);
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            String fullPath = filePath + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            try (FileOutputStream fileOut = new FileOutputStream(fullPath)) {
                workbook.write(fileOut);
            }
            
            return fullPath;
        }
    }

    public String exportBestSellersReport(String filePath, int limit) throws IOException {
        List<Product> bestSellers = productDao.findBestSellers(limit);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("热销品分析");
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("热销品分析报表");
            titleCell.setCellStyle(createTitleStyle(workbook));
            
            Row dateRow = sheet.createRow(1);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("生成时间: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            int rowNum = 3;
            
            String[] headers = {"排名", "商品名称", "SKU", "分类", "售价", "成本价", "销量", "库存", "安全库存", "销售额"};
            Row headerRow = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            int rank = 1;
            for (Product product : bestSellers) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rank++);
                row.createCell(1).setCellValue(product.getName());
                row.createCell(2).setCellValue(product.getSku() != null ? product.getSku() : "");
                row.createCell(3).setCellValue(product.getCategoryName() != null ? product.getCategoryName() : "未分类");
                
                Cell priceCell = row.createCell(4);
                if (product.getPrice() != null) {
                    priceCell.setCellValue(product.getPrice().doubleValue());
                }
                priceCell.setCellStyle(currencyStyle);
                
                Cell costCell = row.createCell(5);
                if (product.getCostPrice() != null) {
                    costCell.setCellValue(product.getCostPrice().doubleValue());
                }
                costCell.setCellStyle(currencyStyle);
                
                row.createCell(6).setCellValue(product.getSalesVolume());
                row.createCell(7).setCellValue(product.getStockQuantity());
                row.createCell(8).setCellValue(product.getSafetyStock());
                
                Cell revenueCell = row.createCell(9);
                if (product.getPrice() != null) {
                    revenueCell.setCellValue(product.getPrice().multiply(BigDecimal.valueOf(product.getSalesVolume())).doubleValue());
                }
                revenueCell.setCellStyle(currencyStyle);
            }
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            String fullPath = filePath + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx";
            try (FileOutputStream fileOut = new FileOutputStream(fullPath)) {
                workbook.write(fileOut);
            }
            
            return fullPath;
        }
    }

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        List<Order> allOrders = orderDao.findAll();
        List<Product> allProducts = productDao.findAll();
        List<Store> allStores = storeDao.findActive();
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int pendingOrders = 0;
        int shippedOrders = 0;
        int deliveredOrders = 0;
        
        for (Order order : allOrders) {
            if (order.getTotalAmount() != null) {
                totalRevenue = totalRevenue.add(order.getTotalAmount());
            }
            
            switch (order.getStatus()) {
                case "PENDING":
                case "PAID":
                    pendingOrders++;
                    break;
                case "SHIPPED":
                    shippedOrders++;
                    break;
                case "DELIVERED":
                    deliveredOrders++;
                    break;
            }
        }
        
        stats.put("totalStores", allStores.size());
        stats.put("totalOrders", allOrders.size());
        stats.put("totalProducts", allProducts.size());
        stats.put("totalRevenue", totalRevenue);
        stats.put("pendingOrders", pendingOrders);
        stats.put("shippedOrders", shippedOrders);
        stats.put("deliveredOrders", deliveredOrders);
        
        return stats;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("$#,##0.00"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("yyyy-mm-dd hh:mm:ss"));
        return style;
    }
}
