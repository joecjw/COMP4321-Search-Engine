package com.comp4321.searchenginebackend.utils;

import org.springframework.core.io.ClassPathResource;
import java.io.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.stream.Collectors;

public class StopStem {
    private  Porter porter;
    private  HashSet<String> stopWords;
    public  boolean isStopWord(String str) {
        return str.isEmpty() || str.isBlank() || stopWords.contains(str.toLowerCase());
    }

    /**
     * constructor to initialize the object with an implemented porter's algorithm object for word stemming
     * and a pre-defined set of stop words by reading from the provided file called "stopwords.txt"
     * @throws IOException
     */
    public StopStem() throws IOException {
        porter = new Porter();
        stopWords = new HashSet<String>();
        ClassPathResource res = new ClassPathResource("stopwords.txt");
        try (BufferedReader br = new BufferedReader(new InputStreamReader(res.getInputStream()))) {
            stopWords.addAll(br.lines().collect(Collectors.toList()));
        }
    }

    /**
     * function to stem processing a string
     * @param str input string for stemming
     * @return a stemmed string
     */
    public String stem(String str) {
        return porter.stripAffixes(str);
    }
}

