/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.chronogram.chronogui;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.gui.chronogram.chronodata.ChronoData;
import com.cburch.logisim.gui.chronogram.chronodata.SignalData;
import com.cburch.logisim.gui.chronogram.chronodata.SignalDataBus;
import com.cburch.logisim.gui.generic.OptionPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * Export chronogram into a png image.
 *
 * @author kgs
 */
public class ImageExporter extends javax.swing.JFrame implements ActionListener {

  private static final long serialVersionUID = 1L;
  private File fileToSave;
  private ChronoData chronoData;
  private final int tickWidth = 20;
  // Dimensions
  private final int lowPos = 32;
  private final int highPos = 6;
  private final int height = 38;
  int middleHeight = 38 / 2;
  private final Color lightGray = new Color(180, 180, 180, 100);
  private SignalData mSignalData;
  // for right panel
  JPanel rightBox;
  // for leftPanel
  private JTable table;
  private Object[][] tableData;
  private HashMap<SignalData, Integer> signalDataPositionInTable; // to have
  private SignalData[] reverseSignalDataPositionInTable; // and the reverse
  private JPanel leftPanel;
  private final int ROWHEIGHT = 38;
  // for timeline
  private JPanel timePanel;
  private TimelineDraw td;
  private ChronoFrame chronoFrame;
  // GUI, select image export options
  private JFrame frame;
  private javax.swing.ButtonGroup buttonGroup1;
  private javax.swing.JRadioButton jRadioBtn_single;
  private javax.swing.JRadioButton jRadioBtn_multiple;
  private JButton jBtnDone;
  private JLabel picture;
  Image img_single;
  Image img_multiple;
  final int PAGE_MAX_WIDTH = 1000;

  /**
   * @param lp
   * @param td
   * @param sdraw
   */
  public ImageExporter(
      ChronoFrame cfp, /*, TimelineDraw td, ArrayList sdraw, ,*/ ChronoData chrdata, int heightp) {
    this.chronoData = chrdata;
    this.chronoFrame = cfp;
  }

  public void createImage(File file) {
    if (chronoData.size() > 0) {
      this.fileToSave = file;
      createRightPanel();
      createLeftPanel();
      cropImage();
    } else { // error message
      OptionPane.showMessageDialog(
          leftPanel,
          "The chronogram is empty. Can't save it as an image.",
          "",
          OptionPane.ERROR_MESSAGE);
    }
  }

  private void createAndShowGUI() {
    frame = new JFrame("Configure image");
    // Set up the content pane.
    addComponentsToPane(frame.getContentPane());
    setAlwaysOnTop(true);
    frame.setLocationRelativeTo(null); // create the frame at screen center

    // Display the window.
    frame.pack();
    frame.setVisible(true);
  }

  // Actually not in use. For a further development with more parameters to export the image
  public void addComponentsToPane(Container pane) {
    pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
    JPanel morphPanel = new JPanel();
    morphPanel.setLayout(new BoxLayout(morphPanel, BoxLayout.X_AXIS));
    JPanel radioBtnMorphPanel = new JPanel();
    radioBtnMorphPanel.setLayout(new BoxLayout(radioBtnMorphPanel, BoxLayout.Y_AXIS));

    jRadioBtn_single = new javax.swing.JRadioButton();
    jRadioBtn_multiple = new javax.swing.JRadioButton();
    jRadioBtn_single.setText("Single line");
    jRadioBtn_single.setToolTipText("Export chronogram as a single line");
    jRadioBtn_multiple.setSelected(true);
    jRadioBtn_multiple.setText("Multiple lines");
    jRadioBtn_multiple.setToolTipText(
        "Export chronogram into several lines if the image is too large, more convenient for documents");
    buttonGroup1 = new javax.swing.ButtonGroup();
    buttonGroup1.add(jRadioBtn_single);
    buttonGroup1.add(jRadioBtn_multiple);
    radioBtnMorphPanel.add(jRadioBtn_single);
    radioBtnMorphPanel.add(jRadioBtn_multiple);
    jRadioBtn_single.addActionListener(this);
    jRadioBtn_multiple.addActionListener(this);
    jBtnDone = new JButton();
    jBtnDone.setText("Done");
    jBtnDone.setToolTipText("Save the image and close the dialog box");
    jBtnDone.addActionListener(this);
    jBtnDone.setAlignmentX(Component.CENTER_ALIGNMENT);

    img_single =
        Toolkit.getDefaultToolkit().getImage("doc" + File.separator + "imgExport_single.gif");
    img_multiple =
        Toolkit.getDefaultToolkit().getImage("doc" + File.separator + "imgExport_multiple.gif");

    picture = new JLabel(new ImageIcon(img_multiple));
    morphPanel.add(radioBtnMorphPanel);
    morphPanel.add(picture);
    pane.add(morphPanel);
    pane.add(jBtnDone);
  }

