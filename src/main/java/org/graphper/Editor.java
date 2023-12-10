package org.graphper;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import org.graphper.api.GraphResource;

public class Editor extends JFrame implements ActionListener, DocumentListener {

  public JEditorPane textPane;
  private JPanel view;

  private JMenuBar menu;
  private JMenuItem copy, paste, cut;
  public boolean changed = false;
  private File file;

  private final AtomicLong lastModifyTime = new AtomicLong(-1);

  public Editor() {
    super("graph-support");
    textPane = new JEditorPane();
    UndoManager undo = new UndoManager();
    Document doc = textPane.getDocument();
    doc.addUndoableEditListener(evt -> undo.addEdit(evt.getEdit()));

    textPane.getActionMap().put("Undo", new AbstractAction("Undo") {
      public void actionPerformed(ActionEvent evt) {
        try {
          if (undo.canUndo()) {
            undo.undo();
          }
        } catch (CannotUndoException e) {
        }
      }
    });

    textPane.getInputMap().put(
        KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit
            .getDefaultToolkit().getMenuShortcutKeyMask()), "Undo");

    view = new JPanel();
    JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textPane, view);
    add(jsp);

    textPane.setSize(500, 800);
    setBounds(300, 200, 1000, 500);
    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setVisible(true);
    jsp.setDividerLocation(0.5);

    add(new JScrollPane(textPane), BorderLayout.WEST);

    textPane.getDocument().addDocumentListener(this);

    menu = new JMenuBar();
    setJMenuBar(menu);
    buildMenu();

    view.setSize(500, 800);
    setSize(1000, 800);
    setVisible(true);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    textPane.setText("digraph G {\n"
                         + "\n"
                         + "  subgraph cluster_0 {\n"
                         + "    color=lightgrey;\n"
                         + "    bgcolor=lightgrey;\n"
                         + "    tempnode1[color=white,fillcolor=white];\n"
                         + "    a0 -> a1 -> a2 -> a3;\n"
                         + "    label = \"process #1\";\n"
                         + "  }\n"
                         + "\n"
                         + "  subgraph cluster_1 {\n"
                         + "    tempnode2[fillcolor=grey];\n"
                         + "    b0 -> b1 -> b2 -> b3;\n"
                         + "    label = \"process #2\";\n"
                         + "    color=blue\n"
                         + "  }\n"
                         + "  start -> a0;\n"
                         + "  start -> b0;\n"
                         + "  a1 -> b3;\n"
                         + "  b2 -> a3;\n"
                         + "  a3 -> a0;\n"
                         + "  a3 -> end;\n"
                         + "  b3 -> end;\n"
                         + "\n"
                         + "  start [shape=diamond];\n"
                         + "  end [shape=rect];\n"
                         + "}");

    schedule();
  }

  private void buildMenu() {
    buildFileMenu();
    buildEditMenu();
  }

  private void buildFileMenu() {
    JMenu file = new JMenu("File");
    file.setMnemonic('F');
    menu.add(file);
    JMenuItem n = new JMenuItem("New");
    n.setMnemonic('N');
    n.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
    n.addActionListener(this);
    file.add(n);
    JMenuItem open = new JMenuItem("Open");
    file.add(open);
    open.addActionListener(this);
    open.setMnemonic('O');
    open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
    JMenuItem save = new JMenuItem("Save");
    file.add(save);
    save.setMnemonic('S');
    save.addActionListener(this);
    save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
    JMenuItem saveas = new JMenuItem("Save as...");
    saveas.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
    file.add(saveas);
    saveas.addActionListener(this);
    JMenuItem quit = new JMenuItem("Quit");
    file.add(quit);
    quit.addActionListener(this);
    quit.setMnemonic('Q');
    quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
  }

  private void buildEditMenu() {
    JMenu edit = new JMenu("Edit");
    menu.add(edit);
    edit.setMnemonic('E');
    // cut
    cut = new JMenuItem("Cut");
    cut.addActionListener(this);
    cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
    cut.setMnemonic('T');
    edit.add(cut);
    // copy
    copy = new JMenuItem("Copy");
    copy.addActionListener(this);
    copy.setMnemonic('C');
    copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
    edit.add(copy);
    // paste
    paste = new JMenuItem("Paste");
    paste.setMnemonic('P');
    paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
    edit.add(paste);
    paste.addActionListener(this);
    // find
    JMenuItem find = new JMenuItem("Find");
    find.setMnemonic('F');
    find.addActionListener(this);
    edit.add(find);
    find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
    // select all
    JMenuItem sall = new JMenuItem("Select All");
    sall.setMnemonic('A');
    sall.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
    sall.addActionListener(this);
    edit.add(sall);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    String action = e.getActionCommand();
    if (action.equals("Quit")) {
      System.exit(0);
    } else if (action.equals("Open")) {
      loadFile();
    } else if (action.equals("Save")) {
      saveFile();
    } else if (action.equals("New")) {
      newFile();
    } else if (action.equals("Save as...")) {
      saveAs("Save as...");
    } else if (action.equals("Select All")) {
      textPane.selectAll();
    } else if (action.equals("Copy")) {
      textPane.copy();
    } else if (action.equals("Cut")) {
      textPane.cut();
    } else if (action.equals("Paste")) {
      textPane.paste();
    } else if (action.equals("Find")) {
      FindDialog find = new FindDialog(this, true);
      find.showDialog();
    }
  }

  private void newFile() {
    if (changed) {
      saveFile();
    }
    file = null;
    textPane.setText("");
    changed = false;
    setTitle("graph-support");
  }

  private void loadFile() {
    JFileChooser dialog = new JFileChooser(System.getProperty("user.home"));
    dialog.setMultiSelectionEnabled(false);
    try {
      int result = dialog.showOpenDialog(this);
      if (result == JFileChooser.CANCEL_OPTION) {
        return;
      }
      if (result == JFileChooser.APPROVE_OPTION) {
        if (changed) {
          saveFile();
        }
        file = dialog.getSelectedFile();
        textPane.setText(readFile(file));
        changed = false;
        setTitle("graph-support - " + file.getName());
      }
    } catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, e, "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private String readFile(File file) {
    StringBuilder result = new StringBuilder();
    try (FileReader fr = new FileReader(file);
        BufferedReader reader = new BufferedReader(fr);) {
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line + "\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "Cannot read file !", "Error !",
                                    JOptionPane.ERROR_MESSAGE);
    }
    return result.toString();
  }

  private void saveFile() {
    if (changed) {
      int ans = JOptionPane.showConfirmDialog(null, "The file has changed. You want to save it?",
                                              "Save file",
                                              JOptionPane.YES_NO_OPTION,
                                              JOptionPane.WARNING_MESSAGE);
      if (ans == JOptionPane.NO_OPTION) {
        return;
      }
    }
    if (file == null) {
      saveAs("Save");
      return;
    }
    String text = textPane.getText();
    try (PrintWriter writer = new PrintWriter(file);) {
      if (!file.canWrite()) {
        throw new Exception("Cannot write file!");
      }
      writer.write(text);
      changed = false;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void saveAs(String dialogTitle) {
    JFileChooser dialog = new JFileChooser(System.getProperty("user.home"));
    dialog.setDialogTitle(dialogTitle);
    int result = dialog.showSaveDialog(this);
    if (result != JFileChooser.APPROVE_OPTION) {
      return;
    }
    file = dialog.getSelectedFile();
    try (PrintWriter writer = new PrintWriter(file);) {
      writer.write(textPane.getText());
      changed = false;
      setTitle("graph-support - " + file.getName());
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    lastModifyTime.set(System.currentTimeMillis());
    changed = true;
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    lastModifyTime.set(System.currentTimeMillis());
    changed = true;
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    lastModifyTime.set(System.currentTimeMillis());
    changed = true;
  }

  public void schedule() {
    Timer timer = new Timer();

    timer.scheduleAtFixedRate(new TimerTask() {

      private JLabel label;

      private JLabel errorLabel;

      @Override
      public void run() {
        if (lastModifyTime.get() > 0 && System.currentTimeMillis() - lastModifyTime.get() > 2000) {
          if (errorLabel != null) {
            view.remove(errorLabel);
          }
          try {
            GraphResource graphResource = new DotParser(textPane.getText()).getGraphResource();
            ImageIcon imageIcon = new ImageIcon(graphResource.bytes(), "graphviz");
            graphResource.close();

            if (label != null) {
              view.remove(label);
            }
            label = new JLabel(imageIcon);
            label.setSize(500, 800);
            view.add("Center", label);
            pack();
          } catch (Throwable e) {
            if (label != null) {
              label.setText("<html><font color='red'>" + e.getLocalizedMessage() + "</font></html>");
              pack();
            }
            e.printStackTrace();
          }
          lastModifyTime.set(-1);
        }
      }
    }, 0L, 1000L);
  }
}
