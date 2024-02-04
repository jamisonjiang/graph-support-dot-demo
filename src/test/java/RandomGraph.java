import com.alexmerz.graphviz.ParseException;
import graph.GraphViz;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import org.graphper.DotParser;
import org.graphper.api.FileType;
import org.graphper.api.Graphviz;
import org.graphper.draw.ExecuteException;

public class RandomGraph {

  private int nodeNum;

  private int lineNum;

  private int clusterNum;

  private int clusterMaxNodeNum;

  private int clusterMaxNestDeep;

  private String graph;

  public RandomGraph(int nodeNum, int lineNum, int clusterNum, int clusterMaxNodeNum,
                     int clusterMaxNestDeep) {
    this.nodeNum = nodeNum;
    this.lineNum = lineNum;
    this.clusterNum = clusterNum;
    this.clusterMaxNodeNum = clusterMaxNodeNum;
    this.clusterMaxNestDeep = clusterMaxNestDeep;
  }

  public Graphviz generate() throws ExecuteException, ParseException, IOException {
    GraphViz gv = new GraphViz();
    gv.addln(gv.start_graph());

    Random random = new Random();

    AtomicInteger clusterNo = new AtomicInteger();
    while (clusterNo.get() < clusterNum) {
      clusterNo.incrementAndGet();
      nestCluster(clusterNo, 0, gv, random);
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

  private void nestCluster(AtomicInteger clusterNo, int deep, GraphViz gv, Random random) {
    if (deep > clusterMaxNestDeep) {
      return;
    }

    gv.addln(gv.start_subgraph(clusterNo.get()));
    gv.addln("style=\"bold\"");
    gv.addln("label=\"cluster_" + clusterNo.get() + "\"");
    int clusterNodeNum = random.nextInt(clusterMaxNodeNum) + 1;
    for (int j = 0; j < clusterNodeNum; j++) {
      gv.addln(String.valueOf(random.nextInt(nodeNum)));
    }

    if (random.nextInt() % 2 == 0) {
      clusterNo.incrementAndGet();
      nestCluster(clusterNo, deep + 1, gv, random);
    }

    gv.addln(String.valueOf(nodeNum + clusterNodeNum));
    gv.addln(gv.end_graph());
  }

  public static void main(String[] args) throws IOException {
    System.setProperty("graph.quality.check", "true");
    for (int i = 0; i < 100; i++) {
      RandomGraph randomGraph = new RandomGraph(150, 130, 30, 25, 6);
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
