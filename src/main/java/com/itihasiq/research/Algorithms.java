package com.itihasiq.research;

import java.util.*;

/** Core DSA engine: Rabin-Karp, Levenshtein DP, weighted Kuhn matching and PriorityQueue Top-K. */
public final class Algorithms {
    private Algorithms() {}

    public static boolean rabinKarp(String text, String pattern) {
        if (pattern == null || pattern.isBlank()) return true;
        if (text == null) return false;
        text = text.toLowerCase(Locale.ROOT);
        pattern = pattern.toLowerCase(Locale.ROOT).trim();
        if (pattern.length() > text.length()) return false;
        int n=text.length(), m=pattern.length(), base=256, prime=101;
        long ph=0, th=0, h=1;
        for(int i=0;i<m-1;i++) h=(h*base)%prime;
        for(int i=0;i<m;i++){ ph=(base*ph+pattern.charAt(i))%prime; th=(base*th+text.charAt(i))%prime; }
        for(int i=0;i<=n-m;i++){
            if(ph==th && text.regionMatches(i,pattern,0,m)) return true;
            if(i<n-m){ th=(base*(th-text.charAt(i)*h)+text.charAt(i+m))%prime; if(th<0) th+=prime; }
        }
        return false;
    }

    public static int levenshtein(String a, String b) {
        if(a==null) a=""; if(b==null) b="";
        a=a.toLowerCase(Locale.ROOT); b=b.toLowerCase(Locale.ROOT);
        if(a.length()<b.length()){String t=a;a=b;b=t;}
        int[] prev=new int[b.length()+1], curr=new int[b.length()+1];
        for(int j=0;j<=b.length();j++) prev[j]=j;
        for(int i=1;i<=a.length();i++){
            curr[0]=i;
            for(int j=1;j<=b.length();j++){
                int cost=a.charAt(i-1)==b.charAt(j-1)?0:1;
                curr[j]=Math.min(Math.min(curr[j-1]+1,prev[j]+1),prev[j-1]+cost);
            }
            int[] t=prev;prev=curr;curr=t;
        }
        return prev[b.length()];
    }

    static double fuzzySimilarity(String query,String text){
        String q=normalize(query); if(q.isBlank()) return 0;
        String[] tokens=normalize(text).split("[^a-z0-9]+");
        double best=0;
        for(String token:tokens){
            if(token.isBlank()) continue;
            int d=levenshtein(q,token);
            best=Math.max(best,1.0-(double)d/Math.max(q.length(),token.length()));
        }
        return Math.max(0,best);
    }

    private static String normalize(String s){return s==null?"":s.toLowerCase(Locale.ROOT).trim();}

    public static void score(Paper p,List<String> topics){
        if(topics.isEmpty()){p.exactMatch=p.fuzzyMatch=p.score=0;return;}
        double exact=0,fuzzy=0;
        for(String topic:topics){
            TopicBreakdown b=topicBreakdown(p,topic);
            exact+=b.exact;
            fuzzy+=b.fuzzy;
        }
        p.exactMatch=exact/topics.size();
        p.fuzzyMatch=fuzzy/topics.size();
        p.score=.70*p.exactMatch+.30*p.fuzzyMatch;
    }

    /** Field-aware scoring used by the GUI and weighted matching. */
    public static TopicBreakdown topicBreakdown(Paper p,String topic){
        String q=normalize(topic);
        if(q.isBlank()) return new TopicBreakdown(0,0,0,0,0,0);
        double titleExact=rabinKarp(p.title,q)?1:0;
        double keywordExact=rabinKarp(p.keywords,q)?1:0;
        double abstractExact=rabinKarp(p.abstractText,q)?1:0;
        double exact=Math.max(titleExact,Math.max(keywordExact,abstractExact));
        // Field-weighted fuzzy evidence: title 50%, keywords 30%, abstract 20%.
        double fuzzy=.50*fuzzySimilarity(q,p.title)+.30*fuzzySimilarity(q,p.keywords)+.20*fuzzySimilarity(q,p.abstractText);
        double score=.70*exact+.30*fuzzy;
        return new TopicBreakdown(exact,fuzzy,score,titleExact,keywordExact,abstractExact);
    }

    /** Weighted Kuhn DFS matching. Edge order is strongest-to-weakest. */
    public static int[] maximumMatching(List<Paper> papers,List<String> topics,double threshold){
        int[] paperToTopic=new int[papers.size()];
        Arrays.fill(paperToTopic,-1);
        for(int topic=0;topic<topics.size();topic++){
            boolean[] seen=new boolean[papers.size()];
            augment(topic,papers,topics,threshold,paperToTopic,seen);
        }
        return paperToTopic;
    }

    private static boolean augment(int topic,List<Paper> papers,List<String> topics,double threshold,int[] match,boolean[] seen){
        List<Integer> candidates=new ArrayList<>();
        for(int i=0;i<papers.size();i++){
            double w=topicRelevance(papers.get(i),topics.get(topic));
            if(w>=threshold) candidates.add(i);
        }
        candidates.sort((a,b)->Double.compare(
                topicRelevance(papers.get(b),topics.get(topic)),
                topicRelevance(papers.get(a),topics.get(topic))));
        for(int i:candidates){
            if(seen[i]) continue;
            seen[i]=true;
            if(match[i]==-1){match[i]=topic;return true;}
            int previousTopic=match[i];
            double newWeight=topicRelevance(papers.get(i),topics.get(topic));
            double previousWeight=topicRelevance(papers.get(i),topics.get(previousTopic));
            if(newWeight<=previousWeight+1e-9) continue;
            if(augment(previousTopic,papers,topics,threshold,match,seen)){
                match[i]=topic;
                return true;
            }
        }
        return false;
    }

    public static double topicRelevance(Paper p,String topic){return topicBreakdown(p,topic).score;}

    public static List<Paper> topK(List<Paper> papers,int k){
        PriorityQueue<Paper> pq=new PriorityQueue<>(Comparator
                .comparingDouble((Paper p)->p.score).reversed()
                .thenComparing(p->p.title));
        pq.addAll(papers);
        List<Paper> out=new ArrayList<>();
        while(!pq.isEmpty()&&out.size()<k) out.add(pq.poll());
        return out;
    }

    public record TopicBreakdown(double exact,double fuzzy,double score,double titleExact,double keywordExact,double abstractExact) {}
}
