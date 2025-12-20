package j25.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
    private CriteriaLine fnLine;
    private CriteriaLine lnLine;
    private JLabel statusLabel;
    
    public SearchUI() {
        setTitle("Fuzzy Search Tester (BestMatchV4)");
        setSize(1300, 700);
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
                .map(line -> line.toUpperCase().split("[,;]"))
                .toList();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading names.csv: " + e.getMessage());
        }
    }
    
    private void setupTopPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Search Criteria"));

        fnLine = new CriteriaLine("First Name:", "ahmed", this::performSearch);
        lnLine = new CriteriaLine("Last Name:", "", this::performSearch);

        mainPanel.add(fnLine);
        mainPanel.add(lnLine);

        JButton executeBtn = new JButton("Run Search");
        executeBtn.setBackground(new Color(70, 130, 180));
        executeBtn.setForeground(Color.WHITE);
        executeBtn.setFocusPainted(false);
        executeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        executeBtn.addActionListener(e -> performSearch());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(executeBtn);
        mainPanel.add(btnPanel);

        add(mainPanel, BorderLayout.NORTH);
    }
    
    private void setupCenterPanel() {
        tableModel = new DefaultTableModel(new String[]{
            "First Name", "Spell (FN)", "Phon (FN)", 
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
        resultTable.setFillsViewportHeight(true);
        resultTable.setRowHeight(25);
        
        // Adjust column widths roughly
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(150);

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
            List<Criteria> criteriaList = new ArrayList<>();
            criteriaList.add(fnLine.getCriteria());
            criteriaList.add(lnLine.getCriteria());
            
            List<SimResult> results = BestMatchV4.bestMatch(database, criteriaList, 0.0, 500);
            
            tableModel.setRowCount(0);
            for (SimResult res : results) {
                String[] cand = res.getCandidate();
                double[] sDetails = res.getSpellingScoreDetails();
                double[] pDetails = res.getPhoneticScoreDetails();
                
                tableModel.addRow(new Object[]{
                    cand.length > 0 ? cand[0] : "",
                    (sDetails != null && sDetails.length > 0) ? String.format("%.0f%%", sDetails[0] * 100) : "0%",
                    (pDetails != null && pDetails.length > 0) ? String.format("%.0f%%", pDetails[0] * 100) : "0%",
                    cand.length > 1 ? cand[1] : "",
                    (sDetails != null && sDetails.length > 1) ? String.format("%.0f%%", sDetails[1] * 100) : "0%",
                    (pDetails != null && pDetails.length > 1) ? String.format("%.0f%%", pDetails[1] * 100) : "0%",
                    String.format("%.4f", res.getScore())
                });
            }
            statusLabel.setText("Total found: " + results.size());
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Weights and Min Scores must be valid numbers.");
        } catch (Exception e) {
            e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage());
        }
    }

    private static class CriteriaLine extends JPanel {
        private JTextField valueField;
        private JComboBox<Criteria.MatchingType> typeCombo;
        private JTextField weightField;
        private JTextField minScoreField;
        private JLabel minScoreLabel;

        private final Runnable onEnter;

        public CriteriaLine(String label, String defaultValue, Runnable onEnter) {
            this.onEnter = onEnter;
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
            
            JLabel lbl = new JLabel(label);
            lbl.setPreferredSize(new Dimension(100, 25));
            add(lbl);

            valueField = new JTextField(defaultValue, 20);
            valueField.addActionListener(e -> onEnter.run());
            add(new JLabel("Value:"));
            add(valueField);

            typeCombo = new JComboBox<>(Criteria.MatchingType.values());
            typeCombo.setSelectedItem(Criteria.MatchingType.SIMILARITY);
            add(new JLabel("Type:"));
            add(typeCombo);

            weightField = new JTextField("1.0", 5);
            weightField.addActionListener(e -> onEnter.run());
            add(new JLabel("Weight:"));
            add(weightField);

            minScoreLabel = new JLabel("Min Sim Score:");
            minScoreField = new JTextField("0.8", 5);
            minScoreField.addActionListener(e -> onEnter.run());
            add(minScoreLabel);
            add(minScoreField);

            typeCombo.addActionListener(e -> {
                boolean isSimilarity = typeCombo.getSelectedItem() == Criteria.MatchingType.SIMILARITY;
                minScoreLabel.setVisible(isSimilarity);
                minScoreField.setVisible(isSimilarity);
                revalidate();
                repaint();
            });

            // Trigger initial visibility
            boolean isSimilarity = typeCombo.getSelectedItem() == Criteria.MatchingType.SIMILARITY;
            minScoreLabel.setVisible(isSimilarity);
            minScoreField.setVisible(isSimilarity);
        }

        public Criteria getCriteria() {
        	
            String val = valueField.getText().trim().toUpperCase();
            if (val.isEmpty()) {
				return null;
			}

            Criteria.MatchingType type = (Criteria.MatchingType) typeCombo.getSelectedItem();
            double weight = Double.parseDouble(weightField.getText());
            double minScore = Double.parseDouble(minScoreField.getText());

            if (type == Criteria.MatchingType.SIMILARITY) {
                return Criteria.similarity(val, weight, minScore);
            } else if (type == Criteria.MatchingType.EXACT) {
                return Criteria.exact(val, weight);
            } else {
                return Criteria.regex(val, weight);
            }
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
