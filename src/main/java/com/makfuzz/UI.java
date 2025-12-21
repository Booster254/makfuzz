package com.makfuzz;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
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
    private JLabel statusLabel;
    
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

        setTitle("Fuzzy Search Pro ✨");
        setSize(1400, 850);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        setupUI();

        setLocationRelativeTo(null);
    }

    private void setupUI() {
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // 0. App Title Section
        JPanel titleBox = new JPanel(new BorderLayout());
        JLabel appTitle = new JLabel("FUZZY MATCHER");
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        appTitle.setForeground(new Color(63, 81, 181));
        titleBox.add(appTitle, BorderLayout.WEST);
        
        JLabel appSubtitle = new JLabel("Optimized Similarity Engine");
        appSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        appSubtitle.setForeground(Color.GRAY);
        titleBox.add(appSubtitle, BorderLayout.SOUTH);
        headerPanel.add(titleBox);
        headerPanel.add(Box.createVerticalStrut(20));

        // 1. Data Source Card
        JPanel sourcePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        sourcePanel.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #ffffff; ");
        
        JLabel srcLabel = new JLabel("Data Source:");
        srcLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        sourcePanel.add(srcLabel);

        sourcePathField = new JTextField("./names.csv", 60);
        sourcePathField.putClientProperty(FlatClientProperties.STYLE, "showClearButton: true; arc: 8");
        sourcePathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Select source CSV...");
        
        JButton browseBtn = new JButton("Browse");
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
        statusBar.setBorder(BorderFactory.createEmptyBorder(3, 15, 3, 15));
        
        statusLabel = new JLabel("<html>Engine ready...</html>");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        statusLabel.setVerticalAlignment(JLabel.TOP);
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        JLabel versionLabel = new JLabel("POC 2025-12");
        versionLabel.setForeground(new Color(255, 255, 255, 180));
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        statusBar.add(versionLabel, BorderLayout.EAST);
        
        add(statusBar, BorderLayout.SOUTH);
    }
    
    private void loadData(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                JOptionPane.showMessageDialog(this, "File not found: " + f.getAbsolutePath());
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
                statusLabel.setText("Database loaded: " + database.size() + " records.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage());
        }
    }
    
    private void setupTopPanel(JPanel parent) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 15; background: #ffffff; ");
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel criteriaLabel = new JLabel("Search Configuration:");
        criteriaLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        criteriaLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(criteriaLabel);

        fnLine = new CriteriaLine("First Name:", "", this::performSearch);
        lnLine = new CriteriaLine("Last Name:", "", this::performSearch);

        mainPanel.add(fnLine);
        mainPanel.add(lnLine);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        bottomBar.add(new JLabel("Global Threshold:"));
        globalThresholdField = new JSpinner(new SpinnerNumberModel(0.3, 0.0, 1.0, 0.05));
        globalThresholdField.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
        globalThresholdField.setPreferredSize(new Dimension(80, 30));
        ((JSpinner.DefaultEditor)globalThresholdField.getEditor()).getTextField().addActionListener(e -> performSearch());
        bottomBar.add(globalThresholdField);

        bottomBar.add(Box.createHorizontalStrut(15));
        bottomBar.add(new JLabel("Top N Limit:"));
        topNField = new JTextField("1000", 5);
        topNField.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
        topNField.addActionListener(e -> { topNField.selectAll(); performSearch(); });
        bottomBar.add(topNField);
        bottomBar.add(Box.createHorizontalStrut(15));

        JButton executeBtn = new JButton("Run Search");
        executeBtn.setBackground(new Color(63, 81, 181));
        executeBtn.setForeground(Color.WHITE);
        executeBtn.putClientProperty(FlatClientProperties.STYLE, "hoverBackground: #303F9F; pressedBackground: #1a237e; arc: 10");
        executeBtn.setFocusPainted(false);
        executeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        executeBtn.addActionListener(e -> performSearch());
        bottomBar.add(executeBtn);

        JButton csvBtn = new JButton("Export CSV");
        csvBtn.putClientProperty(FlatClientProperties.STYLE, "buttonType: toolBarButton");
        csvBtn.addActionListener(e -> exportToCSV());
        bottomBar.add(csvBtn);

        JButton excelBtn = new JButton("Export Excel");
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
        
        // Adjust column widths roughly
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(40); // Index column
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(150); // FN column
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(150); // LN column

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    private void performSearch() {
        try {
            // Load data only when search is clicked
            loadData(sourcePathField.getText());
            if (database == null || database.isEmpty()) {
                return;
            }

            List<Criteria> criteriaList = new ArrayList<>();
            criteriaList.add(fnLine.getCriteria());
            criteriaList.add(lnLine.getCriteria());
            
            double globalThreshold = (Double) globalThresholdField.getValue();
            int topN = Integer.parseInt(topNField.getText());
            SearchResult searchResult = Fuzz.bestMatch(database, criteriaList, globalThreshold, topN);
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
            
            
            // Build status message with candidate details (multi-line HTML)
            StringBuilder statusMsg = new StringBuilder("<html>");
            
            // Total found
            statusMsg.append(String.format("<b>Total:</b> %d<br>", searchResult.getTotalFound()));
            
            // Max Under Threshold
            if (searchResult.getMaxUnderCandidate() != null) {
                SimResult maxUnder = searchResult.getMaxUnderCandidate();
                String[] cand = maxUnder.getCandidate();
                double[] sDetails = maxUnder.getSpellingScoreDetails();
                double[] pDetails = maxUnder.getPhoneticScoreDetails();
                
                StringBuilder details = new StringBuilder();
                details.append(String.format("%.2f%% [", searchResult.getMaxUnderThreshold() * 100));
                
                // First Name
                if (cand.length > 0) {
                    details.append(String.format("FN:%s", cand[0]));
                    if (sDetails != null && sDetails.length > 0) {
                        details.append(String.format("(S:%.0f%%,P:%.0f%%)", 
                            sDetails[0] * 100, 
                            pDetails != null && pDetails.length > 0 ? pDetails[0] * 100 : 0));
                    }
                }
                
                // Last Name
                if (cand.length > 1) {
                    details.append(String.format(" LN:%s", cand[1]));
                    if (sDetails != null && sDetails.length > 1) {
                        details.append(String.format("(S:%.0f%%,P:%.0f%%)", 
                            sDetails[1] * 100, 
                            pDetails != null && pDetails.length > 1 ? pDetails[1] * 100 : 0));
                    }
                }
                
                details.append("]");
                statusMsg.append("<b>Max Under GT :</b> ").append(details.toString()).append("<br>");
            } else {
                statusMsg.append(String.format("<b>Max Under GT :</b> %.2f%%<br>", 
                    searchResult.getMaxUnderThreshold() * 100));
            }
            
            // Min Above Threshold
            if (searchResult.getMinAboveCandidate() != null) {
                String[] cand = searchResult.getMinAboveCandidate().getCandidate();
                String name = String.join(" ", cand);
                statusMsg.append(String.format("<b>Min Above GT :</b> %.2f%% (%s)<br>", 
                    searchResult.getMinAboveThreshold() * 100, name));
            } else {
                statusMsg.append(String.format("<b>Min Above:</b> %.2f%%<br>", 
                    searchResult.getMinAboveThreshold() * 100));
            }
            
            // Max Above Threshold
            if (searchResult.getMaxAboveCandidate() != null) {
                String[] cand = searchResult.getMaxAboveCandidate().getCandidate();
                String name = String.join(" ", cand);
                statusMsg.append(String.format("<b>Max Above:</b> %.2f%% (%s)", 
                    searchResult.getMaxAboveThreshold() * 100, name));
            } else {
                statusMsg.append(String.format("<b>Max Above:</b> %.2f%%", 
                    searchResult.getMaxAboveThreshold() * 100));
            }
            
            statusMsg.append("</html>");
            statusLabel.setText(statusMsg.toString());
            
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
            JOptionPane.showMessageDialog(this, "Weights and Min Scores must be valid numbers.");
        } catch (Exception e) {
            e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage());
        }
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
            JOptionPane.showMessageDialog(this, "No data to export.");
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
                    "The file '" + selectedFile.getName() + "' already exists. Do you want to replace it?",
                    "Confirm Overwrite",
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
            JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
        }
    }

    private void exportToExcel() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.");
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
                    "The file '" + selectedFile.getName() + "' already exists. Do you want to replace it?",
                    "Confirm Overwrite",
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
                JOptionPane.showMessageDialog(this, "Export failed: " + e.getMessage());
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
        private final Runnable onEnter;

        public CriteriaLine(String label, String defaultValue, Runnable onEnter) {
            this.onEnter = onEnter;
            setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
            setOpaque(false);
            
            JLabel lbl = new JLabel(label);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            lbl.setPreferredSize(new Dimension(100, 25));
            add(lbl);

            add(new JLabel("Value:"));
            valueField = new JTextField(defaultValue, 12);
            valueField.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
            valueField.addActionListener(e -> { valueField.selectAll(); onEnter.run(); });
            add(valueField);

            add(new JLabel("Type:"));
            typeCombo = new JComboBox<>(Criteria.MatchingType.values());
            typeCombo.setSelectedItem(Criteria.MatchingType.SIMILARITY);
            typeCombo.putClientProperty(FlatClientProperties.STYLE, "arc: 8");
            add(typeCombo);

            add(new JLabel("Weight:"));
            weightSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
            weightSpinner.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
            add(weightSpinner);

            minSpellingLabel = new JLabel("Min Spell:");
            minSpellingField = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.05));
            minSpellingField.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
            minSpellingField.setPreferredSize(new Dimension(80, 30));
            ((JSpinner.DefaultEditor)minSpellingField.getEditor()).getTextField().addActionListener(e -> onEnter.run());
            add(minSpellingLabel);
            add(minSpellingField);

            minPhoneticLabel = new JLabel("Min Phon:");
            minPhoneticField = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.05));
            minPhoneticField.putClientProperty(FlatClientProperties.STYLE, "arc: 8; ");
            minPhoneticField.setPreferredSize(new Dimension(80, 30));
            ((JSpinner.DefaultEditor)minPhoneticField.getEditor()).getTextField().addActionListener(e -> onEnter.run());
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
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UI().setVisible(true);
        });
    }
}
