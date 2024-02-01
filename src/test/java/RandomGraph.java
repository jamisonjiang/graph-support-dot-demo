import com.alexmerz.graphviz.ParseException;
import graph.GraphViz;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.graphper.DotParser;
import org.graphper.api.FileType;
import org.graphper.api.Graphviz;
import org.graphper.draw.ExecuteException;

public class RandomGraph {

  private int nodeNum;

  private int lineNum;

  private int clusterNum;

  private int clusterMaxNodeNum;

  private String graph;

  public RandomGraph(int nodeNum, int lineNum, int clusterNum, int clusterMaxNodeNum) {
    this.nodeNum = nodeNum;
    this.lineNum = lineNum;
    this.clusterNum = clusterNum;
    this.clusterMaxNodeNum = clusterMaxNodeNum;
  }

  public Graphviz generate() throws ExecuteException, ParseException, IOException {
    GraphViz gv = new GraphViz();
    gv.addln(gv.start_graph());

    Random random = new Random();
    List<String> nodes = new ArrayList<>();
    for (int i = 0; i < nodeNum; i++) {
      nodes.add(String.valueOf(random.nextInt(nodeNum * 10)));
    }

    for (int i = 0; i < clusterNum; i++) {
      gv.addln(gv.start_subgraph(i));
      gv.addln("style=\"bold\"");
      gv.addln("label=\"cluster_" + i + "\"");
      int clusterNodeNum = random.nextInt(clusterMaxNodeNum) + 1;
      for (int j = 0; j < clusterNodeNum; j++) {
        gv.addln(String.valueOf(random.nextInt(nodeNum)));
      }
      gv.addln(String.valueOf(nodeNum + i));
      gv.addln(gv.end_graph());
    }

    for (int i = 0; i < lineNum; i++) {
      int from = random.nextInt(nodeNum);
      int to = random.nextInt(nodeNum);
      gv.addln(from + "->" + to);
    }

    gv.addln(gv.end_graph());

    this.graph = gv.getDotSource();
    DotParser dotParser = new DotParser(graph);
    return dotParser.getGraphviz();
  }

  public static void main(String[] args) throws IOException {
    System.setProperty("graph.quality.check", "true");
    for (int i = 0; i < 100; i++) {
      RandomGraph randomGraph = new RandomGraph(100, 80, 15, 12);
      try {
        randomGraph.generate().toFile(FileType.PNG).save(PathHelper.path, "test_" + i);
      } catch (Exception e) {
        System.err.println(e.getMessage() + "Occurred error and save error graph");
        if (e instanceof ParseException) {
          return;
        }
        saveDot(PathHelper.errorPath, i, randomGraph);
      }
    }

  }

  private static void saveDot(String path, int i, RandomGraph randomGraph) throws IOException {
    try (FileWriter fileWriter = new FileWriter(path + System.currentTimeMillis() + "_" + i + ".dot")) {
      fileWriter.append(randomGraph.graph);
    }
  }
}
