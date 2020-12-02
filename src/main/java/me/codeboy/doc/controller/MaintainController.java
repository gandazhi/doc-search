package me.codeboy.doc.controller;

import me.codeboy.doc.base.CommonResult;
import me.codeboy.doc.data.DocUpdater;
import me.codeboy.doc.db.helper.DbHelper;
import me.codeboy.doc.utils.MyIOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据维护
 * Created by yuedong.li on 2019-12-21
 */
@RestController
public class MaintainController {
    private static final Logger logger = LoggerFactory.getLogger(MaintainController.class);
    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private DbHelper dbHelper;

    @Autowired
    private DocUpdater docUpdater;

    @Value("${file.path}")
    private String fileUploadPath;

    @RequestMapping(value = "/admin/createIndex")
    public CommonResult createEsIndex() {
        logger.info("create es index");
        if (dbHelper.createEsIndex()) {
            return CommonResult.success("创建成功");
        } else {
            return CommonResult.failed("创建失败，可能索引是已经存在");
        }
    }

    @RequestMapping(value = "/admin/update")
    public CommonResult updateDoc() {
        logger.info("start update doc");
        executorService.submit(() -> docUpdater.updateDoc());
        return CommonResult.success("操作成功");
    }

    @RequestMapping(value = "/admin/reset")
    public CommonResult resetDoc() {
        logger.info("start reset doc");
        if (dbHelper.resetEsData() && dbHelper.deleteAllDocs()) {
            return CommonResult.success("数据已清空");
        } else {
            return CommonResult.failed("数据清空失败");
        }
    }

    @PostMapping("/admin/uploadMarkDown")
    public CommonResult<String> uploadMarkDown(MultipartFile file){
        // 校验上传文件格式 这里暂时只能上传markdown
        String suffix = file.getOriginalFilename().split("\\.")[1];
        if (!"md".contains(suffix.toLowerCase())){
            return CommonResult.failed("只能上传md的文件");
        }
        String path = "/fileUpload" + File.separator + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        File folder = new File(fileUploadPath + path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String filePath = path + File.separator + file.getOriginalFilename();
        try {
            MyIOUtils.uploadFile(file.getInputStream(), fileUploadPath + filePath);
        } catch (Exception e) {
            return CommonResult.failed("上传md错误");
        }
        try {
            docUpdater.uploadMd(file);
        } catch (IOException e) {
            return CommonResult.failed("解析md数据到es中错误");
        }
        return CommonResult.failed("成功");
    }

}
