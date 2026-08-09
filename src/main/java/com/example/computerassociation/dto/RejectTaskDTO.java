package com.example.computerassociation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectTaskDTO {

    @Size(max = 500, message = "驳回原因不能超过500个字符")
    private String reason;
}
