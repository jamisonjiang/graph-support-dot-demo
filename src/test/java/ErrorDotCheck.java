import com.alexmerz.graphviz.ParseException;
import java.io.IOException;
import java.util.List;
import org.graphper.DotParser;
import org.graphper.draw.ExecuteException;

public class ErrorDotCheck {

  public static void main(String[] args) throws IOException {

    List<String> errorDot = PathHelper.getErrorDots();
    if (errorDot == null) {
      return;
    }

    System.setProperty("graph.quality.check", "true");
    for (int i = 0; i < errorDot.size(); i++) {
      try {
        DotParser dotParser = new DotParser(errorDot.get(i));
        dotParser.getGraphviz().toSvg().save("./", "test");
//        if (i == 0) {
//          break;
//        }
      } catch (Exception e) {
        System.out.println("Index " + i);
        e.printStackTrace();
        break;
      }
    }
  }
}
