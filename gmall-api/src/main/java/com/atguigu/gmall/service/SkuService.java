package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsSkuInfo;

import java.util.List;

public interface SkuService {
    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuById(String skuId,String remoteAddr);

    List<PmsSkuInfo> getSpuSaleAttrListCheckBySku(String productId);

    List<PmsSkuInfo> getAllSku();
}
