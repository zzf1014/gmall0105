package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class SkuServiceImpl implements SkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        // 插入skuInfo
        int i = pmsSkuInfoMapper.insert(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();
        // 插入平台属性关联

        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue:skuAttrValueList){
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }
        // 插入销售属性关联
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList){
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        // 插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList){
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
    }


    public PmsSkuInfo getSkuByIdDb(String skuId) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        // sku图片集合
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> select = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo1.setSkuImageList(select);
        return pmsSkuInfo1;
    }

    @Override
    public PmsSkuInfo getSkuById(String skuId,String ip) {

        System.out.println("ip为"+ip+"用户"+Thread.currentThread().getName()+"进入商品详情请求");
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        // 链接缓存
        Jedis jedis = redisUtil.getJedis();
        //查询缓存
        String skuKey = "sku:"+skuId+":info";
        String skuJson = jedis.get(skuKey);
        if (StringUtils.isNotBlank(skuJson)){
            System.out.println("ip为"+ip+"用户"+Thread.currentThread().getName()+"从缓存中获取商品详情");
            pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
        }else {
            // 如果没有 查询mysql
            // 设置分布式锁  拿到线程有10秒的过期时间
            System.out.println("ip为"+ip+"用户"+Thread.currentThread().getName()+"发现缓存中没有，申请分布式锁："+"sku:"+skuId+":lock");

            String token = UUID.randomUUID().toString(); // 用来识别是否是自己的锁
            String ok = jedis.set("sku:"+skuId+":lock",token,"nx","px",10*1000);
            if(StringUtils.isNotBlank(ok)&&ok.equals("OK")){
                // 设置成功，有权在10秒内访问数据库
                System.out.println("ip为"+ip+"用户"+Thread.currentThread().getName()+"设置成功，有权在10秒内访问数据库："+"sku:"+skuId+":lock");
                pmsSkuInfo = getSkuByIdDb(skuId);
                /*增加睡眠在此 可测试效果*/
                if (pmsSkuInfo!=null){
                    // 查询结果存入缓存
                    jedis.set(skuKey,JSON.toJSONString(pmsSkuInfo));
                }else {
                    // 数据库中不存在值
                    // 为了防止缓存穿透  将null 或者 空字符串设置给 reids
                    jedis.setex(skuKey,60*3,"");
                }
                // 在访问数据库mysql后 释放分布式锁
                String lockToken = jedis.get("sku:" + skuId + ":lock");
                if (StringUtils.isNotBlank(lockToken)&&token.equals(lockToken)){

                    //String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                   // jedis.eval(script);//可用lua脚本，在查询到key的同时删除该key，防止高并发下的意外发生
                    jedis.del("sku:"+skuId+":lock"); //用token来确认删除的是自己的分布式锁
                }

            }else{
                try{
                    Thread.sleep(3000);
                }catch (Exception e){
                    e.printStackTrace();
                }

                // 设置分布式锁失败  自旋（该线程在睡眠几秒后重新访问本方法）
                System.out.println("ip为"+ip+"用户"+Thread.currentThread().getName()+"没有拿到锁，开始自旋："+"sku:"+skuId+":lock");
                // getSkuById(skuId);  错误的自旋代码，如果这样写，
                // 在第一次进入方法后碰到失败该方法睡眠，在该线程下会再次创建一个子线程 ，该子线程就是一个孤儿线程
                return  getSkuById(skuId,ip); //正确自旋
            }





        }

        jedis.close();
        return pmsSkuInfo;
    }

    @Override
    public List<PmsSkuInfo> getSpuSaleAttrListCheckBySku(String productId) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSpuSaleAttrListCheckBySku( productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku() {
       List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
       for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos){
           String skuId = pmsSkuInfo.getId();

           PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
           pmsSkuAttrValue.setSkuId(skuId);
           List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
           pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValues);
       }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal productPrice) {
        boolean b = false;

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        BigDecimal price = pmsSkuInfo1.getPrice();

        if(price.compareTo(productPrice)==0){
            b = true;
        }


        return b;
    }
}
