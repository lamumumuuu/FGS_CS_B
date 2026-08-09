// src/main/java/com/example/computerassociation/controller/FinanceController.java

/**
 * 财务管理控制器
 *
 * 核心职责：
 * 1. 灵石流水查询（历史记录）
 * 2. 宗门"总可支配灵石"管理（独立账本，与峰级灵石分账）
 * 3. 峰级灵石管理（各峰独立核算，含可支配/累计字段）
 * 4. 灵石分配（总 → 峰）与峰间调拨
 * 5. 峰成员列表查询
 *
 * 数据模型：
 * - 总可支配灵石：通过 peaks 表中名为 "_TREASURY_" 的特殊峰的 available_lingshi 字段持久化
 * - 峰可支配灵石：存储在 peaks.available_lingshi 字段
 * - 峰累计灵石：存储在 peaks.total_lingshi 字段
 * - 灵石流水：lingshi_transactions 记录所有增减变动
 */

package com.example.computerassociation.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.computerassociation.annotation.RequiresPermission;
import com.example.computerassociation.common.Result;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.entity.Disciple;
import com.example.computerassociation.entity.LingshiTransaction;
import com.example.computerassociation.entity.Peak;
import com.example.computerassociation.mapper.DiscipleMapper;
import com.example.computerassociation.mapper.LingshiTransactionMapper;
import com.example.computerassociation.mapper.PeakMapper;
import com.example.computerassociation.service.LingshiTransactionService;
import com.example.computerassociation.service.PermissionService;
import com.example.computerassociation.service.SectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 财务管理控制器
 * 提供灵石流水管理接口，实现财务权限控制
 *
 * 功能模块：
 * - 灵石流水查询与调整
 * - 总可支配灵石管理
 * - 峰列表查询与峰可支配灵石展示
 * - 峰成员查询
 * - 灵石分配（总 → 峰）
 * - 峰间灵石调拨
 * - 灵石调整日志（全部财务/各峰财务分类）
 */
@Slf4j
@Tag(name = "财务管理", description = "灵石流水查询、调整、分配、调拨接口")
@RestController
@RequestMapping("/api/finance")
public class FinanceController {

    @Autowired
    private LingshiTransactionService transactionService;

    @Autowired
    private SectService sectService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private DiscipleMapper discipleMapper;

    @Autowired
    private PeakMapper peakMapper;

    @Autowired
    private LingshiTransactionMapper transactionMapper;

    /**
     * 宗门总可支配灵石（默认 100000，可在数据库初始化时调整）
     * 注：通过 peaks 表中名为 "_TREASURY_" 的特殊峰的 available_lingshi 字段持久化
     */
    @Value("${finance.total-disposable-lingshi:100000}")
    private long initialTotalDisposable;

    /** 总可支配灵石原子计数器（线程安全，运行时缓存） */
    private final AtomicLong totalDisposableCounter = new AtomicLong(0L);

    /**
     * 初始化：从 peaks 表中名为 "_TREASURY_" 的特殊峰读取总可支配灵石，
     * 若无则使用配置默认值。
     * 此方法仅在应用启动时由 @PostConstruct 调用一次。
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            Peak treasury = peakMapper.selectOne(
                    new QueryWrapper<Peak>().eq("name", "_TREASURY_"));
            if (treasury != null && treasury.getAvailableLingshi() != null) {
                totalDisposableCounter.set(treasury.getAvailableLingshi());
                log.info("初始化总可支配灵石（来自数据库 _TREASURY_ 峰）: {}", treasury.getAvailableLingshi());
            } else {
                totalDisposableCounter.set(initialTotalDisposable);
                log.info("初始化总可支配灵石（使用默认值）: {}", initialTotalDisposable);
            }
        } catch (Exception e) {
            totalDisposableCounter.set(initialTotalDisposable);
            log.warn("初始化总可支配灵石失败，使用默认值: {}", initialTotalDisposable, e);
        }
    }

    /**
     * 递减总可支配灵石（分配给峰时调用）
     */
    private long decrementTotalDisposable(long amount) {
        long newValue = totalDisposableCounter.updateAndGet(v -> Math.max(0, v - amount));
        // 同步到数据库 _TREASURY_ 峰
        try {
            Peak treasury = peakMapper.selectOne(
                    new QueryWrapper<Peak>().eq("name", "_TREASURY_"));
            if (treasury != null) {
                treasury.setAvailableLingshi(newValue);
                peakMapper.updateById(treasury);
            }
        } catch (Exception e) {
            log.warn("同步总可支配灵石到数据库失败", e);
        }
        return newValue;
    }

