package com.makfuzz;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
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

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatIntelliJLaf;
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
    private CriteriaLine fnLine;
    private CriteriaLine lnLine;
    private JTextField sourcePathField;
    private JSpinner globalThresholdField;
    private JTextField topNField;
    private JButton executeBtn;
    private JLabel statusLabel;
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
    
    // Card Layout for Center Panel
    private CardLayout centerCardLayout;
    private JPanel centerPanel;
    private static final String CARD_TABLE = "TABLE";
    private static final String CARD_LOADING = "LOADING";
    
    public UI() {
        // Apply Modern Theme
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("ScrollBar.trackArc", 999);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.track", new Color(0xf5f5f5));
        } catch (Exception ignored) {}

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
        sourcePanel.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #ffffff; ");
        
        srcLabel = new JLabel("Data Source:");
        srcLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        sourcePanel.add(srcLabel);

        sourcePathField = new JTextField("./names.csv", 60);
        sourcePathField.putClientProperty(FlatClientProperties.STYLE, "showClearButton: true; arc: 8");
        
        browseBtn = new JButton("Browse");
        browseBtn.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton; focusWidth: 0");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(new File("."));
            chooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                sourcePathField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

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
        statusBar.setBackground(new Color(63, 81, 181));
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        
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
        
        config.fnCriteria = fnLine.getConfig();
        config.lnCriteria = lnLine.getConfig();
        
        File configFile = new File(System.getProperty("user.home"), ".makfuzz_config.xml");
        ConfigManager.saveConfig(config, configFile);
    }

    private void loadSettings() {
        File configFile = new File(System.getProperty("user.home"), ".makfuzz_config.xml");
        if (!configFile.exists()) {
			return;
		}
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

            if (config.fnCriteria != null) {
				fnLine.setConfig(config.fnCriteria);
			}
            if (config.lnCriteria != null) {
				lnLine.setConfig(config.lnCriteria);
			}
        }
    }

    private void updateTexts() {
        setTitle("MakFuzz - Fuzzy Search ✨");
        appTitle.setText("MakFuzz");
        appSubtitle.setText(bundle.getString("app.header.subtitle"));
        srcLabel.setText(bundle.getString("source.label"));
        browseBtn.setText(bundle.getString("source.button.browse"));
        sourcePathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, bundle.getString("source.placeholder"));
        criteriaLabel.setText(bundle.getString("search.config.label"));
        thresholdLabel.setText(bundle.getString("search.label.threshold"));
        limitLabel.setText(bundle.getString("search.label.topn"));
        executeBtn.setText(bundle.getString("search.button.run"));
        csvBtn.setText(bundle.getString("search.button.csv"));
        excelBtn.setText(bundle.getString("search.button.excel"));
        githubLink.setToolTipText(bundle.getString("footer.github.tip"));
        loadingIcon.setText(bundle.getString("status.ready"));
        
        // Update Table Columns
        String[] columnNames = {
            bundle.getString("table.col.index"),
            bundle.getString("table.col.fn"),
            bundle.getString("table.col.sfn"),
            bundle.getString("table.col.pfn"),
            bundle.getString("table.col.ln"),
            bundle.getString("table.col.sln"),
            bundle.getString("table.col.pln"),
            bundle.getString("table.col.score")
        };
        tableModel.setColumnIdentifiers(columnNames);
        applyTableColumnStyles(); 

        fnLine.updateTexts(bundle, bundle.getString("search.label.fn"));
        lnLine.updateTexts(bundle, bundle.getString("search.label.ln"));
        
        if (statusLabel != null && !statusLabel.getText().contains("Searching")) {
            statusLabel.setText(bundle.getString("status.ready"));
        }
    }

    private void loadData(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.error.file_not_found"), f.getAbsolutePath()));
                database = new ArrayList<>();
                return;
            }
            List<String> lines = FileUtils.readLines(f, StandardCharsets.UTF_8);
            // Skip header and split
            database = lines.stream()
                .skip(1)
                .filter(l -> !l.trim().isEmpty())
                .map(line -> line.toUpperCase().split("[,;]"))
                .toList();
            
            if (statusLabel != null) {
                statusLabel.setText(MessageFormat.format(bundle.getString("status.total"), database.size()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.error.load_error"), e.getMessage()));
        }
    }
    
    private void setupTopPanel(JPanel parent) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #ffffff; ");
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        criteriaLabel = new JLabel("Search Configuration:");
        criteriaLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        criteriaLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(criteriaLabel);

        fnLine = new CriteriaLine("First Name:", "", this::performSearch);
        lnLine = new CriteriaLine("Last Name:", "", this::performSearch);

        mainPanel.add(fnLine);
        mainPanel.add(lnLine);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JLabel searchLangLabel = new JLabel("Search Lang:");
        bottomBar.add(searchLangLabel);
        
        langCombo = new JComboBox<>(new String[]{"EN", "FR"});
        langCombo.putClientProperty(FlatClientProperties.STYLE, "arc: 8; background: #3f51b5; foreground: #ffffff; focusWidth: 0;");
        langCombo.setPreferredSize(new Dimension(60, 30));
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
        globalThresholdField.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
        globalThresholdField.setPreferredSize(new Dimension(80, 30));
        ((JSpinner.DefaultEditor)globalThresholdField.getEditor()).getTextField().addActionListener(e -> performSearch());
        globalThresholdField.addChangeListener(e -> performSearch());
        bottomBar.add(globalThresholdField);

        bottomBar.add(Box.createHorizontalStrut(15));
        limitLabel = new JLabel("Top N Limit:");
        bottomBar.add(limitLabel);
        topNField = new JTextField("1000", 5);
        topNField.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
        topNField.addActionListener(e -> { topNField.selectAll(); performSearch(); });
        bottomBar.add(topNField);
        bottomBar.add(Box.createHorizontalStrut(15));

        bottomBar.add(Box.createHorizontalStrut(15));
        
        executeBtn = new JButton("Run Search");
        executeBtn.setBackground(new Color(63, 81, 181));
        executeBtn.setForeground(Color.WHITE);
        executeBtn.putClientProperty(FlatClientProperties.STYLE, "hoverBackground: #303F9F; pressedBackground: #1a237e; arc: 10");
        executeBtn.setFocusPainted(false);
        executeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        executeBtn.addActionListener(e -> performSearch());
        bottomBar.add(executeBtn);

        csvBtn = new JButton("Export CSV");
        csvBtn.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton");
        csvBtn.addActionListener(e -> exportToCSV());
        bottomBar.add(csvBtn);

        excelBtn = new JButton("Export Excel");
        excelBtn.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton");
        excelBtn.addActionListener(e -> exportToExcel());
        bottomBar.add(excelBtn);



        mainPanel.add(bottomBar);

        parent.add(mainPanel);
    }
    
    private void setupCenterPanel() {
        tableModel = new DefaultTableModel(new String[]{
            "#", "First Name", "Spell (FN)", "Phon (FN)", 
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
        
        // Modern alternating colors
        resultTable.putClientProperty(FlatClientProperties.STYLE, "showHorizontalLines: true; showVerticalLines: true; rowHeight: 28;");
        
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
        if (resultTable == null) {
			return;
		}
        
        // Adjust column widths roughly
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(40); // Index column
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(150); // FN column
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(150); // LN column
        
        // Column Alignment
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(javax.swing.JLabel.CENTER);
        resultTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        
        javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
        resultTable.getColumnModel().getColumn(2).setCellRenderer(rightRenderer); 
        resultTable.getColumnModel().getColumn(3).setCellRenderer(rightRenderer); 
        resultTable.getColumnModel().getColumn(5).setCellRenderer(rightRenderer); 
        resultTable.getColumnModel().getColumn(6).setCellRenderer(rightRenderer); 
        resultTable.getColumnModel().getColumn(7).setCellRenderer(rightRenderer); 
        
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
        resultTable.getColumnModel().getColumn(2).setHeaderRenderer(rightHeader);
        resultTable.getColumnModel().getColumn(3).setHeaderRenderer(rightHeader);
        resultTable.getColumnModel().getColumn(5).setHeaderRenderer(rightHeader);
        resultTable.getColumnModel().getColumn(6).setHeaderRenderer(rightHeader);
        resultTable.getColumnModel().getColumn(7).setHeaderRenderer(rightHeader);
    }
    
    private void performSearch() {
        // Prevent concurrent searches
        if (executeBtn != null && !executeBtn.isEnabled()) {
            searchPending = true;
            return;
        }
        
        // Reset pending flag as we are starting a fresh search
        searchPending = false;

        try {
            // Load data first (fast)
            loadData(sourcePathField.getText());
            if (database == null || database.isEmpty()) {
                return;
            }
            
            // UI Preparation
            setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
            if (executeBtn != null) {
				executeBtn.setEnabled(false);
			}
            if (statusLabel != null) {
				statusLabel.setText(bundle.getString("status.loading"));
			}
            
            // Switch to loading view
            centerCardLayout.show(centerPanel, CARD_LOADING);

            // Capture parameters for thread
            String sourcePath = sourcePathField.getText();
            List<Criteria> criteriaList = new ArrayList<>();
            criteriaList.add(fnLine.getCriteria());
            criteriaList.add(lnLine.getCriteria());
            double globalThreshold = (Double) globalThresholdField.getValue();
            int topN = Integer.parseInt(topNField.getText());
            String lang = currentLocale.getLanguage();

            // Run search in background
            SwingWorker<SearchResult, Void> worker = new SwingWorker<>() {
                @Override
                protected SearchResult doInBackground() throws Exception {
                    return Fuzz.bestMatch(database, criteriaList, globalThreshold, topN, lang);
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
            // Ensure UI is reset if synchronous setup fails
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
            
            tableModel.setRowCount(0);
            int rowIndex = 1;
            for (SimResult res : results) {
                String[] cand = res.getCandidate();
                double[] sDetails = res.getSpellingScoreDetails();
                double[] pDetails = res.getPhoneticScoreDetails();
                
                // Helper to determine if we should show a score for an index
                // We show it only if the user actually input something for that line
                boolean hasFN = criteriaList.get(0) != null;
                boolean hasLN = criteriaList.get(1) != null;

                tableModel.addRow(new Object[]{
                    rowIndex++,
                    cand.length > 0 ? cand[0] : "",
                    hasFN ? String.format("%.0f%%", (sDetails != null && sDetails.length > 0) ? sDetails[0] * 100 : 0) : "",
                    hasFN ? String.format("%.0f%%", (pDetails != null && pDetails.length > 0) ? pDetails[0] * 100 : 0) : "",
                    cand.length > 1 ? cand[1] : "",
                    hasLN ? String.format("%.0f%%", (sDetails != null && sDetails.length > 1) ? sDetails[1] * 100 : 0) : "",
                    hasLN ? String.format("%.0f%%", (pDetails != null && pDetails.length > 1) ? pDetails[1] * 100 : 0) : "",
                    String.format("%.2f%%", res.getScore() * 100)
                });
            }
            
            
            
            // Build status message with candidate details using an aligned HTML table
            StringBuilder statusMsg = new StringBuilder("<html><table border='0' cellspacing='0' cellpadding='0'>");
            
            // Header Row
            statusMsg.append("<tr>");
            statusMsg.append("<td align='left'><b>").append(bundle.getString("search.metrics.label")).append("</b></td><td width='20'></td>");
            statusMsg.append("<td align='right'><b>").append(bundle.getString("search.metrics.total")).append("</b></td><td width='20'></td>");
            statusMsg.append("<td align='left'><b>").append(bundle.getString("search.metrics.fn")).append("</b></td><td width='20'></td>");
            statusMsg.append("<td align='right'><b>").append(bundle.getString("search.metrics.s")).append("</b></td><td width='20'></td>");
            statusMsg.append("<td align='right'><b>").append(bundle.getString("search.metrics.p")).append("</b></td><td width='20'></td>");
            statusMsg.append("<td align='left'><b>").append(bundle.getString("search.metrics.ln")).append("</b></td><td width='20'></td>");
            statusMsg.append("<td align='right'><b>").append(bundle.getString("search.metrics.s")).append("</b></td><td width='20'></td>");
            statusMsg.append("<td align='right'><b>").append(bundle.getString("search.metrics.p")).append("</b></td>");
            statusMsg.append("</tr>");

            // Helper to generate row for a candidate
            generateStatusRow(statusMsg, bundle.getString("search.metrics.max_under"), searchResult.getMaxUnderCandidate(), searchResult.getMaxUnderThreshold());
            generateStatusRow(statusMsg, bundle.getString("search.metrics.min_above"), searchResult.getMinAboveCandidate(), searchResult.getMinAboveThreshold());
            generateStatusRow(statusMsg, bundle.getString("search.metrics.max_above"), searchResult.getMaxAboveCandidate(), searchResult.getMaxAboveThreshold());
            
            statusMsg.append("</table></html>");
            statusLabel.setText(statusMsg.toString());
            
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
            
            // Reset sort state
            lastSortedColumn = -1;
            sortState = 0;
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, bundle.getString("dialog.error.numbers"));
        } catch (Exception e) {
            e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.error.search_error"), e.getMessage()));
        }
    }
    
    private void generateStatusRow(StringBuilder sb, String label, SimResult result, double threshold) {
        sb.append("<tr>");
        sb.append("<td align='left'><b>").append(label).append("</b></td><td width='20'></td>");
        
        if (result != null) {
            sb.append("<td align='right'>").append(String.format("%.2f%%", threshold * 100)).append("</td><td width='20'></td>");
            
            String[] cand = result.getCandidate();
            double[] sDetails = result.getSpellingScoreDetails();
            double[] pDetails = result.getPhoneticScoreDetails();
            
            // First Name
            if (cand.length > 0) {
                sb.append("<td align='left'>").append(cand[0]).append("</td><td width='20'></td>");
                sb.append("<td align='right'>").append(sDetails != null && sDetails.length > 0 ? String.format("%.0f%%", sDetails[0]*100) : "-").append("</td><td width='20'></td>");
                sb.append("<td align='right'>").append(pDetails != null && pDetails.length > 0 ? String.format("%.0f%%", pDetails[0]*100) : "-").append("</td><td width='20'></td>");
            } else {
                sb.append("<td>-</td><td></td><td>-</td><td></td><td>-</td><td></td>");
            }
            
            // Last Name
            if (cand.length > 1) {
                sb.append("<td align='left'>").append(cand[1]).append("</td><td width='20'></td>");
                sb.append("<td align='right'>").append(sDetails != null && sDetails.length > 1 ? String.format("%.0f%%", sDetails[1]*100) : "-").append("</td><td width='20'></td>");
                sb.append("<td align='right'>").append(pDetails != null && pDetails.length > 1 ? String.format("%.0f%%", pDetails[1]*100) : "-").append("</td>");
            } else {
               sb.append("<td>-</td><td></td><td>-</td><td></td><td>-</td>");
            }
        } else {
            // No result for this category - spans + spacers needed? Or just fill
            sb.append("<td align='right'>").append(String.format("%.2f%%", threshold * 100)).append("</td>"); 
             // Fill remaining columns: 5 data cols + 5 spacers?
             // Actually simplest to just colspan the rest
            sb.append("<td colspan='11' align='center'>-</td>");
        }
        sb.append("</tr>");
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
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                out.write(tableModel.getColumnName(i) + (i == tableModel.getColumnCount() - 1 ? "" : ","));
            }
            out.write("\n");
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    Object val = tableModel.getValueAt(i, j);
                    out.write((val == null ? "" : val.toString()) + (j == tableModel.getColumnCount() - 1 ? "" : ","));
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
                
                Sheet sheet = workbook.createSheet("Search Results");
                
                // Styling
                org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerFont.setColor(IndexedColors.WHITE.getIndex());
                
                CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFont(headerFont);
                headerStyle.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                headerStyle.setAlignment(HorizontalAlignment.CENTER);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);

                CellStyle defaultStyle = workbook.createCellStyle();
                defaultStyle.setBorderTop(BorderStyle.THIN);
                defaultStyle.setBorderBottom(BorderStyle.THIN);
                defaultStyle.setBorderLeft(BorderStyle.THIN);
                defaultStyle.setBorderRight(BorderStyle.THIN);

                CellStyle percentStyle = workbook.createCellStyle();
                percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0%"));
                percentStyle.setAlignment(HorizontalAlignment.RIGHT);
                percentStyle.setBorderTop(BorderStyle.THIN);
                percentStyle.setBorderBottom(BorderStyle.THIN);
                percentStyle.setBorderLeft(BorderStyle.THIN);
                percentStyle.setBorderRight(BorderStyle.THIN);

                CellStyle percentDecimalStyle = workbook.createCellStyle();
                percentDecimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
                percentDecimalStyle.setAlignment(HorizontalAlignment.RIGHT);
                percentDecimalStyle.setBorderTop(BorderStyle.THIN);
                percentDecimalStyle.setBorderBottom(BorderStyle.THIN);
                percentDecimalStyle.setBorderLeft(BorderStyle.THIN);
                percentDecimalStyle.setBorderRight(BorderStyle.THIN);
                
                // Create Header Row
                Row headerRow = sheet.createRow(0);
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(tableModel.getColumnName(i));
                    cell.setCellStyle(headerStyle);
                }

                // Create Data Rows
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    Row row = sheet.createRow(i + 1);
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        Cell cell = row.createCell(j);
                        Object val = tableModel.getValueAt(i, j);
                        String strVal = (val == null) ? "" : val.toString();

                        if (strVal.endsWith("%")) {
                            try {
                                // Parse e.g. "91%" -> 91.0 or "77.48%" -> 77.48
                                double numericVal = Double.parseDouble(strVal.replace("%", ""));
                                cell.setCellValue(numericVal / 100.0);
                                if (strVal.contains(".")) {
                                    cell.setCellStyle(percentDecimalStyle);
                                } else {
                                    cell.setCellStyle(percentStyle);
                                }
                            } catch (NumberFormatException e) {
                                cell.setCellValue(strVal);
                            }
                        } else if (val instanceof Number) {
                            cell.setCellValue(((Number) val).doubleValue());
                            cell.setCellStyle(defaultStyle);
                        } else {
                            cell.setCellValue(strVal);
                            cell.setCellStyle(defaultStyle);
                        }
                    }
                }

                // Auto-size columns
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(fileOut);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(selectedFile);
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, MessageFormat.format(bundle.getString("dialog.export.failed"), e.getMessage()));
            }
    }

    private static class CriteriaLine extends JPanel {
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

        public CriteriaLine(String label, String defaultValue, Runnable onEnter) {
            this.onEnter = onEnter;
            setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
            setOpaque(false);
            
            mainLabel = new JLabel(label);
            mainLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            mainLabel.setPreferredSize(new Dimension(100, 25));
            add(mainLabel);

            valueLabel = new JLabel("Value:");
            add(valueLabel);
            valueField = new JTextField(defaultValue, 12);
            valueField.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
            valueField.addActionListener(e -> { valueField.selectAll(); onEnter.run(); });
            add(valueField);

            typeLabel = new JLabel("Type:");
            add(typeLabel);
            typeCombo = new JComboBox<>(Criteria.MatchingType.values());
            typeCombo.setSelectedItem(Criteria.MatchingType.SIMILARITY);
            typeCombo.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
            add(typeCombo);

            weightLabel = new JLabel("Weight:");
            add(weightLabel);
            weightSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
            weightSpinner.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
            weightSpinner.addChangeListener(e -> onEnter.run());
            add(weightSpinner);

            minSpellingLabel = new JLabel("Min Spell:");
            minSpellingField = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.05));
            minSpellingField.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
            minSpellingField.setPreferredSize(new Dimension(80, 30));
            ((JSpinner.DefaultEditor)minSpellingField.getEditor()).getTextField().addActionListener(e -> onEnter.run());
            minSpellingField.addChangeListener(e -> onEnter.run());
            add(minSpellingLabel);
            add(minSpellingField);

            minPhoneticLabel = new JLabel("Min Phon:");
            minPhoneticField = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.05));
            minPhoneticField.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
            minPhoneticField.setPreferredSize(new Dimension(80, 30));
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

        public void updateTexts(ResourceBundle bundle, String mainLabelText) {
            mainLabel.setText(mainLabelText);
            valueLabel.setText(bundle.getString("search.criteria.value"));
            typeLabel.setText(bundle.getString("search.criteria.type"));
            weightLabel.setText(bundle.getString("search.criteria.weight"));
            minSpellingLabel.setText(bundle.getString("search.criteria.min_spell"));
            minPhoneticLabel.setText(bundle.getString("search.criteria.min_phon"));
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UI().setVisible(true);
        });
    }
}
