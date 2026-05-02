package com.crossborder.service;

import com.crossborder.dao.OrderDao;
import com.crossborder.dao.StoreDao;
import com.crossborder.entity.Order;
import com.crossborder.entity.OrderItem;
import com.crossborder.entity.Store;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class OrderPullService {
    private final StoreDao storeDao;
    private final OrderDao orderDao;
    private final Random random = new Random();

    private static final String[] PLATFORMS = {"Amazon", "eBay", "AliExpress", "Wish", "Shopify"};
    private static final String[] PRODUCT_NAMES = {
        "无线蓝牙耳机", "智能手表", "便携式充电器", "手机壳", "数据线",
        "摄像头", "无人机", "游戏手柄", "机械键盘", "鼠标",
        "平板电脑", "笔记本支架", "USB集线器", "外置硬盘", "耳机放大器"
    };
    private static final String[] STATUSES = {"PENDING", "PAID", "SHIPPED", "DELIVERED"};
    private static final String[] BUYER_NAMES = {
        "John Smith", "Emma Wilson", "Michael Brown", "Sarah Davis", "James Miller",
        "Lisa Taylor", "Robert Anderson", "Jennifer Thomas", "William Jackson", "Maria White"
    };

    public OrderPullService(StoreDao storeDao, OrderDao orderDao) {
        this.storeDao = storeDao;
        this.orderDao = orderDao;
    }

    public int pullOrdersFromAllStores() {
        List<Store> activeStores = storeDao.findActive();
        int totalPulled = 0;
        
        for (Store store : activeStores) {
            totalPulled += pullOrdersFromStore(store);
        }
        
        return totalPulled;
    }

    public int pullOrdersFromStore(Store store) {
        List<Order> orders = generateMockOrders(store);
        int count = 0;
        
        for (Order order : orders) {
            if (orderDao.findByOrderNo(order.getOrderNo()) == null) {
                orderDao.save(order);
                count++;
            }
        }
        
        return count;
    }

    private List<Order> generateMockOrders(Store store) {
        List<Order> orders = new ArrayList<>();
        int orderCount = random.nextInt(5) + 1;
        
        for (int i = 0; i < orderCount; i++) {
            Order order = new Order();
            order.setOrderNo(generateOrderNo(store.getPlatform()));
            order.setStoreId(store.getId());
            order.setPlatform(store.getPlatform());
            order.setBuyerName(BUYER_NAMES[random.nextInt(BUYER_NAMES.length)]);
            order.setBuyerEmail(order.getBuyerName().toLowerCase().replace(" ", ".") + "@example.com");
            order.setShippingAddress(generateAddress());
            order.setPhone(generatePhone());
            order.setStatus(STATUSES[random.nextInt(STATUSES.length)]);
            order.setPaymentMethod("Credit Card");
            order.setCurrency("USD");
            order.setOrderTime(LocalDateTime.now().minusHours(random.nextInt(72)));
            
            if ("PAID".equals(order.getStatus()) || "SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
                order.setPaymentTime(order.getOrderTime().plusMinutes(random.nextInt(60) + 1));
            }
            
            List<OrderItem> items = generateOrderItems();
            order.setOrderItems(items);
            
            BigDecimal totalAmount = BigDecimal.ZERO;
            for (OrderItem item : items) {
                totalAmount = totalAmount.add(item.getTotalPrice());
            }
            order.setTotalAmount(totalAmount);
            order.setShippingFee(BigDecimal.valueOf(random.nextDouble() * 10).setScale(2, BigDecimal.ROUND_HALF_UP));
            order.setDiscount(BigDecimal.ZERO);
            order.setTax(totalAmount.multiply(BigDecimal.valueOf(0.08)).setScale(2, BigDecimal.ROUND_HALF_UP));
            
            orders.add(order);
        }
        
        return orders;
    }

    private List<OrderItem> generateOrderItems() {
        List<OrderItem> items = new ArrayList<>();
        int itemCount = random.nextInt(3) + 1;
        
        for (int i = 0; i < itemCount; i++) {
            OrderItem item = new OrderItem();
            item.setProductName(PRODUCT_NAMES[random.nextInt(PRODUCT_NAMES.length)]);
            item.setSku("SKU-" + random.nextInt(10000));
            item.setQuantity(random.nextInt(3) + 1);
            item.setUnitPrice(BigDecimal.valueOf(random.nextDouble() * 100 + 10).setScale(2, BigDecimal.ROUND_HALF_UP));
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            item.setStatus("ACTIVE");
            items.add(item);
        }
        
        return items;
    }

    private String generateOrderNo(String platform) {
        String prefix = switch (platform) {
            case "Amazon" -> "AMZ";
            case "eBay" -> "EBY";
            case "AliExpress" -> "AE";
            case "Wish" -> "WSH";
            case "Shopify" -> "SHP";
            default -> "ORD";
        };
        return prefix + "-" + System.currentTimeMillis() + "-" + random.nextInt(1000);
    }

    private String generateAddress() {
        String[] streets = {"Main St", "Oak Ave", "Pine Rd", "Maple Blvd", "Cedar Ln"};
        String[] cities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix"};
        String[] states = {"NY", "CA", "IL", "TX", "AZ"};
        
        return random.nextInt(9999) + 1 + " " + streets[random.nextInt(streets.length)] + ", " +
               cities[random.nextInt(cities.length)] + ", " + states[random.nextInt(states.length)] + " " +
               (random.nextInt(90000) + 10000);
    }

    private String generatePhone() {
        return "+1 (" + (random.nextInt(900) + 100) + ") " + 
               (random.nextInt(900) + 100) + "-" + 
               (random.nextInt(9000) + 1000);
    }
}
