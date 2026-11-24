package com.api.resibo.dtos;

import com.api.resibo.types.ResponseType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * The single response envelope returned by every service and controller.
 * Mirrors the convention used by the gainsoclock project — see resibo-plan.md §6.1.2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto {

    private ResponseType responseType;
    private String code;
    private Object data;
    private String message;
    private String className;
    private String methodName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private Date responseTime;

    @JsonIgnore
    public Boolean isSuccess() {
        return responseType == ResponseType.SUCCESS;
    }

    @JsonIgnore
    public Boolean isFail() {
        return responseType == ResponseType.ERROR;
    }
}
