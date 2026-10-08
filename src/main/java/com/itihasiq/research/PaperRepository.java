package com.itihasiq.research;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class PaperRepository {
    private PaperRepository() {}
    public static List<Paper> load(Path path) throws IOException {
        List<Paper> papers=new ArrayList<>();
        try(BufferedReader br=Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line; boolean first=true;
            while((line=br.readLine())!=null) {
                if(first){first=false; if(line.toLowerCase().startsWith("id,")) continue;}
                if(line.isBlank()) continue;
                List<String> c=parseCsv(line);
                if(c.size()<5) continue;
                papers.add(new Paper(c.get(0),c.get(1),c.get(2),c.get(3),c.get(4)));
            }
        }
        return papers;
    }
    private static List<String> parseCsv(String line) {
        List<String> out=new ArrayList<>(); StringBuilder s=new StringBuilder(); boolean quoted=false;
        for(int i=0;i<line.length();i++) { char ch=line.charAt(i);
            if(ch=='"') { if(quoted && i+1<line.length() && line.charAt(i+1)=='"'){s.append('"');i++;} else quoted=!quoted; }
            else if(ch==',' && !quoted){out.add(s.toString());s.setLength(0);} else s.append(ch);
        }
        out.add(s.toString()); return out;
    }
}
