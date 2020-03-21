package com.atguigu.gmall.manage.util;

import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.web.multipart.MultipartFile;

public class PmsUploadUtil {
    public static String uploadImage(MultipartFile multipartFile) {
        String path = "http://192.168.163.128";
        String[] upload_file = new String[0];

        // 配置fdfsd的全局连接地址
        String tracker = PmsUploadUtil.class.getResource("/tracker.conf").getPath();//获取配置文件路径
       try {
           ClientGlobal.init(tracker);
           TrackerClient trackerClient = new TrackerClient();
           TrackerServer trackerServer = trackerClient.getTrackerServer();
           StorageClient storageClient=new StorageClient(trackerServer,null);

           //获取文件后缀名
           String originalFilename = multipartFile.getOriginalFilename();//原文件名称 a.jpg
           byte[] bytes = multipartFile.getBytes();
           //String name = multipartFile.getName();
           int i = originalFilename.lastIndexOf(".");
           String file_ext_name = originalFilename.substring(i+1);
            upload_file = storageClient.upload_file(bytes, file_ext_name, null);
       }catch (Exception e){
           e.printStackTrace();
       }

        for (int i = 0; i < upload_file.length; i++) {
            String s = upload_file[i];
            path+="/"+s;
            System.out.println(s);
        }
        return path;
    }
}
