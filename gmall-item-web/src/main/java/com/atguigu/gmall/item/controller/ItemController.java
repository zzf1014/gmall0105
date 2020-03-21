package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;

    @Reference
    SpuService spuService;
    @RequestMapping("index")
    public String index(ModelMap modelMap){
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add("循环数据"+i);
        }
        modelMap.put("list",list);
        modelMap.put("check",1);
        modelMap.put("hello","hello thymeleaf!!");
        return "index";
    }


    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap, HttpServletRequest request){
        String remoteAddr = request.getRemoteAddr();// 直接在请求中获取ip地址 如果使用了ngx 获取的是ngx代理的ip
        //request.getHeader(""); nginx负载均衡 的获取Ip
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId,remoteAddr);
        //sku对象
        modelMap.put("skuInfo",pmsSkuInfo);
        //销售属性列表
        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(),pmsSkuInfo.getId());
        modelMap.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        // 查询当前sku的spu的其它sku的集合hash表
        HashMap<String, String> skuSaleAttrHash = new HashMap<>();
        List<PmsSkuInfo> pmsSkuInfos = skuService.getSpuSaleAttrListCheckBySku(pmsSkuInfo.getProductId());
       for(PmsSkuInfo skuInfo:pmsSkuInfos){
           String k = "";
           String v = skuInfo.getId();
           List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
           for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList){
               k += pmsSkuSaleAttrValue.getSaleAttrValueId()+"|";
           }
           skuSaleAttrHash.put(k,v);
       }
       // 将sku的销售属性Hash表放到页面
        String skuSaleAttrHashStr = JSON.toJSONString(skuSaleAttrHash);
       modelMap.put("skuSaleAttrHashStr",skuSaleAttrHashStr);
        return "item";
    }


}
