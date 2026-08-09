package com.example.computerassociation.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.computerassociation.entity.LingshiTransaction;

import java.util.List;

/**
 * 灵石流水服务接口
 */
public interface LingshiTransactionService extends IService<LingshiTransaction> {

    /**
     * 记录灵石流水
     */
    LingshiTransaction recordTransaction(Long discipleId, String discipleName, String type,
                                         Long amount, Long balance, Long operatorId,
                                         String operatorName, String remark);

    /**
     * 记录灵石流水（含峰信息，用于峰级分类统计）
     */
    LingshiTransaction recordTransaction(Long discipleId, String discipleName, String type,
                                         Long amount, Long balance, Long operatorId,
                                         String operatorName, String remark,
                                         Long peakId, String peakName);

    /**
     * 获取弟子的灵石流水
     */
    List<LingshiTransaction> getTransactionsByDiscipleId(Long discipleId);

    /**
     * 获取流水列表（根据用户权限过滤）
     */
    List<LingshiTransaction> getTransactions(Long userId, String type);
}
