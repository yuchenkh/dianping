package com.example.dianping.controller;

import com.example.dianping.dto.Result;
import com.example.dianping.service.IShopTypeService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 商户类型 controller。最后编辑于：2022-4-18。
 * @author yuchen
 */
@RestController
@AllArgsConstructor
@RequestMapping("/shop-type")
public class ShopTypeController {

    private final IShopTypeService typeService;

    @GetMapping("list")
    public Result queryTypeList() {
        return Result.ok(typeService.allTypes());
    }
}
