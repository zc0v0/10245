package com.crossborder.service;

import com.crossborder.dao.ProductDao;
import com.crossborder.entity.Product;

import java.util.*;

public class ProductCategoryService {
    private final ProductDao productDao;
    
    private final Map<String, List<String>> categoryKeywords = new HashMap<>();
    private final Map<String, String> keywordToCategory = new HashMap<>();

    public ProductCategoryService(ProductDao productDao) {
        this.productDao = productDao;
        initializeCategories();
    }

    private void initializeCategories() {
        categoryKeywords.put("电子产品", Arrays.asList(
            "耳机", "蓝牙", "无线", "音箱", "音响", "充电器", "充电宝",
            "键盘", "鼠标", "游戏", "手柄", "摄像头", "无人机", "平板",
            "手机", "电脑", "笔记本", "硬盘", "U盘", "USB", "数据线",
            "手表", "智能", "穿戴"
        ));
        
        categoryKeywords.put("手机配件", Arrays.asList(
            "手机壳", "贴膜", "钢化膜", "保护套", "手机支架", "充电器",
            "数据线", "耳机", "蓝牙", "无线充", "移动电源"
        ));
        
        categoryKeywords.put("电脑配件", Arrays.asList(
            "键盘", "鼠标", "机械", "游戏", "显示器", "显卡", "内存",
            "硬盘", "SSD", "CPU", "主板", "电源", "机箱", "散热",
            "笔记本支架", "USB集线器", "扩展坞"
        ));
        
        categoryKeywords.put("智能家居", Arrays.asList(
            "智能", "家居", "门锁", "摄像头", "灯泡", "开关", "插座",
            "扫地", "机器人", "净化器", "加湿器", "空调", "电视"
        ));
        
        categoryKeywords.put("影音设备", Arrays.asList(
            "耳机", "音箱", "音响", "功放", "麦克风", "话筒", "耳机",
            "播放器", "MP3", "蓝牙", "无线", "降噪"
        ));
        
        categoryKeywords.put("户外装备", Arrays.asList(
            "背包", "帐篷", "睡袋", "登山", "徒步", "户外", "运动",
            "水壶", "手电筒", "头灯", "指南针", "GPS", "手表"
        ));

        for (Map.Entry<String, List<String>> entry : categoryKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                keywordToCategory.put(keyword.toLowerCase(), entry.getKey());
            }
        }
    }

    public String classifyProduct(Product product) {
        String name = product.getName() != null ? product.getName().toLowerCase() : "";
        String description = product.getDescription() != null ? product.getDescription().toLowerCase() : "";
        
        String combinedText = name + " " + description;
        
        Map<String, Integer> categoryScores = new HashMap<>();
        
        for (Map.Entry<String, String> entry : keywordToCategory.entrySet()) {
            String keyword = entry.getKey();
            String category = entry.getValue();
            
            int count = countOccurrences(combinedText, keyword);
            if (count > 0) {
                categoryScores.merge(category, count, Integer::sum);
            }
        }
        
        if (categoryScores.isEmpty()) {
            return "其他";
        }
        
        return Collections.max(categoryScores.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    public int classifyAllProducts() {
        List<Product> products = productDao.findAll();
        int classified = 0;
        
        for (Product product : products) {
            if (product.getCategoryName() == null || product.getCategoryName().isEmpty()) {
                String category = classifyProduct(product);
                product.setCategoryName(category);
                productDao.update(product);
                classified++;
            }
        }
        
        return classified;
    }

    public Map<String, Integer> getCategoryStatistics() {
        List<Product> products = productDao.findAll();
        Map<String, Integer> stats = new HashMap<>();
        
        for (Product product : products) {
            String category = product.getCategoryName() != null ? product.getCategoryName() : "未分类";
            stats.merge(category, 1, Integer::sum);
        }
        
        return stats;
    }
}
