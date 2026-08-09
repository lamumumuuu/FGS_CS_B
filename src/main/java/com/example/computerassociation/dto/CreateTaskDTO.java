package com.example.computerassociation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTaskDTO {

    @NotBlank(message = "任务标题不能为空")
    @Size(min = 5, max = 50, message = "标题长度必须在5-50个字符之间")
    private String title;

    @NotBlank(message = "任务描述不能为空")
    @Size(min = 10, max = 500, message = "描述长度必须在10-500个字符之间")
    private String description;

    @NotBlank(message = "任务难度不能为空")
    private String difficulty;

    @Min(value = 1, message = "悬赏灵石必须大于0")
    private Integer reward;

    private String deadline;

    private String techRequirements;

    private Long peakId;
}