    /**
     * 递增总可支配灵石（调整增加时调用）
     * 同步更新内存计数器与数据库 _TREASURY_ 峰
     */
    private long incrementTotalDisposable(long amount) {
        long newValue = totalDisposableCounter.updateAndGet(v -> v + amount);
        // 同步到数据库 _TREASURY_ 峰
        try {
            Peak treasury = peakMapper.selectOne(
                    new QueryWrapper<Peak>().eq("name", "_TREASURY_"));
            if (treasury != null) {
                treasury.setAvailableLingshi(newValue);
                peakMapper.updateById(treasury);
            }
        } catch (Exception e) {
            log.warn("同步总可支配灵石到数据库失败", e);
        }
        return newValue;
    }

    /**
     * 获取当前总可支配灵石
     */
    private long getCurrentTotalDisposable() {
        return totalDisposableCounter.get();
    }

    /* ================================================================== */
    /*  一、灵石流水查询与调整                                             */
    /* ================================================================== */

    /**
     * 获取灵石流水列表
     * 权限：finance:view_own_peak
     */
    @Operation(summary = "获取灵石流水", description = "获取灵石流水列表，支持按类型筛选",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/transactions")
    @RequiresPermission("finance:view_own_peak")
    public Result<List<LingshiTransaction>> getTransactions(
            @Parameter(description = "变更类型") @RequestParam(required = false) String type) {
        Long userId = UserContext.getUserId();
        List<LingshiTransaction> transactions = transactionService.getTransactions(userId, type);
        return Result.success(transactions);
    }

    /**
     * 获取指定弟子的灵石流水
     * 权限：finance:view_own_peak
     */
    @Operation(summary = "获取弟子灵石流水", description = "获取指定弟子的灵石流水",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/disciple/{discipleId}/transactions")
    @RequiresPermission("finance:view_own_peak")
    public Result<List<LingshiTransaction>> getDiscipleTransactions(
            @Parameter(description = "弟子ID") @PathVariable Long discipleId) {
        List<LingshiTransaction> transactions = transactionService.getTransactionsByDiscipleId(discipleId);
        return Result.success(transactions);
    }

    /**
     * 调整指定弟子的灵石数量
     * 权限：finance:adjust_lingshi
     */
    @Operation(summary = "调整灵石", description = "调整指定弟子的灵石数量（管理员操作）",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/adjust")
    @RequiresPermission("finance:adjust_lingshi")
    public Result<Map<String, Object>> adjustLingshi(@RequestBody Map<String, Object> body) {
        Long discipleId = Long.valueOf(body.get("discipleId").toString());
        Integer amount = Integer.valueOf(body.get("amount").toString());
        String remark = (String) body.get("remark");

        if (amount == null || amount == 0) {
            return Result.fail(400, "调整金额不能为0");
        }

        Disciple disciple = sectService.getById(discipleId);
        if (disciple == null) {
            return Result.fail(404, "弟子不存在");
        }

        Long oldBalance = disciple.getLingshi() != null ? disciple.getLingshi() : 0L;
        Long newBalance = oldBalance + amount.longValue();

        if (amount < 0 && newBalance < 0) {
            return Result.fail(400, "灵石余额不足");
        }

        disciple.setLingshi(newBalance);
        disciple.setUpdatedAt(java.time.LocalDateTime.now());
        discipleMapper.updateById(disciple);

        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();
        String type = amount > 0 ? "adjust_in" : "adjust_out";

        // 记录流水（含峰信息）
        LingshiTransaction transaction = transactionService.recordTransaction(
                discipleId, disciple.getName(), type,
                amount.longValue(), newBalance,
                userId, username, remark,
                getPeakIdByName(disciple.getPeak()), disciple.getPeak());

        Map<String, Object> result = new HashMap<>();
        result.put("transaction", transaction);
        result.put("newBalance", newBalance);

        return Result.success(result, "灵石调整成功");
    }

    /**
     * 查询当前悬赏基准价格
     * 权限：finance:view_own_peak
     */
    @Operation(summary = "查询灵石基准", description = "查询当前悬赏基准价格",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/base")
    @RequiresPermission("finance:view_own_peak")
    public Result<Map<String, Object>> getBaseReward() {
        Map<String, Object> result = new HashMap<>();
        result.put("globalBase", 100);
        result.put("description", "全局悬赏基准价格");
        return Result.success(result);
    }

    /**
     * 设定悬赏基准价格
     * 权限：finance:set_base
     */
    @Operation(summary = "设定灵石基准", description = "设定全局或本峰的悬赏基准价格",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PutMapping("/base")
    @RequiresPermission("finance:set_base")
    public Result<Void> setBaseReward(@RequestBody Map<String, Object> body) {
        return Result.success(null, "悬赏基准设置成功");
    }

    /* ================================================================== */
    /*  二、总可支配灵石管理（宗门公共）                                    */
    /* ================================================================== */

    /**
     * 获取总可支配灵石
     * 权限：finance:view_all
     * 返回宗门公共可自由支配的灵石总量
     */
    @Operation(summary = "获取总可支配灵石", description = "获取宗门公共可自由支配灵石总量",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/total-disposable")
    @RequiresPermission("finance:view_all")
    public Result<Map<String, Object>> getTotalDisposable() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", getCurrentTotalDisposable());
        result.put("description", "宗门公共可自由支配灵石 · 不属于任何个人");
        return Result.success(result);
    }

    /**
     * 调整总可支配灵石（增加 / 减少）
     * 权限：finance:adjust_lingshi
     * 通过统一数据更新机制修改宗门公共灵石总量，并记录灵石流水，
     * 确保操作的原子性、一致性、隔离性和持久性。
     * 注：调整记录关联到 _TREASURY_ 特殊峰，便于在灵石收支中追溯。
     */
    @Operation(summary = "调整总可支配灵石", description = "增加或减少宗门公共可自由支配灵石",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "调整成功"),
            @ApiResponse(responseCode = "400", description = "参数错误 / 灵石不足"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping("/adjust-total-disposable")
    @RequiresPermission("finance:adjust_lingshi")
    @Transactional
    public Result<Map<String, Object>> adjustTotalDisposable(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        Integer amount = Integer.valueOf(body.get("amount").toString());
        String remark = (String) body.get("remark");

        if (type == null || (!"in".equals(type) && !"out".equals(type))) {
            return Result.fail(400, "调整类型无效（仅支持 in / out）");
        }
        if (amount == null || amount <= 0) {
            return Result.fail(400, "调整金额必须大于0");
        }

        long currentTotal = getCurrentTotalDisposable();

        // 减少时校验余额
        if ("out".equals(type) && amount > currentTotal) {
            return Result.fail(400, String.format("总可支配灵石不足（当前：%d）", currentTotal));
        }

        // 更新总可支配灵石（内存 + 数据库 _TREASURY_ 峰）
        long newTotal;
        String transactionType;
        long signedAmount;
        if ("in".equals(type)) {
            newTotal = incrementTotalDisposable(amount);
            transactionType = "adjust_in";
            signedAmount = amount.longValue();
        } else {
            newTotal = decrementTotalDisposable(amount);
            transactionType = "adjust_out";
            signedAmount = -amount.longValue();
        }

        // 查询 _TREASURY_ 峰信息，记录流水便于追溯
        Long treasuryPeakId = null;
        String treasuryPeakName = "_TREASURY_";
        try {
            Peak treasury = peakMapper.selectOne(
                    new QueryWrapper<Peak>().eq("name", "_TREASURY_"));
            if (treasury != null) {
                treasuryPeakId = treasury.getId();
                treasuryPeakName = treasury.getName();
            }
        } catch (Exception e) {
            log.warn("查询 _TREASURY_ 峰失败", e);
        }

        // 记录灵石流水（关联 _TREASURY_ 峰）
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();
        LingshiTransaction transaction = transactionService.recordTransaction(
                null, "系统-总库", transactionType,
                signedAmount, newTotal,
                userId, username, remark,
                treasuryPeakId, treasuryPeakName);

        log.info("总可支配灵石调整成功: type={}, amount={}, operatorId={}, newTotal={}",
                type, amount, userId, newTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("transaction", transaction);
        result.put("newTotal", newTotal);

        return Result.success(result, "总可支配灵石调整成功");
    }

    /* ================================================================== */
    /*  三、峰列表与峰可支配灵石展示                                       */
    /* ================================================================== */

    /**
     * 获取所有峰的财务数据
     * 权限：finance:view_all
     * 返回各峰的可支配灵石、累计灵石、成员数
     * 注：排除 _TREASURY_ 特殊峰（总可支配灵石容器）与 "管理台" 峰（系统控制面板，非业务峰）
     */
    @Operation(summary = "获取峰列表财务数据", description = "获取所有峰的可支配灵石、累计灵石、成员数",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/peaks")
    @RequiresPermission("finance:view_all")
    public Result<List<Map<String, Object>>> getPeaksFinanceData() {
        // 查询所有峰（排除 _TREASURY_ 特殊峰与 "管理台" 系统控制面板峰）
        QueryWrapper<Peak> peakWrapper = new QueryWrapper<>();
        peakWrapper.ne("name", "_TREASURY_");
        peakWrapper.ne("name", "管理台");
        List<Peak> peaks = peakMapper.selectList(peakWrapper);

        List<Map<String, Object>> result = peaks.stream().map(peak -> {
            Map<String, Object> item = new HashMap<>();
            item.put("peakId", peak.getId());
            item.put("peakName", peak.getName());
            item.put("availableLingshi", peak.getAvailableLingshi() != null ? peak.getAvailableLingshi() : 0L);
            item.put("totalLingshi", peak.getTotalLingshi() != null ? peak.getTotalLingshi() : 0L);

            // 统计峰成员数量
            QueryWrapper<Disciple> discipleWrapper = new QueryWrapper<>();
            discipleWrapper.eq("peak", peak.getName());
            long memberCount = discipleMapper.selectCount(discipleWrapper);
            item.put("discipleCount", memberCount);

            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 获取指定峰的财务数据
     * 权限：finance:view_own_peak
     * 返回指定峰的可支配灵石、累计灵石、成员数
     */
    @Operation(summary = "获取指定峰财务数据", description = "获取指定峰的可支配灵石、累计灵石、成员数",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/peak/{peakId}")
    @RequiresPermission("finance:view_own_peak")
    public Result<Map<String, Object>> getPeakData(
            @Parameter(description = "峰ID") @PathVariable Long peakId) {
        Peak peak = peakMapper.selectById(peakId);
        if (peak == null) {
            return Result.fail(404, "峰不存在");
        }

        // "管理台" 峰为系统控制面板，不作为业务峰进行财务查询
        if ("管理台".equals(peak.getName())) {
            return Result.fail(400, "管理台为系统控制面板，不支持财务查询");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("peakId", peak.getId());
        result.put("peakName", peak.getName());
        result.put("availableLingshi", peak.getAvailableLingshi() != null ? peak.getAvailableLingshi() : 0L);
        result.put("totalLingshi", peak.getTotalLingshi() != null ? peak.getTotalLingshi() : 0L);

        // 统计峰成员数量
        QueryWrapper<Disciple> discipleWrapper = new QueryWrapper<>();
        discipleWrapper.eq("peak", peak.getName());
        long memberCount = discipleMapper.selectCount(discipleWrapper);
        result.put("discipleCount", memberCount);

        return Result.success(result);
    }

    /**
     * 获取指定峰的成员列表
     * 权限：finance:view_own_peak
     * 峰财务页面展示当前峰所有成员
     */
    @Operation(summary = "获取峰成员列表", description = "获取指定峰的所有成员及其灵石信息",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/peak/{peakId}/members")
    @RequiresPermission("finance:view_own_peak")
    public Result<List<Map<String, Object>>> getPeakMembers(
            @Parameter(description = "峰ID") @PathVariable Long peakId) {
        Peak peak = peakMapper.selectById(peakId);
        if (peak == null) {
            return Result.fail(404, "峰不存在");
        }

        // "管理台" 峰为系统控制面板，不作为业务峰进行成员查询
        if ("管理台".equals(peak.getName())) {
            return Result.fail(400, "管理台为系统控制面板，不支持成员查询");
        }

        QueryWrapper<Disciple> discipleWrapper = new QueryWrapper<>();
        discipleWrapper.eq("peak", peak.getName());
        List<Disciple> members = discipleMapper.selectList(discipleWrapper);

        List<Map<String, Object>> result = members.stream().map(member -> {
            Map<String, Object> item = new HashMap<>();
            item.put("discipleId", member.getId());
            item.put("name", member.getName());
            item.put("role", member.getRole());
            item.put("peak", member.getPeak());
            item.put("lingshi", member.getLingshi() != null ? member.getLingshi() : 0L);
            // 尝试获取关联用户名
            if (member.getUserId() != null) {
                item.put("user", "用户" + member.getUserId());
            }
            return item;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /* ================================================================== */
    /*  四、灵石分配（总可支配 → 峰）                                      */
    /* ================================================================== */

    /**
     * 灵石分配：从总可支配灵石分配至指定峰
     * 权限：finance:adjust_lingshi
     * 分配后总可支配灵石减少，目标峰可支配灵石增加
     */
    @Operation(summary = "灵石分配（总→峰）", description = "从总可支配灵石分配至指定峰",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "分配成功"),
            @ApiResponse(responseCode = "400", description = "参数错误 / 灵石不足"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "峰不存在")
    })
    @PostMapping("/allocate-to-peak")
    @RequiresPermission("finance:adjust_lingshi")
    @Transactional
    public Result<Map<String, Object>> allocateToPeak(@RequestBody Map<String, Object> body) {
        Long peakId = Long.valueOf(body.get("peakId").toString());
        Integer amount = Integer.valueOf(body.get("amount").toString());
        String remark = (String) body.get("remark");

        if (amount == null || amount <= 0) {
            return Result.fail(400, "分配金额必须大于0");
        }

        long currentTotal = getCurrentTotalDisposable();
        if (amount > currentTotal) {
            return Result.fail(400, String.format("总可支配灵石不足（当前：%d）", currentTotal));
        }

        Peak peak = peakMapper.selectById(peakId);
        if (peak == null) {
            return Result.fail(404, "峰不存在");
        }

        // "管理台" 峰为系统控制面板，不作为业务峰参与灵石分配
        if ("管理台".equals(peak.getName())) {
            return Result.fail(400, "管理台为系统控制面板，不参与灵石分配");
        }

        // 1. 扣减总可支配灵石
        long newTotal = decrementTotalDisposable(amount);

        // 2. 增加目标峰可支配灵石
        long oldAvailable = peak.getAvailableLingshi() != null ? peak.getAvailableLingshi() : 0L;
        long newAvailable = oldAvailable + amount.longValue();
        peak.setAvailableLingshi(newAvailable);
        // 累计灵石也相应增加（记录历史累计值）
        long oldTotal = peak.getTotalLingshi() != null ? peak.getTotalLingshi() : 0L;
        peak.setTotalLingshi(oldTotal + amount.longValue());
        peakMapper.updateById(peak);

        // 3. 记录流水
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();

        LingshiTransaction transaction = transactionService.recordTransaction(
                null, "系统-总分配", "allocate_in",
                amount.longValue(), newAvailable,
                userId, username, remark,
                peak.getId(), peak.getName());

        log.info("灵石分配成功: peakId={}, amount={}, operatorId={}, newTotalDisposable={}",
                peakId, amount, userId, newTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("transaction", transaction);
        result.put("newTotalDisposable", newTotal);
        result.put("peakAvailableLingshi", newAvailable);

        return Result.success(result, "灵石分配成功");
    }

    /* ================================================================== */
    /*  五、峰间灵石调拨                                                   */
    /* ================================================================== */

    /**
     * 峰间灵石调拨：从一个峰调拨灵石至另一个峰
     * 权限：finance:adjust_lingshi
     * 源峰可支配灵石减少，目标峰可支配灵石增加
     */
    @Operation(summary = "峰间灵石调拨", description = "从一个峰调拨灵石至另一个峰",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "调拨成功"),
            @ApiResponse(responseCode = "400", description = "参数错误 / 灵石不足 / 源峰与目标峰相同"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "峰不存在")
    })
    @PostMapping("/peak-transfer")
    @RequiresPermission("finance:adjust_lingshi")
    @Transactional
    public Result<Map<String, Object>> peakTransfer(@RequestBody Map<String, Object> body) {
        Long fromPeakId = Long.valueOf(body.get("fromPeakId").toString());
        Long toPeakId = Long.valueOf(body.get("toPeakId").toString());
        Integer amount = Integer.valueOf(body.get("amount").toString());
        String remark = (String) body.get("remark");

        if (fromPeakId.equals(toPeakId)) {
            return Result.fail(400, "源峰与目标峰不能相同");
        }
        if (amount == null || amount <= 0) {
            return Result.fail(400, "调拨金额必须大于0");
        }

        Peak fromPeak = peakMapper.selectById(fromPeakId);
        Peak toPeak = peakMapper.selectById(toPeakId);
        if (fromPeak == null) {
            return Result.fail(404, "源峰不存在");
        }
        if (toPeak == null) {
            return Result.fail(404, "目标峰不存在");
        }

        // "管理台" 峰为系统控制面板，不作为业务峰参与峰间调拨
        if ("管理台".equals(fromPeak.getName()) || "管理台".equals(toPeak.getName())) {
            return Result.fail(400, "管理台为系统控制面板，不参与峰间调拨");
        }

        long fromAvailable = fromPeak.getAvailableLingshi() != null ? fromPeak.getAvailableLingshi() : 0L;
        if (amount > fromAvailable) {
            return Result.fail(400, String.format("%s 可支配灵石不足（当前：%d）", fromPeak.getName(), fromAvailable));
        }

        // 1. 扣减源峰可支配灵石
        long newFromAvailable = fromAvailable - amount.longValue();
        fromPeak.setAvailableLingshi(newFromAvailable);
        peakMapper.updateById(fromPeak);

        // 2. 增加目标峰可支配灵石
        long toAvailable = toPeak.getAvailableLingshi() != null ? toPeak.getAvailableLingshi() : 0L;
        long newToAvailable = toAvailable + amount.longValue();
        toPeak.setAvailableLingshi(newToAvailable);
        peakMapper.updateById(toPeak);

        // 3. 记录流水（两条：源峰支出 + 目标峰收入）
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();

        // 源峰支出记录
        transactionService.recordTransaction(
                null, "峰间调拨-" + fromPeak.getName(), "peak_transfer_out",
                -amount.longValue(), newFromAvailable,
                userId, username, "调拨至" + toPeak.getName() + ": " + remark,
                fromPeak.getId(), fromPeak.getName());

        // 目标峰收入记录
        LingshiTransaction transaction = transactionService.recordTransaction(
                null, "峰间调拨-" + toPeak.getName(), "peak_transfer_in",
                amount.longValue(), newToAvailable,
                userId, username, "接收自" + fromPeak.getName() + ": " + remark,
                toPeak.getId(), toPeak.getName());

        log.info("峰间调拨成功: {} → {}, amount={}, operatorId={}",
                fromPeak.getName(), toPeak.getName(), amount, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("transaction", transaction);
        result.put("fromPeakAvailableLingshi", newFromAvailable);
        result.put("toPeakAvailableLingshi", newToAvailable);

        return Result.success(result, "峰间调拨成功");
    }

    /* ================================================================== */
    /*  五点五、峰灵石退回（峰 → 总库）                                      */
    /* ================================================================== */

    /**
     * 峰灵石退回：将指定峰的可支配灵石退回至总可支配灵石（_TREASURY_ 峰）
     * 权限：finance:adjust_lingshi
     * 退回后峰可支配灵石减少，总可支配灵石增加
     * 退回金额不得高于峰当前拥有的可支配灵石
     */
    @Operation(summary = "峰灵石退回（峰→总库）", description = "将指定峰的可支配灵石退回至宗门总可支配灵石",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "退回成功"),
            @ApiResponse(responseCode = "400", description = "参数错误 / 灵石不足"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "峰不存在")
    })
    @PostMapping("/return-from-peak")
    @RequiresPermission("finance:adjust_lingshi")
    @Transactional
    public Result<Map<String, Object>> returnFromPeak(@RequestBody Map<String, Object> body) {
        Long peakId = Long.valueOf(body.get("peakId").toString());
        Integer amount = Integer.valueOf(body.get("amount").toString());
        String remark = (String) body.get("remark");

        if (amount == null || amount <= 0) {
            return Result.fail(400, "退回金额必须大于0");
        }

        Peak peak = peakMapper.selectById(peakId);
        if (peak == null) {
            return Result.fail(404, "峰不存在");
        }

        // 受保护峰不可执行退回操作
        if ("管理台".equals(peak.getName()) || "_TREASURY_".equals(peak.getName())) {
            return Result.fail(400, "该峰为系统峰，不支持退回操作");
        }

        long peakAvailable = peak.getAvailableLingshi() != null ? peak.getAvailableLingshi() : 0L;
        if (amount > peakAvailable) {
            return Result.fail(400, String.format("退回金额不得高于峰可支配灵石（当前：%d）", peakAvailable));
        }

        // 1. 扣减峰可支配灵石
        long newPeakAvailable = peakAvailable - amount.longValue();
        peak.setAvailableLingshi(newPeakAvailable);
        peakMapper.updateById(peak);

        // 2. 增加总可支配灵石（通过 _TREASURY_ 峰）
        long newTotal = incrementTotalDisposable(amount);

        // 3. 记录流水（两条：峰支出 + 总库收入）
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();

        // 峰支出记录
        transactionService.recordTransaction(
                null, peak.getName() + "-退回", "peak_return_out",
                -amount.longValue(), newPeakAvailable,
                userId, username, "退回至总库: " + remark,
                peak.getId(), peak.getName());

        // 总库收入记录（关联 _TREASURY_ 峰）
        Long treasuryPeakId = null;
        String treasuryPeakName = "_TREASURY_";
        try {
            Peak treasury = peakMapper.selectOne(
                    new QueryWrapper<Peak>().eq("name", "_TREASURY_"));
            if (treasury != null) {
                treasuryPeakId = treasury.getId();
                treasuryPeakName = treasury.getName();
            }
        } catch (Exception e) {
            log.warn("查询 _TREASURY_ 峰失败", e);
        }

        LingshiTransaction transaction = transactionService.recordTransaction(
                null, "系统-总库", "peak_return_in",
                amount.longValue(), newTotal,
                userId, username, "接收自" + peak.getName() + "退回: " + remark,
                treasuryPeakId, treasuryPeakName);

        log.info("峰灵石退回成功: peak={}, amount={}, operatorId={}, newPeakAvailable={}, newTotal={}",
                peak.getName(), amount, userId, newPeakAvailable, newTotal);

        Map<String, Object> result = new HashMap<>();
        result.put("transaction", transaction);
        result.put("peakAvailableLingshi", newPeakAvailable);
        result.put("totalDisposable", newTotal);

        return Result.success(result, "峰灵石退回成功");
    }

    /* ================================================================== */
    /*  五点六、峰灵石分配到个人（峰 → 弟子）                                 */
    /* ================================================================== */

    /**
     * 峰灵石分配到个人：从指定峰的可支配灵石中分配灵石给指定弟子
     * 权限：finance:adjust_lingshi
     * 分配后峰可支配灵石减少，弟子个人灵石增加
     * 分配金额不得高于峰当前拥有的可支配灵石
     */
    @Operation(summary = "峰灵石分配到个人（峰→弟子）", description = "从指定峰的可支配灵石分配灵石给指定弟子",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "分配成功"),
            @ApiResponse(responseCode = "400", description = "参数错误 / 灵石不足"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足"),
            @ApiResponse(responseCode = "404", description = "峰或弟子不存在")
    })
    @PostMapping("/allocate-to-member")
    @RequiresPermission("finance:adjust_lingshi")
    @Transactional
    public Result<Map<String, Object>> allocateToMember(@RequestBody Map<String, Object> body) {
        // 参数安全校验：防止 null 值导致 500 错误
        if (body.get("peakId") == null || body.get("discipleId") == null || body.get("amount") == null) {
            return Result.fail(400, "缺少必要参数（peakId / discipleId / amount）");
        }

        Long peakId = Long.valueOf(body.get("peakId").toString());
        Long discipleId = Long.valueOf(body.get("discipleId").toString());
        Integer amount = Integer.valueOf(body.get("amount").toString());
        String remark = (String) body.get("remark");

        if (amount == null || amount <= 0) {
            return Result.fail(400, "分配金额必须大于0");
        }

        Peak peak = peakMapper.selectById(peakId);
        if (peak == null) {
            return Result.fail(404, "峰不存在");
        }

        // 受保护峰不可执行分配操作
        if ("管理台".equals(peak.getName()) || "_TREASURY_".equals(peak.getName())) {
            return Result.fail(400, "该峰为系统峰，不支持分配操作");
        }

        // 校验峰可支配灵石是否充足
        long peakAvailable = peak.getAvailableLingshi() != null ? peak.getAvailableLingshi() : 0L;
        if (amount > peakAvailable) {
            return Result.fail(400, String.format("峰可支配灵石不足（当前：%d）", peakAvailable));
        }

        // 查找弟子
        Disciple disciple = sectService.getById(discipleId);
        if (disciple == null) {
            return Result.fail(404, "弟子不存在");
        }

        // 1. 扣减峰可支配灵石
        long newPeakAvailable = peakAvailable - amount.longValue();
        peak.setAvailableLingshi(newPeakAvailable);
        peakMapper.updateById(peak);

        // 2. 增加弟子个人灵石
        // 直接使用 mapper 更新，避免 sectService.updateDisciple 内部的峰级权限校验
        // （财务操作已有 @RequiresPermission("finance:adjust_lingshi") 权限控制）
        Long oldBalance = disciple.getLingshi() != null ? disciple.getLingshi() : 0L;
        Long newBalance = oldBalance + amount.longValue();
        disciple.setLingshi(newBalance);
        disciple.setUpdatedAt(java.time.LocalDateTime.now());
        discipleMapper.updateById(disciple);

        // 3. 记录流水
        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();

        LingshiTransaction transaction = transactionService.recordTransaction(
                discipleId, disciple.getName(), "allocate_to_member",
                amount.longValue(), newBalance,
                userId, username, remark != null && !remark.isEmpty() ? remark : "峰灵石分配",
                peak.getId(), peak.getName());

        log.info("峰灵石分配到个人成功: peak={}, disciple={}, amount={}, operatorId={}, newPeakAvailable={}, discipleBalance={}",
                peak.getName(), disciple.getName(), amount, userId, newPeakAvailable, newBalance);

        Map<String, Object> result = new HashMap<>();
        result.put("transaction", transaction);
        result.put("peakAvailableLingshi", newPeakAvailable);
        result.put("discipleBalance", newBalance);

        return Result.success(result, "峰灵石分配到个人成功");
    }

    /* ================================================================== */
    /*  六、灵石调整日志（全部财务/各峰财务分类）                             */
    /* ================================================================== */

    /**
     * 获取灵石调整日志
     * 权限：finance:view_all（全部财务）/ finance:view_own_peak（指定峰）
     * 支持按类型筛选和按峰ID筛选（区分"全部财务"和"各峰财务"）
     * category 参数支持:
     *   - all: 全部调整日志（默认）
     *   - personal: 个人相关调整日志
     */
    @Operation(summary = "获取灵石调整日志", description = "获取灵石调整操作历史记录，支持按类型和峰筛选",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/adjustments")
    @RequiresPermission("finance:view_all")
    public Result<List<LingshiTransaction>> getAdjustments(
            @Parameter(description = "操作类型筛选") @RequestParam(required = false) String type,
            @Parameter(description = "峰ID筛选（指定峰财务）") @RequestParam(required = false) Long peakId,
            @Parameter(description = "日志分类：all-全部, personal-个人") @RequestParam(required = false) String category,
            @Parameter(description = "返回条数限制") @RequestParam(required = false, defaultValue = "200") Integer limit) {
        QueryWrapper<LingshiTransaction> queryWrapper = new QueryWrapper<>();

        // 仅返回调整类操作的记录
        queryWrapper.in("type", "adjust_in", "adjust_out", "reward", "task_reward",
                "allocate_in", "peak_transfer_in", "peak_transfer_out",
                "peak_return_in", "peak_return_out", "allocate_to_member");

        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("type", type);
        }

        // 如果指定了峰ID，则筛选该峰的记录
        if (peakId != null) {
            queryWrapper.eq("peak_id", peakId);
        }

        // 个人分类：只返回与当前用户相关的记录
        Long userId = UserContext.getUserId();
        if ("personal".equals(category) && userId != null) {
            queryWrapper.and(w -> w.eq("operator_id", userId).or().eq("disciple_id", userId));
        }

        queryWrapper.orderByDesc("created_at");
        queryWrapper.last("LIMIT " + (limit != null ? limit : 200));

        List<LingshiTransaction> transactions = transactionService.list(queryWrapper);
        return Result.success(transactions);
    }

    /**
     * 获取当前峰灵石调整日志（仅峰财务权限用户可访问）
     * 权限：finance:view_own_peak
     * 自动根据当前用户所属峰筛选日志
     */
    @Operation(summary = "获取当前峰灵石调整日志", description = "获取当前用户所属峰的灵石调整日志",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/peak-adjustments")
    @RequiresPermission("finance:view_own_peak")
    public Result<List<LingshiTransaction>> getPeakAdjustments(
            @Parameter(description = "操作类型筛选") @RequestParam(required = false) String type,
            @Parameter(description = "返回条数限制") @RequestParam(required = false, defaultValue = "100") Integer limit) {
        Long userId = UserContext.getUserId();

        // 获取当前用户所属峰ID
        List<Long> userPeakIds = permissionService.getUserPeakIds(userId);

        if (userPeakIds == null || userPeakIds.isEmpty()) {
            return Result.success(java.util.Collections.emptyList());
        }

        QueryWrapper<LingshiTransaction> queryWrapper = new QueryWrapper<>();

        // 仅返回调整类操作的记录
        queryWrapper.in("type", "adjust_in", "adjust_out", "reward", "task_reward",
                "allocate_in", "peak_transfer_in", "peak_transfer_out",
                "peak_return_in", "peak_return_out", "allocate_to_member");

        // 筛选当前用户所属峰的记录
        queryWrapper.in("peak_id", userPeakIds);

        if (type != null && !type.isEmpty()) {
            queryWrapper.eq("type", type);
        }

        queryWrapper.orderByDesc("created_at");
        queryWrapper.last("LIMIT " + (limit != null ? limit : 100));

        List<LingshiTransaction> transactions = transactionService.list(queryWrapper);
        return Result.success(transactions);
    }

    /* ================================================================== */
    /*  工具方法                                                           */
    /* ================================================================== */

    /**
     * 根据峰名称获取峰ID
     */
    private Long getPeakIdByName(String peakName) {
        if (peakName == null || peakName.isEmpty() || "无".equals(peakName)) {
            return null;
        }
        try {
            Peak peak = peakMapper.selectOne(new QueryWrapper<Peak>().eq("name", peakName));
            return peak != null ? peak.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
