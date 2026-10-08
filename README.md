# Smart Research Paper Recommendation System

Complete reviewer-ready implementation based on the Review 3 DSA pipeline.

## Included
- JavaFX reviewer GUI
- Search bar for comma-separated topics
- Dynamic fuzzy-threshold slider
- Ranked Top-5 TableView
- Weighted DFS/augmenting-path bipartite matching
- Topic-to-paper matching panel
- Interactive Canvas graph visualization of topic -> distinct paper assignments
- Field-level relevance breakdown for title, keywords and abstract
- Rabin-Karp exact matching
- Levenshtein dynamic programming fuzzy matching
- 70% exact + 30% fuzzy composite scoring
- PriorityQueue Top-K ranking
- CSV dataset with 12 research papers
- Console implementation retained for DSA demonstration

## Requirements
- JDK 21 or newer (JDK 24 is supported)
- Maven 3.9+
- Internet access on first Maven run so JavaFX dependencies can be downloaded

## Recommended run
Double-click `run-gui.bat`, or run:

```powershell
mvn clean javafx:run
```

## Demo query
```text
machine learning, recommendation systems, graph algorithms
```

## Typo-tolerance demo
```text
recomender systems
```

## What to show the reviewer
1. Enter the multi-topic query and click SEARCH.
2. Change the fuzzy threshold slider and observe the ranked results update.
3. Select a paper in the TableView to show field-level relevance.
4. Use the Topic -> Paper panel to show distinct assignments and edge weights.
5. Use the graph panel to explain the bipartite matching result.
6. Mention that ranking uses PriorityQueue and matching uses DFS augmenting paths.
