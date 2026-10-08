package com.itihasiq.research;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ResearchWebServer {
    private static final int PORT = 8080;
    private static List<Paper> papers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        loadPapers();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/search", ResearchWebServer::search);
        server.createContext("/", ResearchWebServer::staticFile);
        server.setExecutor(null);
        System.out.println("==============================================");
        System.out.println(" Smart Research Paper Recommendation System");
        System.out.println(" Web version: http://localhost:" + PORT);
        System.out.println(" Papers loaded: " + papers.size());
        System.out.println("==============================================");
        server.start();
    }

    private static void loadPapers() throws IOException {
        Path csv = Paths.get("data", "research_papers.csv");
        if (!Files.exists(csv)) throw new FileNotFoundException("Missing " + csv.toAbsolutePath());
        papers = PaperRepository.load(csv);
    }

    private static void staticFile(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..")) { send(ex, 403, "Forbidden", "text/plain; charset=UTF-8"); return; }
        InputStream in = ResearchWebServer.class.getResourceAsStream("/web" + path);
        if (in == null) { send(ex, 404, "Not found", "text/plain; charset=UTF-8"); return; }
        byte[] bytes = in.readAllBytes(); in.close();
        ex.getResponseHeaders().set("Content-Type", contentType(path));
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
    }

    private static void search(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) { send(ex,405,"{\"error\":\"GET required\"}","application/json"); return; }
        Map<String,String> q = queryParams(ex.getRequestURI().getRawQuery());
        String query = q.getOrDefault("query", "").trim();
        double threshold = parseDouble(q.get("threshold"), .20);
        threshold = Math.max(.05, Math.min(.90, threshold));
        if (query.isBlank()) { send(ex,400,"{\"error\":\"Query is required\"}","application/json"); return; }
        try { send(ex,200,buildResult(query, threshold),"application/json; charset=UTF-8"); }
        catch (Exception e) { e.printStackTrace(); send(ex,500,"{\"error\":\""+escape(e.getMessage())+"\"}","application/json"); }
    }

    private static String buildResult(String query, double threshold) {
        List<String> topics = new ArrayList<>();
        for (String s : query.split(",")) if (!s.isBlank()) topics.add(s.trim().toLowerCase(Locale.ROOT));
        if (topics.isEmpty()) topics.add(query.toLowerCase(Locale.ROOT));

        for (Paper p : papers) Algorithms.score(p, topics);
        int[] paperToTopic = Algorithms.maximumMatching(papers, topics, threshold);
        int[] topicToPaper = new int[topics.size()]; Arrays.fill(topicToPaper, -1);
        for (int p=0;p<paperToTopic.length;p++) if (paperToTopic[p]>=0 && paperToTopic[p]<topics.size()) topicToPaper[paperToTopic[p]]=p;
        List<Paper> ranked = Algorithms.topK(papers, 5);
        int matched=0; for(int x:topicToPaper) if(x>=0) matched++;
        double best=ranked.isEmpty()?0:ranked.get(0).score;

        StringBuilder j=new StringBuilder("{");
        j.append("\"query\":\"").append(escape(query)).append("\",");
        j.append("\"threshold\":").append(num(threshold)).append(",");
        j.append("\"topics\":[");
        for(int i=0;i<topics.size();i++){if(i>0)j.append(',');j.append('\"').append(escape(topics.get(i))).append('\"');}
        j.append("],\"stats\":{");
        j.append("\"topics\":").append(topics.size()).append(',');
        j.append("\"papers\":").append(papers.size()).append(',');
        j.append("\"matched\":").append(matched).append(',');
        j.append("\"bestScore\":").append(num(best)).append("},");
        j.append("\"recommendations\":[");
        for(int i=0;i<ranked.size();i++){if(i>0)j.append(',');j.append(paperJson(ranked.get(i),i+1));}
        j.append("],\"matching\":[");
        for(int t=0;t<topics.size();t++){
            if(t>0)j.append(','); j.append('{').append("\"topic\":\"").append(escape(topics.get(t))).append('\"');
            int pi=topicToPaper[t];
            if(pi>=0){ Paper p=papers.get(pi); Algorithms.TopicBreakdown b=Algorithms.topicBreakdown(p,topics.get(t));
                j.append(",\"paperIndex\":").append(pi).append(",\"paper\":\"").append(escape(p.title)).append('\"');
                j.append(",\"score\":").append(num(b.score())).append(",\"exact\":").append(num(b.exact()));
                j.append(",\"fuzzy\":").append(num(b.fuzzy())).append(",\"titleExact\":").append(num(b.titleExact()));
                j.append(",\"keywordExact\":").append(num(b.keywordExact())).append(",\"abstractExact\":").append(num(b.abstractExact())).append(",\"assigned\":true");
            } else j.append(",\"paperIndex\":-1,\"paper\":null,\"score\":0,\"assigned\":false");
            j.append('}');
        }
        j.append("],\"graph\":[");
        boolean first=true;
        for(int t=0;t<topics.size();t++){int pi=topicToPaper[t]; if(pi<0)continue; if(!first)j.append(',');first=false;double w=Algorithms.topicRelevance(papers.get(pi),topics.get(t));j.append("{\"topicIndex\":").append(t).append(",\"paperIndex\":").append(pi).append(",\"weight\":").append(num(w)).append('}');}
        return j.append("]}").toString();
    }

    private static String paperJson(Paper p,int rank){
        return "{\"rank\":"+rank+",\"title\":\""+escape(p.title)+"\",\"keywords\":\""+escape(p.keywords)+"\",\"abstract\":\""+escape(p.abstractText)+"\",\"url\":\""+escape(p.url)+"\",\"score\":"+num(p.score)+",\"exact\":"+num(p.exactMatch)+",\"fuzzy\":"+num(p.fuzzyMatch)+"}";
    }

    private static Map<String,String> queryParams(String raw){
        Map<String,String> m=new HashMap<>(); if(raw==null)return m;
        for(String pair:raw.split("&")){String[] a=pair.split("=",2);String k=decode(a[0]);String v=a.length>1?decode(a[1]):"";m.put(k,v);}return m;
    }
    private static String decode(String s){try{return URLDecoder.decode(s,StandardCharsets.UTF_8);}catch(Exception e){return s;}}
    private static double parseDouble(String s,double d){try{return Double.parseDouble(s);}catch(Exception e){return d;}}
    private static String num(double d){return String.format(Locale.US,"%.6f",Double.isFinite(d)?d:0);}
    private static String escape(String s){if(s==null)return "";return s.replace("\\","\\\\").replace("\"","\\\"").replace("\r","\\r").replace("\n","\\n").replace("\t","\\t");}
    private static String contentType(String p){if(p.endsWith(".html"))return "text/html; charset=UTF-8";if(p.endsWith(".css"))return "text/css; charset=UTF-8";if(p.endsWith(".js"))return "application/javascript; charset=UTF-8";return "text/plain; charset=UTF-8";}
    private static void send(HttpExchange ex,int status,String body,String type)throws IOException{byte[] b=body.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type",type);ex.getResponseHeaders().set("Access-Control-Allow-Origin","*");ex.sendResponseHeaders(status,b.length);try(OutputStream out=ex.getResponseBody()){out.write(b);}}
}
