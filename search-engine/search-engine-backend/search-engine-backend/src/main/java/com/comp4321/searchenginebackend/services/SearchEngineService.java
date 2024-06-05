package com.comp4321.searchenginebackend.services;

import com.comp4321.searchenginebackend.models.Page;
import com.comp4321.searchenginebackend.models.PageResponse;
import com.comp4321.searchenginebackend.utils.StopStem;
import com.comp4321.searchenginebackend.utils.VectorSpaceModel;
import jdbm.htree.HTree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchEngineService {
    private StopStem stopStem;

    @Autowired
    private VectorSpaceModel vsm;

    public SearchEngineService() throws IOException {
        this.stopStem = new StopStem();
        System.out.println("Initializing SearchEngineService Successfully");
    }

    /**
     * function the get the vector space representation of a raw query
     *
     * @param query raw query in string format
     * @param raw
     * @return vector space representation of the target query
     */
    public Map<Integer, Double> getQueryVector(String query, String raw){
        // filter the raw string query with stop word and stem the filter query
        List<String> queryWords = new ArrayList<>();
        if(raw.equals("false")){
            queryWords = Arrays
                    .stream(query.split("\\W+"))
                    .toList();
        }else if(raw.equals("true")){
            queryWords = Arrays
                    .stream(query.split("\\W+"))
                    .filter(word -> !stopStem.isStopWord(word))
                    .map(word -> stopStem.stem(word))
                    .toList();
        }

        if(queryWords.isEmpty()){
            return null;
        }

        //construct the word frequency table for the preprocessed query words
        HashMap<String, Double> queryFreqTable = new HashMap<>();
        queryWords.forEach(word -> queryFreqTable.put(word, queryFreqTable.getOrDefault(word, 0.) + 1.));

        return vsm.processQuery(queryFreqTable);
    }

    /**
     * function to retrieve the relevant pages based on cosine similarity
     * between the keyword query and page term weights
     * either one of the keywords in the query should appear in each retrieved page's target section
     * while the ordering of the keywords is ignored
     * @param queryVector vector representation of raw query keywords
     * @param section section to search
     * @return a list of retrieved pages sorted in descending order of similarity score
     * @throws IOException
     */
    public ArrayList<PageResponse> keywordSearch(Map<Integer, Double> queryVector, String section) throws IOException {

        //find all matched page ids based on target search section
        Set<Integer> combinedMatch = new HashSet<>();
        Set<Integer> titleMatchPageIds;
        Set<Integer> bodyMatchPageIds;

        switch (section) {
            case "both" -> {
                titleMatchPageIds = findKeywordUnionPageIds(queryVector, "title");
                bodyMatchPageIds = findKeywordUnionPageIds(queryVector, "body");
                combinedMatch.addAll(titleMatchPageIds);
                combinedMatch.addAll(bodyMatchPageIds);
            }
            case "title" -> {
                titleMatchPageIds = findKeywordUnionPageIds(queryVector, "title");
                combinedMatch.addAll(titleMatchPageIds);
            }
            case "body" -> {
                bodyMatchPageIds = findKeywordUnionPageIds(queryVector, "body");
                combinedMatch.addAll(bodyMatchPageIds);
            }
            default -> {
                return new ArrayList<>();
            }
        }

        System.out.println("final matched page IDs:" + combinedMatch);

        //return an empty list if no matched page ids found
        if(combinedMatch.isEmpty()){
            return new ArrayList<>();
        }

        //get the vector space dictionary of all pages
        HashMap<Integer, HashMap<Integer, Double>> pageVSpace = this.vsm.getPageVSpace();

        //get the list of similarity scores in descending order
        List<Map.Entry<Integer, Double>> desSortedSimScoreList = calculateSimScores(combinedMatch, queryVector,
                pageVSpace);

        //return the parsed page response list
        return parseRankResult(desSortedSimScoreList);
    }

    /**
     * function to retrieve the relevant pages based on cosine similarity
     * between the phrase query and page term weights
     * For exact match, all keywords must appear and have the same ordering in each retrieved page's target section
     * For non-exact match, it essentially performs simple keyword search of give phrase in each retrieved page's target section
     * @param query string representation of raw query phrase
     * @param section section to search
     * @param raw where the query keywords are stemmed or not
     * @return a map of retrieved exact matched and non-exact matched pages sorted in descending order of similarity score
     * @throws IOException
     */
    public Map<String,ArrayList<PageResponse>> phraseSearch(String query, String section, String raw) throws IOException {

        //find all exact matched page ids based on target search section
        Set<Integer> combinedMatch = new HashSet<>();
        Set<Integer> titleMatchPageIds;
        Set<Integer> bodyMatchPageIds;

        //find all non-exact matched page ids based on target search section
        Set<Integer> nonExactCombinedMatch = new HashSet<>();
        Set<Integer> nonExactTitleMatchPageIds;
        Set<Integer> nonExactBodyMatchPageIds;

        //get the query vector of phrase query string
        Map<Integer, Double> queryVector = this.getQueryVector(query, raw);

        switch (section) {
            case "both" -> {
                //exact match page ids
                titleMatchPageIds = findPhraseIntersectPageIds(query, "title", raw);
                bodyMatchPageIds = findPhraseIntersectPageIds(query, "body", raw);
                combinedMatch.addAll(titleMatchPageIds);
                combinedMatch.addAll(bodyMatchPageIds);

                //non-exact match page ids
                nonExactTitleMatchPageIds = findKeywordUnionPageIds(queryVector, "title");
                nonExactBodyMatchPageIds = findKeywordUnionPageIds(queryVector, "body");
                nonExactCombinedMatch.addAll(nonExactTitleMatchPageIds);
                nonExactCombinedMatch.addAll(nonExactBodyMatchPageIds);
            }
            case "title" -> {
                //exact match page ids
                titleMatchPageIds = findPhraseIntersectPageIds(query, "title", raw);
                combinedMatch.addAll(titleMatchPageIds);

                //non-exact match page ids
                nonExactTitleMatchPageIds = findKeywordUnionPageIds(queryVector, "title");
                nonExactCombinedMatch.addAll(nonExactTitleMatchPageIds);
            }
            case "body" -> {
                //exact match page ids
                bodyMatchPageIds = findPhraseIntersectPageIds(query, "body", raw);
                combinedMatch.addAll(bodyMatchPageIds);

                //non-exact match page ids
                nonExactBodyMatchPageIds = findKeywordUnionPageIds(queryVector, "body");
                nonExactCombinedMatch.addAll(nonExactBodyMatchPageIds);
            }
            default -> {
                return new HashMap<>();
            }
        }

        //remove duplicated non-exact matched page ids
        nonExactCombinedMatch.removeAll(combinedMatch);

        System.out.println("final exact matched page IDs:" + combinedMatch);
        System.out.println("final non-exact matched page IDs:" + nonExactCombinedMatch);

        //return an empty list if no matched page ids found
        if(combinedMatch.isEmpty() && nonExactCombinedMatch.isEmpty()){
            return new HashMap<>();
        }

        //get the vector space dictionary of all pages
        HashMap<Integer, HashMap<Integer, Double>> pageVSpace = this.vsm.getPageVSpace();

        //get the list of similarity scores of exact matched pages in descending order
        List<Map.Entry<Integer, Double>> desSortedExactMatchSimScoreList = calculateSimScores(combinedMatch, queryVector,
                pageVSpace);

        //get the list of similarity scores of non-exact matched pages in descending order
        List<Map.Entry<Integer, Double>> desSortedNonExactSimScoreList = calculateSimScores(nonExactCombinedMatch, queryVector,
                pageVSpace);

        Map<String,ArrayList<PageResponse>> results = new HashMap<>();
        results.put("exactMatch", parseRankResult(desSortedExactMatchSimScoreList));
        results.put("nonExactMatch", parseRankResult(desSortedNonExactSimScoreList));

        //return the parsed page response list
        return results;
    }

    /**
     * function to retrieve the relevant pages based on cosine similarity
     * between the mixed query and page term weights
     * For exact match, the phrase and the extra keywords must appear
     * For non-exact match, the phrase must appear while the extra keywords do not appear
     * @param query string representation of raw mixed query
     * @param section section to search
     * @param raw where the query keywords are stemmed or not
     * @return a map of retrieved exact matched and non-exact matched pages sorted in descending order of similarity score
     * @throws IOException
     */
    public Map<String,ArrayList<PageResponse>> mixedSearch(String query, String section, String raw) throws IOException {
        String[] queryArray = query.split("\"");
        String phrase = queryArray[1];
        String keywords = queryArray[0]+queryArray[queryArray.length-1];
        Map<Integer, Double> keywordsVector = this.getQueryVector(keywords, raw);
        System.out.println("phrase:" + phrase);
        System.out.println("keywords:" + keywords);

        //find all phrase matched page ids based on target search section
        Set<Integer> phraseCombinedMatch = new HashSet<>();
        Set<Integer> phraseTitleMatchPageIds;
        Set<Integer> phraseBodyMatchPageIds;

        //find all keywords matched page ids based on target search section
        Set<Integer> keywordsCombinedMatch = new HashSet<>();
        Set<Integer> keywordsTitleMatchPageIds;
        Set<Integer> keywordsBodyMatchPageIds;

        switch (section) {
            case "both" -> {
                //phrase match page ids
                phraseTitleMatchPageIds = findPhraseIntersectPageIds(phrase, "title", raw);
                phraseBodyMatchPageIds = findPhraseIntersectPageIds(phrase, "body", raw);
                phraseCombinedMatch.addAll(phraseTitleMatchPageIds);
                phraseCombinedMatch.addAll(phraseBodyMatchPageIds);

                //keyword match page ids
                keywordsTitleMatchPageIds = findKeywordUnionPageIds(keywordsVector, "title");
                keywordsBodyMatchPageIds = findKeywordUnionPageIds(keywordsVector, "body");
                keywordsCombinedMatch.addAll(keywordsTitleMatchPageIds);
                keywordsCombinedMatch.addAll(keywordsBodyMatchPageIds);
            }
            case "title" -> {
                //phrase match page ids
                phraseTitleMatchPageIds = findPhraseIntersectPageIds(phrase, "title", raw);
                phraseCombinedMatch.addAll(phraseTitleMatchPageIds);

                //keyword match page ids
                keywordsTitleMatchPageIds = findKeywordUnionPageIds(keywordsVector, "title");
                keywordsCombinedMatch.addAll(keywordsTitleMatchPageIds);
            }
            case "body" -> {
                //phrase match page ids
                phraseBodyMatchPageIds = findPhraseIntersectPageIds(phrase, "body", raw);
                phraseCombinedMatch.addAll(phraseBodyMatchPageIds);

                //keyword match page ids
                keywordsBodyMatchPageIds = findKeywordUnionPageIds(keywordsVector, "body");
                keywordsCombinedMatch.addAll(keywordsBodyMatchPageIds);
            }
            default -> {
                return new HashMap<>();
            }
        }

        //find exact matched page ids
        keywordsCombinedMatch.retainAll(phraseCombinedMatch);
        Set<Integer> exactMatch = new HashSet<>(keywordsCombinedMatch);
        phraseCombinedMatch.removeAll(exactMatch);
        Set<Integer> nonExactMatch = new HashSet<>(phraseCombinedMatch);

        System.out.println("final exact matched page IDs:" + exactMatch);
        System.out.println("final non-exact matched page IDs:" + nonExactMatch);

        //return an empty list if no matched page ids found
        if(exactMatch.isEmpty() && nonExactMatch.isEmpty()){
            return new HashMap<>();
        }

        //get the vector space dictionary of all pages
        HashMap<Integer, HashMap<Integer, Double>> pageVSpace = this.vsm.getPageVSpace();

        //get the query vector
        Map<Integer, Double> queryVector = this.getQueryVector(query, raw);

        //get the list of similarity scores of exact matched pages in descending order
        List<Map.Entry<Integer, Double>> desSortedExactMatchSimScoreList = calculateSimScores(exactMatch, queryVector,
                pageVSpace);

        //get the list of similarity scores of non-exact matched pages in descending order
        List<Map.Entry<Integer, Double>> desSortedNonExactSimScoreList = calculateSimScores(nonExactMatch, queryVector,
                pageVSpace);

        Map<String,ArrayList<PageResponse>> results = new HashMap<>();
        results.put("exactMatch", parseRankResult(desSortedExactMatchSimScoreList));
        results.put("nonExactMatch", parseRankResult(desSortedNonExactSimScoreList));

        //return the parsed page response list
        return results;
    }

    /**
     * helper function to find all the page ids that either one of the keywords in a query vector have appeared in
     * @param queryVector keywords to match
     * @param section match appearance based on the section
     * @return a set of matched page ids
     * @throws IOException
     */
    public Set<Integer> findKeywordUnionPageIds(Map<Integer, Double> queryVector, String section) throws IOException {

        System.out.println("section:"+section);
        System.out.println("query vector:"+queryVector);

        HTree iIdx;
        //set the inverted index based on target section
        if(section.equals("title")){
            iIdx = this.vsm.getTitleInvertedIndex();
        }else {
            iIdx = this.vsm.getBodyInvertedIndex();
        }

        // find all page ids matched for the keywords in query vector
        Set<Integer> matchedPageIds = new HashSet<>();
        for(Map.Entry<Integer, Double> queryEntry : queryVector.entrySet()) {
            //for each keyword in query, find its appeared page list
            HashMap<Integer, List<Integer>> pageList = (HashMap<Integer, List<Integer>>) iIdx.get(queryEntry.getKey());
            if (pageList != null) {
                //add all page ids found
                System.out.println("page for "+queryEntry.getKey()+"="+pageList);
                matchedPageIds.addAll(pageList.keySet());
            } else {
                System.out.println("page for "+queryEntry.getKey()+" Not Found");
            }
        }
        System.out.println("MatchedPageIds:"+matchedPageIds);

        return matchedPageIds;
    }

    /**
     * helper function to find all the page ids that exact match all keywords appeared in a phrase with order checking
     * @param query phrase to match
     * @param section match appearance based on the section
     * @return a set of matched page ids
     * @throws IOException
     */
    public Set<Integer> findPhraseIntersectPageIds(String query, String section, String raw) throws IOException {
        // filter the raw string query with stop word and stem the filter query
        List<String> queryWords = new ArrayList<>();
        if(raw.equals("false")){
            queryWords = Arrays
                    .stream(query.split("\\W+"))
                    .toList();
        }else if(raw.equals("true")){
            queryWords = Arrays
                    .stream(query.split("\\W+"))
                    .filter(word -> !stopStem.isStopWord(word))
                    .map(word -> stopStem.stem(word))
                    .toList();
        }

        if(queryWords.isEmpty()){
            return new HashSet<>();
        }

        //map each query word string to keywordId in database
        HTree keywordIdLookupIndex = this.vsm.getKeywordIdLookupIndex();
        List<Integer> queryWordIds = queryWords.stream().map(str -> {
            try {
                Integer keywordId = (Integer) keywordIdLookupIndex.get(str);
                if(keywordId != null){
                    return keywordId;
                }else {
                    return -1;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();

        System.out.println("section:"+section);
        System.out.println("query word ids:"+queryWordIds);

        HTree iIdx;
        //set the inverted index based on target section
        if(section.equals("title")){
            iIdx = this.vsm.getTitleInvertedIndex();
        }else {
            iIdx = this.vsm.getBodyInvertedIndex();
        }

        Set<Integer> matchedPageIds = new HashSet<>();
        // find all page ids matched for the phrase
        for(int i=0; i<queryWordIds.size(); i++) {
            // find the page list for the current keyword
            HashMap<Integer, List<Integer>> pageList = (HashMap<Integer, List<Integer>>) iIdx.get(queryWordIds.get(i));
            if (i == 0) {
                //if it is the first keyword, add all page ids to the set
                if (pageList != null) {
                    System.out.println("page for "+queryWordIds.get(i)+"="+pageList);
                    matchedPageIds = new HashSet<>();
                    matchedPageIds.addAll(pageList.keySet());
                } else {
                    System.out.println("page for "+queryWordIds.get(i)+" Not Found");
                    return new HashSet<>();
                }
            } else {
                //for other keywords, keep the appeared page ids in both current keyword page list
                //and the matchedPageIds set, remove page ids not intersecting the two sets
                if (pageList != null) {
                    System.out.println("page for "+queryWordIds.get(i)+"="+pageList);
                    matchedPageIds.retainAll(pageList.keySet());
                } else {
                    System.out.println("page for "+queryWordIds.get(i)+" Not Found");
                    return new HashSet<>();
                }
            }
            System.out.println("after intersection:"+matchedPageIds);
        }

        System.out.println("MatchedPageIds:"+matchedPageIds);

        if(matchedPageIds.isEmpty()){
            return new HashSet<>();
        }

        //store the offset-adjusted position ordering of appeared keywords in phrase query in each matched page
        HashMap<Integer,HashMap<Integer,Set<Integer>>> pageWordPos = new HashMap<>();
        matchedPageIds.forEach(pageId->{
            HashMap<Integer,Set<Integer>> wordPos = new HashMap<>();
            for(int i=0; i<queryWordIds.size(); i++){
                try {
                    //for each keyword id in query phrase, get its page list
                    HashMap<Integer, List<Integer>> pageList = (HashMap<Integer, List<Integer>>) iIdx.get(queryWordIds.get(i));
                    if(pageList != null){
                        //get the position list of current keyword in current page
                        //map it to a set with each element subtracted by a position offset indicated by the current
                        //keyword position in the phrase query, store it in the pageWordPos table
                        final int posOffSet = i;
                        wordPos.put(queryWordIds.get(i), pageList.get(pageId)
                                .stream()
                                .map(pos->pos-posOffSet)
                                .collect(Collectors.toSet()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            pageWordPos.put(pageId, wordPos);
        });

        System.out.println("pageWordPos:"+pageWordPos);

        //check the position ordering for each match page
        for(Map.Entry<Integer,HashMap<Integer,Set<Integer>>> pageIdHashMapEntry: pageWordPos.entrySet()){
            //get the first keyword position list in current page and add it to a set
            HashMap<Integer,Set<Integer>> keywordPos = pageIdHashMapEntry.getValue();
            Set<Integer> firstWordPos = new HashSet<>();
            firstWordPos.addAll(keywordPos.get(queryWordIds.get(0)));
            System.out.println("firstWordPos:"+firstWordPos);

            //for each subsequent keywords, get the position lists in current page and keep all intersected positions
            //(adjusted keywords with the same position due to position offset)
            for(int i=1; i<keywordPos.size(); i++){
                firstWordPos.retainAll(keywordPos.get(queryWordIds.get(i)));
                System.out.println("firstWordPos after:"+firstWordPos);
            }

            //if the set is empty after intersecting all keywords
            //the target phrase is not in current page and remove it
            //from the matched page id set
            if(firstWordPos.isEmpty()){
                matchedPageIds.remove(pageIdHashMapEntry.getKey());
            }
        }

        System.out.println("MatchedPageIds:"+matchedPageIds);

        //return final matched page ids set after position checking
        return matchedPageIds;
    }

    /**
     *
     * @param pageIds set of page ids of pages to calculate the similarity score
     * @param queryVector vector representation of a query to calculate the similarity score
     * @param pageVSpace vector space dictionary of all pages
     * @return a list of pageId <-> similarity score entries sorted in descending order of scores
     */
    public List<Map.Entry<Integer, Double>> calculateSimScores(Set<Integer> pageIds, Map<Integer, Double> queryVector,
                                                               HashMap<Integer, HashMap<Integer, Double>>  pageVSpace){
        //Initialize map to store the exact matched page ids and their similarity scores
        TreeMap<Integer, Double> simScore = new TreeMap<>();

        //calculate the cosine similarity between the query vector and each matched page
        pageIds.forEach(pageId->{
            //get term weights for current page
            HashMap<Integer, Double> pageWeights = pageVSpace.get(pageId);

            double pageSim = 0.;
            //calculate and sum the similarity score for each keyword in the query
            for(HashMap.Entry<Integer, Double> queryEntry : queryVector.entrySet()){
                if(pageWeights.containsKey(queryEntry.getKey())){
                    pageSim += pageWeights.get(queryEntry.getKey()) * queryEntry.getValue();
                }
            }

            //normalize the similarity score with respect to page length and query length
            if(pageSim != 0.){
                Double pageNorm = Math.sqrt(pageWeights
                        .values()
                        .stream()
                        .map(v->Math.pow(v,2))
                        .reduce(Double::sum)
                        .get());
                Double queryNorm = Math.sqrt(queryVector
                        .values()
                        .stream()
                        .map(v->Math.pow(v,2))
                        .reduce(Double::sum)
                        .get());

                Double cosSim = pageSim/(pageNorm * queryNorm);

                //store normalized similarity score for current page in the simScore TreeMap
                simScore.put(pageId, cosSim);
            }
        });

        //convert the treemap to a linked list and sort the scores in descending order
        List<Map.Entry<Integer, Double>> desSortedSimScoreList = new ArrayList<>(simScore.entrySet());
        desSortedSimScoreList.sort(Map.Entry.<Integer, Double>comparingByValue().reversed());

        return desSortedSimScoreList;
    }

    /**
     * helper function to get other information of retrieved pages and store all needed information in Page Response objects
     * @param desSortedSimScoreList list of retrieved page ids and their similarity scores
     * @return a list of Page Response objects representing retrieved pages
     * @throws IOException
     */
    public ArrayList<PageResponse> parseRankResult(List<Map.Entry<Integer, Double>> desSortedSimScoreList) throws IOException {
        HTree forwardIndex = vsm.getForwardIndex();
        ArrayList<PageResponse> resultPageList = new ArrayList<>();

        //retrieve the real Page object based on provided pageID and
        //store all needed info into Page Response entity
        desSortedSimScoreList.forEach(idScoreEntry -> {
            Page page = null;
            try {
                page = (Page) forwardIndex.get(idScoreEntry.getKey());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(page != null){

                //get the top k(here 5) frequent keywords appears in the page
                List<Map.Entry<String, Integer>> topKFreq = getTopKFreq(page.getTitleFreqTable(), page.getFreqTable(), 5);

                //get other fields directly through the Page object
                PageResponse pRes = new PageResponse();
                pRes.setPageID(idScoreEntry.getKey());
                pRes.setSimScore(idScoreEntry.getValue());
                pRes.setTopKFreq(topKFreq);
                pRes.setUrl(page.getUrl());
                pRes.setSize(page.getSize());
                pRes.setTitle(page.getTitle());
                pRes.setParentUrls(page.getParentUrls());
                pRes.setChildUrls(page.getChildUrls());
                pRes.setLastModification(page.getLastModification());
                resultPageList.add(pRes);
            }
        });

        return resultPageList;
    }

    /**
     * helper function to get the top k frequent keywords appear in a page
     * @param titleFreq title keyword frequency table of a page
     * @param bodyFreq body keyword frequency table of a page
     * @param limit range of top frequent keywords
     * @return list of frequent keywords with corresponding frequencies based on limit
     */
    public List<Map.Entry<String, Integer>> getTopKFreq(HashMap<Integer, Integer> titleFreq,
                                                        HashMap<Integer, Integer> bodyFreq,
                                                        Integer limit){
        HTree keywordLookupIndex = this.vsm.getKeywordLookupIndex();
        //combine the frequencies of keywords in title and body and store them in a linked list
        List<Map.Entry<Integer, Integer>> freqList = new LinkedList<>((Stream
                .of(titleFreq.keySet(), bodyFreq.keySet())
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toMap(
                        k -> k,
                        v -> Integer.sum(
                                titleFreq.getOrDefault(v, 0),
                                bodyFreq.getOrDefault(v, 0)))
                ).entrySet()));

        //sort the list base on value(frequency) in descending order
        freqList.sort(Map.Entry.<Integer, Integer>comparingByValue().reversed());

        //keep and return the top k(limit) entries
        if(freqList.size() > limit){
            freqList.subList(limit, freqList.size()).clear();
        }

        return freqList.stream().map(entry ->{
            try {
                String keyword = (String) keywordLookupIndex.get(entry.getKey());
                if(keyword != null){
                    return Map.entry(keyword, entry.getValue());
                }else{
                    throw new RuntimeException("keyword lookup error");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }
}
