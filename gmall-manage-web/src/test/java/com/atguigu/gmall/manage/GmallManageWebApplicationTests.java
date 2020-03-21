package com.atguigu.gmall.manage;


import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;


@SpringBootTest
@RunWith(SpringRunner.class)
class GmallManageWebApplicationTests {

    @Test
    void contextLoads() throws IOException, MyException {
        // 配置fdfsd的全局连接地址
        String tracker = GmallManageWebApplicationTests.class.getResource("/trackr.conf").getPath();//获取配置文件路径
        ClientGlobal.init(tracker);

        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getTrackerServer();
        StorageClient storageClient=new StorageClient(trackerServer,null);

        String orginalFilename="e://victor.jpg";
        String[] upload_file = storageClient.upload_file(orginalFilename, "png", null);

        String path = "http://192.168.163.128";

        for (int i = 0; i < upload_file.length; i++) {
            String s = upload_file[i];
            path+="/"+s;
            System.out.println("s = " + s);
        }
    }

}
