package top.weeki;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import us.codecraft.webmagic.Site;

@RestController
@RequestMapping("book")
public class BookController {

    @GetMapping("fetch")
    public void fetchBook(@RequestParam String url, @RequestParam String name) {
        // 笔趣阁
        if(url.contains("www.qu.la")){
        }
        // 顶点小说
        else if(url.contains("www.23us.so")){

        }
    }
}
