package com.makfuzz;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.pushingpixels.radiance.theming.api.RadianceThemingCortex;

import com.makfuzz.core.Criteria;
import com.makfuzz.core.Fuzz;
import com.makfuzz.core.SearchResult;
import com.makfuzz.core.SimResult;

public class UI extends JFrame {
    private List<String[]> database;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    
    // Sorting state
    private List<Object[]> originalTableData;
    private int lastSortedColumn = -1;
    private int sortState = 0; // 0 = unsorted, 1 = descending, 2 = ascending
    
    // UI Components
    private List<CriteriaLine> criteriaLines = new ArrayList<>();
    private JPanel criteriaContainer;
    private List<ConfigManager.CriteriaConfig> selectedColumnsConfigs = new ArrayList<>();
    private String lastLoadedPath = "";
    private long lastLoadedTimestamp = -1;
    private List<Integer> lastSelectedIndices = new ArrayList<>();
    private JTextField sourcePathField;
    private JSpinner globalThresholdField;
    private JTextField topNField;
    private JButton executeBtn;
    private JLabel statusLabel;
    private JPanel metricsPanel;
    private JLabel totalLabel;
    
    // I18N Fields
    private ResourceBundle bundle;
    private Locale currentLocale;
    private JComboBox<String> langCombo;
    
    // UI components that need dynamic text updates
    private JLabel appTitle;
    private JLabel appSubtitle;
    private JLabel srcLabel;
    private JButton browseBtn;
    private JLabel criteriaLabel;
    private JLabel thresholdLabel;
    private JLabel limitLabel;
    private JButton csvBtn;
    private JButton excelBtn;
    private JLabel versionLabel;
    private JLabel githubLink;
    private JLabel loadingIcon;
    
    private boolean searchPending = false;
    private boolean isInitializing = false;
    
    // Card Layout for Center Panel
    private CardLayout centerCardLayout;
    private JPanel centerPanel;
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_LOADING = "LOADING";
    
