package com.comp4321.searchenginebackend.services;

import com.comp4321.searchenginebackend.utils.Crawler;
import org.htmlparser.util.ParserException;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class CrawlerService {

    private Crawler crawler;

    public CrawlerService(){
        System.out.println("Initializing CrawlerService Successfully");
    }

    public Crawler getCrawler(){
        return this.crawler;
    }

    /**
     * funtion to call the crawler object to crawl the webpages
     * @param rootUrl root url to crawl
     * @param maxPages max number of pages to crawl
     * @return a map of crawl results
     * @throws ParserException
     * @throws IOException
     * @throws ParseException
     */
    public HashMap<String, Object> crawlRootURL(String rootUrl, Integer maxPages, JdbmService jdbmService) throws ParserException, IOException, ParseException {

        long startTime = System.currentTimeMillis();
        crawler = new Crawler(rootUrl, maxPages);
        crawler.crawl(jdbmService);
        long stopTime = System.currentTimeMillis();
        long msDuration = (stopTime - startTime) % 1000;
        long secDuration = TimeUnit.MILLISECONDS.toSeconds(stopTime - startTime);
        HashMap<String, Object> crawlResult = new HashMap<>();
        crawlResult.put("executionTime", String.valueOf(secDuration) + " sec" + String.valueOf(msDuration) + " ms");
        crawlResult.put("rootUrl", crawler.getRootURL());
        crawlResult.put("retrievedPageCount",crawler.getRetrievedNum());
        return crawlResult;
    }
}
