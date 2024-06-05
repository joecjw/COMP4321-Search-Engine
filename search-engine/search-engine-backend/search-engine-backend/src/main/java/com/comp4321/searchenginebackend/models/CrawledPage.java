package com.comp4321.searchenginebackend.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CrawledPage implements Serializable {
    private String title;
    private String url;
    private String size;
    private HashMap<String, Integer> freqTable;
    private HashMap<String, Integer> titleFreqTable;
    private HashMap<String, ArrayList<Integer>> wordPositions;
    private HashMap<String, ArrayList<Integer>> titlePositions;
    private ArrayList<String> parentUrls;
    private ArrayList<String> childUrls;
    private Date lastModification;

}