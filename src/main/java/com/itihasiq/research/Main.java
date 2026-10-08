package com.itihasiq.research;

import java.nio.file.*;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        Path csv=args.length>0?Paths.get(args[0]):Paths.get("data/research_papers.csv");
        List<Paper> papers=PaperRepository.load(csv);
        if(papers.isEmpty()) { System.out.println("No papers loaded from: "+csv.toAbsolutePath()); return; }
        System.out.println("\n==============================================");
        System.out.println(" SMART RESEARCH PAPER RECOMMENDATION SYSTEM");
        System.out.println("==============================================");
        System.out.println("Dataset loaded: "+papers.size()+" research papers");
        System.out.println("Enter multiple topics separated by commas (example: machine learning, graph algorithms)");
        System.out.print("Search topics: ");
        Scanner sc=new Scanner(System.in);
        String input=sc.nextLine().trim();
        List<String> topics=new ArrayList<>();
        for(String t:input.split(",")) if(!t.isBlank()) topics.add(t.trim().toLowerCase());
        if(topics.isEmpty()){System.out.println("Please enter at least one topic.");return;}
        for(Paper p:papers) Algorithms.score(p,topics);
        double threshold=0.20;
        int[] matching=Algorithms.maximumMatching(papers,topics,threshold);
        System.out.println("\n--- TOP RECOMMENDATIONS ---");
        List<Paper> results=Algorithms.topK(papers,5);
        int rank=1;
        for(Paper p:results) {
            if(p.score<=0) continue;
            System.out.printf("%n%d. %s%n   Relevance: %.1f%% | Exact: %.1f%% | Fuzzy: %.1f%%%n",rank++,p.title,p.score*100,p.exactMatch*100,p.fuzzyMatch*100);
            System.out.println("   Keywords: "+p.keywords);
            System.out.println("   Why: "+explanation(p,topics));
            if(!p.url.isBlank()) System.out.println("   Reference: "+p.url);
        }
        System.out.println("\n--- TOPIC COVERAGE (BIPARTITE MATCHING) ---");
        boolean[] covered=new boolean[topics.size()]; int count=0;
        for(int i=0;i<matching.length;i++) if(matching[i]>=0){covered[matching[i]]=true;count++; System.out.printf("%s  -->  %s%n",topics.get(matching[i]),papers.get(i).title);}
        for(int i=0;i<topics.size();i++) if(!covered[i]) System.out.println(topics.get(i)+"  -->  No distinct paper met threshold");
        System.out.printf("Coverage: %d/%d topics (%s)%n",count,topics.size(),count==topics.size()?"COMPLETE":"PARTIAL");
        System.out.println("\nAlgorithms used: Rabin-Karp | Levenshtein DP | Composite scoring | Bipartite matching | PriorityQueue");
    }
    private static String explanation(Paper p,List<String> topics) {
        List<String> matches=new ArrayList<>();
        for(String t:topics) if(Algorithms.rabinKarp(p.searchableText(),t)) matches.add(t);
        return matches.isEmpty()?"Approximate spelling/term similarity":"Exact topic match: "+String.join(", ",matches);
    }
}
