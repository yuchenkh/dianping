package com.example.dianping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    private Boolean success;

    // 错误信息，请求处理不成功时提供
    private String errorMsg;

    private Object data;

    private Long total;

    // 请求成功，不返回数据
    public static Result ok(){
        return new Result(true, null, null, null);
    }

    public static Result ok(Object data){
        return new Result(true, null, data, null);
    }

    public static Result ok(List<?> data, Long total){
        return new Result(true, null, data, total);
    }

    public static Result fail(String errorMsg){
        return new Result(false, errorMsg, null, null);
    }
}
