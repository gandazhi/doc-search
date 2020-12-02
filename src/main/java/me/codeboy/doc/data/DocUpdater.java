package me.codeboy.doc.data;

import me.codeboy.doc.db.domain.Doc;
import me.codeboy.doc.db.helper.DbHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 文档更新器
 * Created by yuedong.li on 2019/12/21
 */
@Component
public class DocUpdater {
    private static final Logger logger = LoggerFactory.getLogger(DocUpdater.class);
    @Autowired
    private DbHelper dbHelper;

    @Value("${spring.gitbook.ds}")
    private String dataSource = "";

    private boolean updating = false;

    /**
     * 更新文档
     */
    public void updateDoc() {
        if (updating) {
            logger.info("doc is updating");
            return;
        }

        updating = true;
        long now = System.currentTimeMillis() - 1000;
        String[] data = dataSource.split(",");
        for (String entryUrl : data) {
            updateSiteDocs(entryUrl.trim());
        }
        List<Doc> docList = dbHelper.queryAllOldDocs(now);
        for (Doc doc : docList) {
            dbHelper.deleteEsData(doc.getEsId());
        }
        dbHelper.deleteAllOldDocs(now);
        updating = false;
    }

    /**
     * 上传的md文件 解析后存入es中
     * @param file
     * @throws IOException
     */
    public void uploadMd(MultipartFile file) throws IOException {
        // 预览文件url 暂时不用
        String docUrl = "";
        // 文件title 使用文件名
        String docTitle = file.getOriginalFilename();
        String docContent = new String(file.getBytes(),"UTF-8");
        String esId = dbHelper.addEsData(docTitle, docContent, docUrl);
        dbHelper.saveDoc(Doc.create(docTitle, docUrl, esId));
        logger.info("add doc success: title={}, url={}， esId={}", docTitle, docUrl, esId);
    }

    /**
     * 更新一个站点的文章
     *
     * @param entryUrl 入口地址
     */
    private void updateSiteDocs(String entryUrl) {
        try {
            Map<String, String> docInfos = DocSucker.getDocLinks(entryUrl);
            for (Map.Entry<String, String> docInfo : docInfos.entrySet()) {
                String docUrl = docInfo.getKey();
                String docTile = docInfo.getValue();
                String docContent = DocSucker.getDocContent(docUrl);
                docContent = docContent.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                Doc doc = dbHelper.queryDocByUrl(docUrl);
                if (doc != null) {  //文档已经存在，更新
                    dbHelper.updateDoc(doc);
                    dbHelper.updateEsData(doc.getEsId(), docTile, docContent, docUrl);
                    logger.info("update doc success: title={}, url={}", docTile, docUrl);
                } else {
                    String esId = dbHelper.addEsData(docTile, docContent, docUrl);
                    dbHelper.saveDoc(Doc.create(docTile, docUrl, esId));
                    logger.info("add doc success: title={}, url={}", docTile, docUrl);
                }
            }
        } catch (Exception e) {
            logger.warn("update error", e);
        }
    }
}
