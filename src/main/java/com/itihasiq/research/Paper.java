package com.itihasiq.research;

public class Paper {
    public final String id, title, keywords, abstractText, url;
    public double exactMatch, fuzzyMatch, score;
    public Paper(String id, String title, String keywords, String abstractText, String url) {
        this.id=id; this.title=title; this.keywords=keywords; this.abstractText=abstractText; this.url=url;
    }
    public String getTitle(){ return title; }
    public String getKeywords(){ return keywords; }
    public String searchableText() { return (title + " " + keywords + " " + abstractText).toLowerCase(); }
}
