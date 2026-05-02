package com.crossborder.service;

import com.crossborder.dao.AlertDao;
import com.crossborder.dao.RefundRequestDao;
import com.crossborder.entity.Alert;
import com.crossborder.entity.RefundRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RefundInterceptionService {
    private final RefundRequestDao refundRequestDao;
    private final AlertDao alertDao;
    
    private static final double HIGH_RISK_THRESHOLD = 0.7;
    private static final double MEDIUM_RISK_THRESHOLD = 0.4;
    private static final BigDecimal LARGE_AMOUNT_THRESHOLD = new BigDecimal("500");
    private static final int FREQUENT_REFUND_THRESHOLD = 3;
    private static final int DAYS_TO_CHECK = 30;

    public RefundInterceptionService(RefundRequestDao refundRequestDao, AlertDao alertDao) {
        this.refundRequestDao = refundRequestDao;
        this.alertDao = alertDao;
    }

    public RiskAssessment assessRisk(RefundRequest refund) {
        double riskScore = 0.0;
        List<String> riskReasons = new ArrayList<>();
        
        if (isLargeAmount(refund)) {
            riskScore += 0.3;
            riskReasons.add("退款金额较大: $" + refund.getRefundAmount());
        }
        
        if (isFrequentRefunder(refund.getBuyerName())) {
            riskScore += 0.4;
            riskReasons.add("买家近期退款次数过多");
        }
        
        if (isSuspiciousReason(refund.getReason())) {
            riskScore += 0.2;
            riskReasons.add("退款理由存在疑问");
        }
        
        if (isOrderCompletedRecently(refund)) {
            riskScore += 0.3;
            riskReasons.add("订单刚完成即申请退款");
        }
        
        String riskLevel;
        if (riskScore >= HIGH_RISK_THRESHOLD) {
            riskLevel = "HIGH";
        } else if (riskScore >= MEDIUM_RISK_THRESHOLD) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }
        
        return new RiskAssessment(riskScore, riskLevel, String.join("; ", riskReasons));
    }

    private boolean isLargeAmount(RefundRequest refund) {
        return refund.getRefundAmount().compareTo(LARGE_AMOUNT_THRESHOLD) >= 0;
    }

    private boolean isFrequentRefunder(String buyerName) {
        List<RefundRequest> allRefunds = refundRequestDao.findAll();
        int count = 0;
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(DAYS_TO_CHECK);
        
        for (RefundRequest r : allRefunds) {
            if (r.getBuyerName().equals(buyerName) && 
                r.getRequestTime() != null && 
                r.getRequestTime().isAfter(thresholdDate)) {
                count++;
            }
        }
        
        return count >= FREQUENT_REFUND_THRESHOLD;
    }

    private boolean isSuspiciousReason(String reason) {
        if (reason == null) return false;
        String lowerReason = reason.toLowerCase();
        
        String[] suspiciousKeywords = {
            "不想要了", "后悔", "拍错了", "不想买", "价格贵",
            "changed mind", "don't want", "wrong item", "too expensive"
        };
        
        for (String keyword : suspiciousKeywords) {
            if (lowerReason.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }

    private boolean isOrderCompletedRecently(RefundRequest refund) {
        List<RefundRequest> allRefunds = refundRequestDao.findAll();
        for (RefundRequest r : allRefunds) {
            if (r.getOrderNo().equals(refund.getOrderNo()) && r.getRequestTime() != null) {
                long days = ChronoUnit.DAYS.between(r.getRequestTime(), LocalDateTime.now());
                return days < 3;
            }
        }
        return false;
    }

    public void processRefund(RefundRequest refund) {
        RiskAssessment assessment = assessRisk(refund);
        
        refund.setRiskScore(assessment.getScore());
        refund.setRiskReason(assessment.getReasons());
        
        if ("HIGH".equals(assessment.getLevel())) {
            refund.setMalicious(true);
            refund.setStatus("INTERCEPTED");
            createRefundAlert(refund, assessment);
        } else {
            refund.setMalicious(false);
            refund.setStatus("PENDING");
        }
        
        refundRequestDao.update(refund);
    }

    public int processPendingRefunds() {
        List<RefundRequest> pendingRefunds = refundRequestDao.findPending();
        int processed = 0;
        
        for (RefundRequest refund : pendingRefunds) {
            processRefund(refund);
            processed++;
        }
        
        return processed;
    }

    private void createRefundAlert(RefundRequest refund, RiskAssessment assessment) {
        Alert alert = new Alert();
        alert.setAlertType("MALICIOUS_REFUND");
        alert.setTitle("恶意退款拦截 - 订单 " + refund.getOrderNo());
        alert.setContent(String.format(
            "检测到高风险退款申请！订单号：%s，买家：%s，退款金额：$%s，风险评分：%.2f，风险原因：%s。该退款已被自动拦截，请人工审核。",
            refund.getOrderNo(), refund.getBuyerName(), 
            refund.getRefundAmount(), assessment.getScore(), assessment.getReasons()
        ));
        alert.setStoreId(refund.getStoreId());
        alert.setPlatform(refund.getPlatform());
        alert.setRelatedId(refund.getRefundNo());
        alert.setPriority("CRITICAL");
        alert.setStatus("PENDING");
        alert.setRead(false);
        alert.setAlertTime(LocalDateTime.now());
        
        alertDao.save(alert);
    }

    public Map<String, Integer> getRefundStatistics() {
        List<RefundRequest> allRefunds = refundRequestDao.findAll();
        Map<String, Integer> stats = new HashMap<>();
        
        int total = 0;
        int malicious = 0;
        int pending = 0;
        int intercepted = 0;
        
        for (RefundRequest refund : allRefunds) {
            total++;
            if (refund.isMalicious()) malicious++;
            if ("PENDING".equals(refund.getStatus())) pending++;
            if ("INTERCEPTED".equals(refund.getStatus())) intercepted++;
        }
        
        stats.put("total", total);
        stats.put("malicious", malicious);
        stats.put("pending", pending);
        stats.put("intercepted", intercepted);
        
        return stats;
    }

    public static class RiskAssessment {
        private final double score;
        private final String level;
        private final String reasons;

        public RiskAssessment(double score, String level, String reasons) {
            this.score = score;
            this.level = level;
            this.reasons = reasons;
        }

        public double getScore() {
            return score;
        }

        public String getLevel() {
            return level;
        }

        public String getReasons() {
            return reasons;
        }
    }
}
