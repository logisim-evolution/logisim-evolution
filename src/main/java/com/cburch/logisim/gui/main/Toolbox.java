/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.icons.symbolic.AddCircuitIcon;
import com.cburch.logisim.gui.icons.symbolic.AddVhdlIcon;
import com.cburch.logisim.gui.icons.symbolic.AppearanceIcon;
import com.cburch.logisim.gui.icons.symbolic.CircuitGlyphIcon;
import com.cburch.logisim.gui.icons.symbolic.DeleteIcon;
import com.cburch.logisim.gui.icons.symbolic.GridToggleIcon;
import com.cburch.logisim.gui.icons.symbolic.ListToggleIcon;
import com.cburch.logisim.gui.icons.symbolic.MoveDownIcon;
import com.cburch.logisim.gui.icons.symbolic.MoveUpIcon;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.LogisimMenuItem;
import com.cburch.logisim.gui.menu.MenuListener;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.Tool;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Modern Left Panel (Toolbox / ExplorerPanel) hosting two-row header, action strip,
 * in-place search, and List / Tile views.
 */
public class Toolbox extends JPanel implements ToolboxModel.Listener, ProjectListener, LibraryListener {

  private static final long serialVersionUID = 1L;

  private final Project proj;
  private final Frame frame;
  private final MenuListener menu;
  private final ToolboxModel model;

  private final ProjectExplorer toolbox;
  private final CircuitsList circuitsList;
  private final TileView tileView;

  private final JLabel projectTitleLabel;
  private final JButton btnAddCircuit;
  private final JButton btnAddVhdl;
  private final JButton btnMoveUp;
  private final JButton btnMoveDown;
  private final JToggleButton btnAppearance;
  private final JButton btnDelete;
  private final JToggleButton btnTiles;
  private final JToggleButton btnList;

  private final JTextField searchField;
  private final JButton clearSearchBtn;

  private final CardLayout cardLayout;
  private final JPanel cardsPanel;

  private final JPanel projectHeaderPanel;
  private final JPanel segBox;
  private final JPanel searchBox;
  private final JPanel treeContainer;
  private final JPanel libHeaderBox;
  private final JLabel libHeaderLabel;

  private static Color getSeparatorBorderColor() {
    Color c = UIManager.getColor("Separator.foreground");
    if (c == null) {
      c = UIManager.getColor("Component.borderColor");
    }
    if (c == null) {
      c = UIManager.getColor("Surface.borderSubtle");
    }
    if (c == null) {
      c = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
          ? new Color(0x59, 0x5C, 0x5E)
          : new Color(0xD5, 0xDA, 0xE2);
    }
    return c;
  }

