package top.weeki.domain.dingdian;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DingdianJsonFilePipeline implements Pipeline {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private String path;

    /**
     * new JsonFilePageModelPipeline with default path "/data/webmagic/"
     */
    public DingdianJsonFilePipeline(String path) {
        this.path = path;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        String path = this.path + "\\" + task.getUUID() + "\\";
        try {
            String bookName = resultItems.get("bookName");
            Integer index = resultItems.get("index");
            if (index != null) {
                path = path + bookName + "\\";
                PrintWriter printWriter = new PrintWriter(new FileWriter(getFile(path + index + ".json")));
                printWriter.write(JSON.toJSONString(resultItems.getAll()));
                printWriter.close();
            }
        } catch (IOException e) {
            logger.warn("write file error", e);
        }
    }

    public File getFile(String fullName) {
        checkAndMakeParentDirecotry(fullName);
        return new File(fullName);
    }

    public void checkAndMakeParentDirecotry(String fullName) {
        int index = fullName.lastIndexOf("\\");
        if (index > 0) {
            String path = fullName.substring(0, index);
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

}
