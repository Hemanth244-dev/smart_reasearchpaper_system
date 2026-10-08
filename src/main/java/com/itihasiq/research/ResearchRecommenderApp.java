package com.itihasiq.research;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ResearchRecommenderApp extends Application {
    private final List<Paper> papers=new ArrayList<>();
    private final TableView<Paper> table=new TableView<>();
    private final VBox matchBox=new VBox(8);
    private final VBox detailBox=new VBox(10);
    private final Canvas graph=new Canvas(760,380);
    private final Label coverageLabel=new Label("Coverage: —");
    private final Label status=new Label("Ready. Enter topics and click SEARCH.");
    private final TextField searchField=new TextField("machine learning, recommendation systems, graph algorithms");
    private final Slider threshold=new Slider(.05,.90,.20);
    private List<String> topics=new ArrayList<>();
    private int[] matching=new int[0];

    @Override public void start(Stage stage) throws Exception {
        Path csv=Paths.get("data/research_papers.csv");
        papers.addAll(PaperRepository.load(csv));
        stage.setTitle("Smart Research Paper Recommendation System");
        BorderPane root=new BorderPane(); root.setPadding(new Insets(18)); root.setStyle("-fx-background-color:#f5f7fb;");
        root.setTop(header());
        root.setCenter(mainContent());
        Scene scene=new Scene(root,1400,900);
        stage.setScene(scene); stage.setMinWidth(1150); stage.setMinHeight(760); stage.show();
        runSearch();
    }

    private VBox header(){
        Label title=new Label("SMART RESEARCH PAPER RECOMMENDATION SYSTEM");
        title.setFont(Font.font("System",24)); title.setStyle("-fx-font-weight:bold; -fx-text-fill:#16213e;");
        Label sub=new Label("Rabin–Karp  •  Levenshtein DP  •  Weighted Bipartite Matching  •  PriorityQueue Top-K");
        sub.setStyle("-fx-text-fill:#52607a; -fx-font-size:13px;");
        VBox box=new VBox(5,title,sub); box.setPadding(new Insets(0,0,16,0)); return box;
    }

    private VBox mainContent(){
        VBox content=new VBox(12,searchPanel(),resultsPanel(),bottomPanel());
        VBox.setVgrow(content,Priority.ALWAYS); return content;
    }

    private VBox searchPanel(){
        Label ql=new Label("Search topics"); ql.setStyle("-fx-font-weight:bold; -fx-text-fill:#27324a;");
        Button search=new Button("SEARCH"); search.setDefaultButton(true); search.setOnAction(e->runSearch());
        search.setStyle("-fx-font-weight:bold; -fx-padding:8 18 8 18;");
        Button clear=new Button("CLEAR"); clear.setOnAction(e->{searchField.clear();status.setText("Enter one or more topics separated by commas.");});
        HBox row=new HBox(10,searchField,search,clear); HBox.setHgrow(searchField,Priority.ALWAYS);
        Label tl=new Label("Fuzzy threshold: "+String.format(Locale.US,"%.0f%%",threshold.getValue()*100));
        threshold.valueProperty().addListener((obs,o,n)->tl.setText("Fuzzy threshold: "+String.format(Locale.US,"%.0f%%",n.doubleValue()*100)));
        threshold.setShowTickMarks(true); threshold.setShowTickLabels(true); threshold.setMajorTickUnit(.15); threshold.setBlockIncrement(.05);
        threshold.valueProperty().addListener((obs,o,n)->{if(!searchField.getText().isBlank())runSearch();});
        HBox sliderRow=new HBox(12,tl,threshold); sliderRow.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(threshold,Priority.ALWAYS);
        status.setStyle("-fx-text-fill:#52607a; -fx-font-size:12px;");
        VBox box=new VBox(7,ql,row,sliderRow,status); box.setPadding(new Insets(14)); box.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-border-color:#dfe4ee; -fx-border-radius:12;"); return box;
    }

    private VBox resultsPanel(){
        Label h=new Label("Ranked recommendations — Top 5"); h.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#16213e;");
        TableColumn<Paper,String> title=new TableColumn<>("Research Paper"); title.setCellValueFactory(new PropertyValueFactory<>("title")); title.setPrefWidth(430);
        TableColumn<Paper,String> score=new TableColumn<>("Relevance"); score.setCellValueFactory(c->new SimpleStringProperty(String.format(Locale.US,"%.1f%%",c.getValue().score*100))); score.setPrefWidth(110);
        TableColumn<Paper,String> exact=new TableColumn<>("Exact"); exact.setCellValueFactory(c->new SimpleStringProperty(String.format(Locale.US,"%.1f%%",c.getValue().exactMatch*100))); exact.setPrefWidth(90);
        TableColumn<Paper,String> fuzzy=new TableColumn<>("Fuzzy"); fuzzy.setCellValueFactory(c->new SimpleStringProperty(String.format(Locale.US,"%.1f%%",c.getValue().fuzzyMatch*100))); fuzzy.setPrefWidth(90);
        TableColumn<Paper,String> keywords=new TableColumn<>("Keywords"); keywords.setCellValueFactory(new PropertyValueFactory<>("keywords")); keywords.setPrefWidth(440);
        table.getColumns().setAll(title,score,exact,fuzzy,keywords); table.setPrefHeight(245); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getSelectionModel().selectedItemProperty().addListener((obs,o,n)->showDetails(n));
        VBox box=new VBox(8,h,table); VBox.setVgrow(table,Priority.ALWAYS); box.setPadding(new Insets(14)); box.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-border-color:#dfe4ee; -fx-border-radius:12;"); return box;
    }

    private HBox bottomPanel(){
        Label mh=new Label("Topic → paper matching"); mh.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#16213e;");
        coverageLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#246b45;");
        matchBox.getChildren().setAll(mh,coverageLabel); ScrollPane matchScroll=new ScrollPane(matchBox); matchScroll.setFitToWidth(true); matchScroll.setPrefWidth(500); matchScroll.setStyle("-fx-background:white;");
        Label dh=new Label("Field-level relevance breakdown"); dh.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#16213e;");
        detailBox.getChildren().setAll(dh,new Label("Select a recommendation above.")); ScrollPane detailScroll=new ScrollPane(detailBox); detailScroll.setFitToWidth(true); detailScroll.setPrefWidth(520);
        VBox graphBox=new VBox(8,new Label("Bipartite graph visualization"),graph); graphBox.getChildren().get(0).setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#16213e;");
        HBox.setHgrow(graphBox,Priority.ALWAYS); VBox.setVgrow(graph,Priority.ALWAYS);
        HBox box=new HBox(12,matchScroll,detailScroll,graphBox); box.setPrefHeight(400);
        box.getChildren().forEach(n->{if(n instanceof Region r) r.setStyle("-fx-background-color:white; -fx-background-radius:12; -fx-border-color:#dfe4ee; -fx-border-radius:12; -fx-padding:14;");});
        return box;
    }

    private void runSearch(){
        String raw=searchField.getText()==null?"":searchField.getText();
        topics=new ArrayList<>(); for(String t:raw.split(",")) if(!t.isBlank()) topics.add(t.trim().toLowerCase(Locale.ROOT));
        if(topics.isEmpty()){status.setText("Please enter at least one topic.");return;}
        for(Paper p:papers) Algorithms.score(p,topics);
        double th=threshold.getValue(); matching=Algorithms.maximumMatching(papers,topics,th);
        List<Paper> ranked=Algorithms.topK(papers,5);
        ObservableList<Paper> visible=FXCollections.observableArrayList(); for(Paper p:ranked) if(p.score>0) visible.add(p); table.setItems(visible);
        updateMatching(); drawGraph(); status.setText("Processed "+topics.size()+" topic(s) across "+papers.size()+" papers using the DSA pipeline.");
        if(!visible.isEmpty()) table.getSelectionModel().select(0);
    }

    private void updateMatching(){
        matchBox.getChildren().clear(); Label h=new Label("Topic → paper matching"); h.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#16213e;"); matchBox.getChildren().add(h);
        int count=0;
        for(int i=0;i<matching.length;i++) if(matching[i]>=0) count++;
        coverageLabel.setText("Coverage: "+count+"/"+topics.size()+" topics — "+(count==topics.size()?"COMPLETE":"PARTIAL"));
        matchBox.getChildren().add(coverageLabel);
        for(int t=0;t<topics.size();t++){
            int paperIndex=-1; for(int i=0;i<matching.length;i++) if(matching[i]==t){paperIndex=i;break;}
            VBox card=new VBox(3); card.setPadding(new Insets(8)); card.setStyle("-fx-background-color:#f7f9fd; -fx-background-radius:8;");
            Label topic=new Label(topics.get(t)); topic.setStyle("-fx-font-weight:bold; -fx-text-fill:#27324a;");
            if(paperIndex>=0){ Paper p=papers.get(paperIndex); double w=Algorithms.topicRelevance(p,topics.get(t)); card.getChildren().addAll(topic,new Label("↓  "+p.title),new Label(String.format(Locale.US,"Edge weight: %.1f%%",w*100))); }
            else card.getChildren().addAll(topic,new Label("↓  No distinct paper met threshold"));
            matchBox.getChildren().add(card);
        }
    }

    private void showDetails(Paper p){
        detailBox.getChildren().clear(); Label h=new Label("Field-level relevance breakdown"); h.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#16213e;"); detailBox.getChildren().add(h);
        if(p==null){detailBox.getChildren().add(new Label("Select a recommendation above."));return;}
        Label title=new Label(p.title); title.setWrapText(true); title.setStyle("-fx-font-weight:bold; -fx-font-size:15px;"); detailBox.getChildren().add(title);
        detailBox.getChildren().add(new Label(String.format(Locale.US,"Composite relevance: %.1f%%    Exact: %.1f%%    Fuzzy: %.1f%%",p.score*100,p.exactMatch*100,p.fuzzyMatch*100)));
        for(String t:topics){
            Algorithms.TopicBreakdown b=Algorithms.topicBreakdown(p,t);
            VBox card=new VBox(3); card.setPadding(new Insets(8)); card.setStyle("-fx-background-color:#f7f9fd; -fx-background-radius:8;");
            Label topic=new Label("Topic: "+t); topic.setStyle("-fx-font-weight:bold;");
            card.getChildren().addAll(topic,
                    new Label(String.format(Locale.US,"Topic score: %.1f%% | Exact: %.1f%% | Fuzzy: %.1f%%",b.score()*100,b.exact()*100,b.fuzzy()*100)),
                    new Label(String.format(Locale.US,"Title: %.0f%%   Keywords: %.0f%%   Abstract: %.0f%%",b.titleExact()*100,b.keywordExact()*100,b.abstractExact()*100)));
            detailBox.getChildren().add(card);
        }
        Label key=new Label("Keywords: "+p.keywords); key.setWrapText(true); Label abs=new Label("Abstract: "+p.abstractText); abs.setWrapText(true); Label ref=new Label("Reference: "+p.url); ref.setWrapText(true);
        detailBox.getChildren().addAll(key,abs,ref);
    }

    private void drawGraph(){
        GraphicsContext g=graph.getGraphicsContext2D(); double w=graph.getWidth(),h=graph.getHeight(); g.setFill(Color.WHITE); g.fillRect(0,0,w,h);
        if(topics.isEmpty()) return;
        int n=topics.size(); double leftX=120,rightX=w-170;
        for(int i=0;i<n;i++){
            double y=(i+1)*h/(n+1); g.setFill(Color.web("#dfe8ff")); g.fillOval(leftX-12,y-12,24,24); g.setFill(Color.web("#27324a")); g.setFont(Font.font(12)); g.fillText(topics.get(i),15,y+4);
            int paper=-1; for(int j=0;j<matching.length;j++) if(matching[j]==i){paper=j;break;}
            if(paper>=0){
                double py=(paper+1)*h/(papers.size()+1); g.setStroke(Color.web("#4c6fff")); g.setLineWidth(2.5); g.strokeLine(leftX+12,y,rightX-12,py);
            }
        }
        Set<Integer> used=new HashSet<>(); for(int x:matching) if(x>=0) used.add(x);
        int row=0; for(int i:used){double y=(i+1)*h/(papers.size()+1);g.setFill(Color.web("#e6f6ed"));g.fillOval(rightX-12,y-12,24,24);g.setFill(Color.web("#27324a"));g.fillText(papers.get(i).title,rightX+5,y+4);row++;}
        g.setFill(Color.web("#52607a"));g.fillText("Topics (U)",leftX-35,18);g.fillText("Distinct papers (V)",rightX-35,18);
    }

    public static void main(String[] args){launch(args);}
}
