package top.weeki.domain.dingdian;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DingdianProcessor implements PageProcessor {
    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000);
    public String indexRegix = "https://www.23us.so/.*/index.html";
    public String detailRegix = "https://www.23us.so/.*/\\d+.html";

    @Override
    public void process(Page page) {

        // 部分二：定义如何抽取页面信息，并保存下来
        // 1. 抽取列表页
        if (page.getUrl().regex(indexRegix).match()) {
            fetchPageList(page);
        }
        // 2. 抽取文章页
        else if (page.getUrl().regex(detailRegix).match()) {
            fetchPageDetail(page);
        }
    }

    private void fetchPageDetail(Page page) {
        String bookName = page.getHtml().xpath("//div[@id='amain']/dl/dt/a[3]/text()").toString();
        String title = page.getHtml().xpath("//div[@id='amain']//h1/text()").toString();
        String content = page.getHtml().xpath("//dd[@id='contents']/text()").toString().replace("&nbsp;", "").replace(" ", " ").replace("    ", "\n ");
        String url = page.getRequest().getUrl().toString();
        int index = Integer.parseInt(url.substring(url.lastIndexOf("=") + 1));
        page.putField("bookName", bookName);
        page.putField("index", index);
        page.putField("title", title.trim());
        page.putField("content", "    " + content.trim());
    }

    private void fetchPageList(Page page) {
        String indexUrl = page.getUrl().toString();
        String pageRegix = indexUrl.replace("index.html", "\\d+.html");
        List<String> list = page.getHtml().links().regex(pageRegix).all();
//        list = list.subList(0, 10);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, list.get(i) + "?index=" + i);
        }
        page.addTargetRequests(list);
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        String path = "D:\\tmp\\book\\";
        Spider spider = Spider.create(new DingdianProcessor())
                .addUrl("https://www.23us.so/files/article/html/1/1188/index.html")
                .addPipeline(new DingdianJsonFilePipeline("D:\\tmp\\book\\"))
                //开启5个线程抓取
                .thread(100);
        //启动爬虫
        spider.start();
        while (true) {
            if (0 == Spider.Status.Stopped.compareTo(spider.getStatus())) {
                break;
            }
            Thread.sleep(10000);
        }
        // 汇总txt
        String bookName = "异常生物见闻录";
        Stream<Path> files = Files.list(new File(path + spider.getUUID() + "\\" + bookName).toPath());
//        Stream<Path> files = Files.list(new File("D:\\tmp\\book\\www.23us.so\\仙界归来").toPath());

        List<Integer> indexList = new ArrayList<>();
        files.forEach(path1 -> indexList.add(Integer.parseInt(path1.getFileName().toString().replace(".json", ""))));
        indexList.sort(Integer::compareTo);

        File resultFile = new File(path + bookName + ".txt");
        if (!resultFile.exists()) {
            resultFile.createNewFile();
        }
        PrintWriter printWriter = new PrintWriter(new FileWriter(resultFile));
        for (Integer index : indexList) {
            List<String> lines = Files.readAllLines(new File(path + spider.getUUID() + "\\" + bookName + "\\" + index + ".json").toPath());
//            List<String> lines = Files.readAllLines(new File(path + "www.23us.so\\" + bookName + "\\" + index + ".json").toPath());
            if (lines.isEmpty()) {
                continue;
            }
            JSONObject book = JSON.parseObject(lines.get(0));
            printWriter.println(book.getString("title"));
            printWriter.println(book.getString("content"));
            printWriter.flush();
        }
        printWriter.close();
    }
}
