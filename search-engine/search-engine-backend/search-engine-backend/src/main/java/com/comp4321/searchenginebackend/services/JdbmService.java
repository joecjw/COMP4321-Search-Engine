package com.comp4321.searchenginebackend.services;

import com.comp4321.searchenginebackend.models.CrawledPage;
import com.comp4321.searchenginebackend.models.Page;
import com.comp4321.searchenginebackend.utils.VectorSpaceModel;
import jakarta.annotation.PreDestroy;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class JdbmService {
    private RecordManager recman;
    @Getter
    private HTree forwardIndex;
    @Getter
    private HTree bodyInvertedIndex;
    @Getter
    private HTree titleInvertedIndex;
    @Getter
    private HTree urlPageIdIndex;
    @Getter
    private HTree keywordLookupIndex;
    @Getter
    private HTree keywordIdLookupIndex;
    private FastIterator keyIter;
    private FastIterator valueIter;
    private Integer initPageId;
    private Integer initKeywordId;

    /**
     * constructor to initialize the JDBM database
     * load indices if exists, else create new indices
     * @throws IOException exception when r/w the database file
     */
    public JdbmService() throws IOException {
        // create or open index record manager
        Properties props = new Properties();
        recman = RecordManagerFactory.createRecordManager("searchIndex", props);

        // create or load urlPageIdIndex
        long uRecid = recman.getNamedObject("urlPageIdIndex");
        if (uRecid != 0) {
            System.out.println("Reloading existing urlPageIdIndex...");
            urlPageIdIndex = HTree.load( recman, uRecid );
            //set new pageId sequence start from the max. of loaded table +1
            FastIterator iter = urlPageIdIndex.values();
            int maxId = -1;
            Integer pageId = (Integer) iter.next();
            while (pageId != null){
                if(pageId > maxId){
                    maxId = pageId;
                }
                pageId = (Integer) iter.next();
            }
            initPageId = maxId + 1;
        } else {
            System.out.println("Creating new urlPageIdIndex...");
            urlPageIdIndex = HTree.createInstance(recman);
            recman.setNamedObject("urlPageIdIndex", urlPageIdIndex.getRecid());
            recman.commit();
            //set new pageId sequence start from 0
            initPageId = 0;
        }

        // create or load keywordIdLookupIndex
        long kIRecid = recman.getNamedObject("keywordIdLookupIndex");
        if (kIRecid != 0) {
            System.out.println("Reloading existing keywordIdLookupIndex...");
            keywordIdLookupIndex = HTree.load( recman, kIRecid );
            //set new keyword sequence start from the max. of loaded table +1
            FastIterator iter = keywordIdLookupIndex.values();
            int maxId = -1;
            Integer keywordId = (Integer) iter.next();
            while (keywordId != null){
                if(keywordId > maxId){
                    maxId = keywordId;
                }
                keywordId = (Integer) iter.next();
            }
            initKeywordId = maxId + 1;
        } else {
            System.out.println("Creating new keywordIdLookupIndex...");
            keywordIdLookupIndex = HTree.createInstance(recman);
            recman.setNamedObject("keywordIdLookupIndex", keywordIdLookupIndex.getRecid());
            recman.commit();
            //set new keywordId sequence start from 0
            initKeywordId = 0;
        }

        // create or load keywordLookupIndex
        long kRecid = recman.getNamedObject("keywordLookupIndex");
        if (kRecid != 0) {
            System.out.println("Reloading existing keywordLookupIndex...");
            keywordLookupIndex = HTree.load( recman, kRecid );
        } else {
            System.out.println("Creating new keywordLookupIndex...");
            keywordLookupIndex = HTree.createInstance(recman);
            recman.setNamedObject("keywordLookupIndex", keywordLookupIndex.getRecid());
            recman.commit();
            //set new keywordId sequence start from 0
            initKeywordId = 0;
        }

        // create or load forwardIndex
        long fRecid = recman.getNamedObject("forwardIndex");
        if (fRecid != 0) {
            System.out.println("Reloading existing forwardIndex...");
            forwardIndex = HTree.load(recman, fRecid);
        } else {
            System.out.println("Creating new forwardIndex...");
            forwardIndex = HTree.createInstance(recman);
            recman.setNamedObject("forwardIndex", forwardIndex.getRecid());
            recman.commit();
        }

        // create or load titleInvertedIndex
        long iTRecid = recman.getNamedObject("titleInvertedIndex");
        if (iTRecid != 0) {
            System.out.println("Reloading existing titleInvertedIndex...");
            titleInvertedIndex = HTree.load(recman, iTRecid);
        } else {
            System.out.println("Creating new titleInvertedIndex...");
            titleInvertedIndex = HTree.createInstance(recman );
            recman.setNamedObject("titleInvertedIndex", titleInvertedIndex.getRecid());
            recman.commit();
        }

        // create or load bodyInvertedIndex
        long iBRecid = recman.getNamedObject("bodyInvertedIndex");
        if (iBRecid != 0) {
            System.out.println("Reloading existing bodyInvertedIndex...");
            bodyInvertedIndex = HTree.load(recman, iBRecid);
        } else {
            System.out.println("Creating new bodyInvertedIndex...");
            bodyInvertedIndex = HTree.createInstance(recman );
            recman.setNamedObject("bodyInvertedIndex", bodyInvertedIndex.getRecid());
            recman.commit();
        }
    }

    /**
     * method to close the record manager on application shutdown
     * so that the data stored temporarily on .lg file will be written to .db file
     * @throws IOException exception when writing the file
     */
    @PreDestroy
    public void destroy() throws IOException {
        System.out.println("closing record manager on shutdown");
        recman.commit();
        recman.close();
    }

    /**
     * function to index all the crawled pages to
     * 1.forward index 2.url<->pageId lookup index 3.keyword lookup index 4.inverted index(title and body)
     * @param crawlerService object that contains the crawler which holds all retrieved pages
     * @return a map of indexing results
     * @throws IOException exception when r/w the database file
     */
    public HashMap<String, Object> indexCrawledPages(CrawlerService crawlerService, VectorSpaceModel vsm) throws IOException {
        System.out.println("Start Indexing ForwardIndex Table, Url PageId Table, Keyword Tables...");
        long startTime = System.currentTimeMillis();

        //get retrieved pages for indexing
        ArrayList<CrawledPage> retrievedPages = crawlerService.getCrawler().getRetrievedPages();

        //define map <keywordId, pageList<pageID, wordPosTable>>
        //to store info for constructing body and title inverted index
        HashMap<Integer, HashMap<Integer, List<Integer>>> bodyInvertedIndices = new HashMap<>();
        HashMap<Integer, HashMap<Integer, List<Integer>>> titleInvertedIndices = new HashMap<>();

        //store changed page ids for later vector space updating
        Set<Integer> changedPageIds = new HashSet<>();

        int fIdxInserted = 0;
        int fIdxModified = 0;
        for(CrawledPage crawledPage : retrievedPages) {
            Integer pageID = (Integer) urlPageIdIndex.get(crawledPage.getUrl());
            if (pageID == null) {
                // directly insert the page into db if the index is not exist
                // before insertion, update the keyword lookup index
                // and replace the tables in the page object with new keyword ids

                //define page object to store in db
                Page page = new Page();

                // find unique keywords set for current crawled page
                Set<String> keywords = new HashSet<>();
                keywords.addAll(crawledPage.getTitleFreqTable().keySet());
                keywords.addAll(crawledPage.getFreqTable().keySet());

                // update keyword and keywordId lookup index here
                keywords.forEach((keyword) -> {
                    try {
                        Integer keywordId = (Integer) keywordIdLookupIndex.get(keyword);
                        if (keywordId == null) {
                            keywordIdLookupIndex.put(keyword, initKeywordId);
                            keywordLookupIndex.put(initKeywordId, keyword);
                            initKeywordId += 1;
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

                //replace the tables in the crawled page with keyword ids here
                HashMap<Integer, Integer> freqTable = new HashMap<>();
                for (Map.Entry<String, Integer> entry : crawledPage.getFreqTable().entrySet()) {
                    freqTable.put((Integer) keywordIdLookupIndex.get(entry.getKey()), entry.getValue());
                }

                HashMap<Integer, Integer> titleFreqTable = new HashMap<>();
                for (Map.Entry<String, Integer> entry : crawledPage.getTitleFreqTable().entrySet()) {
                    titleFreqTable.put((Integer) keywordIdLookupIndex.get(entry.getKey()), entry.getValue());
                }

                HashMap<Integer, ArrayList<Integer>> wordPositions = new HashMap<>();
                for (Map.Entry<String, ArrayList<Integer>> entry : crawledPage.getWordPositions().entrySet()) {
                    wordPositions.put((Integer) keywordIdLookupIndex.get(entry.getKey()), entry.getValue());
                }

                HashMap<Integer, ArrayList<Integer>> titlePositions = new HashMap<>();
                for (Map.Entry<String, ArrayList<Integer>> entry : crawledPage.getTitlePositions().entrySet()) {
                    titlePositions.put((Integer) keywordIdLookupIndex.get(entry.getKey()), entry.getValue());
                }

                //set all fields for page object
                page.setTitle(crawledPage.getTitle());
                page.setUrl(crawledPage.getUrl());
                page.setSize(crawledPage.getSize());
                page.setFreqTable(freqTable);
                page.setTitleFreqTable(titleFreqTable);
                page.setWordPositions(wordPositions);
                page.setTitlePositions(titlePositions);
                page.setParentUrls(crawledPage.getParentUrls());
                page.setChildUrls(crawledPage.getChildUrls());
                page.setLastModification(crawledPage.getLastModification());

                //insert the page into forward index and also update url pageId index
                forwardIndex.put(initPageId, page);
                urlPageIdIndex.put(page.getUrl(), initPageId);
                changedPageIds.add(initPageId);
                initPageId += 1;
                fIdxInserted++;


                //update body and title InvertedIndices map for later construction
                HashMap<Integer, ArrayList<Integer>> wordPos = page.getWordPositions();
                for (HashMap.Entry<Integer, ArrayList<Integer>> entry : wordPos.entrySet()) {
                    Integer keywordId = entry.getKey();
                    HashMap<Integer, List<Integer>> pageList = bodyInvertedIndices.getOrDefault(keywordId, new HashMap<>());
                    pageList.put((Integer) urlPageIdIndex.get(page.getUrl()), entry.getValue());
                    bodyInvertedIndices.put(keywordId, pageList);
                }
                HashMap<Integer, ArrayList<Integer>> titleWordPos = page.getTitlePositions();
                for (HashMap.Entry<Integer, ArrayList<Integer>> entry : titleWordPos.entrySet()) {
                    Integer keywordId = entry.getKey();
                    HashMap<Integer, List<Integer>> pageList = titleInvertedIndices.getOrDefault(keywordId, new HashMap<>());
                    pageList.put((Integer) urlPageIdIndex.get(page.getUrl()), entry.getValue());
                    titleInvertedIndices.put(keywordId, pageList);
                }
            } else {
                Page dbPage = (Page) forwardIndex.get(pageID);
                if (dbPage != null) {
                    Date dbDate = dbPage.getLastModification();
                    Date crawlDate = crawledPage.getLastModification();
                    if (dbDate.before(crawlDate) && !dbDate.equals(crawlDate)) {
                        //check date, only update if the indexed one is old
                        System.out.println("Replacing Modified Page with Id:" + pageID);

                        try{
                            //find unique keywords set for db page
                            Set<Integer> keywordIds = new HashSet<>();
                            keywordIds.addAll(dbPage.getTitleFreqTable().keySet());
                            keywordIds.addAll(dbPage.getFreqTable().keySet());

                            //before remove the db page, update keyword index and inverted index
                            keywordIds.forEach(keywordId -> {
                                try {
                                    HashMap<Integer, List<Integer>> titlePageList = (HashMap<Integer, List<Integer>>) titleInvertedIndex.get(keywordId);
                                    HashMap<Integer, List<Integer>> bodyPageList = (HashMap<Integer, List<Integer>>) bodyInvertedIndex.get(keywordId);

                                    if(titlePageList != null){
                                        if(titlePageList.containsKey(pageID)){
                                            if(titlePageList.size() == 1){
                                                titleInvertedIndex.remove(keywordId);
                                            }else {
                                                ((HashMap<?, ?>) titleInvertedIndex.get(keywordId)).remove(pageID);
                                            }
                                        }
                                    }

                                    if(bodyPageList != null){
                                        if(bodyPageList.containsKey(pageID)){
                                            if(bodyPageList.size() == 1){
                                                bodyInvertedIndex.remove(keywordId);
                                            }else {
                                                ((HashMap<?, ?>) bodyInvertedIndex.get(keywordId)).remove(pageID);
                                            }
                                        }
                                    }

                                    if(titleInvertedIndex.get(keywordId) == null && bodyInvertedIndex.get(keywordId) == null){
                                        keywordIdLookupIndex.remove(keywordLookupIndex.get(keywordId));
                                        keywordLookupIndex.remove(keywordId);
                                    }
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            //before remove the db page, check if other pages in db need to be updated
                            Set<String> dbChildUrls = new HashSet<>(dbPage.getChildUrls());
                            Set<String> crawledChildUrls = new HashSet<>(crawledPage.getChildUrls());

                            crawledChildUrls.retainAll(dbChildUrls);
                            dbChildUrls.removeAll(crawledChildUrls);

                            //get the set of page ids to check
                            Set<Integer> pageIdsToCheck = dbChildUrls.
                                    stream()
                                    .map(url -> {
                                        try {
                                            return (Integer) urlPageIdIndex.get(url);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .collect(Collectors.toSet());

                            pageIdsToCheck.forEach(pageId -> {
                                try {
                                    Page page = (Page) forwardIndex.get(pageId);
                                    if(page.getParentUrls().contains(dbPage.getUrl()) && page.getParentUrls().size() == 1){
                                        //page is unreachable after update, delete it
                                        System.out.println("Removing Unreachable Page with Id:"+pageId);
                                        urlPageIdIndex.remove(page.getUrl());
                                        forwardIndex.remove(pageId);
                                        vsm.removePageVector(pageID);
                                    } else {
                                        // page is still reachable after update, update parent links
                                        page.getParentUrls().remove(dbPage.getUrl());
                                    }

                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                        }catch (Exception e){
                            System.out.println("Replacing Modified Page Directly");
                        }

                        //remove the db page
                        forwardIndex.remove(pageID);

                        //replace with newly modified page
                        //define page object to store in db
                        Page page = new Page();

                        // find unique keywords set for current crawled page
                        Set<String> keywords = new HashSet<>();
                        keywords.addAll(crawledPage.getTitleFreqTable().keySet());
                        keywords.addAll(crawledPage.getFreqTable().keySet());

                        // update keyword lookup index here
                        keywords.forEach((keyword) -> {
                            try {
                                Integer keywordId = (Integer) keywordIdLookupIndex.get(keyword);
                                if(keywordId == null){
                                    keywordIdLookupIndex.put(keyword, initKeywordId);
                                    keywordLookupIndex.put(initKeywordId, keyword);
                                    initKeywordId += 1;
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                        //replace the tables in the crawled page with keyword ids here
                        HashMap<Integer,Integer> freqTable = new HashMap<>();
                        for(Map.Entry<String, Integer> entry : crawledPage.getFreqTable().entrySet()){
                            freqTable.put((Integer) keywordIdLookupIndex.get(entry.getKey()), entry.getValue());
                        }

                        HashMap<Integer,Integer> titleFreqTable = new HashMap<>();
                        for(Map.Entry<String, Integer> entry : crawledPage.getTitleFreqTable().entrySet()){
                            titleFreqTable.put((Integer) keywordIdLookupIndex.get(entry.getKey()), entry.getValue());
                        }

                        HashMap<Integer, ArrayList<Integer>> wordPositions = new HashMap<>();
                        for(Map.Entry<String, ArrayList<Integer>> entry : crawledPage.getWordPositions().entrySet()){
                            wordPositions.put((Integer) keywordIdLookupIndex.get(entry.getKey()), entry.getValue());
                        }

                        HashMap<Integer, ArrayList<Integer>> titlePositions = new HashMap<>();
                        for(Map.Entry<String, ArrayList<Integer>> entry : crawledPage.getTitlePositions().entrySet()){
                            titlePositions.put((Integer) keywordIdLookupIndex.get(entry.getKey()), entry.getValue());
                        }

                        //set all fields for page object
                        page.setTitle(crawledPage.getTitle());
                        page.setUrl(crawledPage.getUrl());
                        page.setSize(crawledPage.getSize());
                        page.setFreqTable(freqTable);
                        page.setTitleFreqTable(titleFreqTable);
                        page.setWordPositions(wordPositions);
                        page.setTitlePositions(titlePositions);
                        page.setParentUrls(crawledPage.getParentUrls());
                        page.setChildUrls(crawledPage.getChildUrls());
                        page.setLastModification(crawledPage.getLastModification());

                        //insert the updated page to forward index
                        forwardIndex.put(pageID, crawledPage);
                        changedPageIds.add(pageID);
                        fIdxModified++;

                        //update body and title InvertedIndices map for later construction
                        HashMap<Integer, ArrayList<Integer>> wordPos = page.getWordPositions();
                        for (HashMap.Entry<Integer, ArrayList<Integer>> entry : wordPos.entrySet()) {
                            Integer keywordId = entry.getKey();
                            HashMap<Integer, List<Integer>> pageList = bodyInvertedIndices.getOrDefault(keywordId, new HashMap<>());
                            pageList.put((Integer) urlPageIdIndex.get(page.getUrl()), entry.getValue());
                            bodyInvertedIndices.put(keywordId, pageList);
                        }
                        HashMap<Integer, ArrayList<Integer>> titleWordPos = page.getTitlePositions();
                        for (HashMap.Entry<Integer, ArrayList<Integer>> entry : titleWordPos.entrySet()) {
                            Integer keywordId = entry.getKey();
                            HashMap<Integer, List<Integer>> pageList = titleInvertedIndices.getOrDefault(keywordId, new HashMap<>());
                            pageList.put((Integer) urlPageIdIndex.get(page.getUrl()), entry.getValue());
                            titleInvertedIndices.put(keywordId, pageList);
                        }
                    }
                }
            }
        }
        System.out.println("Indexing ForwardIndex Table, Url PageId Table, Keyword Tables, Done");
        recman.commit();

        System.out.println("Start Indexing BodyInvertedIndex Table...");
        int bIIdxInserted = 0;
        int bIIdxModified = 0;
        for (HashMap.Entry<Integer, HashMap<Integer, List<Integer>>> entry : bodyInvertedIndices.entrySet()) {

            HashMap<Integer, List<Integer>> pageList = (HashMap<Integer, List<Integer>>) bodyInvertedIndex.get(entry.getKey());
            if(pageList != null){
                //modify the existing index
                //merge with existing map
                for(Map.Entry<Integer, List<Integer>> idWordPosEntry: entry.getValue().entrySet()){
                    List<Integer>  dbWordPos = pageList.get(idWordPosEntry.getKey());
                    if(dbWordPos == null) {
                        pageList.put(idWordPosEntry.getKey(), idWordPosEntry.getValue());
                    }else {
                        pageList.remove(idWordPosEntry.getKey());
                        pageList.put(idWordPosEntry.getKey(), idWordPosEntry.getValue());
                    }
                }
                bodyInvertedIndex.remove(entry.getKey());
                bodyInvertedIndex.put(entry.getKey(), pageList);
                bIIdxModified++;
            }else {
                //insert index directly
                bodyInvertedIndex.put(entry.getKey(), entry.getValue());
                bIIdxInserted++;
            }
        }
        System.out.println("Indexing BodyInvertedIndex Table Done");
        recman.commit();

        System.out.println("Start Indexing TitleInvertedIndex Table...");
        int tIIdxInserted = 0;
        int tIIdxModified = 0;
        for (HashMap.Entry<Integer, HashMap<Integer, List<Integer>>> entry : titleInvertedIndices.entrySet()) {

            HashMap<Integer, List<Integer>> pageList = (HashMap<Integer, List<Integer>>) titleInvertedIndex.get(entry.getKey());
            if(pageList != null){
                //modify the existing index
                //merge with existing pageList map
                for(Map.Entry<Integer, List<Integer>> idWordPosEntry: entry.getValue().entrySet()){
                    List<Integer>  dbWordPos = pageList.get(idWordPosEntry.getKey());
                    if(dbWordPos == null) {
                        pageList.put(idWordPosEntry.getKey(), idWordPosEntry.getValue());
                    }else {
                        pageList.remove(idWordPosEntry.getKey());
                        pageList.put(idWordPosEntry.getKey(), idWordPosEntry.getValue());
                    }
                }
                titleInvertedIndex.remove(entry.getKey());
                titleInvertedIndex.put(entry.getKey(), pageList);
                tIIdxModified++;
            }else {
                //insert index directly
                titleInvertedIndex.put(entry.getKey(), entry.getValue());
                tIIdxInserted++;
            }
        }
        System.out.println("Indexing TitleInvertedIndex Table Done");
        recman.commit();

        System.out.println("Updating Vector Space Model");
        //for each page id that has changed content, update its corresponding vector space entry
        changedPageIds.forEach(pageID -> {
            try {
                vsm.updatePageVector(pageID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("Updating Vector Space Model Done");

        long stopTime = System.currentTimeMillis();
        long msDuration = (stopTime - startTime) % 1000;
        long secDuration = TimeUnit.MILLISECONDS.toSeconds(stopTime - startTime);

        HashMap<String, Object> dbIndexingResult = new HashMap<>();
        HashMap<String, String> fIdxResult = new HashMap<>();
        HashMap<String, String> iIdxResult = new HashMap<>();

        dbIndexingResult.put("executionTime", String.valueOf(secDuration) + "." + String.valueOf(msDuration) + "sec");
        fIdxResult.put("inserted",  fIdxInserted + " entries");
        fIdxResult.put("modified",  fIdxModified + " entries");
        iIdxResult.put("Body: inserted",  bIIdxInserted + " entries");
        iIdxResult.put("Body: modified",  bIIdxModified + " entries");
        iIdxResult.put("Title: inserted",  tIIdxInserted + " entries");
        iIdxResult.put("Title: modified",  tIIdxModified + " entries");
        dbIndexingResult.put("ForwardIndex", fIdxResult);
        dbIndexingResult.put("InvertedIndex", iIdxResult);
        return dbIndexingResult;
    }

    /**
     * function to call the Jdbm database to write the crawled results to file
     * @return a map of write results
     * @throws IOException
     */
    public HashMap<String, Object> writeResultToFile() throws IOException {

        long startTime = System.currentTimeMillis();
        String result = writePagesToFile();
        long stopTime = System.currentTimeMillis();
        long msDuration = (stopTime - startTime) % 1000;
        long secDuration = TimeUnit.MILLISECONDS.toSeconds(stopTime - startTime);
        HashMap<String, Object> writeResult = new HashMap<>();
        writeResult.put("executionTime", String.valueOf(secDuration) + "." + String.valueOf(msDuration) + "sec");
        writeResult.put("message", result);
        return writeResult;
    }

    /**
     * function to create/rewrite the spider_result.txt
     * @return a String of write result
     * @throws IOException exception when r/w the database file
     */
    public String writePagesToFile() throws IOException {
        //check if there is at least one page to write
        valueIter = forwardIndex.values();
        Page initialPage = (Page) valueIter.next();

        if(initialPage != null){
            //check if file already exists, delete and rewrite if exists
            ClassPathResource res = new ClassPathResource("spider_result.txt");
            File newFile = new File(res.getPath());
            boolean result = Files.deleteIfExists(newFile.toPath());
            newFile.createNewFile();
            FileWriter fileWriter = new FileWriter(newFile);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            System.out.println("Start Writing Pages to spider_result.txt File...");
            //iterate through the forward indices' value and retrieve pages
            valueIter = forwardIndex.values();
            Page page = (Page) valueIter.next();
            while ( page != null ) {
                //combine the page's keyword frequency tables
                Page writePage = page;
                HashMap<Integer, Integer> combinedFreqTable  = new HashMap<>(Stream
                        .of(writePage.getTitleFreqTable().keySet(), writePage.getFreqTable().keySet())
                        .flatMap(Collection::stream)
                        .distinct()
                        .collect(Collectors.toMap(
                                k -> k,
                                v -> Integer.sum(
                                        writePage.getTitleFreqTable().getOrDefault(v, 0),
                                        writePage.getFreqTable().getOrDefault(v, 0)))
                        ));

//                //sort the frequency table by value in descending order
//                combinedFreqTable = combinedFreqTable
//                        .entrySet()
//                        .stream()
//                        .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
//                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
//                                (e1, e2) -> e1, LinkedHashMap::new));

                //construct and format the page content
                StringBuilder builder = new StringBuilder();
                builder.append(writePage.getTitle()).append('\n');
                builder.append(writePage.getUrl()).append('\n');
                builder.append(writePage.getLastModification()).append(", ").append(writePage.getSize()).append('\n');
                int counter = 0;
                for (HashMap.Entry<Integer, Integer> entry : combinedFreqTable.entrySet()) {
                    if(counter < 10){
                        builder.append(keywordLookupIndex.get(entry.getKey())).append(' ').append(entry.getValue()).append(';');
                        counter++;
                    }else {
                        break;
                    }
                }
                builder.append('\n');
                counter = 0;
                for(String childUrl : writePage.getChildUrls()){
                    if(counter < 10){
                        builder.append(childUrl).append("\n");
                        counter++;
                    }else {
                        break;
                    }
                }
                builder.append("-------------------------------------------------------------\n");

                //write page content to the output file
                printWriter.print(builder.toString());
                page = (Page) valueIter.next();
            }
            printWriter.close();
            System.out.println("Writing Pages to spider_result.txt Done");
            return "Result Write to spider_result.txt Successfully";
        }
        return "No Pages Retrieved";
    }

    /**
     * helper function to get all the pages with pageIds
     * @return a map contains pageID as key and corresponding page as value
     * @throws IOException exception when r/w the database file
     */
    public Map<Integer, Page> getAllPages() throws IOException {
        Map<Integer, Page> forwardDic = new HashMap<>();
        keyIter = forwardIndex.keys();
        valueIter = forwardIndex.values();
        Integer pageId = (Integer) keyIter.next();
        Page page = (Page) valueIter.next();
        while ( pageId != null ) {
            forwardDic.put(pageId, page);
            pageId = (Integer) keyIter.next();
            page = (Page) valueIter.next();
        }
        return forwardDic;
    }

    /**
     * helper function to get all the  page urls with pageIds
     * @return a map of url<->pageId entries
     * @throws IOException exception when r/w the database file
     */
    public Map<String, Integer> getAllUrlPageIdIndexes() throws IOException {
        Map<String, Integer> lookupDic = new HashMap<>();
        keyIter = urlPageIdIndex.keys();
        valueIter = urlPageIdIndex.values();
        String pageUrl = (String) keyIter.next();
        Integer pageId = (Integer) valueIter.next();
        while ( pageUrl != null ) {
            lookupDic.put(pageUrl, pageId);
            pageUrl = (String) keyIter.next();
            pageId = (Integer) valueIter.next();
        }
        return lookupDic;
    }

    /**
     * helper function to get all the keywords with their ids
     * @return a map of keyword<->keywordId entries
     * @throws IOException exception when r/w the database file
     */
    public Map<String, Integer> getAllKeywordIdLookupIndexes() throws IOException {
        Map<String, Integer> lookupDic = new HashMap<>();
        keyIter = keywordIdLookupIndex.keys();
        valueIter = keywordIdLookupIndex.values();
        String keyword = (String) keyIter.next();
        Integer keywordId = (Integer) valueIter.next();
        while ( keyword != null ) {
            lookupDic.put(keyword, keywordId);
            keyword = (String) keyIter.next();
            keywordId = (Integer) valueIter.next();
        }
        return lookupDic;
    }

    /**
     * helper function to get all the keyword ids with representing keyword string
     * @return a map of keywordId<->keyword entries
     * @throws IOException exception when r/w the database file
     */
    public Map<Integer, String> getAllKeywordLookupIndexes() throws IOException {
        Map<Integer, String> lookupDic = new HashMap<>();
        keyIter = keywordLookupIndex.keys();
        valueIter = keywordLookupIndex.values();
        Integer keywordId = (Integer) keyIter.next();
        String keyword = (String) valueIter.next();
        while ( keywordId != null ) {
            lookupDic.put(keywordId, keyword);
            keywordId = (Integer) keyIter.next();
            keyword = (String) valueIter.next();
        }
        return lookupDic;
    }

    /**
     * helper function to get all the body keywords with pageLists
     * @return a map of the keyword<->pageList entries
     * @throws IOException exception when r/w the database file
     */
    public Map<Integer, Map<Integer, List<Integer>>> getAllBodyInvertedIndexes() throws IOException {
        Map<Integer, Map<Integer, List<Integer>>> bInvertDic = new HashMap<>();
        keyIter = bodyInvertedIndex.keys();
        valueIter = bodyInvertedIndex.values();
        Integer keywordId = (Integer) keyIter.next();
        HashMap<Integer, List<Integer>> pageList = (HashMap<Integer, List<Integer>>) valueIter.next();
        while ( keywordId != null ) {
            bInvertDic.put(keywordId, pageList);
            keywordId = (Integer) keyIter.next();
            pageList = (HashMap<Integer, List<Integer>>) valueIter.next();
        }
        return bInvertDic;
    }

    /**
     * helper function to get all the title keywords with pageLists
     * @return a map of the keyword<->pageList entries
     * @throws IOException exception when r/w the database file
     */
    public Map<Integer, Map<Integer, List<Integer>>> getAllTitleInvertedIndexes() throws IOException {
        Map<Integer, Map<Integer, List<Integer>>> tInvertDic = new HashMap<>();
        keyIter = titleInvertedIndex.keys();
        valueIter = titleInvertedIndex.values();
        Integer keywordId = (Integer) keyIter.next();
        HashMap<Integer, List<Integer>> pageList = (HashMap<Integer, List<Integer>>) valueIter.next();
        while ( keywordId != null ) {
            tInvertDic.put(keywordId, pageList);
            keywordId = (Integer) keyIter.next();
            pageList = (HashMap<Integer, List<Integer>>) valueIter.next();
        }
        return tInvertDic;
    }

    /**
     * function to remove all data and initialize a new database
     * @throws IOException exception when r/w the database file
     */
    public void clearDB() throws IOException {
        recman.close();

        File db_file = new File("searchIndex.db");
        boolean isDbDeleted = db_file.delete();
        if(isDbDeleted) {
            System.out.println("db cleared successfully");
        } else {
            System.out.println("db file doesn't exist");
            return;
        }

        // create index record manager
        Properties props = new Properties();
        recman = RecordManagerFactory.createRecordManager("searchIndex", props);

        // create urlPageIdIndex
        System.out.println("Creating new urlPageIdIndex...");
        urlPageIdIndex = HTree.createInstance(recman);
        recman.setNamedObject("urlPageIdIndex", urlPageIdIndex.getRecid());
        recman.commit();
        //set new pageId sequence start from 0
        initPageId = 0;

        // create keywordIdLookupIndex
        System.out.println("Creating new keywordIdLookupIndex...");
        keywordIdLookupIndex = HTree.createInstance(recman);
        recman.setNamedObject("keywordIdLookupIndex", keywordIdLookupIndex.getRecid());
        recman.commit();
        //set new keywordId sequence start from 0
        initKeywordId = 0;


        // create keywordLookupIndex
        System.out.println("Creating new keywordLookupIndex...");
        keywordLookupIndex = HTree.createInstance(recman);
        recman.setNamedObject("keywordLookupIndex", keywordLookupIndex.getRecid());
        recman.commit();
        //set new keywordId sequence start from 0
        initKeywordId = 0;


        // create forwardIndex
        System.out.println("Creating new forwardIndex...");
        forwardIndex = HTree.createInstance(recman);
        recman.setNamedObject("forwardIndex", forwardIndex.getRecid());
        recman.commit();


        // create titleInvertedIndex
        System.out.println("Creating new titleInvertedIndex...");
        titleInvertedIndex = HTree.createInstance(recman );
        recman.setNamedObject("titleInvertedIndex", titleInvertedIndex.getRecid());
        recman.commit();


        // create bodyInvertedIndex
        System.out.println("Creating new bodyInvertedIndex...");
        bodyInvertedIndex = HTree.createInstance(recman );
        recman.setNamedObject("bodyInvertedIndex", bodyInvertedIndex.getRecid());
        recman.commit();

        System.out.println("new empty db initialized successfully");
    }

    /**
     * function to get all the stemmed keywords in database
     * @return a string set of keywords sorted in alphabetic order
     * @throws IOException
     */
    public Map<String, Object> getKeywords(int page) throws IOException {
        final int MAX_PAGES = 10;
        //construct set to store keywords in alphabetic order
        Set<String> keywords = new TreeSet<>();

        //iterate through title inverted index and store all keywords
        keyIter = keywordIdLookupIndex.keys();
        String keyword = (String) keyIter.next();
        while ( keyword != null ) {
            keywords.add(keyword);
            keyword = (String) keyIter.next();
        }

        Object[] resultKeywords = null;
        int totalKeywordNum = keywords.size();
        int pageCapacity = totalKeywordNum / MAX_PAGES;
        if(page * pageCapacity <= totalKeywordNum){
            if((page+1) * pageCapacity <= totalKeywordNum){
                resultKeywords = Arrays.stream(keywords.toArray(), page * pageCapacity, (page+1) * pageCapacity).toArray();
            }else {
                resultKeywords = Arrays.stream(keywords.toArray(), page * pageCapacity, totalKeywordNum).toArray();
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("keywords", resultKeywords);

        return result;
    }
}
