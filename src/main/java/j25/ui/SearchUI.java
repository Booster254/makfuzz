package j25.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.io.FileUtils;

import j25.core.BestMatchV4;
import j25.core.Criteria;
import j25.core.SimResult;

public class SearchUI extends JFrame {
    private List<String[]> database;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    
    // UI Components
    private JTextField valueField;
    private JComboBox<String> columnCombo;
    private JComboBox<Criteria.MatchingType> typeCombo;
    private JTextField weightField;
    private JTextField minScoreField;
    private JLabel minScoreLabel;
    private JLabel statusLabel;
    
    public SearchUI() {
        setTitle("Fuzzy Search Tester (BestMatchV4)");
        setSize(1300, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        loadData();
        setupTopPanel();
        setupCenterPanel();
        
        setLocationRelativeTo(null);
    }
    
    private void loadData() {
        try {
            File f = new File("./names.csv");
            if (!f.exists()) {
                System.out.println("names.csv not found in " + f.getAbsolutePath());
                database = new ArrayList<>();
                return;
            }
            List<String> lines = FileUtils.readLines(f, StandardCharsets.UTF_8);
            // Skip header and split
            database = lines.stream()
                .skip(1)
                .filter(l -> !l.trim().isEmpty())
                .map(line -> line.split("[,;]")) // Keep original case for display, but logic uses toUpperCase
                .toList();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading names.csv: " + e.getMessage());
        }
    }
    
    private void setupTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Search Criteria"));
        
        columnCombo = new JComboBox<>(new String[]{"First Name (Col 0)", "Last Name (Col 1)"});
        valueField = new JTextField(24);
        valueField.setText("ahmed");
        
        typeCombo = new JComboBox<>(Criteria.MatchingType.values());
        typeCombo.setSelectedItem(Criteria.MatchingType.SIMILARITY);
        weightField = new JTextField("1.0", 3);
        
        minScoreLabel = new JLabel("Min Sim Score:");
        minScoreField = new JTextField("0.5", 3);
        
        JButton executeBtn = new JButton("Run Search");
        executeBtn.setBackground(new Color(70, 130, 180));
        executeBtn.setForeground(Color.WHITE);
        executeBtn.setFocusPainted(false);
        
        panel.add(new JLabel("Column:"));
        panel.add(columnCombo);
        panel.add(new JLabel("Value:"));
        panel.add(valueField);
        panel.add(new JLabel("Type:"));
        panel.add(typeCombo);
        panel.add(new JLabel("Weight:"));
        panel.add(weightField);
        panel.add(minScoreLabel);
        panel.add(minScoreField);
        panel.add(executeBtn);
        
        typeCombo.addActionListener(e -> updateVisibility());
        executeBtn.addActionListener(e -> performSearch());
        
        updateVisibility();
        add(panel, BorderLayout.NORTH);
    }
    
    private void updateVisibility() {
        boolean isSimilarity = typeCombo.getSelectedItem() == Criteria.MatchingType.SIMILARITY;
        minScoreLabel.setVisible(isSimilarity);
        minScoreField.setVisible(isSimilarity);
        panelRevalidate();
    }
    
    private void panelRevalidate() {
        Component parent = minScoreLabel.getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
    }
    
    private void setupCenterPanel() {
        tableModel = new DefaultTableModel(new String[]{"First Name", "Last Name", "Score"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setFillsViewportHeight(true);
        resultTable.setRowHeight(25);
        
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        statusLabel = new JLabel("Total found: 0");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 10));
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(statusLabel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    private void performSearch() {
        try {
            int colIdx = columnCombo.getSelectedIndex();
            String val = valueField.getText();
            if (val == null || val.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a search value.");
                return;
            }
            
            Criteria.MatchingType type = (Criteria.MatchingType) typeCombo.getSelectedItem();
            double weight = Double.parseDouble(weightField.getText());
            double minScore = Double.parseDouble(minScoreField.getText());
            
            List<Criteria> criteriaList = new ArrayList<>();
            
            Criteria c = null;
            if (type == Criteria.MatchingType.SIMILARITY) {
				c = Criteria.similarity(val, weight, minScore);
			} else if (type == Criteria.MatchingType.EXACT) {
				c = Criteria.exact(val, weight);
			} else {
				c = Criteria.regex(val, weight);
			}
                
            if (colIdx == 0) {
                criteriaList.add(c);
                criteriaList.add(null);
            } else {
                criteriaList.add(null);
                criteriaList.add(c);
            }
            
            List<SimResult> results = BestMatchV4.bestMatch(database, criteriaList, 0.0, 500);
            
            tableModel.setRowCount(0);
            for (SimResult res : results) {
                String[] cand = res.getCandidate();
                tableModel.addRow(new Object[]{
                    cand.length > 0 ? cand[0] : "",
                    cand.length > 1 ? cand[1] : "",
                    String.format("%.4f", res.getScore())
                });
            }
            statusLabel.setText("Total found: " + results.size());
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Weight and Min Score must be numbers.");
        } catch (Exception e) {
            e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new SearchUI().setVisible(true);
        });
    }
}
