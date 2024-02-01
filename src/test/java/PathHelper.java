import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class PathHelper {

  public static final String path = "./src/test/result/";
  public static final String errorPath = "./src/test/error/";

  public static List<String> getErrorDots() throws IOException {
    File file = new File(errorPath);
    if (!file.isDirectory()) {
      return null;
    }

    File[] files = file.listFiles();
    if (files == null) {
      return null;
    }

    List<String> dots = new ArrayList<>(files.length);
    for (File dot : files) {
      StringBuilder sb = new StringBuilder();

      try(BufferedReader br = new BufferedReader(new FileReader(dot))) {
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line).append("\n");
        }
      }
      dots.add(sb.toString());
    }

    return dots;
  }
}
