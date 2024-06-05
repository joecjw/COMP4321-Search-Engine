package com.comp4321.searchenginebackend.models;

import lombok.Data;
import java.util.*;

@Data
public class PageResponse {
    private Integer pageID;
    private String title;
    private String url;
    private String size;
    private List<Map.Entry<String, Integer>> topKFreq;
    private ArrayList<String> parentUrls;
    private ArrayList<String> childUrls;
    private Date lastModification;
    private Double simScore;
}


