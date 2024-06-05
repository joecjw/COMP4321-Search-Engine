package com.comp4321.searchenginebackend.utils;

import com.comp4321.searchenginebackend.models.Page;
import com.comp4321.searchenginebackend.services.JdbmService;
import com.sun.tools.jconsole.JConsoleContext;
import jakarta.annotation.PostConstruct;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.sql.SQLSyntaxErrorException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class VectorSpaceModel {
    private final Double PAGE_BODY_WEIGHT = 1.0;

    //mechanism to favor matches in title by assigning a larger weight to title terms
    private final Double PAGE_TITLE_WEIGHT = 1.5;
    @Autowired
    private JdbmService jdbmService;
    private HTree forwardIndex;
    private HTree titleInvertedIndex;
    private HTree bodyInvertedIndex;
    private HTree keywordLookupIndex;
    private HTree keywordIdLookupIndex;

    private HashMap<Integer, HashMap<Integer,Double>> pageVSpace = new HashMap<>();

    public HTree getForwardIndex(){ return this.forwardIndex; }

    public HTree getKeywordLookupIndex(){ return this.keywordLookupIndex; }

    public HTree getKeywordIdLookupIndex(){ return this.keywordIdLookupIndex; }

    public HTree getTitleInvertedIndex() { return this.titleInvertedIndex; }

    public  HTree getBodyInvertedIndex() { return  this.bodyInvertedIndex; }

    public HashMap<Integer, HashMap<Integer,Double>> getPageVSpace(){
        return this.pageVSpace;
    }

    /**
     * function to construct the vector space of all pages after loading the database instances
     * by utilizing information of index in loaded database
     * @throws IOException
     */
    @PostConstruct
    public void init() throws IOException {
        System.out.println("Initializing VectorSpaceModel");

        forwardIndex = jdbmService.getForwardIndex();
        keywordLookupIndex = jdbmService.getKeywordLookupIndex();
        keywordIdLookupIndex = jdbmService.getKeywordIdLookupIndex();
        titleInvertedIndex = jdbmService.getTitleInvertedIndex();
        bodyInvertedIndex = jdbmService.getBodyInvertedIndex();

        //check if there is indexed entries in database
        FastIterator iter = forwardIndex.keys();
        Integer initPageId = (Integer) iter.next();
        if (initPageId == null) {
            //return if no indexed entries in database
            System.out.println("No Indexed Entries for Page");
            System.out.println("Initializing Empty VectorSpaceModel Successfully");
            return;
        }

        //iterate through the forward index and store the ids of pages in a list
        List<Integer> pageIDs = new ArrayList<>();
        FastIterator forwardIter = forwardIndex.keys();
        Integer pageId = (Integer) forwardIter.next();
        while (pageId != null) {
            pageIDs.add(pageId);
            pageId = (Integer) forwardIter.next();
        }

        //get the total page number through the list size
        Double totalPages = (double) pageIDs.size();

        //for each page(page id), construct its corresponding vector space
        pageIDs.forEach(pageID -> {
            //get the normalized term frequencies of the current page
            HashMap<Integer, Double> pageNormalizedTF = null;
            try {
                pageNormalizedTF = getNormalizedTF(pageID);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            //convert the normalized term frequencies into term weights calculated by tf*idf
            HashMap<Integer, Double> pageTermWeight = new HashMap<>(pageNormalizedTF.entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey(),
                            e -> {
                                try {
                                    return e.getValue() * getIDF(e.getKey(), totalPages);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            })
                    ));

            //put the constructed term weight vector into the vector space model using current page id as key
            this.pageVSpace.put(pageID, pageTermWeight);
        });
        System.out.println("Initializing VectorSpaceModel Successfully");
    }

    /**
     * function to update a target page term weight entry in vector space
     * @param pageID unique key to identify the target page to be updated
     * @throws IOException
     */
    public void updatePageVector(Integer pageID) throws IOException {
        //iterate through the forward index and get all page ids in a list
        FastIterator forwardIter = forwardIndex.keys();
        List<Integer> pageIDs = new ArrayList<>();
        Integer pageId = (Integer) forwardIter.next();
        while (pageId != null) {
            pageIDs.add(pageId);
            pageId = (Integer) forwardIter.next();
        }

        //get the total page number through the list size
        Double totalPages = (double) pageIDs.size();

        //get the normalized term frequencies of the target page
        HashMap<Integer, Double> pageNormalizedTF = null;
        try {
            pageNormalizedTF = getNormalizedTF(pageID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //convert the normalized term frequencies into term weights calculated by tf*idf
        HashMap<Integer, Double> pageTermWeight = new HashMap<>(pageNormalizedTF.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> {
                            try {
                                return e.getValue() * getIDF(e.getKey(), totalPages);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                ));

        //update the page term weight vector in vector space
        if(this.pageVSpace.containsKey(pageID)){
            this.pageVSpace.remove(pageID);
        }
        this.pageVSpace.put(pageID, pageTermWeight);
    }

    /**
     * function to remove a target page term weight entry in vector space
     * @param pageID unique key to identify the target page to be removed
     * @throws IOException
     */
    public void removePageVector(Integer pageID) throws IOException {
        if(this.pageVSpace.containsKey(pageID)){
            this.pageVSpace.remove(pageID);
        }
    }

    /**
     * function to get the query vector represented by a frequency table
     * @param queryFreqTable frequency table of a target query used to construct the vector
     * @return a normalized vector for a query
     */
    public HashMap<Integer, Double> processQuery(Map<String, Double> queryFreqTable){
        //calculate the maximum term frequency in a given keyword frequency table of a query
        Double queryMaxTF = queryFreqTable
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .get().getValue();

        AtomicInteger tempKey = new AtomicInteger();
        //construct the normalized vector for the query
        HashMap<Integer, Double> queryVector = new HashMap<>(queryFreqTable
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e->{
                            try {
                                Integer keywordId = (Integer) keywordIdLookupIndex.get(e.getKey());
                                if(keywordId != null){
                                    return keywordId;
                                }else {
                                    tempKey.addAndGet(-1);
                                    return tempKey.get();
                                }
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        },
                        e->e.getValue()/queryMaxTF)
                ));
        return queryVector;
    }

    /**
     * function to calculate the normalized term frequencies of a given page
     * @param pageID unique key to identify the target page
     * @return normalized term frequencies based on maximum term frequency
     * represented by a map of <keyword, frequency> entries
     * @throws IOException
     */
    public HashMap<Integer, Double> getNormalizedTF(Integer pageID) throws IOException {
        Page page = (Page) forwardIndex.get(pageID);
        if(page != null){
            //get the combined and weighted frequency table from the target page's title and body frequency tables
            HashMap<Integer, Double> weightedCombinedFreq = getWeightedCombinedFreqTable(page.getTitleFreqTable(),
                    page.getFreqTable());

            //calculate the maximum term frequencies in the combined frequency table
            Double maxTF = weightedCombinedFreq
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getValue();

            //normalize the term frequencies based on the maximum term frequency
            HashMap<Integer, Double> normalizedTF = new HashMap<>(weightedCombinedFreq
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e->e.getKey(),
                            e->e.getValue()/maxTF)
                    ));
            return normalizedTF;
        }
        return null;
    }

    /**
     * function to get a combined and weighted version of two input frequency tables
     * @param titleFreq title frequency table of a page to be weighted and combined
     * @param bodyFreq body frequency table of a page to be weighted and combined
     * @return combined and weighted frequency table
     */
    public HashMap<Integer, Double> getWeightedCombinedFreqTable(HashMap<Integer, Integer> titleFreq,
                                                        HashMap<Integer, Integer> bodyFreq){

        //calculate the weighted title frequency table
        HashMap<Integer, Double> titleFreqTable = new HashMap<>(titleFreq
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue() * PAGE_TITLE_WEIGHT)
                ));

        //calculate the weighted body frequency table
        HashMap<Integer, Double> bodyFreqTable = new HashMap<>(bodyFreq
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue() * PAGE_BODY_WEIGHT)
                ));

        //combine and return the two weighted frequency tables
        HashMap<Integer, Double> combinedFreqTable  = new HashMap<>(Stream
                .of(titleFreqTable.keySet(), bodyFreqTable.keySet())
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toMap(
                        k -> k,
                        v -> Double.sum(
                                titleFreqTable.getOrDefault(v, 0.),
                                bodyFreqTable.getOrDefault(v, 0.)))
                ));
        return  combinedFreqTable;
    }

    /**
     * function to calculate the idf of a keyword in all stored pages
     * @param keywordId keyword to find the frequency
     * @param totalPages number of stored pages in database
     * @return calculated idf of the target keyword
     * @throws IOException
     */
    public Double getIDF(Integer keywordId, Double totalPages) throws IOException {
        HashMap<Integer, List<Integer>> tPageList = (HashMap<Integer, List<Integer>>) titleInvertedIndex.get(keywordId);
        HashMap<Integer, List<Integer>> bPageList = (HashMap<Integer, List<Integer>>) bodyInvertedIndex.get(keywordId);

        //handle cases which the keyword dose not exists in pages
        if(tPageList == null && bPageList == null){
            return null;
        }

        //handle cases which the keyword exists in only one part of pages
        //calculate the idf
        if(tPageList == null){
            Double df = (double) bPageList.size();
            Double idf = Math.log(1+(totalPages/df)) / Math.log(2.00);
            return idf;
        }

        if(bPageList == null){
            Double df = (double) tPageList.size();
            Double idf = Math.log(1+(totalPages/df)) / Math.log(2.00);
            return idf;
        }

        //handle cases which the keyword exists in both title and body of pages
        //calculate the idf
        ArrayList<Integer> matchedPageList = new ArrayList<>();
        tPageList.keySet().forEach((pageId -> {
            if(!matchedPageList.contains(pageId)){
                matchedPageList.add(pageId);
            }
        }));
        bPageList.keySet().forEach((pageId -> {
            if(!matchedPageList.contains(pageId)){
                matchedPageList.add(pageId);
            }
        }));

        Double df = (double) matchedPageList.size();
        return Math.log(1+(totalPages/df)) / Math.log(2.00);
    }
}
