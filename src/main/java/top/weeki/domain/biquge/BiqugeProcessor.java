package top.weeki.domain.biquge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import top.weeki.domain.dingdian.DingdianJsonFilePipeline;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

public class BiqugeProcessor
    implements PageProcessor {
    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000);
    public static String indexRegix = "https://www.biquge5200.cc/\\d+_\\d+/";
    public static String detailRegix =
        "https://www.biquge5200.cc/\\d+_\\d+/\\d+.html";

    @Override
    public void process(
        Page page) {

        // 2. 抽取文章页
        if (page.getUrl().regex(detailRegix).match()) {
            fetchPageDetail(page);
        }
        // 部分二：定义如何抽取页面信息，并保存下来
        // 1. 抽取列表页
        else if (page.getUrl().regex(indexRegix).match()) {
            fetchPageList(page);
        }
    }

    private void fetchPageDetail(
        Page page) {
        String bookName = page.getHtml()
            .xpath("//div[@class='con_top']/a[3]/text()").toString();
        String title = page.getHtml()
            .xpath("//div[@class='bookname']/h1/text()").toString();
        List<String> contents =
            page.getHtml().xpath("//div[@id='content']/p/text()").all();
        String url = page.getRequest().getUrl().toString();
        int index = Integer.parseInt(url.substring(url.lastIndexOf("=") + 1));
        page.putField("bookName", bookName);
        page.putField("index", index);
        page.putField("title", title.trim());
        page.putField("content", contents);
    }

    private void fetchPageList(
        Page page) {
        String indexUrl = page.getUrl().toString();
        String pageRegix = indexUrl + "\\d+.html";
        List<String> list = page.getHtml().links().regex(pageRegix).all();
        // list = list.subList(0, 10);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, list.get(i) + "?index=" + i);
        }
        page.addTargetRequests(list);
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(
        String[] args)
        throws InterruptedException, IOException {
        String path = "D:\\tmp\\book\\";
        Spider spider = Spider.create(new BiqugeProcessor())
            .addUrl("https://www.biquge5200.cc/0_87/")
            .addPipeline(new DingdianJsonFilePipeline("D:\\tmp\\book\\"))
            // 开启5个线程抓取
            .thread(100);
        // 启动爬虫
        spider.start();
        while (true) {
            if (0 == Spider.Status.Stopped.compareTo(spider.getStatus())) {
                break;
            }
            Thread.sleep(10000);
        }
        // 汇总txt
        String bookName = "武极天下";
        Stream<Path> files = Files
            .list(new File(path
                + spider.getUUID()
                + "\\"
                + bookName).toPath());
        // .list(new File(path + "www.biquge5200.cc" + "\\" + bookName)
        // .toPath());
        // Stream<Path> files = Files.list(new
        // File("D:\\tmp\\book\\www.23us.so\\仙界归来").toPath());

        List<Integer> indexList = new ArrayList<>();
        files.forEach(path1 -> indexList.add(Integer
            .parseInt(path1.getFileName().toString().replace(".json", ""))));
        indexList.sort(Integer::compareTo);

        File resultFile = new File(path + bookName + ".txt");
        if (!resultFile.exists()) {
            resultFile.createNewFile();
        }
        PrintWriter printWriter = new PrintWriter(new FileWriter(resultFile));
        for (Integer index : indexList) {
            List<String> lines = Files
                .readAllLines(new File(path
                    + spider.getUUID()
                    // + "www.biquge5200.cc"
                    + "\\"
                    + bookName
                    + "\\"
                    + index
                    + ".json").toPath());
            // List<String> lines = Files.readAllLines(new File(path +
            // "www.23us.so\\" + bookName + "\\" + index +
            // ".json").toPath());
            if (lines.isEmpty()) {
                continue;
            }
            JSONObject book = JSON.parseObject(lines.get(0));
            printWriter.println(book.getString("title"));
            try {
                JSONArray contents = book.getJSONArray("content");
                for (int i = 0; i < contents.size(); i++) {
                    printWriter.println(contents.getString(i));
                }

            } catch (Exception e) {
                System.out.println(book);
            }
            printWriter.flush();
        }
        printWriter.close();
    }
}
