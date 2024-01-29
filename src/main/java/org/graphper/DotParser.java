package org.graphper;

import com.alexmerz.graphviz.ParseException;
import com.alexmerz.graphviz.Parser;
import com.alexmerz.graphviz.objects.Edge;
import com.alexmerz.graphviz.objects.Graph;
import com.alexmerz.graphviz.objects.Id;
import com.alexmerz.graphviz.objects.PortNode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache_gs.commons.lang3.StringUtils;
import org.graphper.api.Cluster;
import org.graphper.api.Cluster.ClusterBuilder;
import org.graphper.api.FileType;
import org.graphper.api.FloatLabel;
import org.graphper.api.GraphContainer;
import org.graphper.api.GraphContainer.GraphContainerBuilder;
import org.graphper.api.GraphResource;
import org.graphper.api.Graphviz;
import org.graphper.api.Graphviz.GraphvizBuilder;
import org.graphper.api.Html;
import org.graphper.api.Html.Table;
import org.graphper.api.Html.Td;
import org.graphper.api.Line;
import org.graphper.api.Line.LineBuilder;
import org.graphper.api.Node;
import org.graphper.api.Node.NodeBuilder;
import org.graphper.api.Subgraph;
import org.graphper.api.Subgraph.SubgraphBuilder;
import org.graphper.api.attributes.ArrowShape;
import org.graphper.api.attributes.ClusterShape;
import org.graphper.api.attributes.ClusterShapeEnum;
import org.graphper.api.attributes.ClusterStyle;
import org.graphper.api.attributes.Color;
import org.graphper.api.attributes.Dir;
import org.graphper.api.attributes.Labeljust;
import org.graphper.api.attributes.Labelloc;
import org.graphper.api.attributes.LineStyle;
import org.graphper.api.attributes.NodeShapeEnum;
import org.graphper.api.attributes.NodeStyle;
import org.graphper.api.attributes.Port;
import org.graphper.api.attributes.Rank;
import org.graphper.api.attributes.Rankdir;
import org.graphper.api.attributes.Splines;
import org.graphper.api.attributes.Tend;
import org.graphper.draw.ExecuteException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DotParser {

  private static final Map<String, Color> COLOR_CACHE;

  private static final Pattern htmlPattern = Pattern.compile(
      "<(\\n)*<table(.|\\n)*?</table>(\\n)*>");

  static {
    Class<Color> clazz = Color.class;
    Map<String, Color> colorCache = new HashMap<>();
    for (Field field : clazz.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      try {
        field.setAccessible(true);
        Object value = field.get(clazz);
        if (!(value instanceof Color)) {
          continue;
        }
        colorCache.put(field.getName().toLowerCase(), (Color) value);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
    colorCache.put("bisque", Color.ofRGB("#ffe4c4"));
    colorCache.put("lightgrey", Color.ofRGB("#d3d3d3"));
    colorCache.put("lightblue", Color.ofRGB("#ffff00"));
    colorCache.put("chartreuse", Color.ofRGB("#7fff00"));
    COLOR_CACHE = Collections.unmodifiableMap(colorCache);
  }

  private Graphviz graphviz;

  private GraphResource graphResource;

  private Map<String, String> tableMap = new HashMap<>();

  public DotParser(String dot) throws ExecuteException, ParseException, IOException {
    graphResource = toSvg(dot);
  }

  public GraphResource getGraphResource() {
    return graphResource;
  }

  public Graphviz getGraphviz() {
    return graphviz;
  }

  public GraphResource toSvg(String dot)
      throws ParseException, IOException, ExecuteException {
    StringBuffer sb = preHandle(dot);

    Parser p = new Parser();
    p.parse(sb);
    ArrayList<Graph> graphs = p.getGraphs();
    Graph root = null;
    for (Graph graph : p.getGraphs()) {
      root = graph;
      break;
    }
    if (root == null) {
      return null;
    }

    Map<String, Node> nodeRecord = new HashMap<>();
    GraphvizBuilder graphvizBuilder;
    if (root.getType() == Graph.DIRECTED) {
      graphvizBuilder = Graphviz.digraph();
    } else {
      graphvizBuilder = Graphviz.graph();
    }
    setGraphvizAttr(graphvizBuilder, root);
    for (Graph g : graphs) {
      setNodesAndEdges(nodeRecord, graphvizBuilder, g);
      setSubgraph(nodeRecord, graphvizBuilder, g);
    }

    graphviz = graphvizBuilder.build();
    return graphviz.toFile(FileType.PNG);
  }

  private StringBuffer preHandle(String dot) {
    String s = dot.toLowerCase().replaceAll("\r\n", "\n");
    Matcher m = htmlPattern.matcher(s);

    int id = 0;
    while (m.find()) {

      // get the matching group
      String table = m.group();
      String tableId = "replace_table_" + id++;
      s = s.replaceAll(Pattern.quote(table), "\"" + tableId + "\"");

      table = table.trim();
      table = table.replaceAll("\n", "");
      table = table.substring(1);
      table = table.substring(0, table.length() - 1);
      tableMap.put(tableId.replaceAll("\"", "").replaceAll("\\\\", ""), table);
    }

    return new StringBuffer(s);
  }

  private String tableHandle(String label, Consumer<Table> tableConsumer) {
    String table = tableMap.get(label);
    if (table == null) {
      return label;
    }

    try {
      ByteArrayInputStream is = new ByteArrayInputStream(table.getBytes());
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document document = db.parse(is);
      parseTable(tableConsumer, document.getChildNodes());
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private static void parseTable(Consumer<Table> tableConsumer, NodeList html) {
    for (int i = 0; i < html.getLength(); i++) {
      org.w3c.dom.Node t = html.item(i);
      if (!"table".equalsIgnoreCase(t.getNodeName())) {
        continue;
      }
      Table htmlTable = Html.table();
      NodeList childNodes = t.getChildNodes();

      NamedNodeMap attributes = t.getAttributes();
      if (attributes != null) {
        org.w3c.dom.Node cellspacing = attributes.getNamedItem("cellspacing");
        if (cellspacing != null) {
          htmlTable.cellSpacing(Integer.parseInt(cellspacing.getNodeValue()));
        }
        org.w3c.dom.Node cellpadding = attributes.getNamedItem("cellpadding");
        if (cellpadding != null) {
          htmlTable.cellPadding(Integer.parseInt(cellpadding.getNodeValue()));
        }
        org.w3c.dom.Node border = attributes.getNamedItem("border");
        if (border != null) {
          htmlTable.border(Integer.parseInt(border.getNodeValue()));
        }
        org.w3c.dom.Node size = attributes.getNamedItem("width");
        if (size != null) {
          htmlTable.width(Double.parseDouble(size.getNodeValue()));
        }
        size = attributes.getNamedItem("height");
        if (size != null) {
          htmlTable.height(Double.parseDouble(size.getNodeValue()));
        }
        org.w3c.dom.Node color = attributes.getNamedItem("color");
        if (color != null) {
          setColor(color::getNodeValue, htmlTable::color);
        }
        color = attributes.getNamedItem("bgcolor");
        if (color != null) {
          setColor(color::getNodeValue, htmlTable::bgColor);
        }
      }

      for (int j = 0; j < childNodes.getLength(); j++) {
        org.w3c.dom.Node tr = childNodes.item(j);

        String nodeName = tr.getNodeName();
        if (!"tr".equalsIgnoreCase(nodeName)) {
          continue;
        }

        List<Td> htmlTds = new ArrayList<>();
        NodeList tds = tr.getChildNodes();
        for (int k = 0; k < tds.getLength(); k++) {
          org.w3c.dom.Node td = tds.item(k);
          if (!"td".equalsIgnoreCase(td.getNodeName())) {
            continue;
          }

          Td htmlTd = Html.td();
          htmlTds.add(htmlTd);
          NodeList childTableNodes = td.getChildNodes();
          if (childTableNodes.getLength() > 0) {
            parseTable(htmlTd::table, childTableNodes);
          }

          if (htmlTd.getTable() == null) {
            String text = td.getTextContent();
            if (text != null) {
              htmlTd.text(text);
            }
          }

          attributes = td.getAttributes();
          if (attributes != null) {
            org.w3c.dom.Node rowspan = attributes.getNamedItem("rowspan");
            if (rowspan != null) {
              htmlTd.rowSpan(Integer.parseInt(rowspan.getNodeValue()));
            }
            org.w3c.dom.Node colspan = attributes.getNamedItem("colspan");
            if (colspan != null) {
              htmlTd.colSpan(Integer.parseInt(colspan.getNodeValue()));
            }
            org.w3c.dom.Node port = attributes.getNamedItem("port");
            if (port != null) {
              htmlTd.id(port.getNodeValue());
            }
            org.w3c.dom.Node size = attributes.getNamedItem("width");
            if (size != null) {
              htmlTd.width(Double.parseDouble(size.getNodeValue()));
            }
            size = attributes.getNamedItem("height");
            if (size != null) {
              htmlTd.height(Double.parseDouble(size.getNodeValue()));
            }
            org.w3c.dom.Node color = attributes.getNamedItem("color");
            if (color != null) {
              setColor(color::getNodeValue, htmlTd::color);
            }
            color = attributes.getNamedItem("bgcolor");
            if (color != null) {
              setColor(color::getNodeValue, htmlTd::bgColor);
            }
            color = attributes.getNamedItem("fontcolor");
            if (color != null) {
              setColor(color::getNodeValue, htmlTd::fontColor);
            }
            org.w3c.dom.Node fontname = attributes.getNamedItem("fontname");
            if (fontname != null) {
              htmlTd.fontName(fontname.getNodeValue());
            }
            org.w3c.dom.Node shape = attributes.getNamedItem("shape");
            if (shape != null) {
              NodeShapeEnum s = null;
              for (NodeShapeEnum nodeShapeEnum : NodeShapeEnum.values()) {
                if (nodeShapeEnum.getName().equalsIgnoreCase(shape.getNodeValue())) {
                  s = nodeShapeEnum;
                  break;
                }
              }
              if (s != null) {
                htmlTd.shape(s);
              }
            }
            setAlign(attributes, htmlTd);
          }
        }

        htmlTable.tr(htmlTds.toArray(new Td[0]));
      }

      tableConsumer.accept(htmlTable);
    }
  }

  private static void setAlign(NamedNodeMap attributes, Td htmlTd) {
    org.w3c.dom.Node valign = attributes.getNamedItem("valign");
    if (valign != null) {
      String labelloc = valign.getNodeValue();
      if ("t".equalsIgnoreCase(labelloc) || "top".equalsIgnoreCase(labelloc)) {
        htmlTd.valign(Labelloc.TOP);
      }
      if ("c".equalsIgnoreCase(labelloc) || "center".equalsIgnoreCase(labelloc)) {
        htmlTd.valign(Labelloc.CENTER);
      }
      if ("b".equalsIgnoreCase(labelloc) || "bottom".equalsIgnoreCase(labelloc)) {
        htmlTd.valign(Labelloc.BOTTOM);
      }
    }
    org.w3c.dom.Node align = attributes.getNamedItem("align");
    if (align != null) {
      String labeljust = align.getNodeValue();
      if (StringUtils.isNotEmpty(labeljust)) {
        if ("l".equalsIgnoreCase(labeljust) || "left".equalsIgnoreCase(labeljust)) {
          htmlTd.align(Labeljust.LEFT);
        }
        if ("c".equalsIgnoreCase(labeljust) || "center".equalsIgnoreCase(labeljust)) {
          htmlTd.align(Labeljust.CENTER);
        }
        if ("r".equalsIgnoreCase(labeljust) || "right".equalsIgnoreCase(labeljust)) {
          htmlTd.align(Labeljust.RIGHT);
        }
      }
    }
  }

  private void setNodesAndEdges(Map<String, Node> nodeRecord,
                                GraphContainerBuilder graphContainerBuilder, Graph g) {
    for (com.alexmerz.graphviz.objects.Node node : g.getNodes(false)) {
      addNode(nodeRecord, graphContainerBuilder, node);
    }

    for (Edge edge : g.getEdges()) {
      PortNode source = edge.getSource();
      PortNode target = edge.getTarget();

      Node n = addNode(nodeRecord, graphContainerBuilder, source.getNode());
      Node w = addNode(nodeRecord, graphContainerBuilder, target.getNode());
      if (n == null || w == null) {
        continue;
      }

      LineBuilder builder = Line.builder(n, w);
      setLineAttrs(edge::getAttribute, builder);
      graphContainerBuilder.addLine(builder.build());
    }
  }

  private void setLineAttrs(Function<String, String> attrFunc, LineBuilder builder) {
    String label = attrFunc.apply("label");
    if (label != null) {
      label = tableHandle(label, builder::table);
      builder.label(label);
    }
    String style =  attrFunc.apply("style");
    if (style != null) {
      String[] styles = style.split(",");
      LineStyle[] lineStyles = new LineStyle[styles.length];
      for (int i = 0; i < styles.length; i++) {
        LineStyle ls = null;
        for (LineStyle value : LineStyle.values()) {
          if (value.name().equalsIgnoreCase(styles[i])) {
            ls = value;
            break;
          }
        }
        lineStyles[i] = ls;
      }
      builder.style(lineStyles);
    }

    String tailPort = attrFunc.apply("tailport");
    if (tailPort != null) {
      builder.tailPort(Port.valueOfCode(tailPort));
    }
    String headPort = attrFunc.apply("headport");
    if (headPort != null) {
      builder.headPort(Port.valueOfCode(headPort));
    }
    String penwidth = attrFunc.apply("penwidth");
    if (penwidth != null) {
      builder.penWidth(Double.parseDouble(penwidth));
    }
    builder.tailCell(attrFunc.apply("tailcell"));
    builder.headCell(attrFunc.apply("headcell"));
    builder.controlPoints(
        Boolean.TRUE.toString().equalsIgnoreCase(attrFunc.apply("controlPoints")));
    builder.showboxes(Boolean.TRUE.toString().equalsIgnoreCase(attrFunc.apply("showboxes")));
    String tailclip = attrFunc.apply("tailclip");
    if (tailclip != null) {
      builder.tailclip(Boolean.TRUE.toString().equalsIgnoreCase(tailclip));
    }
    String headclip = attrFunc.apply("headclip");
    if (headclip != null) {
      builder.headclip(Boolean.TRUE.toString().equalsIgnoreCase(headclip));
    }

    String radian = attrFunc.apply("radian");
    if (radian != null) {
      try {
        builder.radian(Double.parseDouble(radian));
      } catch (NumberFormatException e) {
      }
    }
    String weight = attrFunc.apply("weight");
    if (weight != null) {
      builder.weight(Double.parseDouble(weight));
    }
    builder.ltail(attrFunc.apply("ltail"));
    builder.lhead(attrFunc.apply("lhead"));
    String minlen = attrFunc.apply("minlen");
    if (minlen != null) {
      try {
        builder.minlen(Integer.parseInt(minlen));
      } catch (NumberFormatException e) {
      }
    }

    setColor(() ->  attrFunc.apply("color"), builder::color);
    setColor(() ->  attrFunc.apply("fontcolor"), builder::fontColor);
    builder.fontName(attrFunc.apply("fontname"));

    String dir = attrFunc.apply("dir");
    if (dir != null) {
      for (Dir d : Dir.values()) {
        if (d.name().equalsIgnoreCase(dir)) {
          builder.dir(d);
          break;
        }
      }
    }

    setArrowShape(attrFunc.apply("arrowtail"), builder::arrowTail);
    setArrowShape(attrFunc.apply("arrowhead"), builder::arrowHead);
    String arrowsize = attrFunc.apply("arrowsize");
    if (arrowsize != null) {
      builder.arrowSize(Double.parseDouble(arrowsize));
    }
    String fontsize = attrFunc.apply("fontsize");
    if (fontsize != null) {
      builder.fontSize(Double.parseDouble(fontsize));
    }

    List<FloatLabel> floatLabels = null;
    String taillabel = attrFunc.apply("taillabel");
    if (taillabel != null) {
      floatLabels = new ArrayList<>();
      floatLabels.add(FloatLabel.builder()
                          .label(taillabel)
                          .tend(Tend.TAIL)
                          .build());
    }
    String headlabel = attrFunc.apply("headlabel");
    if (headlabel != null) {
      if (floatLabels == null) {
        floatLabels = new ArrayList<>();
      }
      floatLabels.add(FloatLabel.builder()
                          .label(headlabel)
                          .tend(Tend.HEAD)
                          .build());
    }
    if (floatLabels != null) {
      builder.floatLabels(floatLabels.toArray(new FloatLabel[0]));
    }
  }

  private void setArrowShape(String arrowShape, Consumer<ArrowShape> shapeConsumer) {
    if (arrowShape == null) {
      return;
    }

    for (ArrowShape shape : ArrowShape.values()) {
      if (shape.name().equalsIgnoreCase(arrowShape)) {
        shapeConsumer.accept(shape);
        break;
      }
    }
  }

  private Node addNode(Map<String, Node> nodeRecord,
                       GraphContainerBuilder graphContainerBuilder,
                       com.alexmerz.graphviz.objects.Node node) {
    if (node.isSubgraph()) {
      return null;
    }

    Id id = node.getId();
    if (id == null) {
      return null;
    }
    String idStr = StringUtils.isEmpty(id.getId()) ? id.getLabel() : id.getId();

    if (idStr.startsWith("templine")) {
      LineBuilder lineBuilder = Line.tempLine();
      setLineAttrs(node::getAttribute, lineBuilder);
      graphContainerBuilder.tempLine(lineBuilder.build());
      return null;
    }

    Node n = nodeRecord.get(idStr);
    if (n != null) {
      graphContainerBuilder.addNode(n);
      return n;
    }

    NodeBuilder builder = Node.builder();
    String label = node.getAttribute("label");
    if (label != null) {
      label = tableHandle(label, builder::table);
      builder.label(label);
    } else {
      builder.label(idStr);
    }
    String labelloc = node.getAttribute("labelloc");
    if (StringUtils.isNotEmpty(labelloc)) {
      if ("t".equalsIgnoreCase(labelloc) || "top".equalsIgnoreCase(labelloc)) {
        builder.labelloc(Labelloc.TOP);
      }
      if ("c".equalsIgnoreCase(labelloc) || "center".equalsIgnoreCase(labelloc)) {
        builder.labelloc(Labelloc.CENTER);
      }
      if ("b".equalsIgnoreCase(labelloc) || "bottom".equalsIgnoreCase(labelloc)) {
        builder.labelloc(Labelloc.BOTTOM);
      }
    }
    String labeljust = node.getAttribute("labeljust");
    if (StringUtils.isNotEmpty(labeljust)) {
      if ("l".equalsIgnoreCase(labeljust) || "left".equalsIgnoreCase(labeljust)) {
        builder.labeljust(Labeljust.LEFT);
      }
      if ("c".equalsIgnoreCase(labeljust) || "center".equalsIgnoreCase(labeljust)) {
        builder.labeljust(Labeljust.CENTER);
      }
      if ("r".equalsIgnoreCase(labeljust) || "right".equalsIgnoreCase(labeljust)) {
        builder.labeljust(Labeljust.RIGHT);
      }
    }

    String shape = node.getAttribute("shape");
    if (StringUtils.isNotEmpty(shape)) {
      NodeShapeEnum s = null;
      for (NodeShapeEnum nodeShapeEnum : NodeShapeEnum.values()) {
        if (nodeShapeEnum.getName().equalsIgnoreCase(shape)) {
          s = nodeShapeEnum;
          break;
        }
      }
      if (s != null) {
        builder.shape(s);
      }
    }
    String fontsize = node.getAttribute("fontsize");
    if (fontsize != null) {
      builder.fontSize(Double.parseDouble(fontsize));
    }
    String margin = node.getAttribute("margin");
    if (margin != null) {
      String[] hw = margin.split(",");
      try {
        if (hw.length == 1) {
          builder.margin(Double.parseDouble(hw[0]));
        }
        if (hw.length == 2) {
          builder.margin(Double.parseDouble(hw[0]), Double.parseDouble(hw[1]));
        }
      } catch (NumberFormatException e) {
      }
    }
    String height = node.getAttribute("height");
    if (height != null) {
      builder.height(Double.parseDouble(height));
    }
    String width = node.getAttribute("width");
    if (width != null) {
      builder.width(Double.parseDouble(width));
    }
    String fixedsize = node.getAttribute("fixedsize");
    if (fixedsize != null) {
      builder.fixedSize(Boolean.parseBoolean(fixedsize));
    }
    String sides = node.getAttribute("sides");
    if (sides != null) {
      builder.sides(Integer.parseInt(sides));
    }
    String penwidth = node.getAttribute("penwidth");
    if (penwidth != null) {
      builder.penWidth(Double.parseDouble(penwidth));
    }

    setColor(() ->  node.getAttribute("color"), builder::color);
    setColor(() ->  node.getAttribute("fillcolor"), builder::fillColor);
    setColor(() ->  node.getAttribute("fontcolor"), builder::fontColor);
    builder.fontName(node.getAttribute("fontname"));

    String style = node.getAttribute("style");
    if (style != null) {
      String[] styles = style.split(",");
      NodeStyle[] nodeStyles = new NodeStyle[styles.length];
      for (int i = 0; i < styles.length; i++) {
        NodeStyle ns = null;
        for (NodeStyle value : NodeStyle.values()) {
          if (value.name().equalsIgnoreCase(styles[i])) {
            ns = value;
            break;
          }
        }
        nodeStyles[i] = ns;
      }
      builder.style(nodeStyles);
    }

    n = builder.build();
    if (idStr.startsWith("tempnode")) {
      graphContainerBuilder.tempNode(n);
    } else {
      nodeRecord.put(idStr, n);
      graphContainerBuilder.addNode(n);
    }
    return n;
  }

  private void setGraphvizAttr(GraphvizBuilder graphvizBuilder, Graph graph) {
    String rankdir = graph.getAttribute("rankdir");
    if (StringUtils.isNotEmpty(rankdir)) {
      Rankdir rd = Rankdir.valueOf(rankdir.toUpperCase());
      graphvizBuilder.rankdir(rd);
    }
    String label = graph.getAttribute("label");
    if (StringUtils.isNotEmpty(label)) {
      label = tableHandle(label, graphvizBuilder::table);
      graphvizBuilder.label(label);
    }

    String fontsize = graph.getAttribute("fontsize");
    if (fontsize != null) {
      graphvizBuilder.fontSize(Double.parseDouble(fontsize));
    }
    String labelloc = graph.getAttribute("labelloc");
    if (StringUtils.isNotEmpty(labelloc)) {
      if ("t".equalsIgnoreCase(labelloc) || "top".equalsIgnoreCase(labelloc)) {
        graphvizBuilder.labelloc(Labelloc.TOP);
      }
      if ("c".equalsIgnoreCase(labelloc) || "center".equalsIgnoreCase(labelloc)) {
        graphvizBuilder.labelloc(Labelloc.CENTER);
      }
      if ("b".equalsIgnoreCase(labelloc) || "bottom".equalsIgnoreCase(labelloc)) {
        graphvizBuilder.labelloc(Labelloc.BOTTOM);
      }
    }
    String labeljust = graph.getAttribute("labeljust");
    if (StringUtils.isNotEmpty(labeljust)) {
      if ("l".equalsIgnoreCase(labeljust) || "left".equalsIgnoreCase(labeljust)) {
        graphvizBuilder.labeljust(Labeljust.LEFT);
      }
      if ("c".equalsIgnoreCase(labeljust) || "center".equalsIgnoreCase(labeljust)) {
        graphvizBuilder.labeljust(Labeljust.CENTER);
      }
      if ("r".equalsIgnoreCase(labeljust) || "right".equalsIgnoreCase(labeljust)) {
        graphvizBuilder.labeljust(Labeljust.RIGHT);
      }
    }
    String splines = graph.getAttribute("splines");
    if (splines != null) {
      for (Splines s : Splines.values()) {
        if (s.name().equalsIgnoreCase(splines)) {
          graphvizBuilder.splines(s);
          break;
        }
      }
    }
    String compound = graph.getAttribute("compound");
    if (Boolean.TRUE.toString().equalsIgnoreCase(compound)) {
      graphvizBuilder.compound(true);
    }

    String size = graph.getAttribute("size");
    if (size != null) {
      String[] hw = size.split(",");
      try {
        if (hw.length == 1) {
          graphvizBuilder.scale(Double.parseDouble(hw[0]) / 10);
        }
        if (hw.length == 2) {
          graphvizBuilder.scale(Double.parseDouble(hw[0]) / 10, Double.parseDouble(hw[1]) / 10);
        }
      } catch (NumberFormatException e) {
      }
    }
    String margin = graph.getAttribute("margin");
    if (margin != null) {
      String[] hw = margin.split(",");
      try {
        if (hw.length == 1) {
          graphvizBuilder.margin(Double.parseDouble(hw[0]));
        }
        if (hw.length == 2) {
          graphvizBuilder.margin(Double.parseDouble(hw[0]), Double.parseDouble(hw[1]));
        }
      } catch (NumberFormatException e) {
      }
    }

    String ranksep = graph.getAttribute("ranksep");
    if (ranksep != null) {
      try {
        graphvizBuilder.rankSep(Double.parseDouble(ranksep));
      } catch (NumberFormatException e) {
      }
    }
    String nodesep = graph.getAttribute("nodesep");
    if (nodesep != null) {
      graphvizBuilder.nodeSep(Double.parseDouble(nodesep));
    }
    String nslimit = graph.getAttribute("nslimit");
    if (nslimit != null) {
      graphvizBuilder.nslimit(Integer.parseInt(nslimit));
    }
    String nslimit1 = graph.getAttribute("nslimit1");
    if (nslimit1 != null) {
      graphvizBuilder.nslimit1(Integer.parseInt(nslimit1));
    }
    String mclimit = graph.getAttribute("mslimit");
    if (mclimit != null) {
      graphvizBuilder.mclimit(Integer.parseInt(mclimit));
    }

    String showgrid = graph.getAttribute("showgrid");
    if (showgrid != null && Boolean.TRUE.toString().equalsIgnoreCase(showgrid)) {
      graphvizBuilder.showGrid(true);
    }

    setColor(() ->  graph.getAttribute("bgcolor"), graphvizBuilder::bgColor);
    setColor(() ->  graph.getAttribute("fontcolor"), graphvizBuilder::fontColor);
  }

  private void setSubgraph(Map<String, Node> nodeRecord,
                           GraphContainerBuilder containerBuilder, Graph graph) {

    for (Graph subgraph : graph.getSubgraphs()) {
      Id id = subgraph.getId();
      if (id == null) {
        continue;
      }
      GraphContainerBuilder c;
      if (StringUtils.isEmpty(id.getId())) {
        SubgraphBuilder s = Subgraph.builder();
        String r = subgraph.getAttribute("rank");
        if (r != null) {
          try {
            Rank rank = Rank.valueOf(r.toUpperCase());
            s.rank(rank);
          } catch (IllegalArgumentException e) {
            e.printStackTrace();
          }
        }
        c = s;
      } else {
        ClusterBuilder cluster = Cluster.builder();
        cluster.id(id.getId());
        String label = subgraph.getAttribute("label");
        if (label != null) {
          label = tableHandle(label, cluster::table);
          cluster.label(label);
        }
        String fontsize = graph.getAttribute("fontsize");
        if (fontsize != null) {
          cluster.fontSize(Double.parseDouble(fontsize));
        }
        String penWidth = subgraph.getAttribute("penwidth");
        if (penWidth != null) {
          try {
            cluster.penWidth(Double.parseDouble(penWidth));
          } catch (NumberFormatException e) {
          }
        }

        String style = subgraph.getAttribute("style");
        if (style != null) {
          String[] styles = style.split(",");
          ClusterStyle[] clusterStyles = new ClusterStyle[styles.length];
          for (int i = 0; i < styles.length; i++) {
            ClusterStyle cs = null;
            for (ClusterStyle value : ClusterStyle.values()) {
              if (value.name().equalsIgnoreCase(styles[i])) {
                cs = value;
                break;
              }
            }
            clusterStyles[i] = cs;
          }
          cluster.style(clusterStyles);
        }
        String shape = subgraph.getAttribute("shape");
        for (ClusterShapeEnum clusterShape : ClusterShapeEnum.values()) {
          if (clusterShape.getName().equalsIgnoreCase(shape)) {
            cluster.shape(clusterShape);
            break;
          }
        }

        String margin = subgraph.getAttribute("margin");
        if (margin != null) {
          String[] hw = margin.split(",");
          try {
            if (hw.length == 1) {
              cluster.margin(Double.parseDouble(hw[0]));
            }
            if (hw.length == 2) {
              cluster.margin(Double.parseDouble(hw[0]), Double.parseDouble(hw[1]));
            }
          } catch (NumberFormatException e) {
          }
        }

        String labelloc = subgraph.getAttribute("labelloc");
        if (StringUtils.isNotEmpty(labelloc)) {
          if ("t".equalsIgnoreCase(labelloc) || "top".equalsIgnoreCase(labelloc)) {
            cluster.labelloc(Labelloc.TOP);
          }
          if ("c".equalsIgnoreCase(labelloc) || "center".equalsIgnoreCase(labelloc)) {
            cluster.labelloc(Labelloc.CENTER);
          }
          if ("b".equalsIgnoreCase(labelloc) || "bottom".equalsIgnoreCase(labelloc)) {
            cluster.labelloc(Labelloc.BOTTOM);
          }
        }
        String labeljust = subgraph.getAttribute("labeljust");
        if (StringUtils.isNotEmpty(labeljust)) {
          if ("l".equalsIgnoreCase(labeljust) || "left".equalsIgnoreCase(labeljust)) {
            cluster.labeljust(Labeljust.LEFT);
          }
          if ("c".equalsIgnoreCase(labeljust) || "center".equalsIgnoreCase(labeljust)) {
            cluster.labeljust(Labeljust.CENTER);
          }
          if ("r".equalsIgnoreCase(labeljust) || "right".equalsIgnoreCase(labeljust)) {
            cluster.labeljust(Labeljust.RIGHT);
          }
        }
        setColor(() ->  subgraph.getAttribute("color"), cluster::color);
        setColor(() ->  subgraph.getAttribute("bgcolor"), cluster::bgColor);
        setColor(() ->  subgraph.getAttribute("fontcolor"), cluster::fontColor);
        c = cluster;
      }

      setNodesAndEdges(nodeRecord, c, subgraph);
      setSubgraph(nodeRecord, c, subgraph);

      GraphContainer sub = c.build();
      if (sub.isSubgraph()) {
        containerBuilder.subgraph((Subgraph) sub);
      }
      if (sub.isCluster()) {
        containerBuilder.cluster((Cluster) sub);
      }
    }
  }

  private static void setColor(Supplier<String> getColor, Consumer<Color> setColor) {
    String color = getColor.get();
    if (color != null) {
      setColor.accept(getColor(color));
    }
  }

  private static Color getColor(String color) {
    if (color == null) {
      return Color.BLACK;
    }
    Color c = COLOR_CACHE.get(color.toLowerCase());
    if (c == null) {
      try {
        c = Color.ofRGB(color);
      } catch (Exception e) {
      }
    }

    return c == null ? Color.BLACK : c;
  }
}
