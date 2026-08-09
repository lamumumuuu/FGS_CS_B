package com.example.computerassociation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.computerassociation.entity.LingshiTransaction;
import com.example.computerassociation.exception.BusinessException;
import com.example.computerassociation.mapper.LingshiTransactionMapper;
import com.example.computerassociation.service.LingshiTransactionService;
import com.example.computerassociation.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 灵石流水服务实现类
 */
@Slf4j
@Service
public class LingshiTransactionServiceImpl extends ServiceImpl<LingshiTransactionMapper, LingshiTransaction> implements LingshiTransactionService {

    @Autowired
    private LingshiTransactionMapper transactionMapper;

    @Autowired
    private PermissionService permissionService;

    @Override
    @Transactional
    public LingshiTransaction recordTransaction(Long discipleId, String discipleName, String type,
                                                Long amount, Long balance, Long operatorId,
                                                String operatorName, String remark) {
        return recordTransaction(discipleId, discipleName, type, amount, balance,
                operatorId, operatorName, remark, null, null);
    }

    @Override
    @Transactional
    public LingshiTransaction recordTransaction(Long discipleId, String discipleName, String type,
                                                Long amount, Long balance, Long operatorId,
                                                String operatorName, String remark,
                                                Long peakId, String peakName) {
        LingshiTransaction transaction = new LingshiTransaction();
        transaction.setDiscipleId(discipleId);
        transaction.setDiscipleName(discipleName);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalance(balance);
        transaction.setOperatorId(operatorId);
        transaction.setOperatorName(operatorName);
        transaction.setRemark(remark);
        transaction.setPeakId(peakId);
        transaction.setPeakName(peakName);
        transaction.setCreatedAt(LocalDateTime.now());

        transactionMapper.insert(transaction);
        log.info("记录灵石流水: discipleId={}, type={}, amount={}, balance={}, peakId={}",
                discipleId, type, amount, balance, peakId);

        return transaction;
    }

    @Override
    public List<LingshiTransaction> getTransactionsByDiscipleId(Long discipleId) {
        return transactionMapper.selectByDiscipleId(discipleId);
    }

    @Override
    public List<LingshiTransaction> getTransactions(Long userId, String type) {
        QueryWrapper<LingshiTransaction> queryWrapper = new QueryWrapper<>();

        // 全局用户可以查看所有流水
        // 双保险：isGlobalRoleUser 或拥有 finance:view_all 权限
        if (!permissionService.isGlobalRoleUser(userId)
                && !permissionService.hasPermission(userId, "finance:view_all")) {
            // 非全局用户只能查看自己作为操作人的流水
            queryWrapper.eq("operator_id", userId);
        }
        
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("type", type);
        }
        
        queryWrapper.orderByDesc("created_at");
        queryWrapper.last("LIMIT 200");
        
        return transactionMapper.selectList(queryWrapper);
    }
}