    public UI() {
        // Apply Radiance Mariner Theme for a trending, high-fidelity modern UI
        try {
            UIManager.setLookAndFeel("org.pushingpixels.radiance.theming.api.skin.RadianceMarinerLookAndFeel");
            
            // Global overrides for consistent UI
            UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 12));
            UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 12));
            UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 12));
            UIManager.put("Table.rowHeight", 28);
            
            // Optimization for Windows JFileChooser performance
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                UIManager.put("FileChooser.useShellFolder", Boolean.FALSE);
            }
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {}
        }

        setTitle("MakFuzz - Fuzzy Search ✨");
        setSize(1400, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize I18N
        currentLocale = Locale.getDefault();
        if (!currentLocale.getLanguage().equals("fr")) {
            currentLocale = Locale.ENGLISH;
        }
        bundle = ResourceBundle.getBundle("messages", currentLocale);

        setupUI();
        updateTexts();

        setLocationRelativeTo(null);
    }

    private void setupUI() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));

        // 0. App Title Section
        JPanel titleBox = new JPanel(new BorderLayout());
        appTitle = new JLabel("MakFuzz");
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        appTitle.setForeground(new Color(63, 81, 181));
        titleBox.add(appTitle, BorderLayout.WEST);
        
        appSubtitle = new JLabel("Optimized Similarity Engine");
        appSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        appSubtitle.setForeground(Color.GRAY);
        titleBox.add(appSubtitle, BorderLayout.SOUTH);
        headerPanel.add(titleBox);
        headerPanel.add(Box.createVerticalStrut(20));

        // 1. Data Source Card
        JPanel sourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        sourcePanel.setBackground(Color.WHITE);
        sourcePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        srcLabel = new JLabel("Data Source:");
        srcLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        sourcePanel.add(srcLabel);

        sourcePathField = new JTextField("./names.csv", 60);
        sourcePathField.setEditable(false);
        
        browseBtn = new JButton("Browse");
        browseBtn.addActionListener(e -> chooseFileAndColumns());

        sourcePanel.add(sourcePathField);
        sourcePanel.add(browseBtn);

        headerPanel.add(sourcePanel);
        headerPanel.add(Box.createVerticalStrut(15));

        // 2. Search Card
        setupTopPanel(headerPanel);

        add(headerPanel, BorderLayout.NORTH);

        // 3. Table Panel
        setupCenterPanel();
        
        // 4. Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(15, 30, 60)); // Solid Dark Nautical Blue for high contrast
        // Margin/Padding/Border removed from statusBar
        statusBar.setBorder(null);
        
        statusLabel = new JLabel("<html>Engine ready...</html>");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        statusLabel.setVerticalAlignment(JLabel.TOP);
        
        totalLabel = new JLabel("Total Found: 0");
        totalLabel.setForeground(Color.WHITE);
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        totalLabel.setVerticalAlignment(JLabel.TOP);
        
        // Container for Status Table and Total Label (West)
        JPanel westStatusPanel = new JPanel();
        westStatusPanel.setLayout(new BoxLayout(westStatusPanel, BoxLayout.X_AXIS));
        westStatusPanel.setOpaque(false);
        westStatusPanel.add(statusLabel);
        
        metricsPanel = new JPanel(new GridBagLayout());
        metricsPanel.setOpaque(false);
        westStatusPanel.add(metricsPanel);

        westStatusPanel.add(Box.createHorizontalStrut(50));
        westStatusPanel.add(totalLabel);
        
        statusBar.add(westStatusPanel, BorderLayout.WEST);
        

        JPanel eastStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        eastStatusPanel.setOpaque(false);
        
        githubLink = new JLabel("<html><u>GitHub</u></html>");
        githubLink.setForeground(Color.WHITE);
        githubLink.setFont(new Font("SansSerif", Font.PLAIN, 10));
        githubLink.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        githubLink.setToolTipText("Open Makfuzz Project on GitHub");
        githubLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                        java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/melkarama/makfuzz"));
                    }
                } catch (Exception ex) { 
                    ex.printStackTrace(); 
                }
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                githubLink.setForeground(new Color(187, 222, 251)); // Lighter blue
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                githubLink.setForeground(Color.WHITE);
            }
        });
        
        versionLabel = new JLabel("v1.0 2025");
        versionLabel.setForeground(new Color(255, 255, 255, 180));
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        
        eastStatusPanel.add(githubLink);
        eastStatusPanel.add(versionLabel);
        
        statusBar.add(eastStatusPanel, BorderLayout.EAST);
        
        add(statusBar, BorderLayout.SOUTH);
        
        // Restore settings
        loadSettings();
        
        // Save on exit
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                saveSettings();
            }
        });
    }
    
    private void saveSettings() {
        ConfigManager.AppConfig config = new ConfigManager.AppConfig();
        config.sourcePath = sourcePathField.getText();
        config.globalThreshold = (Double) globalThresholdField.getValue();
        try { config.topN = Integer.parseInt(topNField.getText()); } catch(Exception e) { config.topN = 1000; }
        config.language = currentLocale.getLanguage();
        
        config.criteriaList = new ArrayList<>();
        for (CriteriaLine line : criteriaLines) {
            config.criteriaList.add(line.getConfig());
        }
        
        File configFile = new File(System.getProperty("user.home"), ".makfuzz_config.xml");
        ConfigManager.saveConfig(config, configFile);
    }

    private void loadSettings() {
        File configFile = new File(System.getProperty("user.home"), ".makfuzz_config.xml");
        if (!configFile.exists()) {
			return;
		}
        
        isInitializing = true;
        try {
            ConfigManager.AppConfig config = ConfigManager.loadConfig(configFile);
            
            if (config != null) {
                sourcePathField.setText(config.sourcePath);
                try { globalThresholdField.setValue(config.globalThreshold); } catch (Exception e) {}
                topNField.setText(String.valueOf(config.topN));
                
                if (config.language != null) {
                    if (config.language.equals("fr")) {
                        currentLocale = Locale.FRENCH;
                        langCombo.setSelectedItem("FR");
                    } else {
                        currentLocale = Locale.ENGLISH;
                        langCombo.setSelectedItem("EN");
                    }
                    bundle = ResourceBundle.getBundle("messages", currentLocale);
                    updateTexts();
                }

                if (config.criteriaList != null && !config.criteriaList.isEmpty()) {
                    criteriaContainer.removeAll();
                    criteriaLines.clear();
                    for (ConfigManager.CriteriaConfig cc : config.criteriaList) {
                        CriteriaLine line = new CriteriaLine(cc.columnName, cc.value, () -> performSearch(), this::removeCriteriaLine);
                        line.setColumnIndex(cc.columnIndex);
                        line.setConfig(cc);
                        criteriaLines.add(line);
                        criteriaContainer.add(line);
                    }
                    criteriaContainer.revalidate();
                    criteriaContainer.repaint();
                    
                    // Update table columns to match loaded criteria
                    updateTexts();
                }
            }
        } finally {
            isInitializing = false;
        }

        // Trigger a single search/load after initialization is complete
        if (!sourcePathField.getText().isEmpty() && !criteriaLines.isEmpty()) {
            performSearch();
        }
    }

    private void updateTexts() {
        setTitle("MakFuzz - Fuzzy Search ✨");
        appTitle.setText("MakFuzz");
        appSubtitle.setText(bundle.getString("app.header.subtitle"));
        srcLabel.setText(bundle.getString("source.label"));
        browseBtn.setText(bundle.getString("source.button.browse"));
        criteriaLabel.setText(bundle.getString("search.config.label"));
        thresholdLabel.setText(bundle.getString("search.label.threshold"));
        limitLabel.setText(bundle.getString("search.label.topn"));
        executeBtn.setText(bundle.getString("search.button.run"));
        csvBtn.setText(bundle.getString("search.button.csv"));
        excelBtn.setText(bundle.getString("search.button.excel"));
        githubLink.setToolTipText(bundle.getString("footer.github.tip"));
        loadingIcon.setText(bundle.getString("status.ready"));
        
        for (CriteriaLine cl : criteriaLines) {
            cl.updateTexts(bundle);
        }

        // Update Table Columns
        List<String> colNames = new ArrayList<>();
        colNames.add(bundle.getString("table.col.index"));
        colNames.add(bundle.getString("table.col.file_index"));
        for (CriteriaLine cl : criteriaLines) {
            colNames.add(cl.getColumnName());
            colNames.add(bundle.getString("search.metrics.s"));
            colNames.add(bundle.getString("search.metrics.p"));
        }
        colNames.add(bundle.getString("table.col.score"));
        tableModel.setColumnIdentifiers(colNames.toArray());
        applyTableColumnStyles();
        
        if (statusLabel != null && !statusLabel.getText().contains("Searching")) {
            statusLabel.setText(bundle.getString("status.ready"));
            metricsPanel.removeAll();
            metricsPanel.revalidate();
            metricsPanel.repaint();
        }
    }

    private void loadData(String path) {
        if (path == null || path.isEmpty()) {
			return;
		}
        
        List<Integer> selectedIndices = criteriaLines.stream()
                .map(CriteriaLine::getColumnIndex)
                .toList();
        
        File f = new File(path);
        // Only reload if file changed or columns changed
        if (path.equals(lastLoadedPath) && f.lastModified() == lastLoadedTimestamp && selectedIndices.equals(lastSelectedIndices) && database != null) {
            return;
        }

        try {
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.error.file_not_found"), f.getAbsolutePath()));
                database = new ArrayList<>();
                return;
            }
            
            List<String> lines = FileUtils.readLines(f, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                database = new ArrayList<>();
                return;
            }

            // Parse headers to verify indices if needed, or just skip
            // We assume the first line is header
            
            List<String[]> data = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) {
					continue; // Skip header
				}
                String lineText = lines.get(i);
                if (lineText.trim().isEmpty()) {
					continue;
				}
                
                String[] parts = lineText.split("[,;]");
                String[] selectedParts = new String[selectedIndices.size() + 1];
                for (int j = 0; j < selectedIndices.size(); j++) {
                    int idx = selectedIndices.get(j);
                    if (idx >= 0 && idx < parts.length) {
                        selectedParts[j] = parts[idx].toUpperCase().trim();
                    } else {
                        selectedParts[j] = "";
                    }
                }
                // Store 1-based line index as the last element
                selectedParts[selectedParts.length - 1] = String.valueOf(i + 1);
                data.add(selectedParts);
            }
            database = data;
            
            lastLoadedPath = path;
            lastLoadedTimestamp = f.lastModified();
            lastSelectedIndices = new ArrayList<>(selectedIndices);

            if (statusLabel != null) {
                statusLabel.setText(MessageFormat.format(bundle.getString("status.total"), database.size()));
                metricsPanel.removeAll();
                metricsPanel.revalidate();
                metricsPanel.repaint();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.error.load_error"), e.getMessage()));
        }
    }

    private void chooseFileAndColumns() {
        JFileChooser chooser = new JFileChooser();
        if (!sourcePathField.getText().isEmpty()) {
            chooser.setSelectedFile(new File(sourcePathField.getText()));
        }
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            sourcePathField.setText(selectedFile.getAbsolutePath());
            
            try {
                List<String> lines = FileUtils.readLines(selectedFile, StandardCharsets.UTF_8);
                if (lines.isEmpty()) {
					return;
				}
                
                String headerLine = lines.get(0);
                String[] columns = headerLine.split("[,;]");
                
                // Show column selection dialog
                JPanel panel = new JPanel();
                panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                panel.add(new JLabel(bundle.getString("dialog.select_columns.msg")));
                
                javax.swing.JCheckBox[] checkBoxes = new javax.swing.JCheckBox[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    checkBoxes[i] = new javax.swing.JCheckBox(columns[i].trim());
                    // Pre-select if already in criteria
                    for (CriteriaLine cl : criteriaLines) {
                        if (cl.getColumnIndex() == i) {
                            checkBoxes[i].setSelected(true);
                            break;
                        }
                    }
                    panel.add(checkBoxes[i]);
                }
                
                int result = JOptionPane.showConfirmDialog(this, new JScrollPane(panel), 
                        bundle.getString("dialog.select_columns.title"), JOptionPane.OK_CANCEL_OPTION);
                
                if (result == JOptionPane.OK_OPTION) {
                    criteriaContainer.removeAll();
                    criteriaLines.clear();
                    
                    for (int i = 0; i < checkBoxes.length; i++) {
                        if (checkBoxes[i].isSelected()) {
                            String colName = columns[i].trim();
                            CriteriaLine cl = new CriteriaLine(colName, "", () -> performSearch(), this::removeCriteriaLine);
                            cl.setColumnIndex(i);
                            criteriaLines.add(cl);
                            criteriaContainer.add(cl);
                        }
                    }
                    
                    criteriaContainer.revalidate();
                    criteriaContainer.repaint();
                    
                    // Reload data with new columns
                    loadData(selectedFile.getAbsolutePath());
                    
                    // Trigger dynamic text update for new lines
                    updateTexts();
                    
                    saveSettings();
                }
                
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error reading file headers: " + e.getMessage());
            }
        }
    }
    
    private void removeCriteriaLine(CriteriaLine line) {
        criteriaLines.remove(line);
        criteriaContainer.remove(line);
        criteriaContainer.revalidate();
        criteriaContainer.repaint();
        updateTexts();
        saveSettings();
        // Optionnaly trigger search if user wants real-time update
        // performSearch(); 
    }

    private void setupTopPanel(JPanel parent) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230)),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));


        criteriaLabel = new JLabel("Search Configuration:");
        criteriaLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        criteriaLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(criteriaLabel);

        criteriaContainer = new JPanel();
        criteriaContainer.setLayout(new BoxLayout(criteriaContainer, BoxLayout.Y_AXIS));
        criteriaContainer.setOpaque(false);
        mainPanel.add(criteriaContainer);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JLabel searchLangLabel = new JLabel("Search Lang:");
        bottomBar.add(searchLangLabel);
        
        langCombo = new JComboBox<>(new String[]{"EN", "FR"});
        langCombo.setPreferredSize(new Dimension(80, 32));
        langCombo.addActionListener(e -> {
            String selected = (String) langCombo.getSelectedItem();
            if ("EN".equals(selected)) {
                currentLocale = Locale.ENGLISH;
            } else {
                currentLocale = Locale.FRENCH;
            }
            bundle = ResourceBundle.getBundle("messages", currentLocale);
            updateTexts();
            performSearch();
        });
        bottomBar.add(langCombo);

        bottomBar.add(Box.createHorizontalStrut(15));
        
        thresholdLabel = new JLabel("Global Threshold:");
        bottomBar.add(thresholdLabel);
        globalThresholdField = new JSpinner(new SpinnerNumberModel(0.3, 0.0, 1.0, 0.05));
        globalThresholdField.setPreferredSize(new Dimension(100, 32));
        ((JSpinner.DefaultEditor)globalThresholdField.getEditor()).getTextField().addActionListener(e -> performSearch());
        globalThresholdField.addChangeListener(e -> performSearch());
        bottomBar.add(globalThresholdField);

        bottomBar.add(Box.createHorizontalStrut(15));
        limitLabel = new JLabel("Top N Limit:");
        bottomBar.add(limitLabel);
        topNField = new JTextField("1000", 5);
        topNField.setPreferredSize(new Dimension(80, 32));
        topNField.addActionListener(e -> { topNField.selectAll(); performSearch(); });
        bottomBar.add(topNField);
        bottomBar.add(Box.createHorizontalStrut(15));

        bottomBar.add(Box.createHorizontalStrut(15));
        
        executeBtn = new JButton("Run Search");
        executeBtn.setPreferredSize(new Dimension(210, 32)); // 1.5x original width
        executeBtn.setBackground(new Color(15, 30, 60)); // Matches footer background
        executeBtn.setForeground(Color.WHITE); 
        
        // Radiance specific: Force white text by preventing theme-based color changes
        RadianceThemingCortex.ComponentOrParentChainScope.setColorizationFactor(executeBtn, 1.0);
        
        executeBtn.setFocusPainted(false);
        executeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        executeBtn.addActionListener(e -> performSearch());
        bottomBar.add(executeBtn);

        csvBtn = new JButton("Export CSV");
        csvBtn.setPreferredSize(new Dimension(120, 32));
        csvBtn.addActionListener(e -> exportToCSV());
        bottomBar.add(csvBtn);

        excelBtn = new JButton("Export Excel");
        excelBtn.setPreferredSize(new Dimension(120, 32));
        excelBtn.addActionListener(e -> exportToExcel());
        bottomBar.add(excelBtn);



        mainPanel.add(bottomBar);

        parent.add(mainPanel);
    }
    
    private void setupCenterPanel() {
        tableModel = new DefaultTableModel(new String[]{
            "#", "File Row", "First Name", "Spell (FN)", "Phon (FN)", 
            "Last Name", "Spell (LN)", "Phon (LN)", 
            "Total Score"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setShowGrid(true);
        resultTable.setGridColor(new Color(230, 230, 230));
        resultTable.setRowHeight(25);
        resultTable.setSelectionBackground(new Color(232, 234, 246));
        resultTable.setSelectionForeground(Color.BLACK);
        
        // Style table header with soft background
        resultTable.getTableHeader().setBackground(new Color(232, 234, 246));
        resultTable.getTableHeader().setForeground(new Color(63, 81, 181));
        resultTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        

        
        // Add double-click listener to table header for sorting
        resultTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int column = resultTable.columnAtPoint(e.getPoint());
                    sortTableByColumn(column);
                }
            }
        });
        
        applyTableColumnStyles();

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        // Setup Loading Panel
        JPanel loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setBackground(Color.WHITE);
        
        JPanel loadingContent = new JPanel();
        loadingContent.setLayout(new BoxLayout(loadingContent, BoxLayout.Y_AXIS));
        loadingContent.setOpaque(false);
        
        loadingIcon = new JLabel("Searching..."); 
        loadingIcon.setFont(new Font("SansSerif", Font.BOLD, 18));
        loadingIcon.setForeground(new Color(63, 81, 181));
        loadingIcon.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        
        JProgressBar centerSpinner = new JProgressBar();
        centerSpinner.setIndeterminate(true);
        centerSpinner.setPreferredSize(new Dimension(200, 4));
        centerSpinner.setAlignmentX(JProgressBar.CENTER_ALIGNMENT);
        
        loadingContent.add(loadingIcon);
        loadingContent.add(Box.createVerticalStrut(10));
        loadingContent.add(centerSpinner);
        
        loadingPanel.add(loadingContent);

        // Setup Card Layout
        centerCardLayout = new CardLayout();
        centerPanel = new JPanel(centerCardLayout);
        
        centerPanel.add(scrollPane, CARD_TABLE);
        centerPanel.add(loadingPanel, CARD_LOADING);
        
        add(centerPanel, BorderLayout.CENTER);
    }

    private void applyTableColumnStyles() {
        if (resultTable == null || resultTable.getColumnCount() == 0) {
            return;
        }
        
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(javax.swing.JLabel.CENTER);
        
        javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
        
        // Table Index column (#)
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        resultTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        // File Row Index column
        if (resultTable.getColumnCount() > 1) {
            resultTable.getColumnModel().getColumn(1).setPreferredWidth(70);
            resultTable.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        }
        
        int colCount = resultTable.getColumnCount();
        for (int i = 2; i < colCount; i++) {
            if (i == colCount - 1) {
                // Score column
                resultTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
            } else {
                int internalIdx = (i - 2) % 3;
                if (internalIdx == 0) {
                   // Value column (Name, etc.)
                   resultTable.getColumnModel().getColumn(i).setPreferredWidth(150);
                } else {
                   // Score details (Spell, Phon)
                   resultTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
                }
            }
        }

        // Header Alignment
        javax.swing.table.DefaultTableCellRenderer centerHeader = new javax.swing.table.DefaultTableCellRenderer();
        centerHeader.setHorizontalAlignment(javax.swing.JLabel.CENTER);
        centerHeader.setBackground(new Color(232, 234, 246));
        centerHeader.setForeground(new Color(63, 81, 181));
        centerHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        centerHeader.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        resultTable.getColumnModel().getColumn(0).setHeaderRenderer(centerHeader);

        javax.swing.table.DefaultTableCellRenderer rightHeader = new javax.swing.table.DefaultTableCellRenderer();
        rightHeader.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
        rightHeader.setBackground(new Color(232, 234, 246));
        rightHeader.setForeground(new Color(63, 81, 181));
        rightHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
        rightHeader.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        
        for (int i = 1; i < colCount; i++) {
            if (i == colCount - 1) {
                 resultTable.getColumnModel().getColumn(i).setHeaderRenderer(rightHeader);
            } else {
                int internalIdx = (i - 1) % 3;
                if (internalIdx != 0) {
                    resultTable.getColumnModel().getColumn(i).setHeaderRenderer(rightHeader);
                }
            }
        }
    }
    

    
    private void performSearch() {
        if (isInitializing) {
			return;
		}

        // Prevent concurrent searches
        if (executeBtn != null && !executeBtn.isEnabled()) {
            searchPending = true;
            return;
        }
        
        // Reset pending flag as we are starting a fresh search
        searchPending = false;

        try {
            // Load data first (using cache if possible)
            loadData(sourcePathField.getText());
            if (database == null || database.isEmpty()) {
                if (criteriaLines.isEmpty()) {
                    JOptionPane.showMessageDialog(this, bundle.getString("dialog.error.no_columns_selected"));
                }
                return;
            }
            
            // UI Preparationc
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            if (executeBtn != null) {
				executeBtn.setEnabled(false);
			}
            if (statusLabel != null) {
				statusLabel.setText(bundle.getString("status.loading"));
                metricsPanel.removeAll();
                metricsPanel.revalidate();
                metricsPanel.repaint();
			}
            
            // Switch to loading view
            centerCardLayout.show(centerPanel, CARD_LOADING);

            // Capture parameters for thread
            List<String[]> currentDb = database;
            List<Criteria> criteriaList = new ArrayList<>();
            for (CriteriaLine cl : criteriaLines) {
                criteriaList.add(cl.getCriteria());
            }
            double globalThreshold = (Double) globalThresholdField.getValue();
            int topN = Integer.parseInt(topNField.getText());
            String lang = currentLocale.getLanguage();

            // Run search in background
            SwingWorker<SearchResult, Void> worker = new SwingWorker<>() {
                @Override
                protected SearchResult doInBackground() throws Exception {
                    return Fuzz.bestMatch(currentDb, criteriaList, globalThreshold, topN, lang);
                }

                @Override
                protected void done() {
                    try {
                        SearchResult searchResult = get();
                        updateResults(searchResult, criteriaList);
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(UI.this, MessageFormat.format(bundle.getString("dialog.error.search_error"), e.getMessage()));
                        statusLabel.setText("<html>Error occurred.</html>");
                    } finally {
                        // UI Cleanup
                        setCursor(java.awt.Cursor.getDefaultCursor());
                        if (executeBtn != null) {
							executeBtn.setEnabled(true);
						}
                        // Switch back to table view
                        centerCardLayout.show(centerPanel, CARD_TABLE);
                        
                        // If a search was requested while we were running, execute it now
                        if (searchPending) {
                            performSearch();
                        }
                    }
                }
            };
            worker.execute();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.error.setup_error"), e.getMessage()));
            setCursor(java.awt.Cursor.getDefaultCursor());
            if (executeBtn != null) {
				executeBtn.setEnabled(true);
			}
            centerCardLayout.show(centerPanel, CARD_TABLE);
        }
    }

    private void updateResults(SearchResult searchResult, List<Criteria> criteriaList) {
        try {
            List<SimResult> results = searchResult.getResults();
            int numCriteria = criteriaList.size();
            
            // Expected columns: TableIndex + FileIndex + (3 * numCriteria) + Score
            int expectedCols = 3 + 3 * numCriteria;
            if (tableModel.getColumnCount() != expectedCols) {
                updateTexts();
            }
            
            tableModel.setRowCount(0);
            int tableRowIndex = 1;
            for (SimResult res : results) {
                String[] cand = res.getCandidate();
                double[] sDetails = res.getSpellingScoreDetails();
                double[] pDetails = res.getPhoneticScoreDetails();
                
                Object[] rowValues = new Object[tableModel.getColumnCount()];
                // Column 0: Table Relative Index
                rowValues[0] = tableRowIndex++;
                
                // Column 1: File Row Index (stored at the end of the candidate array)
                rowValues[1] = (cand != null && cand.length > numCriteria) ? cand[numCriteria] : "-";
                
                int currentCell = 2;
                for (int i = 0; i < numCriteria; i++) {
                    boolean hasCriteria = criteriaList.get(i) != null;
                    
                    rowValues[currentCell++] = (cand != null && i < cand.length) ? cand[i] : "";
                    rowValues[currentCell++] = hasCriteria ? String.format("%.0f%%", (sDetails != null && i < sDetails.length) ? sDetails[i] * 100 : 0) : "";
                    rowValues[currentCell++] = hasCriteria ? String.format("%.0f%%", (pDetails != null && i < pDetails.length) ? pDetails[i] * 100 : 0) : "";
                }
                // Score column is always the last one
                if (currentCell < rowValues.length) {
                    rowValues[currentCell] = String.format("%.2f%%", res.getScore() * 100);
                }
                tableModel.addRow(rowValues);
            }
            
            // Detailed metrics in footer
            statusLabel.setText("");
            populateMetricsPanel(searchResult, criteriaList);
            
            // Update separate Total Label
            totalLabel.setText(MessageFormat.format(bundle.getString("status.total"), searchResult.getTotalFound()));
            
            // Save original table data for sorting
            originalTableData = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Object[] row = new Object[tableModel.getColumnCount()];
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    row[j] = tableModel.getValueAt(i, j);
                }
                originalTableData.add(row);
            }
            
            lastSortedColumn = -1;
            sortState = 0;
            
        } catch (Exception e) {
            e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.error.search_error"), e.getMessage()));
        }
    }

    private void populateMetricsPanel(SearchResult searchResult, List<Criteria> criteriaList) {
        metricsPanel.removeAll();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(1, 0, 1, 0);

        // Header Row
        int col = 0;
        addMetricHeader(bundle.getString("search.metrics.label"), col++, 0, gbc, true);
        addMetricHeader(bundle.getString("search.metrics.total"), col++, 0, gbc, false);
        
        // Use criteriaList to ensure we match the results we received
        // We still use current criteriaLines for names if possible, or fall back to Generic
        for (int i = 0; i < criteriaList.size(); i++) {
            String name = (i < criteriaLines.size()) ? criteriaLines.get(i).getColumnName() : "Col " + (i + 1);
            addMetricHeader(name, col++, 0, gbc, true);
            addMetricHeader(bundle.getString("search.metrics.s"), col++, 0, gbc, false);
            addMetricHeader(bundle.getString("search.metrics.p"), col++, 0, gbc, false);
        }

        // Data Rows
        gbc.insets = new Insets(0, 0, 0, 0); // No vertical margins between rows
        addRowToMetrics(bundle.getString("search.metrics.max_under"), searchResult.getMaxUnderCandidate(), searchResult.getMaxUnderThreshold(), 1, gbc, criteriaList);
        addRowToMetrics(bundle.getString("search.metrics.min_above"), searchResult.getMinAboveCandidate(), searchResult.getMinAboveThreshold(), 2, gbc, criteriaList);
        addRowToMetrics(bundle.getString("search.metrics.max_above"), searchResult.getMaxAboveCandidate(), searchResult.getMaxAboveThreshold(), 3, gbc, criteriaList);

        metricsPanel.revalidate();
        metricsPanel.repaint();
    }

    private void addMetricHeader(String text, int x, int y, GridBagConstraints gbc, boolean left) {
        JLabel lbl = new JLabel("<html><b>" + text + "</b></html>");
        styleMetricLabel(lbl, x, y, gbc, left);
        metricsPanel.add(lbl, gbc);
    }

    private void styleMetricLabel(JLabel lbl, int x, int y, GridBagConstraints gbc, boolean left) {
        lbl.setForeground(Color.WHITE);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setHorizontalAlignment(left ? JLabel.LEFT : JLabel.RIGHT);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10)); // No vertical padding

        // Segmented background for clarity with SOLID colors
        int blockIdx = (x < 2) ? 0 : ((x - 2) / 3) + 1;
        if (blockIdx % 2 == 1) {
            lbl.setBackground(new Color(45, 65, 120)); // Lighter solid nautical blue
        } else {
            lbl.setBackground(new Color(25, 35, 75));  // Darker solid nautical blue
        }
        lbl.setOpaque(true);
        
        gbc.gridx = x;
        gbc.gridy = y;
    }

    private void addRowToMetrics(String label, SimResult result, double threshold, int row, GridBagConstraints gbc, List<Criteria> criteriaList) {
        int col = 0;
        addMetricValue(label, col++, row, gbc, true, false, null, 0);
        
        // Threshold/Total column
        addMetricValue(String.format("%.2f%%", threshold * 100), col++, row, gbc, false, true, 
            val -> globalThresholdField.setValue(val), threshold);

        int numCriteria = criteriaList.size();
        if (result != null) {
            String[] cand = result.getCandidate();
            double[] sDetails = result.getSpellingScoreDetails();
            double[] pDetails = result.getPhoneticScoreDetails();

            for (int i = 0; i < numCriteria; i++) {
                // If the current criteriaLines is out of sync, we don't allow clicking the metric
                // but we still display the value
                boolean sync = (i < criteriaLines.size());
                CriteriaLine cl = sync ? criteriaLines.get(i) : null;
                
                // Value
                addMetricValue(cand != null && i < cand.length ? cand[i] : "-", col++, row, gbc, true, false, null, 0);
                
                // S
                double s = (sDetails != null && i < sDetails.length) ? sDetails[i] : -1;
                addMetricValue(s >= 0 ? String.format("%.0f%%", s * 100) : "-", col++, row, gbc, false, sync && s >= 0,
                    val -> { if (cl != null) {
						cl.setMinSpelling(val);
					} }, s);

                // P
                double p = (pDetails != null && i < pDetails.length) ? pDetails[i] : -1;
                addMetricValue(p >= 0 ? String.format("%.0f%%", p * 100) : "-", col++, row, gbc, false, sync && p >= 0,
                    val -> { if (cl != null) {
						cl.setMinPhonetic(val);
					} }, p);
            }
        } else {
             for (int i = 0; i < numCriteria * 3; i++) {
                 addMetricValue("-", col++, row, gbc, (i % 3 == 0), false, null, 0);
             }
        }
    }
    
    private static final boolean[] leftCols = {true, false, false, true, false, false};

    private void addMetricValue(String text, int x, int y, GridBagConstraints gbc, boolean left, boolean clickable, java.util.function.Consumer<Double> onUpdate, double value) {
        JLabel lbl = new JLabel("<html>" + text + "</html>");
        styleMetricLabel(lbl, x, y, gbc, left);
        
        if (clickable) {
            lbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            lbl.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    // Round down to 2 decimal places: 0.523 -> 0.52
                    double rounded = Math.floor(value * 100) / 100.0;
                    onUpdate.accept(rounded);
                    performSearch();
                }
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    lbl.setText("<html><u>" + text + "</u></html>");
                    lbl.setForeground(Color.YELLOW); // Yellow on hover as requested
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    lbl.setText(text);
                    lbl.setForeground(Color.WHITE);
                }
            });
        }
        metricsPanel.add(lbl, gbc);
    }
    
    private void sortTableByColumn(int column) {
        if (originalTableData == null || originalTableData.isEmpty()) {
            return;
        }
        
        // If clicking a different column, reset to descending
        if (column != lastSortedColumn) {
            lastSortedColumn = column;
            sortState = 1; // Start with descending
        } else {
            // Cycle through states: 0 (unsorted) -> 1 (desc) -> 2 (asc) -> 0 (unsorted)
            sortState = (sortState + 1) % 3;
        }
        
        if (sortState == 0) {
            // Restore original unsorted data
            tableModel.setRowCount(0);
            for (Object[] row : originalTableData) {
                tableModel.addRow(row);
            }
        } else {
            // Sort the data
            List<Object[]> sortedData = new ArrayList<>(originalTableData);
            final int sortColumn = column;
            final boolean ascending = (sortState == 2);
            
            sortedData.sort((row1, row2) -> {
                Object val1 = row1[sortColumn];
                Object val2 = row2[sortColumn];
                
                int comparison = compareValues(val1, val2);
                return ascending ? comparison : -comparison;
            });
            
            // Update table with sorted data
            tableModel.setRowCount(0);
            for (Object[] row : sortedData) {
                tableModel.addRow(row);
            }
        }
    }
    
    private int compareValues(Object val1, Object val2) {
        // Handle null values
        if (val1 == null && val2 == null) {
			return 0;
		}
        if (val1 == null) {
			return -1;
		}
        if (val2 == null) {
			return 1;
		}
        
        String str1 = val1.toString();
        String str2 = val2.toString();
        
        // Try to parse as numbers (for percentages and numeric values)
        try {
            // Remove % sign if present
            String num1 = str1.replace("%", "").trim();
            String num2 = str2.replace("%", "").trim();
            
            double d1 = Double.parseDouble(num1);
            double d2 = Double.parseDouble(num2);
            return Double.compare(d1, d2);
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return str1.compareToIgnoreCase(str2);
        }
    }

    private File getNextFile(File file) {
        String parent = file.getParent();
        String name = file.getName();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }

        int count = 1;
        // Check if name already has a suffix like "-1", "-2"
        if (base.contains("-")) {
            int dash = base.lastIndexOf('-');
            try {
                count = Integer.parseInt(base.substring(dash + 1)) + 1;
                base = base.substring(0, dash);
            } catch (NumberFormatException e) {
                // Not a numeric suffix, just append -1
            }
        }

        return new File(parent, base + "-" + count + ext);
    }

    private void exportToCSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, bundle.getString("dialog.export.no_data"));
            return;
        }
        promptAndSaveCSV(new File("search_results.csv"));
    }

    private void promptAndSaveCSV(File initialFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(initialFile);
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile.exists()) {
                int response = JOptionPane.showConfirmDialog(this,
                    MessageFormat.format(bundle.getString("dialog.export.replace_confirm"), selectedFile.getName()),
                    bundle.getString("dialog.export.replace_title"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                
                if (response == JOptionPane.NO_OPTION) {
                    promptAndSaveCSV(getNextFile(selectedFile));
                    return;
                } else if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            saveCSV(selectedFile);
        }
    }

    private void saveCSV(File file) {
        try (FileWriter out = new FileWriter(file)) {
            // Fetch original lines and headers
            java.util.Map<Integer, String> originalLines = fetchOriginalLines();
            String[] originalHeaders = fetchOriginalHeaders();
            
            // Write Headers
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                out.write(escapeCSV(tableModel.getColumnName(i)) + ",");
            }
            // Add original CSV headers
            for (int i = 0; i < originalHeaders.length; i++) {
                out.write(escapeCSV(originalHeaders[i]));
                if (i < originalHeaders.length - 1) {
					out.write(",");
				}
            }
            out.write("\n");

            // Write Data
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    Object val = tableModel.getValueAt(i, j);
                    String v = (val == null ? "" : val.toString());
                    out.write(escapeCSV(v) + ",");
                }
                
                // Append original row fields as individual columns
                try {
                    int fileRowIdx = Integer.parseInt(tableModel.getValueAt(i, 1).toString());
                    String fullLine = originalLines.getOrDefault(fileRowIdx, "");
                    String[] fields = parseCSVLine(fullLine);
                    for (int k = 0; k < fields.length; k++) {
                        out.write(escapeCSV(fields[k]));
                        if (k < fields.length - 1) {
							out.write(",");
						}
                    }
                } catch (Exception e) {
                    // Write empty fields if parsing fails
                    for (int k = 0; k < originalHeaders.length; k++) {
                        if (k > 0) {
							out.write(",");
						}
                    }
                }
                out.write("\n");
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.export.failed"), e.getMessage()));
        }
    }

    private String[] fetchOriginalHeaders() {
        File file = new File(sourcePathField.getText());
        if (!file.exists()) {
			return new String[0];
		}
        
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine != null) {
                return parseCSVLine(headerLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String[0];
    }
    
    private String[] parseCSVLine(String line) {
        if (line == null || line.trim().isEmpty()) {
			return new String[0];
		}
        // Simple CSV parser - splits on comma or semicolon
        String[] parts = line.split("[,;]");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
            // Remove quotes if present
            if (parts[i].startsWith("\"") && parts[i].endsWith("\"")) {
                parts[i] = parts[i].substring(1, parts[i].length() - 1).replace("\"\"", "\"");
            }
        }
        return parts;
    }
    
    private String escapeCSV(String value) {
        if (value == null) {
			return "";
		}
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private java.util.Map<Integer, String> fetchOriginalLines() { 
        java.util.Map<Integer, String> lineMap = new java.util.HashMap<>();
        java.util.Set<Integer> neededIndices = new java.util.HashSet<>();
        
        // Collect indices from table (Column 1 is File Row Index)
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                Object val = tableModel.getValueAt(i, 1);
                if (val != null) {
                    neededIndices.add(Integer.parseInt(val.toString()));
                }
            } catch (NumberFormatException e) {
                // Ignore non-numeric indices
            }
        }
        
        if (neededIndices.isEmpty()) {
			return lineMap;
		}
        
        File file = new File(sourcePathField.getText());
        if (!file.exists()) {
			return lineMap;
		}
        
        // Read file in single pass
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            int currentLine = 1; // 1-based index
            // Optimize: stop when we found all needed lines if max index is reached?
            // Since file might not be sorted by index if shuffled, we can't stop early unless we track found count.
            int foundCount = 0;
            int totalNeeded = neededIndices.size();
            
            while ((line = br.readLine()) != null) {
                if (neededIndices.contains(currentLine)) {
                    lineMap.put(currentLine, line);
                    foundCount++;
                    if (foundCount >= totalNeeded) {
						break;
					} 
                }
                currentLine++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineMap;
    }

    private void exportToExcel() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, bundle.getString("dialog.export.no_data"));
            return;
        }
        promptAndSaveExcel(new File("search_results.xlsx"));
    }

    private void promptAndSaveExcel(File initialFile) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(initialFile);
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Files", "xlsx"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = chooser.getSelectedFile();
            if (selectedFile.exists()) {
                int response = JOptionPane.showConfirmDialog(this,
                    MessageFormat.format(bundle.getString("dialog.export.replace_confirm"), selectedFile.getName()),
                    bundle.getString("dialog.export.replace_title"),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                
                if (response == JOptionPane.NO_OPTION) {
                    promptAndSaveExcel(getNextFile(selectedFile));
                    return;
                } else if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            saveExcel(selectedFile);
        }
    }

    private void saveExcel(File selectedFile) {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(selectedFile)) {
                
                // Fetch original lines and headers
                java.util.Map<Integer, String> originalLines = fetchOriginalLines();
                String[] originalHeaders = fetchOriginalHeaders();

                Sheet sheet = workbook.createSheet("Search Results");
                
                // Styling for synthesis columns (blue)
                org.apache.poi.ss.usermodel.Font synthesisFont = workbook.createFont();
                synthesisFont.setBold(true);
                synthesisFont.setColor(IndexedColors.WHITE.getIndex());
                
                CellStyle synthesisHeaderStyle = workbook.createCellStyle();
                synthesisHeaderStyle.setFont(synthesisFont);
                synthesisHeaderStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
                synthesisHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                synthesisHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
                synthesisHeaderStyle.setBorderTop(BorderStyle.THIN);
                synthesisHeaderStyle.setBorderBottom(BorderStyle.THIN);
                synthesisHeaderStyle.setBorderLeft(BorderStyle.THIN);
                synthesisHeaderStyle.setBorderRight(BorderStyle.THIN);
                
                // Styling for original columns (green)
                org.apache.poi.ss.usermodel.Font originalFont = workbook.createFont();
                originalFont.setBold(true);
                originalFont.setColor(IndexedColors.WHITE.getIndex());
                
                CellStyle originalHeaderStyle = workbook.createCellStyle();
                originalHeaderStyle.setFont(originalFont);
                originalHeaderStyle.setFillForegroundColor(IndexedColors.SEA_GREEN.getIndex());
                originalHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                originalHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
                originalHeaderStyle.setBorderTop(BorderStyle.THIN);
                originalHeaderStyle.setBorderBottom(BorderStyle.THIN);
                originalHeaderStyle.setBorderLeft(BorderStyle.THIN);
                originalHeaderStyle.setBorderRight(BorderStyle.THIN);
                
                // Write Header
                Row headerRow = sheet.createRow(0);
                int colIdx = 0;
                
                // Synthesis columns (blue headers)
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    Cell cell = headerRow.createCell(colIdx++);
                    cell.setCellValue(tableModel.getColumnName(i));
                    cell.setCellStyle(synthesisHeaderStyle);
                }
                
                // Original CSV headers (green headers)
                for (String header : originalHeaders) {
                    Cell cell = headerRow.createCell(colIdx++);
                    cell.setCellValue(header);
                    cell.setCellStyle(originalHeaderStyle);
                }
                
                // Write Data
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    Row row = sheet.createRow(i + 1);
                    colIdx = 0;
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        Cell cell = row.createCell(colIdx++);
                        Object val = tableModel.getValueAt(i, j);
                        if (val instanceof Number) {
                            cell.setCellValue(((Number) val).doubleValue());
                        } else {
                            cell.setCellValue(val == null ? "" : val.toString());
                        }
                    }
                    
                    // Append original row fields as individual columns
                    try {
                        int fileRowIdx = Integer.parseInt(tableModel.getValueAt(i, 1).toString());
                        String fullLine = originalLines.getOrDefault(fileRowIdx, "");
                        String[] fields = parseCSVLine(fullLine);
                        for (String field : fields) {
                            row.createCell(colIdx++).setCellValue(field);
                        }
                    } catch (Exception e) {
                        // Write empty cells if parsing fails
                        for (int k = 0; k < originalHeaders.length; k++) {
                            row.createCell(colIdx++);
                        }
                    }
                }
                
                // Auto-size all columns to fit content
                int totalColumns = tableModel.getColumnCount() + originalHeaders.length;
                for (int i = 0; i < totalColumns; i++) {
                    sheet.autoSizeColumn(i);
                }
                
                // Enable AutoFilter for all columns
                if (tableModel.getRowCount() > 0) {
                    sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                        0, // First row (header)
                        tableModel.getRowCount(), // Last row
                        0, // First column
                        totalColumns - 1 // Last column
                    ));
                }
                
                // Freeze panes: Keep synthesis columns visible when scrolling horizontally
                // Freeze after all synthesis columns (before original CSV columns start)
                int freezeAfterColumn = tableModel.getColumnCount();
                sheet.createFreezePane(freezeAfterColumn, 1); // Freeze columns and first row (header)
                
                workbook.write(fileOut);
                
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(selectedFile);
                }
                
        } catch (IOException e) {
             e.printStackTrace();
             JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.export.failed"), e.getMessage()));
        }
    }


    private static class CriteriaLine extends JPanel {
        private String columnName = "";
        private int columnIndex = -1;
        private JTextField valueField;
        private JComboBox<Criteria.MatchingType> typeCombo;
        private JSpinner weightSpinner;
        private JSpinner minSpellingField;
        private JSpinner minPhoneticField;
        private JLabel minSpellingLabel;
        private JLabel minPhoneticLabel;
        private JLabel mainLabel;
        private JLabel valueLabel;
        private JLabel typeLabel;
        private JLabel weightLabel;
        private final Runnable onEnter;

        private JButton removeBtn;

        public CriteriaLine(String colName, String defaultValue, Runnable onEnter, java.util.function.Consumer<CriteriaLine> removeAction) {
            this.columnName = colName;
            this.onEnter = onEnter;
            setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
            setOpaque(false);
            
            removeBtn = new JButton("×");
            removeBtn.setToolTipText("Remove this criterion");
            removeBtn.setForeground(new Color(211, 47, 47)); // Material Red
            removeBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
            removeBtn.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            removeBtn.setContentAreaFilled(false);
            removeBtn.setFocusPainted(false);
            removeBtn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            removeBtn.addActionListener(e -> removeAction.accept(this));
            add(removeBtn);

            mainLabel = new JLabel(colName);
            mainLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            mainLabel.setPreferredSize(new Dimension(100, 25));
            add(mainLabel);

            valueLabel = new JLabel("Value:");
            add(valueLabel);
            valueField = new JTextField(defaultValue, 12);
            valueField.setPreferredSize(new Dimension(150, 32));
            valueField.addActionListener(e -> { valueField.selectAll(); onEnter.run(); });
            add(valueField);

            typeLabel = new JLabel("Type:");
            add(typeLabel);
            typeCombo = new JComboBox<>(Criteria.MatchingType.values());
            typeCombo.setSelectedItem(Criteria.MatchingType.SIMILARITY);
            typeCombo.setPreferredSize(new Dimension(140, 32));
            add(typeCombo);

            weightLabel = new JLabel("Weight:");
            add(weightLabel);
            weightSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
            weightSpinner.setPreferredSize(new Dimension(70, 32));
            weightSpinner.addChangeListener(e -> onEnter.run());
            add(weightSpinner);

            minSpellingLabel = new JLabel("Min Spell:");
            minSpellingField = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.05));
            minSpellingField.setPreferredSize(new Dimension(80, 32));
            ((JSpinner.DefaultEditor)minSpellingField.getEditor()).getTextField().addActionListener(e -> onEnter.run());
            minSpellingField.addChangeListener(e -> onEnter.run());
            add(minSpellingLabel);
            add(minSpellingField);

            minPhoneticLabel = new JLabel("Min Phon:");
            minPhoneticField = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.05));
            minPhoneticField.setPreferredSize(new Dimension(80, 32));
            ((JSpinner.DefaultEditor)minPhoneticField.getEditor()).getTextField().addActionListener(e -> onEnter.run());
            minPhoneticField.addChangeListener(e -> onEnter.run());
            add(minPhoneticLabel);
            add(minPhoneticField);

            typeCombo.addActionListener(e -> {
                boolean isSimilarity = typeCombo.getSelectedItem() == Criteria.MatchingType.SIMILARITY;
                minSpellingLabel.setVisible(isSimilarity);
                minSpellingField.setVisible(isSimilarity);
                minPhoneticLabel.setVisible(isSimilarity);
                minPhoneticField.setVisible(isSimilarity);
                revalidate();
                repaint();
            });

            // Trigger initial visibility
            boolean isSimilarity = typeCombo.getSelectedItem() == Criteria.MatchingType.SIMILARITY;
            minSpellingLabel.setVisible(isSimilarity);
            minSpellingField.setVisible(isSimilarity);
            minPhoneticLabel.setVisible(isSimilarity);
            minPhoneticField.setVisible(isSimilarity);
        }

        public String getColumnName() { return columnName; }
        public void setColumnIndex(int idx) { this.columnIndex = idx; }
        public int getColumnIndex() { return this.columnIndex; }

        public Criteria getCriteria() {
            String val = valueField.getText().trim().toUpperCase();
            if (val.isEmpty()) {
				return null;
			}

            Criteria.MatchingType type = (Criteria.MatchingType) typeCombo.getSelectedItem();
            int weight = (Integer) weightSpinner.getValue();
            double minSpell = (Double) minSpellingField.getValue();
            double minPhon = (Double) minPhoneticField.getValue();

            if (type == Criteria.MatchingType.SIMILARITY) {
                return Criteria.similarity(val, weight, minSpell, minPhon);
            } else if (type == Criteria.MatchingType.EXACT) {
                return Criteria.exact(val, weight);
            } else {
                return Criteria.regex(val, weight);
            }
        }
        
        public ConfigManager.CriteriaConfig getConfig() {
            ConfigManager.CriteriaConfig cc = new ConfigManager.CriteriaConfig();
            cc.columnName = this.columnName;
            cc.columnIndex = this.columnIndex;
            cc.value = valueField.getText();
            cc.type = ((Criteria.MatchingType) typeCombo.getSelectedItem()).name();
            cc.weight = (Integer) weightSpinner.getValue();
            cc.minSpelling = (Double) minSpellingField.getValue();
            cc.minPhonetic = (Double) minPhoneticField.getValue();
            return cc;
        }

        public void setConfig(ConfigManager.CriteriaConfig cc) {
            if (cc == null) {
				return;
			}
            valueField.setText(cc.value);
            try {
                typeCombo.setSelectedItem(Criteria.MatchingType.valueOf(cc.type));
            } catch (Exception e) {
                typeCombo.setSelectedItem(Criteria.MatchingType.SIMILARITY);
            }
            weightSpinner.setValue(cc.weight);
            minSpellingField.setValue(cc.minSpelling);
            minPhoneticField.setValue(cc.minPhonetic);
        }

        public void updateTexts(ResourceBundle bundle) {
            valueLabel.setText(bundle.getString("search.criteria.value"));
            typeLabel.setText(bundle.getString("search.criteria.type"));
            weightLabel.setText(bundle.getString("search.criteria.weight"));
            minSpellingLabel.setText(bundle.getString("search.criteria.min_spell"));
            minPhoneticLabel.setText(bundle.getString("search.criteria.min_phon"));
            removeBtn.setToolTipText(bundle.getString("search.criteria.remove.tip"));
        }

        public void setMinSpelling(double val) {
            minSpellingField.setValue(val);
        }

        public void setMinPhonetic(double val) {
            minPhoneticField.setValue(val);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UI().setVisible(true);
        });
    }
}