  public Toolbox(Project proj, Frame frame, MenuListener menu) {
    super(new BorderLayout());
    this.proj = proj;
    this.frame = frame;
    this.menu = menu;
    this.model = new ToolboxModel(proj, frame);

    // Row 1: Project Header (32px)
    projectHeaderPanel = new JPanel(new BorderLayout());
    projectHeaderPanel.setPreferredSize(new Dimension(-1, AppPreferences.getScaled(32)));
    projectHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, AppPreferences.getScaled(8), 0, AppPreferences.getScaled(8)));
    projectHeaderPanel.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background;");

    final var titleBox = new JPanel();
    titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.X_AXIS));
    titleBox.setOpaque(false);

    final var projIcon = new CircuitGlyphIcon(false);
    Color fgMuted = new Color(154, 160, 168);
    projIcon.setColor(fgMuted);

    projectTitleLabel = new JLabel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (proj != null && proj.getLogisimFile() != null && proj.getLogisimFile().isDirty()) {
          final var g2 = (Graphics2D) g.create();
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          Color warn = UIManager.getColor("Chrome.warning");
          if (warn == null) {
            warn = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
                ? new Color(0xE5, 0xA5, 0x0A)
                : new Color(0xB8, 0x7D, 0x00);
          }
          g2.setColor(warn);
          int dotSize = AppPreferences.getScaled(6);
          int textW = getFontMetrics(getFont()).stringWidth(getText());
          int iconW = getIcon() != null ? getIcon().getIconWidth() + getIconTextGap() : 0;
          int x = iconW + textW + AppPreferences.getScaled(4);
          int y = (getHeight() - dotSize) / 2;
          g2.fillOval(x, y, dotSize, dotSize);
          g2.dispose();
        }
      }
    };
    projectTitleLabel.setIcon(projIcon);
    projectTitleLabel.setIconTextGap(AppPreferences.getScaled(6));
    projectTitleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, AppPreferences.getScaled(12)));
    updateProjectTitle();
    titleBox.add(projectTitleLabel);
    projectHeaderPanel.add(titleBox, BorderLayout.WEST);

    // Segmented Toggle: [☰] (List) | [⊞] (Tiles) with 16px vector glyphs
    segBox = new JPanel();
    segBox.setLayout(new BoxLayout(segBox, BoxLayout.X_AXIS));
    segBox.putClientProperty(FlatClientProperties.STYLE, "arc: 6; border: 1,1,1,1,$Component.borderColor; background: $Panel.background;");
    final var viewGroup = new ButtonGroup();

    int segSize = AppPreferences.getScaled(24);
    int segBoxW = segSize * 2 + AppPreferences.getScaled(2);
    int segBoxH = segSize + AppPreferences.getScaled(2);
    segBox.setPreferredSize(new Dimension(segBoxW, segBoxH));
    segBox.setMinimumSize(new Dimension(segBoxW, segBoxH));
    segBox.setMaximumSize(new Dimension(segBoxW, segBoxH));

    btnList = new JToggleButton(new ListToggleIcon());
    btnList.setToolTipText(S.get("toolboxViewListTip"));
    btnList.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    btnList.putClientProperty(FlatClientProperties.STYLE, "selectedBackground: $Component.accentColor; selectedForeground: #ffffff; arc: 4; margin: 1,1,1,1;");
    btnList.setPreferredSize(new Dimension(segSize, segSize));
    btnList.setMinimumSize(new Dimension(segSize, segSize));
    btnList.setMaximumSize(new Dimension(segSize, segSize));
    btnList.addActionListener(e -> model.setViewMode(AppPreferences.EXPLORER_VIEW_LIST));

    btnTiles = new JToggleButton(new GridToggleIcon());
    btnTiles.setToolTipText(S.get("toolboxViewTilesTip"));
    btnTiles.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    btnTiles.putClientProperty(FlatClientProperties.STYLE, "selectedBackground: $Component.accentColor; selectedForeground: #ffffff; arc: 4; margin: 1,1,1,1;");
    btnTiles.setPreferredSize(new Dimension(segSize, segSize));
    btnTiles.setMinimumSize(new Dimension(segSize, segSize));
    btnTiles.setMaximumSize(new Dimension(segSize, segSize));
    btnTiles.addActionListener(e -> model.setViewMode(AppPreferences.EXPLORER_VIEW_TILES));

    viewGroup.add(btnList);
    viewGroup.add(btnTiles);
    if (AppPreferences.EXPLORER_VIEW_TILES.equals(model.getViewMode())) {
      btnTiles.setSelected(true);
    } else {
      btnList.setSelected(true);
    }

    segBox.add(btnList);
    segBox.add(btnTiles);

    final var segBoxWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, AppPreferences.getScaled(3)));
    segBoxWrapper.setOpaque(false);
    segBoxWrapper.add(segBox);
    projectHeaderPanel.add(segBoxWrapper, BorderLayout.EAST);

    // Row 2: Full-Width Action Strip (32px)
    final var actionStrip = new JPanel();
    actionStrip.setLayout(new BoxLayout(actionStrip, BoxLayout.X_AXIS));
    actionStrip.setOpaque(false);
    actionStrip.setPreferredSize(new Dimension(-1, AppPreferences.getScaled(32)));
    actionStrip.setBorder(BorderFactory.createEmptyBorder(
        AppPreferences.getScaled(2),
        AppPreferences.getScaled(8),
        AppPreferences.getScaled(2),
        AppPreferences.getScaled(8)));

    btnAddCircuit = createActionButton(new AddCircuitIcon(16), S.get("projectAddCircuitTip"), LogisimMenuBar.ADD_CIRCUIT);
    btnAddVhdl = createActionButton(new AddVhdlIcon(16), S.get("projectAddVhdlItem"), LogisimMenuBar.ADD_VHDL);
    btnMoveUp = createActionButton(new MoveUpIcon(16), S.get("projectMoveCircuitUpTip"), LogisimMenuBar.MOVE_CIRCUIT_UP);
    btnMoveDown = createActionButton(new MoveDownIcon(16), S.get("projectMoveCircuitDownTip"), LogisimMenuBar.MOVE_CIRCUIT_DOWN);
    btnAppearance = createToggleActionButton(new AppearanceIcon(16), S.get("projectEditAppearanceTip"), LogisimMenuBar.TOGGLE_APPEARANCE);

    btnDelete = createActionButton(new DeleteIcon(16), S.get("projectRemoveCircuitTip"), LogisimMenuBar.REMOVE_CIRCUIT);
    btnDelete.putClientProperty(FlatClientProperties.STYLE,
        "arc: 4; margin: 2,2,2,2; focusWidth: 2; focusColor: #c01c2880; hoverBackground: #c01c2818; hoverForeground: #c01c28;");

    // Group 1: Add Circuit, Add VHDL
    actionStrip.add(btnAddCircuit);
    actionStrip.add(Box.createHorizontalStrut(AppPreferences.getScaled(2)));
    actionStrip.add(btnAddVhdl);

    // Divider 1
    actionStrip.add(createDivider());

    // Group 2: Move Up, Move Down
    actionStrip.add(btnMoveUp);
    actionStrip.add(Box.createHorizontalStrut(AppPreferences.getScaled(2)));
    actionStrip.add(btnMoveDown);

    // Divider 2
    actionStrip.add(createDivider());

    // Group 3: Toggle Appearance
    actionStrip.add(btnAppearance);

    // Flexible Space
    actionStrip.add(Box.createHorizontalGlue());

    // Group 4: Delete alone at far right
    actionStrip.add(btnDelete);

    // Top Two-Row Header Container
    final var topHeaderContainer = new JPanel();
    topHeaderContainer.setLayout(new BoxLayout(topHeaderContainer, BoxLayout.Y_AXIS));
    topHeaderContainer.setOpaque(false);
    topHeaderContainer.add(projectHeaderPanel);
    topHeaderContainer.add(actionStrip);
    add(topHeaderContainer, BorderLayout.NORTH);

    // Search and cards area
    final var centerPanel = new JPanel(new BorderLayout());
    centerPanel.setOpaque(false);

    // Shared Search Field (28px)
    final var searchPanel = new JPanel(new BorderLayout());
    searchPanel.setPreferredSize(new Dimension(-1, AppPreferences.getScaled(28)));
    searchPanel.setBorder(BorderFactory.createEmptyBorder(1, 8, 3, 8));
    searchPanel.setOpaque(false);

    searchBox = new JPanel(new BorderLayout());
    searchBox.putClientProperty(FlatClientProperties.STYLE, "arc: 5; background: $TextField.background;");
    searchField = new JTextField();
    searchField.setToolTipText(S.get("toolboxFilterTip"));
    searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, S.get("toolboxFilterLabel").replace(":", "") + "...");
    searchField.putClientProperty(FlatClientProperties.STYLE, "arc: 5; margin: 0,4,0,4; showClearButton: true;");
    searchField.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, AppPreferences.getScaled(12)));
    for (final var comp : searchField.getComponents()) {
      if (comp instanceof JButton clearBtn) {
        clearBtn.setToolTipText(S.get("toolboxFilterClearTip"));
      }
    }

    final var keyHint = new JLabel("/");
    keyHint.setFont(new Font(Font.MONOSPACED, Font.BOLD, AppPreferences.getScaled(11)));
    keyHint.setForeground(new Color(154, 160, 168));
    keyHint.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, AppPreferences.getScaled(8)));
    searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, keyHint);

    // Esc clears search field
    searchField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
    searchField.getActionMap().put("clearSearch", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        searchField.setText("");
        searchField.transferFocus();
      }
    });

    clearSearchBtn = new JButton("✕");
    clearSearchBtn.setVisible(false);

    searchField.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        update();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        update();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        update();
      }

      private void update() {
        final var txt = searchField.getText();
        clearSearchBtn.setVisible(!txt.isEmpty());
        model.setSearchQuery(txt);
      }
    });

    searchBox.add(searchField, BorderLayout.CENTER);
    searchPanel.add(searchBox, BorderLayout.CENTER);
    centerPanel.add(searchPanel, BorderLayout.NORTH);

    // Card Container: List View vs Tile View
    cardLayout = new CardLayout();
    cardsPanel = new JPanel(cardLayout);
    cardsPanel.setOpaque(false);

    // Card 1: List View (Circuits List + Tree with In-Place Search)
    final var listCard = new JPanel(new BorderLayout());
    listCard.setOpaque(false);

    circuitsList = new CircuitsList(model);
    listCard.add(circuitsList, BorderLayout.NORTH);

    toolbox = new ProjectExplorer(proj, false, false);
    toolbox.setListener(new ToolboxManip(proj, toolbox));
    toolbox.setRootVisible(false);
    final var treeScroll = new JScrollPane(toolbox);
    treeScroll.setBorder(BorderFactory.createEmptyBorder());
    treeScroll.setOpaque(false);
    treeScroll.getViewport().setOpaque(false);
    treeScroll.putClientProperty(FlatClientProperties.STYLE, "border: 0,0,0,0;");

    Color borderSubtle = getSeparatorBorderColor();

    treeContainer = new JPanel(new BorderLayout());
    treeContainer.setOpaque(false);
    treeContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));

    libHeaderBox = new JPanel(new BorderLayout());
    libHeaderBox.setOpaque(false);
    libHeaderBox.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, borderSubtle),
        BorderFactory.createEmptyBorder(6, 12, 4, 12)));

    libHeaderLabel = new JLabel("LIBRARIES");
    libHeaderLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, AppPreferences.getScaled(11)));
    Color libMuted = UIManager.getColor("Surface.mutedText");
    if (libMuted == null) libMuted = UIManager.getColor("Chrome.mutedText");
    if (libMuted == null) libMuted = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(0x9E, 0x9E, 0x9E) : new Color(0x5A, 0x5D, 0x66);
    libHeaderLabel.setForeground(libMuted);
    updateLibrariesCount();
    libHeaderBox.add(libHeaderLabel, BorderLayout.WEST);

    treeContainer.add(libHeaderBox, BorderLayout.NORTH);
    treeContainer.add(treeScroll, BorderLayout.CENTER);

    listCard.add(treeContainer, BorderLayout.CENTER);

    cardsPanel.add(listCard, AppPreferences.EXPLORER_VIEW_LIST);

    // Card 2: Tile View
    tileView = new TileView(model);
    cardsPanel.add(tileView, AppPreferences.EXPLORER_VIEW_TILES);

    centerPanel.add(cardsPanel, BorderLayout.CENTER);
    add(centerPanel, BorderLayout.CENTER);

    setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));

    // Register shortcuts: '/' focuses search (Ctrl+K is reserved for Simulate ticks)
    final var imap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final var amap = getActionMap();
    imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, 0), "focusSearch");
    amap.put("focusSearch", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        searchField.requestFocusInWindow();
        searchField.selectAll();
      }
    });

    // Model listeners
    model.addListener(this);
    proj.addProjectListener(this);
    if (proj.getLogisimFile() != null) {
      proj.getLogisimFile().addLibraryListener(this);
    }
    if (menu != null) {
      menu.addEnabledListener(source -> updateActionButtons());
    }

    cardLayout.show(cardsPanel, model.getViewMode());
    updateActionButtons();
  }

  public int getMeasuredStripWidth() {
    return AppPreferences.getScaled(266);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(getMeasuredStripWidth(), super.getMinimumSize().height);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    return new Dimension(Math.max(d.width, getMeasuredStripWidth()), d.height);
  }

  private static JComponent createDivider() {
    final var div = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final var g2 = (Graphics2D) g.create();
        Color c = UIManager.getColor("Separator.foreground");
        if (c == null) {
          c = UIManager.getColor("Chrome.subtleBorderColor");
        }
        if (c == null) {
          c = UIManager.getColor("Component.borderColor");
        }
        if (c == null) {
          c = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get())
              ? new Color(0x59, 0x5C, 0x5E)
              : new Color(0xD0, 0xD5, 0xD0);
        }
        g2.setColor(c);
        int midX = getWidth() / 2;
        int padY = AppPreferences.getScaled(6);
        g2.drawLine(midX, padY, midX, getHeight() - padY);
        g2.dispose();
      }
    };
    div.setOpaque(false);
    int w = AppPreferences.getScaled(13);
    int h = AppPreferences.getScaled(34);
    div.setPreferredSize(new Dimension(w, h));
    div.setMinimumSize(new Dimension(w, h));
    div.setMaximumSize(new Dimension(w, h));
    return div;
  }

  private JButton createActionButton(Icon icon, String defaultTip, LogisimMenuItem item) {
    final var btn = new JButton(icon);
    btn.setToolTipText(defaultTip);
    btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    btn.putClientProperty(FlatClientProperties.STYLE,
        "arc: 4; margin: 2,2,2,2; focusWidth: 2; focusColor: #3584e480;");
    int btnSize = AppPreferences.getScaled(26);
    btn.setPreferredSize(new Dimension(btnSize, btnSize));
    btn.setMinimumSize(new Dimension(btnSize, btnSize));
    btn.setMaximumSize(new Dimension(btnSize, btnSize));
    btn.addActionListener(e -> {
      if (menu != null && menu.isEnabled(item)) {
        menu.doAction(item);
      }
    });
    return btn;
  }

  private JToggleButton createToggleActionButton(Icon icon, String defaultTip, LogisimMenuItem item) {
    final var btn = new JToggleButton(icon);
    btn.setToolTipText(defaultTip);
    btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    btn.putClientProperty(FlatClientProperties.STYLE,
        "arc: 4; margin: 2,2,2,2; focusWidth: 2; focusColor: #3584e480; selectedBackground: null; selectedBorderColor: #1c71d8; borderWidth: 1;");
    int btnSize = AppPreferences.getScaled(26);
    btn.setPreferredSize(new Dimension(btnSize, btnSize));
    btn.setMinimumSize(new Dimension(btnSize, btnSize));
    btn.setMaximumSize(new Dimension(btnSize, btnSize));
    btn.addActionListener(e -> {
      if (menu != null && menu.isEnabled(item)) {
        menu.doAction(item);
      }
    });
    return btn;
  }

  private void updateLibrariesCount() {
    if (libHeaderLabel != null) {
      int count = (model != null) ? model.getLibraries().size() : 0;
      libHeaderLabel.setText("LIBRARIES (" + count + ")");
    }
  }

  private void updateProjectTitle() {
    final var file = proj != null ? proj.getLogisimFile() : null;
    final var name = (file != null) ? file.getName() : "Project";
    projectTitleLabel.setText(name);
    projectTitleLabel.setToolTipText(name);
    repaint();
  }

  public void setEditorView(String view) {
    if (btnAppearance != null) {
      boolean isApp = Frame.EDIT_APPEARANCE.equals(view);
      btnAppearance.setSelected(isApp);
      btnAppearance.setToolTipText(isApp ? S.get("projectEditCircuitLayoutItem") : S.get("projectEditAppearanceTip"));
    }
  }

  public void updateActionButtons() {
    final var cur = (proj != null) ? proj.getCurrentCircuit() : null;

    if (btnAddCircuit != null) {
      btnAddCircuit.setEnabled(menu != null && menu.isEnabled(LogisimMenuBar.ADD_CIRCUIT));
      btnAddCircuit.setToolTipText(S.get("projectAddCircuitTip"));
    }
    if (btnAddVhdl != null) {
      btnAddVhdl.setEnabled(menu != null && menu.isEnabled(LogisimMenuBar.ADD_VHDL));
      btnAddVhdl.setToolTipText(S.get("projectAddVhdlItem"));
    }
    if (btnMoveUp != null) {
      boolean canUp = menu != null ? menu.isEnabled(LogisimMenuBar.MOVE_CIRCUIT_UP)
          : ProjectCircuitActions.canMoveUp(proj, cur);
      btnMoveUp.setEnabled(canUp);
      if (canUp) {
        btnMoveUp.setToolTipText(S.get("projectMoveCircuitUpTip"));
      } else {
        btnMoveUp.setToolTipText(S.get("toolboxMoveUpDisabledTip"));
      }
    }
    if (btnMoveDown != null) {
      boolean canDown = menu != null ? menu.isEnabled(LogisimMenuBar.MOVE_CIRCUIT_DOWN)
          : ProjectCircuitActions.canMoveDown(proj, cur);
      btnMoveDown.setEnabled(canDown);
      if (canDown) {
        btnMoveDown.setToolTipText(S.get("projectMoveCircuitDownTip"));
      } else {
        btnMoveDown.setToolTipText(S.get("toolboxMoveDownDisabledTip"));
      }
    }
    if (btnAppearance != null) {
      boolean isApp = false;
      try {
        isApp = frame != null && Frame.EDIT_APPEARANCE.equals(frame.getEditorView());
      } catch (Exception ignored) {
        // Frame still constructing
      }
      btnAppearance.setSelected(isApp);
      btnAppearance.setEnabled(menu != null && menu.isEnabled(LogisimMenuBar.TOGGLE_APPEARANCE));
      btnAppearance.setToolTipText(isApp ? S.get("projectEditCircuitLayoutItem") : S.get("projectEditAppearanceTip"));
    }
    if (btnDelete != null) {
      boolean canDelete = menu != null && menu.isEnabled(LogisimMenuBar.REMOVE_CIRCUIT);
      btnDelete.setEnabled(canDelete);
      if (canDelete) {
        btnDelete.setToolTipText(S.get("projectRemoveCircuitTip"));
      } else {
        btnDelete.setToolTipText(S.get("toolboxRemoveDisabledTip"));
      }
    }
  }

  void setHaloedTool(Tool value) {
    toolbox.setHaloedTool(value);
  }

  public void updateStructure() {
    toolbox.updateStructure();
    circuitsList.refresh();
    updateLibrariesCount();
    updateProjectTitle();
    updateActionButtons();
  }

  @Override
  public void toolArmed(Tool tool) {}

  @Override
  public void circuitChanged(Circuit circuit) {
    updateProjectTitle();
    updateActionButtons();
  }

  @Override
  public void structureChanged() {
    updateStructure();
  }

  @Override
  public void filterChanged(String query) {
    if (toolbox != null) {
      toolbox.setFilterText(query);
    }
  }

  public JButton getBtnAddCircuit() {
    return btnAddCircuit;
  }

  public JButton getBtnAddVhdl() {
    return btnAddVhdl;
  }

  public JButton getBtnMoveUp() {
    return btnMoveUp;
  }

  public JButton getBtnMoveDown() {
    return btnMoveDown;
  }

  public JToggleButton getBtnAppearance() {
    return btnAppearance;
  }

  public JButton getBtnDelete() {
    return btnDelete;
  }

  @Override
  public void viewModeChanged(String newMode) {
    if (AppPreferences.EXPLORER_VIEW_TILES.equals(newMode)) {
      btnTiles.setSelected(true);
      final var selPath = toolbox.getSelectionPath();
      if (selPath != null && selPath.getPathCount() >= 2) {
        final var node = selPath.getPathComponent(1);
        if (node instanceof com.cburch.logisim.gui.generic.ProjectExplorerLibraryNode libNode) {
          tileView.selectCategory(libNode.getValue().getDisplayName());
        }
      }
    } else {
      btnList.setSelected(true);
    }
    cardLayout.show(cardsPanel, newMode);
  }

  @Override
  public void projectChanged(ProjectEvent event) {
    updateProjectTitle();
    updateActionButtons();
    if (circuitsList != null) {
      circuitsList.refresh();
    }
  }

  @Override
  public void libraryChanged(LibraryEvent event) {
    updateProjectTitle();
    updateLibrariesCount();
    updateActionButtons();
    if (circuitsList != null) {
      circuitsList.refresh();
    }
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (projectHeaderPanel != null) {
      projectHeaderPanel.putClientProperty(FlatClientProperties.STYLE, "background: $Panel.background;");
    }
    if (segBox != null) {
      segBox.putClientProperty(FlatClientProperties.STYLE, "arc: 6; border: 1,1,1,1,$Component.borderColor; background: $Panel.background;");
    }
    if (searchBox != null) {
      searchBox.putClientProperty(FlatClientProperties.STYLE, "arc: 6; background: $TextField.background;");
    }
    if (libHeaderBox != null) {
      Color borderSubtle = getSeparatorBorderColor();
      libHeaderBox.setBorder(BorderFactory.createCompoundBorder(
          BorderFactory.createMatteBorder(1, 0, 0, 0, borderSubtle),
          BorderFactory.createEmptyBorder(6, 12, 4, 12)));
      if (treeContainer != null) {
        treeContainer.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));
      }
      setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderSubtle));
    }
    if (libHeaderLabel != null) {
      Color libMuted = UIManager.getColor("Surface.mutedText");
      if (libMuted == null) libMuted = UIManager.getColor("Chrome.mutedText");
      if (libMuted == null) libMuted = AppPreferences.isDarkTheme(AppPreferences.LookAndFeel.get()) ? new Color(0x9E, 0x9E, 0x9E) : new Color(0x5A, 0x5D, 0x66);
      libHeaderLabel.setForeground(libMuted);
      updateLibrariesCount();
    }
    if (circuitsList != null) {
      circuitsList.updateUI();
    }
    if (tileView != null) {
      tileView.updateUI();
    }
    if (toolbox != null) {
      toolbox.updateUI();
    }
    if (projectTitleLabel != null) {
      updateProjectTitle();
    }
    updateActionButtons();
  }
}