  // Actually not in use. For a further development with more parameters to export the image
  private void doNotCropImage() {
    JPanel pan = new JPanel();
    pan.setLayout(new BorderLayout());
    pan.add(leftPanel, BorderLayout.WEST);
    pan.add(rightBox, BorderLayout.EAST);
    SaveImage(pan);
  }

  // Cut the image if larger than PAGE_MAX_WIDTH.
  private void cropImage() { // create another image with multiple lines
    JPanel pan = new JPanel();
    pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));
    pan.setBackground(Color.white);
    BufferedImage img = null;
    BufferedImage img_names = null;

    // convert jpanel signals to buffered image
    JFrame fright = new JFrame("Show remain invisible");
    fright.setContentPane(rightBox);
    fright.pack();
    BufferedImage imgright =
        new BufferedImage(rightBox.getWidth(), rightBox.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D gright = (Graphics2D) imgright.createGraphics();
    rightBox.paint(gright);
    gright.dispose();
    // convert jpanel names to buffered image
    JFrame fleft = new JFrame("Show remain invisible");
    fleft.setContentPane(leftPanel);
    fleft.pack();
    BufferedImage imgleft =
        new BufferedImage(leftPanel.getWidth(), leftPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D gleft = (Graphics2D) imgleft.createGraphics();
    leftPanel.paint(gleft);
    gleft.dispose();

    for (int i = 0; i <= imgright.getWidth(); i = i + PAGE_MAX_WIDTH) {
      BufferedImage img_signals;
      if ((i + PAGE_MAX_WIDTH) > imgright.getWidth()) { // rest
        img_signals = imgright.getSubimage(i, 0, imgright.getWidth() - i, imgright.getHeight());
      } else {
        img_signals = imgright.getSubimage(i, 0, PAGE_MAX_WIDTH, imgright.getHeight());
      }
      JPanel panelX = new JPanel();
      panelX.setLayout(new BoxLayout(panelX, BoxLayout.X_AXIS));
      panelX.setAlignmentX(Component.LEFT_ALIGNMENT);
      JLabel jlabNames = new JLabel(new ImageIcon(imgleft));
      JLabel jlabSignals = new JLabel(new ImageIcon(img_signals));
      panelX.add(jlabNames);
      panelX.add(jlabSignals);
      pan.add(panelX);
    }
    SaveImage(pan);
  }

  // Create the signals panel
  private void createRightPanel() {
    rightBox = new JPanel();
    rightBox.setLayout(new BoxLayout(rightBox, BoxLayout.Y_AXIS));
    BufferedImage bi = null;
    int nbElem = 0;
    for (String name : chronoData.getSignalOrder()) {
      if (!name.equals("sysclk")) {
        if (nbElem == 0) {
          bi = CreateUpperBlankLine(chronoData.get(name).getSignalValues());
          JLabel jlabl = new JLabel(new ImageIcon(bi));
          rightBox.add(jlabl);
        }
        bi = CreateSignalImage(chronoData.get(name).getSignalValues(), name);
        JLabel jlab = new JLabel(new ImageIcon(bi));
        rightBox.add(jlab);
        nbElem++;
      }
    }
    rightBox.setSize(bi.getWidth(), bi.getHeight() * nbElem);
  }

  // Create the names panel
  private void createLeftPanel() {
    leftPanel = new JPanel();
    leftPanel.setLayout(new BorderLayout());
    leftPanel.setBackground(Color.white);

    String[] names = {S.get("SignalNameName")};
    tableData = new Object[chronoData.size() - 1][1];
    int pos = 0;
    for (String signalName : chronoData.getSignalOrder()) {
      if (!signalName.equals("sysclk")) {
        Object[] currentData = {signalName};
        tableData[pos++] = currentData;
      }
    }
    // creates the JTable
    DefaultTableModel model = new DefaultTableModel(tableData, names);
    table =
        new JTable(model) {

          private static final long serialVersionUID = 1L;

          @SuppressWarnings({"unchecked", "rawtypes"})
          public Class getColumnClass(int column) {
            return getValueAt(0, column).getClass();
          }

          @Override
          public boolean isCellEditable(int rowIndex, int colIndex) {
            if (colIndex == 1) {
              return true; // Disallow the editing of any cell
            }
            return false;
          }
        };
    table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "none");
    table.setRowHeight(ROWHEIGHT);

    JTableHeader header = table.getTableHeader();
    Dimension d = header.getPreferredSize();
    d.height = 20;
    d.width = 200;
    header.setPreferredSize(d);

    leftPanel.add(header, BorderLayout.NORTH);
    leftPanel.add(table, BorderLayout.CENTER);
  }

  /** Not implemented yet. */
  private void CreateTimeLine() {}

  /** Create a 20 pixel high strip. */
  private BufferedImage CreateUpperBlankLine(ArrayList<String> valList) {
    JPanel mRightPanel = new JPanel();
    mRightPanel.setSize(tickWidth * valList.size(), /*lowPos + 6*/ 20);
    mRightPanel.setBackground(Color.white);
    // mRightPanel.setBackground(Color.white);
    BufferedImage bi =
        new BufferedImage(
            mRightPanel.getWidth(), mRightPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D g = bi.createGraphics();
    g.setBackground(Color.white);
    g.clearRect(0, 0, tickWidth * valList.size(), height);
    g.dispose();
    return bi;
  }

  /**
   * Create the image with the signal for the right panel.
   *
   * @param mRightPanel
   */
  private BufferedImage CreateSignalImage(
      /*ChronoData chrdata, String name, SignalData sigData,*/ ArrayList<String> valList,
      String name) {
    String prec, suiv;
    int busCrossingPosition = (tickWidth - 5) < 1 ? 0 : 5;
    JPanel mRightPanel = new JPanel();

    int posX = 0;
    int i = 0;

    mRightPanel.setSize(tickWidth * valList.size(), lowPos + 6);
    mRightPanel.setBackground(Color.white);
    BufferedImage bi =
        new BufferedImage(
            mRightPanel.getWidth(), mRightPanel.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D g = bi.createGraphics();

    prec = valList.get(i);

    g.setBackground(Color.white);
    g.clearRect(0, 0, tickWidth * valList.size(), height);
    g.setColor(Color.black);
    // for each value in the signal
    for (int index = 0; index < valList.size(); index++) {
      suiv = valList.get(i++);
      String transi = prec + suiv;
      if (suiv.contains("E")) {
        g.setColor(Color.red);
        g.drawLine(posX, highPos, posX + tickWidth, middleHeight);
        g.drawLine(posX, middleHeight, posX + tickWidth, highPos);
        g.drawLine(posX, middleHeight, posX + tickWidth, lowPos);
        g.drawLine(posX, lowPos, posX + tickWidth, middleHeight);
        g.setColor(Color.black);
      } else if (suiv.contains("x")) {
        g.setColor(Color.blue);
        g.drawLine(posX, highPos, posX + tickWidth, middleHeight);
        g.drawLine(posX, middleHeight, posX + tickWidth, highPos);
        g.drawLine(posX, middleHeight, posX + tickWidth, lowPos);
        g.drawLine(posX, lowPos, posX + tickWidth, middleHeight);
        g.setColor(Color.black);
      } else if (suiv.equals("0")) {
        g.drawLine(posX, lowPos, posX + tickWidth, lowPos);
      } else if (suiv.equals("1")) {
        g.setColor(lightGray);
        g.fillRect(posX + 1, highPos, tickWidth, lowPos - highPos);
        g.setColor(Color.black);
        g.drawLine(posX, highPos, posX + tickWidth, highPos);
      } else {
        mSignalData = this.chronoData.get(name);

        if (mSignalData instanceof SignalDataBus) {
          SignalDataBus sdb = (SignalDataBus) mSignalData;

          // first value
          if (i == 2) {
            g.drawString(/*suiv*/ sdb.getValueInFormat(suiv), posX + 2, middleHeight);
          }
          // bus transition
          if (!suiv.contains("x") && !suiv.contains("E") && !suiv.equals(prec)) {
            g.drawLine(posX, lowPos, posX + busCrossingPosition, highPos);
            g.drawLine(posX, highPos, posX + busCrossingPosition, lowPos);
            g.drawLine(posX + busCrossingPosition, highPos, posX + tickWidth, highPos);
            g.drawLine(posX + busCrossingPosition, lowPos, posX + tickWidth, lowPos);
            g.drawString(/*suiv*/ sdb.getValueInFormat(suiv), posX + tickWidth, middleHeight);
          } else {
            g.drawLine(posX, lowPos, posX + tickWidth, lowPos);
            g.drawLine(posX, highPos, posX + tickWidth, highPos);
          }
        }
      }
      // transition
      if (transi.equals("10")) {
        g.drawLine(posX, highPos, posX, lowPos);
      } else if (transi.equals("01")) {
        g.drawLine(posX, lowPos, posX, highPos);
      }
      prec = suiv;
      posX += tickWidth;
    }
    g.dispose();
    return bi;
  }

  private void SaveImage(JPanel jp) {
    JFrame f = new JFrame("Show remain invisible");
    f.setContentPane(jp);
    f.pack();
    BufferedImage img =
        new BufferedImage(jp.getWidth(), jp.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D g = (Graphics2D) img.createGraphics();
    jp.paint(g);
    g.dispose();
    try {
      ImageIO.write(img, "png", fileToSave);
      // ImageIO.write(img, "png", new File("savedcall.png"));
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  // Actually not in use. For a further development with more parameters to export the image
  @Override
  public void actionPerformed(ActionEvent ae) {
    if (ae.getActionCommand() == "Single line") {
      picture.setIcon(new ImageIcon(img_single));
    } else if (ae.getActionCommand() == "Multiple lines") {
      picture.setIcon(new ImageIcon(img_multiple));
    } else if (ae.getActionCommand() == "Done") {
      if (jRadioBtn_multiple.isSelected()) {
        cropImage();
      } else {
        doNotCropImage();
      }
      frame.dispose();
    }
  }
}
