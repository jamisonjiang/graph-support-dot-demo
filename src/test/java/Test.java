import com.alexmerz.graphviz.ParseException;
import java.io.IOException;
import org.graphper.DotParser;
import org.graphper.api.GraphResource;
import org.graphper.draw.ExecuteException;

public class Test {

  public static void main(String[] args) throws ExecuteException, ParseException, IOException {
    String dot = "digraph G {\n"
        + "splines=none\n"
        + "subgraph cluster_0 {\n"
        + "98\n"
        + "79\n"
        + "0\n"
        + "36\n"
        + "72\n"
        + "96\n"
        + "93\n"
        + "10\n"
        + "31\n"
        + "52\n"
        + "86\n"
        + "100\n"
        + "}\n"
        + "subgraph cluster_1 {\n"
        + "22\n"
        + "70\n"
        + "30\n"
        + "78\n"
        + "16\n"
        + "42\n"
        + "101\n"
        + "}\n"
        + "subgraph cluster_2 {\n"
        + "87\n"
        + "48\n"
        + "46\n"
        + "95\n"
        + "31\n"
        + "14\n"
        + "74\n"
        + "102\n"
        + "}\n"
        + "subgraph cluster_3 {\n"
        + "24\n"
        + "25\n"
        + "82\n"
        + "57\n"
        + "81\n"
        + "94\n"
        + "25\n"
        + "29\n"
        + "62\n"
        + "15\n"
        + "103\n"
        + "}\n"
        + "subgraph cluster_4 {\n"
        + "96\n"
        + "76\n"
        + "85\n"
        + "25\n"
        + "77\n"
        + "2\n"
        + "93\n"
        + "97\n"
        + "5\n"
        + "46\n"
        + "61\n"
        + "30\n"
        + "104\n"
        + "}\n"
        + "subgraph cluster_5 {\n"
        + "53\n"
        + "52\n"
        + "25\n"
        + "25\n"
        + "5\n"
        + "43\n"
        + "5\n"
        + "105\n"
        + "}\n"
        + "subgraph cluster_6 {\n"
        + "66\n"
        + "72\n"
        + "70\n"
        + "98\n"
        + "97\n"
        + "85\n"
        + "80\n"
        + "1\n"
        + "70\n"
        + "49\n"
        + "106\n"
        + "}\n"
        + "subgraph cluster_7 {\n"
        + "95\n"
        + "107\n"
        + "}\n"
        + "subgraph cluster_8 {\n"
        + "39\n"
        + "31\n"
        + "43\n"
        + "9\n"
        + "44\n"
        + "51\n"
        + "94\n"
        + "60\n"
        + "68\n"
        + "108\n"
        + "}\n"
        + "subgraph cluster_9 {\n"
        + "38\n"
        + "7\n"
        + "81\n"
        + "91\n"
        + "75\n"
        + "88\n"
        + "60\n"
        + "64\n"
        + "23\n"
        + "109\n"
        + "}\n"
        + "subgraph cluster_10 {\n"
        + "24\n"
        + "110\n"
        + "}\n"
        + "subgraph cluster_11 {\n"
        + "60\n"
        + "30\n"
        + "53\n"
        + "39\n"
        + "51\n"
        + "15\n"
        + "111\n"
        + "}\n"
        + "subgraph cluster_12 {\n"
        + "45\n"
        + "12\n"
        + "86\n"
        + "92\n"
        + "31\n"
        + "25\n"
        + "55\n"
        + "112\n"
        + "}\n"
        + "subgraph cluster_13 {\n"
        + "74\n"
        + "22\n"
        + "33\n"
        + "82\n"
        + "21\n"
        + "35\n"
        + "92\n"
        + "42\n"
        + "113\n"
        + "}\n"
        + "subgraph cluster_14 {\n"
        + "0\n"
        + "95\n"
        + "5\n"
        + "30\n"
        + "62\n"
        + "83\n"
        + "114\n"
        + "}\n"
        + "70->53\n"
        + "90->5\n"
        + "41->62\n"
        + "88->45\n"
        + "97->71\n"
        + "54->98\n"
        + "33->52\n"
        + "71->8\n"
        + "9->71\n"
        + "15->18\n"
        + "42->23\n"
        + "53->9\n"
        + "9->8\n"
        + "0->74\n"
        + "0->67\n"
        + "3->22\n"
        + "6->0\n"
        + "84->28\n"
        + "86->21\n"
        + "40->89\n"
        + "36->10\n"
        + "65->1\n"
        + "92->22\n"
        + "21->63\n"
        + "56->25\n"
        + "38->93\n"
        + "53->61\n"
        + "96->66\n"
        + "37->7\n"
        + "63->20\n"
        + "92->11\n"
        + "30->42\n"
        + "28->7\n"
        + "14->17\n"
        + "83->33\n"
        + "22->79\n"
        + "14->29\n"
        + "41->29\n"
        + "81->6\n"
        + "82->56\n"
        + "78->25\n"
        + "48->19\n"
        + "65->40\n"
        + "39->1\n"
        + "97->65\n"
        + "70->79\n"
        + "42->74\n"
        + "46->50\n"
        + "80->10\n"
        + "12->10\n"
        + "61->85\n"
        + "59->70\n"
        + "70->34\n"
        + "67->27\n"
        + "82->70\n"
        + "26->32\n"
        + "53->72\n"
        + "24->29\n"
        + "37->70\n"
        + "6->85\n"
        + "75->37\n"
        + "87->70\n"
        + "92->41\n"
        + "28->51\n"
        + "18->56\n"
        + "62->61\n"
        + "83->50\n"
        + "79->34\n"
        + "86->31\n"
        + "85->28\n"
        + "6->88\n"
        + "87->31\n"
        + "55->60\n"
        + "30->73\n"
        + "10->6\n"
        + "63->24\n"
        + "65->80\n"
        + "80->85\n"
        + "98->91\n"
        + "84->45\n"
        + "}\n";

//    System.setProperty("dot.coordinate.v1", "true");
    DotParser dotParser = new DotParser(dot);
    dotParser.getGraphviz().toSvg().save("./", "test");
  }
}
