package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service/*使用dubbo中的注解*/
public class UserServiceImpl implements UserService {

    @Autowired
    UserMapper userMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;
    @Autowired
    RedissonClient redissonClient;
    @Override
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMembers = userMapper.selectAll();
        return umsMembers;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        Example example = new Example(UmsMemberReceiveAddress.class);
        example.createCriteria().andEqualTo("memberId",memberId);
        return umsMemberReceiveAddressMapper.selectByExample(example);

    }

    @Override
    public UmsMember login(UmsMember umsMember) {
        Jedis jedis =null;
        try {
            jedis = redisUtil.getJedis();

            if (jedis!=null){
                String userMemberStr = jedis.get("user:" + umsMember.getPassword() + ":info");
                if (StringUtils.isNotBlank(userMemberStr)){
                    // 密码正确
                    UmsMember umsMemberFromCache = JSON.parseObject(userMemberStr, UmsMember.class);
                    return umsMemberFromCache;
                }else{
                    // 密码错误
                    // 缓存中没有开启数据库
                    UmsMember umsMemberFromDb = loginFromDb(umsMember);
                    if (umsMemberFromDb!=null){
                        jedis.setex("user:" + umsMember.getPassword() + ":info",60*60*24,JSON.toJSONString(umsMemberFromDb));
                    }
                    return umsMemberFromDb;
                }
            }else{
                // 连接Reids失败 开启数据库 设置分布式锁
                RLock userLock = redissonClient.getLock("userLock");
                userLock.lock();
                UmsMember umsMemberFromDb = loginFromDb(umsMember);
                userLock.unlock();
                return umsMemberFromDb;
            }

        }finally {
            jedis.close();
        }
    }

    @Override
    public void addUserToken(String token, String memberId) {
        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:"+memberId+":token",60*60*2,token);
        jedis.close();
    }

    private UmsMember loginFromDb(UmsMember umsMember) {
        List<UmsMember> umsMembers = userMapper.select(umsMember);
        if (umsMembers!=null){
            return umsMembers.get(0);
        }
        return null;
    }
}
