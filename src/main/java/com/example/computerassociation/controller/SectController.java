// src/main/java/com/example/computerassociation/controller/SectController.java

/**
 * 宗门事务控制器
 *
 * 提供宗门相关 REST API，包括：
 * - 当前用户宗门信息
 * - 弟子列表（全部 / 按峰筛选 / 管理层 / 搜索）
 * - 峰列表
 * - 弟子新增、更新、删除、移动门派
 * 敏感操作通过 @RequiresPermission 注解限制权限。
 */

package com.example.computerassociation.controller;

import com.example.computerassociation.annotation.RequiresPermission;
import com.example.computerassociation.common.Result;
import com.example.computerassociation.common.UserContext;
import com.example.computerassociation.entity.AuditLog;
import com.example.computerassociation.entity.Disciple;
import com.example.computerassociation.entity.Peak;
import com.example.computerassociation.service.PeakService;
import com.example.computerassociation.service.SectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "宗门事务", description = "弟子管理、门派信息等接口")
@RestController
@RequestMapping("/api/sect")
public class SectController {

    @Autowired
    private SectService sectService;           /// 宗门业务服务

    @Autowired
    private PeakService peakService;           /// 峰业务服务

    /* ------------------------------------------------------------------ */
    /*  当前用户宗门信息                                                  */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "获取当前用户宗门信息", description = "获取当前登录用户的宗门相关信息",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/current-user")
    public Result<Map<String, Object>> getCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            return Result.fail(401, "用户未登录");
        }

        // 从 disciples 表查询当前用户对应的弟子记录
        Disciple disciple = sectService.lambdaQuery()
                .eq(Disciple::getUserId, userId)
                .one();

        Map<String, Object> result = new HashMap<>();
        if (disciple != null) {
            result.put("id", String.valueOf(disciple.getId()));
            result.put("name", disciple.getName());
            result.put("role", mapRoleToChinese(disciple.getRole()));   // 英文角色转中文
            result.put("peak", disciple.getPeak());
            result.put("permissions", new String[]{});                  // 暂时空数组，可后续扩展
        } else {
            // 用户未关联弟子记录，返回默认信息
            result.put("id", String.valueOf(userId));
            result.put("name", "用户" + userId);
            result.put("role", "弟子");
            result.put("peak", "无");
            result.put("permissions", new String[]{});
        }

        return Result.success(result);
    }

    /* ------------------------------------------------------------------ */
    /*  弟子查询接口                                                     */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "获取所有弟子列表", description = "获取宗门所有弟子的列表",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @GetMapping("/disciples")
    @RequiresPermission("member:view_all")
    public Result<List<Disciple>> getAllDisciples(
            @Parameter(description = "按门派筛选") @RequestParam(required = false) String peak) {
        List<Disciple> disciples;
        if (peak != null && !peak.isEmpty() && !"全部".equals(peak)) {
            disciples = sectService.getDisciplesByPeak(peak);          // 按峰筛选
        } else {
            disciples = sectService.getAllDisciples();                 // 全部弟子
        }
        return Result.success(disciples);
    }

    @Operation(summary = "获取管理团队弟子", description = "获取宗门管理层弟子列表",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/disciples/management")
    @RequiresPermission("member:view_all")
    public Result<List<Disciple>> getManagementDisciples() {
        List<Disciple> disciples = sectService.getManagementDisciples();
        return Result.success(disciples);
    }

    @Operation(summary = "搜索弟子", description = "根据姓名或学号搜索弟子",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/disciples/search")
    @RequiresPermission("member:view_own_peak")
    public Result<List<Disciple>> searchDisciples(
            @Parameter(description = "搜索关键词") @RequestParam String keyword) {
        List<Disciple> disciples = sectService.searchDisciples(keyword);
        return Result.success(disciples);
    }

    @Operation(summary = "获取弟子详情", description = "根据ID获取弟子详细信息",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/disciples/{id}")
    @RequiresPermission("member:view_own_peak")
    public Result<Disciple> getDiscipleById(@Parameter(description = "弟子ID") @PathVariable Long id) {
        Disciple disciple = sectService.getDiscipleById(id);
        return Result.success(disciple);
    }

    /* ------------------------------------------------------------------ */
    /*  峰列表查询                                                       */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "获取所有峰列表", description = "获取宗门所有峰的信息",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/peaks")
    @RequiresPermission("peak:view")
    public Result<List<Peak>> getAllPeaks() {
        List<Peak> peaks = peakService.getAllPeaks();    // 使用自定义方法获取所有峰
        // 填充各峰的弟子数量（Peak.memberCount 为非持久化瞬态字段）
        peaks.forEach(peak -> peak.setMemberCount(peakService.getMemberCount(peak.getId())));
        return Result.success(peaks);
    }

    /* ------------------------------------------------------------------ */
    /*  弟子增删改与操作                                                 */
    /* ------------------------------------------------------------------ */
    @Operation(summary = "添加新弟子", description = "添加新的弟子记录",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/disciples")
    @RequiresPermission("member:update_role")
    public Result<Disciple> addDisciple(@RequestBody Disciple disciple) {
        Disciple result = sectService.addDisciple(disciple);
        return Result.success(result, "添加成功");
    }

    @Operation(summary = "更新弟子信息", description = "更新弟子的基本信息",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PutMapping("/disciples/{id}")
    @RequiresPermission("member:update_role")
    public Result<Boolean> updateDisciple(
            @Parameter(description = "弟子ID") @PathVariable Long id,
            @RequestBody Disciple disciple) {
        disciple.setId(id);
        boolean success = sectService.updateDisciple(disciple);
        return Result.success(success, success ? "更新成功" : "更新失败");
    }

    @Operation(summary = "删除弟子", description = "删除指定弟子，需提供删除原因",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/disciples/{id}")
    @RequiresPermission("member:expel")
    public Result<Boolean> deleteDisciple(
            @Parameter(description = "弟子ID") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        boolean success = sectService.deleteDisciple(id, reason);
        return Result.success(success, success ? "删除成功" : "删除失败");
    }

    @Operation(summary = "移动弟子门派", description = "将弟子移动到另一个峰",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @PutMapping("/disciples/{id}/move")
    @RequiresPermission("peak:manage_members")
    public Result<Boolean> moveDisciplePeak(
            @Parameter(description = "弟子ID") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String newPeak = body.get("peak");
        boolean success = sectService.moveDisciplePeak(id, newPeak);
        return Result.success(success, success ? "移动成功" : "移动失败");
    }

    /* ------------------------------------------------------------------ */
    /*  弟子历史记录接口                                                  */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "获取弟子历史记录", description = "获取指定弟子的所有操作历史",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/disciples/{id}/history")
    @RequiresPermission("member:view_all")
    public Result<List<AuditLog>> getDiscipleHistory(
            @Parameter(description = "弟子ID") @PathVariable Long id) {
        List<AuditLog> history = sectService.getDiscipleHistory(id);
        return Result.success(history);
    }

    @Operation(summary = "获取所有弟子历史记录", description = "获取所有弟子的操作历史，支持分页，仅宗主可用",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/disciples/history/all")
    @RequiresPermission("member:view_all")
    public Result<List<AuditLog>> getAllDiscipleHistory(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size) {
        List<AuditLog> history = sectService.getAllDiscipleHistory(page, size);
        return Result.success(history);
    }

    /* ------------------------------------------------------------------ */
    /*  峰管理接口                                                        */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "创建峰", description = "创建新的峰",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping("/peaks")
    @RequiresPermission("peak:create")
    public Result<Peak> createPeak(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        Peak peak = peakService.addPeak(name, description);
        return Result.success(peak);
    }

    @Operation(summary = "修改峰信息", description = "修改指定峰的信息",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PutMapping("/peaks/{id}")
    // 注：权限检查在 PeakServiceImpl.updatePeak 中完成（区分 peak:edit_any, peak:edit_own）
    public Result<Peak> updatePeak(
            @Parameter(description = "峰ID") @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String description = body.get("description");
        Peak peak = peakService.updatePeak(id, name, description);
        return Result.success(peak);
    }

    @Operation(summary = "解散峰", description = "解散指定峰",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "解散成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @DeleteMapping("/peaks/{id}")
    @RequiresPermission("peak:create")
    public Result<Void> deletePeak(@Parameter(description = "峰ID") @PathVariable Long id) {
        boolean success = peakService.deletePeak(id);
        if (!success) {
            return Result.fail("峰不存在或峰内还有成员");
        }
        return Result.success(null);
    }

    /* ------------------------------------------------------------------ */
    /*  财务管理接口                                                      */
    /* ------------------------------------------------------------------ */

    @Operation(summary = "调整灵石", description = "调整指定弟子的灵石数量",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "调整成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping("/finance/adjust")
    @RequiresPermission("finance:adjust_lingshi")
    public Result<Void> adjustLingshi(@RequestBody Map<String, Object> body) {
        Long discipleId = Long.valueOf(body.get("discipleId").toString());
        Integer amount = Integer.valueOf(body.get("amount").toString());
        String reason = (String) body.get("reason");
        
        Disciple disciple = sectService.getDiscipleById(discipleId);
        if (disciple == null) {
            return Result.fail("弟子不存在");
        }
        
        Long newLingshi = (disciple.getLingshi() != null ? disciple.getLingshi() : 0L) + amount.longValue();
        if (newLingshi < 0) {
            return Result.fail("灵石余额不足");
        }
        
        disciple.setLingshi(newLingshi);
        sectService.updateDisciple(disciple);
        
        return Result.success(null, "灵石调整成功");
    }

    @Operation(summary = "设定悬赏基准", description = "设定全局或本峰的悬赏基准价格",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping("/finance/base")
    @RequiresPermission("finance:set_base")
    public Result<Void> setBaseReward(@RequestBody Map<String, Object> body) {
        // 此处为示例实现，实际应存储到专门的配置表
        return Result.success(null, "悬赏基准设置成功");
    }

    /* ------------------------------------------------------------------ */
    /*  工具方法：英文角色标识转中文                                      */
    /* ------------------------------------------------------------------ */
    private String mapRoleToChinese(String role) {
        if (role == null) return "弟子";
        // 兼容英文标识和中文显示名（历史数据可能存储中文角色名）
        return switch (role) {
            case "sect_master", "宗主" -> "宗主";
            case "grand_elder", "大长老" -> "大长老";
            case "supreme_elder", "太上长老" -> "太上长老";
            case "honor_elder", "荣誉长老" -> "荣誉长老";
            case "elder", "长老" -> "长老";
            case "inner_disciple", "内门弟子" -> "内门弟子";
            case "outer_disciple", "外门弟子" -> "外门弟子";
            default -> "弟子";
        };
    }
}