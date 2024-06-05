package com.comp4321.searchenginebackend.controllers;

import com.comp4321.searchenginebackend.models.PageResponse;
import com.comp4321.searchenginebackend.services.CrawlerService;
import com.comp4321.searchenginebackend.services.JdbmService;
import com.comp4321.searchenginebackend.services.SearchEngineService;
import com.comp4321.searchenginebackend.utils.VectorSpaceModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@CrossOrigin(origins = "http://localhost:5173", maxAge = 7200)
@RestController
@RequestMapping("/")
public class MainController {
    @Autowired
    private CrawlerService crawlerService;
    @Autowired
    private JdbmService jdbmService;
    @Autowired
    private SearchEngineService searchEngineService;
    @Autowired
    private VectorSpaceModel vectorSpaceModel;

    /**
     * api end-point to handle webpage crawling request
     * @param rootUrl root webpage to crawl
     * @param maxPages max number of web pages to crawl
     * @return crawling response entity in json format
     */
    @GetMapping("/crawl")
    public ResponseEntity<?> requestCrawl(@RequestParam(name = "rootUrl") String rootUrl,
                                          @RequestParam(name = "maxPages") Integer maxPages) {
        System.out.println("requestCrawl:Handling");
        HashMap<String, Object> res = new HashMap<>();
        try {
            new URL(rootUrl).toURI();
        } catch (Exception e) {
            res.put("status","BAD_REQUEST");
            res.put("message","Invalid URL");
            return new ResponseEntity<>(res,HttpStatus.BAD_REQUEST);
        }

        if(maxPages <= 0){
            res.put("status","BAD_REQUEST");
            res.put("message","Invalid Requested Number of Pages");
            return new ResponseEntity<>(res,HttpStatus.BAD_REQUEST);
        }

        HashMap<String, Object> crawlResult;
        HashMap<String, Object> writeResult;
        HashMap<String, Object> dbIndexingResult;

        try {
            long startTime = System.currentTimeMillis();

            crawlResult = crawlerService.crawlRootURL(rootUrl, maxPages, jdbmService);
            dbIndexingResult = jdbmService.indexCrawledPages(crawlerService, vectorSpaceModel);
            writeResult = jdbmService.writeResultToFile();

            long stopTime = System.currentTimeMillis();
            long msDuration = (stopTime - startTime) % 1000;
            long secDuration = TimeUnit.MILLISECONDS.toSeconds(stopTime - startTime);
            res.put("executionTime", secDuration + "." + msDuration + "sec");
            res.put("writeResult", writeResult);
            res.put("dbIndexingResult", dbIndexingResult);
            res.put("crawlResult", crawlResult);
            System.out.println("requestCrawl:Handled Successfully");
        } catch (Exception e) {
            return new ResponseEntity<>(e.getLocalizedMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    /**
     * api end-point to handle review database index info request
     * @return response entity containing database content in json format
     */
    @GetMapping("/requestDBStatus")
    public ResponseEntity<?> requestDBStatus() {
        System.out.println("requestDBStatus:Handling");
        Map<String, Object> res = new HashMap<>();
        try {
            res.put("URL PageID Lookup Index", jdbmService.getAllUrlPageIdIndexes());
            res.put("KeywordId Lookup Index", jdbmService.getAllKeywordIdLookupIndexes());
            res.put("Keyword Lookup Index", jdbmService.getAllKeywordLookupIndexes());
            res.put("Forward Index", jdbmService.getAllPages());
            res.put("Title Inverted Index", jdbmService.getAllTitleInvertedIndexes());
            res.put("Body Inverted Index",  jdbmService.getAllBodyInvertedIndexes());
        } catch (IOException e) {
            res.put("error", e.getLocalizedMessage());
            return new ResponseEntity<>(res, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        System.out.println("requestDBStatus:Handled Successfully");
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    /**
     * api end-point to handle clear all database content request
     * @return response entity containing operation execution status message
     */
    @GetMapping("/clearDB")
    public ResponseEntity<?> clearDB() {
        System.out.println("clearDB:Handling");
        Map<String, Object> res = new HashMap<>();
        try {
            jdbmService.clearDB();
            res.put("message", "database cleared successfully");
        } catch (IOException e) {
            res.put("error", e.getLocalizedMessage());
            return new ResponseEntity<>(res, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        System.out.println("clearDB:Handled Successfully");
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    /**
     * api end-point to handle request for getting all stemmed keywords in database
     * @return response entity containing all stemmed keywords in json format
     */
    @GetMapping("/keywords")
    public  ResponseEntity<?> getKeywords(@RequestParam(name = "keywordPage") String keywordPage) {
        System.out.println("requestKeywords:Handling");
        Map<String, Object> res = new HashMap<>();
        int page;
        try {
            page = Integer.parseInt(keywordPage);
        }catch (Exception e){
            res.put("status","BAD_REQUEST");
            res.put("message","Invalid Page Number");
            return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
        }
        if(page < 0){
            res.put("status","BAD_REQUEST");
            res.put("message","Invalid Page Number");
            return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
        }
        System.out.println("request for keyword page:" + page);
        try {
            res = jdbmService.getKeywords(page);
            if(res.get("keywords") == null) {
                System.out.println("requestKeywords:Handled Successfully");
                res.put("status","BAD_REQUEST");
                res.put("message","Page Number Too Large");
                return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
            }
            System.out.println("requestKeywords:Handled Successfully");
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception e) {
            res.put("error", e.getLocalizedMessage());
            return new ResponseEntity<>(res, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * api end-point to handle word search request
     * @param query the raw word query in String format
     * @param mode the mode of searching: either keyword or phrase search, default by keyword
     * @param section the target section of search: title, body or both, default by both
     * @param raw indicate if the query is raw (need stop-stem) or not
     * @return response entity containing all found pages in json format
     */
    @GetMapping("/search")
    public ResponseEntity<?> requestPages(@RequestParam(name = "query") String query,
                                          @RequestParam(name = "mode", defaultValue = "keyword", required = false ) String mode,
                                          @RequestParam(name = "section", defaultValue = "both", required = false ) String section,
                                          @RequestParam(name = "raw", defaultValue = "true", required = false) String raw) {
        System.out.println();
        System.out.println("requestSearch:Handling");

        HashMap<String, Object> res = new HashMap<>();

        //handle bad request cases caused by invalid parameters
        if(query.isEmpty() || query.isBlank()) {
            res.put("status","BAD_REQUEST");
            res.put("message","Invalid Query");
            System.out.println("requestSearch:Handled Successfully");
            return new ResponseEntity<>(res,HttpStatus.BAD_REQUEST);
        }

        if(!section.equals("both")  && !section.equals("title") && !section.equals("body")){
            res.put("status","BAD_REQUEST");
            res.put("message","Invalid Search Section");
            System.out.println("requestSearch:Handled Successfully");
            return new ResponseEntity<>(res,HttpStatus.BAD_REQUEST);
        }

        System.out.println("query:" + query);

        //start the searching process, record the search action results and store the retrieved pages in response entity
        //call specific service function according to the search mode parameter
        long startTime = System.currentTimeMillis();
        Map<Integer, Double> queryVec;
        ArrayList<PageResponse> keywordSearchResult;
        Map<String, ArrayList<PageResponse>> phraseSearchResult;
        Map<String, ArrayList<PageResponse>> mixedSearchResult;
        try {
            switch (mode) {
                case "keyword" -> {
                    System.out.println("mode: Keyword Search");
                    queryVec = searchEngineService.getQueryVector(query, raw);
                    if (queryVec == null) {
                        res.put("status", "NOT_FOUND");
                        res.put("message", "Page Not Found");
                        System.out.println("requestSearch:Handled Successfully");
                        return new ResponseEntity<>(res, HttpStatus.NOT_FOUND);
                    }
                    keywordSearchResult = searchEngineService.keywordSearch(queryVec, section);
                    long stopTime = System.currentTimeMillis();
                    long duration = stopTime - startTime;
                    res.put("executionTime", String.valueOf(duration) + " ms");
                    if(keywordSearchResult.isEmpty()){
                        res.put("status","NOT_FOUND");
                        res.put("message","Page Not Found");
                        System.out.println("requestSearch:Handled Successfully");
                        return new ResponseEntity<>(res, HttpStatus.NOT_FOUND);
                    }else {
                        res.put("retrievedNumber", keywordSearchResult.size());
                        res.put("retrievedPageList", keywordSearchResult);
                        System.out.println("requestSearch:Handled Successfully");
                        return new ResponseEntity<>(res, HttpStatus.FOUND);
                    }
                }
                case "phrase" -> {
                    System.out.println("mode: Phrase Search");
                    phraseSearchResult = searchEngineService.phraseSearch(query, section, raw);
                    long stopTime = System.currentTimeMillis();
                    long duration = stopTime - startTime;
                    res.put("executionTime", String.valueOf(duration) + " ms");
                    if(phraseSearchResult.isEmpty()){
                        res.put("status","NOT_FOUND");
                        res.put("message","Page Not Found");
                        System.out.println("requestSearch:Handled Successfully");
                        return new ResponseEntity<>(res, HttpStatus.NOT_FOUND);
                    }else {
                        res.put("retrievedNumber", phraseSearchResult.getOrDefault("exactMatch",new ArrayList<>()).size()
                                + phraseSearchResult.getOrDefault("nonExactMatch", new ArrayList<>()).size());
                        res.put("retrievedPageMap", phraseSearchResult);
                        System.out.println("requestSearch:Handled Successfully");
                        return new ResponseEntity<>(res, HttpStatus.FOUND);
                    }
                }

                case "mix" -> {
                    System.out.println("mode: Mixed Search");
                    mixedSearchResult = searchEngineService.mixedSearch(query, section, raw);
                    long stopTime = System.currentTimeMillis();
                    long duration = stopTime - startTime;
                    res.put("executionTime", String.valueOf(duration) + " ms");
                    if (mixedSearchResult.isEmpty()) {
                        res.put("status", "NOT_FOUND");
                        res.put("message", "Page Not Found");
                        System.out.println("requestSearch:Handled Successfully");
                        return new ResponseEntity<>(res, HttpStatus.NOT_FOUND);
                    } else {
                        res.put("retrievedNumber", mixedSearchResult.getOrDefault("exactMatch", new ArrayList<>()).size()
                                + mixedSearchResult.getOrDefault("nonExactMatch", new ArrayList<>()).size());
                        res.put("retrievedPageMap", mixedSearchResult);
                        System.out.println("requestSearch:Handled Successfully");
                        return new ResponseEntity<>(res, HttpStatus.FOUND);
                    }
                }
                default -> {
                    res.put("status", "BAD_REQUEST");
                    res.put("message", "Invalid Search Mode");
                    System.out.println("requestSearch:Handled Successfully");
                    return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
                }
            }
        }catch (Exception e) {
            return new ResponseEntity<>(e.getLocalizedMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
