package com.comp4321.searchenginebackend.utils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.comp4321.searchenginebackend.models.CrawledPage;
import com.comp4321.searchenginebackend.models.Page;
import com.comp4321.searchenginebackend.services.JdbmService;
import org.htmlparser.util.ParserException;
import org.htmlparser.beans.LinkBean;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class Crawler {
    private String rootURL;
    private ArrayList<String> visitedURL;
    public ArrayList<String> unVisitedURL;
    private Integer retrievedNum;
    private  ArrayList<CrawledPage> retrievedPages;
    private Integer maxPageNum;

    /**
     * constructor of the crawler, initialize of defined variables
     * @param _url root url to be crawled
     * @param _maxPageNum maximum number of webpages to be crawled
     */
    public Crawler(String _url, Integer _maxPageNum) {
        this.rootURL = _url;
        this.visitedURL = new ArrayList<>();
        this.unVisitedURL = new ArrayList<>();
        this.retrievedNum = 0;
        this.retrievedPages = new ArrayList<>();
        this.maxPageNum = _maxPageNum;

        unVisitedURL.add(this.rootURL);
    }

    public String getRootURL(){
        return this.rootURL;
    }

    public ArrayList<CrawledPage> getRetrievedPages(){
        return this.retrievedPages;
    }

    public Integer getRetrievedNum(){
        return this.retrievedNum;
    }

    /**
     * helper function to crawl all the words inside a document object
     * @param document document object to be crawled, retrieved from a http request
     * @return a list of String words extracted
     * @throws IOException
     */
    private List<String> extractWords(Document document) throws IOException {

        StopStem stopStem = new StopStem();

        //get the body content String
        String bodyString = document.body().text();

        //filter out non-alphabet and non-numeric words
        //followed by stop-stem processing
        List<String> l_word = new ArrayList<>(Arrays
                .stream(bodyString.split("\\W+"))
                .filter(word -> !stopStem.isStopWord(word))
                .map(word -> stopStem.stem(word))
                .toList());

        while(l_word.contains("")){
            l_word.remove("");
        }

        return l_word;
    }

    /**
     * helper function to crawl all the words of a title String
     * @param title title String to be crawled
     * @return a list of String words extracted
     * @throws IOException
     */
    private List<String> extractTitleWords(String title) throws IOException {
        StopStem stopStem = new StopStem();

        //filter out non-alphabet and non-numeric words
        //followed by stop-stem processing
        List<String> l_titleWord = new ArrayList<>(Arrays
                .stream(title.split("\\W+"))
                .filter(word -> !stopStem.isStopWord(word))
                .map(word -> stopStem.stem(word))
                .toList());

        while(l_titleWord.contains("")){
            l_titleWord.remove("");
        }

        return l_titleWord;
    }

    /**
     * helper function to crawl all the links in a webpage
     * @param url the webpage url to be crawled
     * @return a vector of extracted links/urls
     */
    private ArrayList<String> extractLinks(String url) throws ParserException {
        ArrayList<String> l_link = new ArrayList<String>();
        LinkBean lb = new LinkBean();
        lb.setURL(url);
        l_link.addAll(Arrays.stream(lb.getLinks()).map(link->String.valueOf(link)).toList());
        return l_link;
    }

    /**
     * helper function to get http connection for a given url
     * @param url website link to crawl
     * @return a response object of the http request
     * @throws IOException
     */
    private Response getUrlResponse(String url) throws IOException {
        Connection conn = Jsoup.connect(url);
        Response response = conn.execute();
        return response;
    }

    /**
     * helper function to generate the position lists for a vector of keywords
     * @param words a vector of keywords to be parsed
     * @return a map of <keyword String, positionList of Integer>
     */
    private HashMap<String, ArrayList<Integer>> parseWordPos(List<String> words){
        HashMap<String, ArrayList<Integer>>  wordPositions = new HashMap<>();
        for (int i = 0; i < words.size(); i++) {
            //get the positionList for current keyword, create new positionList if not exists
            ArrayList<Integer> pos = wordPositions.getOrDefault(words.get(i), new ArrayList<>());
            //add i - the position in words list
            pos.add(i);
            //update the positionList in wordPositions
            wordPositions.put(words.get(i), pos);
        }
        return wordPositions;
    }

    /**
     * main function of the crawler to crawl urls
     * and store the data into defined variables
     */
    public void crawl(JdbmService jdbmService) throws IOException, ParserException, ParseException {

        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        System.out.println("Start Crawling root url: " + this.rootURL + " ...");
        while (!unVisitedURL.isEmpty()) {
            //fetching the urls using BFS
            String curUrl = unVisitedURL.remove(0);
            if (visitedURL.contains(curUrl)) {
                //handle cyclic links gracefully
                continue;
            }

            if (retrievedNum >= maxPageNum) {
                //stop at boundary
                break;
            }

            //get http response for current url and fetch the document object
            Response res = this.getUrlResponse(curUrl).bufferUp();
            Document doc = res.parse();

            //extract last modified date from response
            String lastModified = res.header("Last-Modified");
            if (lastModified == null || lastModified.equals("")) {
                lastModified = res.header("Date");
            }

            Integer pageId = (Integer) jdbmService.getUrlPageIdIndex().get(curUrl);
            if(pageId != null){
                Page page = (Page) jdbmService.getForwardIndex().get(pageId);
                Date dbDate = page.getLastModification();
                Date crawlDate = format.parse(lastModified);

                //no need to crawl the url again if page already in db and up-to-date
                if(dbDate.after(crawlDate) || dbDate.equals(crawlDate)){
                    //add all child link for BFS crawling
                    unVisitedURL.addAll(page.getChildUrls());

                    //mark it as visited and increase counting
                    visitedURL.add(curUrl);
                    retrievedNum++;
                    continue;
                }
            }

            //get and extract document title to word list
            String title = res.parse().title();
            List<String> titleWords = this.extractTitleWords(title);

            //extract words in document to a list
            List<String> words = this.extractWords(doc);

            //extract urls in document to a list
            ArrayList<String> urls = this.extractLinks(curUrl);

            //separate the urls into child list and parent list
            ArrayList<String> parentUrls = new ArrayList<>();
            ArrayList<String> childUrls = new ArrayList<>();
            urls.forEach(url -> {
                if(visitedURL.contains(url)){
                    if(!parentUrls.contains(url)) parentUrls.add(url);
                }else {
                    if(!childUrls.contains(url)) childUrls.add(url);
                }
            });

            //prepare childUrls for BFS recursive crawling
            unVisitedURL.addAll(childUrls);

            //extract page size from response
            String docSize = res.header("Content-Length");
            if(docSize == null || docSize.equals("")){
                docSize = String.valueOf(res.bodyAsBytes().length);
            }

            //construct word frequency table for extracted body words
            HashMap<String, Integer> freqTable = new HashMap<>();
            words.forEach(word -> freqTable.put(word, freqTable.getOrDefault(word, 0) + 1));

            //construct word frequency table for extracted title words
            HashMap<String, Integer> titleFreqTable = new HashMap<>();
            titleWords.forEach(titleWord -> titleFreqTable.put(titleWord, titleFreqTable.getOrDefault(titleWord, 0) + 1));

            HashMap<String, ArrayList<Integer>> wordPositions = this.parseWordPos(words);
            HashMap<String, ArrayList<Integer>> titlePositions = this.parseWordPos(titleWords);

            //create the page object with crawled data
            CrawledPage page = new CrawledPage();
            page.setTitle(title);
            page.setUrl(curUrl);
            page.setSize(docSize);
            page.setFreqTable(freqTable);
            page.setTitleFreqTable(titleFreqTable);
            page.setWordPositions(wordPositions);
            page.setTitlePositions(titlePositions);
            page.setParentUrls(parentUrls);
            page.setChildUrls(childUrls);
            page.setLastModification(format.parse(lastModified));

            // store the page in crawler variables
            retrievedPages.add(page);

            //crawling for current url done
            //mark it as visited and increase counting
            visitedURL.add(curUrl);
            retrievedNum++;
        }
        System.out.println("Crawling Done");
    }
}
