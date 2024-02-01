import com.alexmerz.graphviz.ParseException;
import java.io.IOException;
import java.util.List;
import org.graphper.DotParser;
import org.graphper.draw.ExecuteException;

public class ErrorDotCheck {

  public static void main(String[] args) throws ExecuteException, ParseException, IOException {

    List<String> errorDot = PathHelper.getErrorDots();
    if (errorDot == null) {
      return;
    }

    System.setProperty("graph.quality.check", "true");
    for (String dot : errorDot) {
      DotParser dotParser = new DotParser(dot);
      dotParser.getGraphviz().toSvg().save("./", "test");
    }
  }
}
