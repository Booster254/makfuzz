package com.makfuzz;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import net.miginfocom.swing.MigLayout;
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
import javax.swing.JFormattedTextField;
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
import com.formdev.flatlaf.extras.FlatSVGIcon;

import com.makfuzz.core.Criteria;
import com.makfuzz.core.Fuzz;
import com.makfuzz.core.LineSimResult;
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
	private List<ConfigManager.ColumnConfig> availableColumns = new ArrayList<>();
	private List<Integer> lastSelectedIndices = new ArrayList<>();
	private JLabel selectedColumnsLabel;
	private JTextField sourcePathField;
	private JSpinner globalThresholdField;
	private JTextField topNField;
	private JButton executeBtn;
	private JButton addCriteriaBtn;
	private JLabel statusLabel;
	private JLabel totalFoundLabel;
	private SearchResult lastSearchResult;

	// I18N Fields
	private ResourceBundle bundle;
	private Locale currentLocale;
	private JComboBox<String> langCombo;

	// UI components that need dynamic text updates
	private JLabel appTitle;
	private JLabel appSubtitle;
	private JLabel srcLabel;
	private JButton browseBtn;
	private JButton colsBtn;
	private JLabel criteriaLabel;
	private JLabel thresholdLabel;
	private JLabel limitLabel;
	private JButton csvBtn;
	private JButton excelBtn;

	private boolean searchPending = false;
	private boolean isInitializing = false;
	private boolean isCommitting = false;

	// Card Layout for Center Panel
	private CardLayout centerCardLayout;
	private JPanel centerPanel;
	private static final String CARD_TABLE = "TABLE";
	private static final String CARD_LOADING = "LOADING";

	public UI() {
		// Apply FlatLaf Light theme with modern customizations
		com.formdev.flatlaf.FlatLightLaf.setup();

		// Modern rounded corners
		UIManager.put("Button.arc", 8);
		UIManager.put("Component.arc", 8);
		UIManager.put("TextComponent.arc", 8);
		UIManager.put("ProgressBar.arc", 8);
		UIManager.put("CheckBox.arc", 6);
		UIManager.put("ComboBox.arc", 8);

		// Professional color scheme
		UIManager.put("Button.background", new Color(99, 102, 241)); // Vibrant indigo
		UIManager.put("Button.foreground", Color.WHITE);
		UIManager.put("Button.hoverBackground", new Color(79, 70, 229));
		UIManager.put("Button.pressedBackground", new Color(67, 56, 202));

		// Table styling
		UIManager.put("Table.rowHeight", 26);
		UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 11));

		UIManager.put("Table.showHorizontalLines", true);
		UIManager.put("Table.showVerticalLines", false);
		UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
		UIManager.put("Table.selectionBackground", new Color(224, 231, 255));
		UIManager.put("Table.selectionForeground", new Color(30, 30, 30));
		UIManager.put("TableHeader.background", new Color(249, 250, 251));
		UIManager.put("TableHeader.foreground", new Color(55, 65, 81));
		UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 11));
		UIManager.put("TableHeader.separatorColor", new Color(229, 231, 235));

		// Panel backgrounds
		UIManager.put("Panel.background", new Color(249, 250, 251));


		setTitle("MakFuzz - Fuzzy Search ✨");
		setSize(1400, 850);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout(new MigLayout("fill, ins 0, wrap 1", "[grow]", "[][grow][]"));

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
		JPanel headerPanel = new JPanel(new MigLayout("fillx, ins 15 10 8 10, wrap 1", "[grow]"));
		headerPanel.setBackground(new Color(249, 250, 251)); // Light gray background



		// 0. App Title Section
		JPanel titleBox = new JPanel(new MigLayout("fillx, ins 0", "[grow][]"));
		appTitle = new JLabel("MakFuzz");
		appTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
		appTitle.setForeground(new Color(79, 70, 229)); // Modern indigo

		titleBox.add(appTitle, "growx");

		appSubtitle = new JLabel("Optimized Similarity Engine");
		appSubtitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
		appSubtitle.setForeground(new Color(107, 114, 128)); // Soft gray
		titleBox.add(appSubtitle, "newline, growx");
		headerPanel.add(titleBox, "growx, gapbottom 12");


		// 1. Data Source Card
		JPanel sourcePanel = new JPanel(new MigLayout("ins 8, fillx", "[][grow][][]"));
		sourcePanel.setBackground(Color.WHITE);
		sourcePanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
				BorderFactory.createEmptyBorder(5, 5, 5, 5)));


		srcLabel = new JLabel("Data Source:");
		srcLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
		srcLabel.setForeground(new Color(55, 65, 81));

		sourcePathField = new JTextField("", 60);
		sourcePathField.setEditable(false);
		sourcePathField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, bundle.getString("source.placeholder"));
		sourcePathField.putClientProperty(FlatClientProperties.STYLE, "arc: 12;");


		browseBtn = new JButton("Browse");
		browseBtn.addActionListener(e -> chooseFileAndColumns());

		colsBtn = new JButton("Columns");
		colsBtn.addActionListener(e -> {
			String path = sourcePathField.getText();
			if (path != null && !path.isEmpty()) {
				showColumnSelectionDialog(new File(path));
			}
		});

		sourcePanel.add(srcLabel);
		sourcePanel.add(sourcePathField, "growx");
		sourcePanel.add(browseBtn);
		sourcePanel.add(colsBtn);

		headerPanel.add(sourcePanel, "growx");


		JPanel sourceInfoPanel = new JPanel(new MigLayout("ins 0 15 0 15", "[grow]"));
		sourceInfoPanel.setOpaque(false);
		selectedColumnsLabel = new JLabel("Selected Columns: None");
		selectedColumnsLabel.setFont(new Font("SansSerif", Font.ITALIC, 10));
		selectedColumnsLabel.setForeground(new Color(107, 114, 128));
		sourceInfoPanel.add(selectedColumnsLabel, "growx");
		headerPanel.add(sourceInfoPanel, "growx, gaptop 3, gapbottom 10");


		// 2. Search Card
		setupTopPanel(headerPanel);

		add(headerPanel, "growx");

		// 2.5 Footer Panel (Status Bar)
		setupFooter();

		// 3. Table Panel
		setupCenterPanel();

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
		commitSpinners();
		ConfigManager.AppConfig config = new ConfigManager.AppConfig();
		config.sourcePath = sourcePathField.getText();
		config.globalThreshold = (Double) globalThresholdField.getValue();
		try {
			config.topN = Integer.parseInt(topNField.getText());
		} catch (Exception e) {
			config.topN = 1000;
		}
		config.language = currentLocale.getLanguage();

		config.criteriaList = new ArrayList<>();
		for (CriteriaLine cl : criteriaLines) {
			config.criteriaList.add(cl.getConfig());
		}

		config.availableColumns = new ArrayList<>(availableColumns);

		File configFile = new File(System.getProperty("user.home"), ".makfuzz_config.xml");
		ConfigManager.saveConfig(config, configFile);
	}

	private void commitSpinners() {
		isCommitting = true;
		try {
			if (globalThresholdField != null) {
				globalThresholdField.commitEdit();
			}
			for (CriteriaLine cl : criteriaLines) {
				cl.commitSpinners();
			}
		} catch (Exception e) {
			// Ignore parse errors, will revert to valid value
		} finally {
			isCommitting = false;
		}
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
				try {
					globalThresholdField.setValue(config.globalThreshold);
				} catch (Exception e) {
				}
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
				}

				if (config.availableColumns != null) {
					availableColumns = new ArrayList<>(config.availableColumns);
					updateSelectedColumnsLabel();
				}

				if (config.criteriaList != null && !config.criteriaList.isEmpty()) {
					criteriaContainer.removeAll();
					criteriaLines.clear();
					for (ConfigManager.CriteriaConfig cc : config.criteriaList) {
						CriteriaLine line = new CriteriaLine(cc.value, () -> performSearch(), this::removeCriteriaLine);
						line.setConfig(cc);
						criteriaLines.add(line);
						criteriaContainer.add(line);
					}
					criteriaContainer.revalidate();
					criteriaContainer.repaint();
				}
				updateTexts();
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
		setLocale(currentLocale);
		Locale.setDefault(currentLocale);
		setTitle("MakFuzz - Fuzzy Search ✨");
		appTitle.setText("MakFuzz");
		appSubtitle.setText(bundle.getString("app.header.subtitle"));
		srcLabel.setText(bundle.getString("source.label"));
		browseBtn.setText(bundle.getString("source.button.browse"));
		colsBtn.setText(bundle.getString("source.button.columns"));
		updateSelectedColumnsLabel();
		criteriaLabel.setText(bundle.getString("search.config.label"));
		addCriteriaBtn.setText(bundle.getString("search.config.add_btn"));
		thresholdLabel.setText(bundle.getString("search.label.threshold"));
		limitLabel.setText(bundle.getString("search.label.topn"));
		if (executeBtn != null) {
			executeBtn.setText(bundle.getString("search.button.run"));
		}
		if (csvBtn != null) {
			csvBtn.setText(bundle.getString("search.button.csv"));
		}
		if (excelBtn != null) {
			excelBtn.setText(bundle.getString("search.button.excel"));
		}

		for (CriteriaLine cl : criteriaLines) {
			cl.updateTexts(bundle);
		}

		// Update Table Columns
		List<String> colNames = new ArrayList<>();
		colNames.add(bundle.getString("table.col.index"));
		colNames.add(bundle.getString("table.col.file_index"));

		// Add selected columns from CSV
		for (ConfigManager.ColumnConfig cc : availableColumns) {
			colNames.add(cc.name);
		}

		colNames.add(bundle.getString("table.col.score"));

		for (int i = 0; i < criteriaLines.size(); i++) {
			if (!criteriaLines.get(i).isActive()) {
				continue;
			}
			colNames.add("Crit " + (i + 1));
			colNames.add(bundle.getString("search.metrics.s"));
			colNames.add(bundle.getString("search.metrics.p"));
		}
		colNames.add("HIDDEN_DATA"); // Hidden column to store LineSimResult
		tableModel.setColumnIdentifiers(colNames.toArray());
		applyTableColumnStyles();

	}

	private void loadData(String path) {
		if (path == null || path.isEmpty()) {
			return;
		}

		File f = new File(path);
		// Only reload if file changed. Column selection changes don't require reloading the whole CSV into 'database' field
		if (path.equals(lastLoadedPath) && f.lastModified() == lastLoadedTimestamp && database != null) {
			return;
		}

		try {
			if (!f.exists()) {
				JOptionPane.showMessageDialog(this,
						MessageFormat.format(bundle.getString("dialog.error.file_not_found"), f.getAbsolutePath()));
				database = new ArrayList<>();
				return;
			}

			List<String> lines = FileUtils.readLines(f, StandardCharsets.UTF_8);
			if (lines.isEmpty()) {
				database = new ArrayList<>();
				return;
			}

			List<String[]> data = new ArrayList<>();
			for (int i = 0; i < lines.size(); i++) {
				if (i == 0) {
					continue; // Skip header
				}
				String lineText = lines.get(i);
				if (lineText.trim().isEmpty()) {
					continue;
				}

				// USE ROBUST PARSER
				String[] parts = parseCSVLine(lineText);
				String[] fullRow = new String[parts.length + 1];
				System.arraycopy(parts, 0, fullRow, 0, parts.length);
				// Store 1-based line index as the last element
				fullRow[parts.length] = String.valueOf(i + 1);
				data.add(fullRow);
			}
			database = data;

			lastLoadedPath = path;
			lastLoadedTimestamp = f.lastModified();

		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					MessageFormat.format(bundle.getString("dialog.error.load_error"), e.getMessage()));
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
			showColumnSelectionDialog(selectedFile);
		}
	}

	private void showColumnSelectionDialog(File selectedFile) {
		try {
			List<String> lines = FileUtils.readLines(selectedFile, StandardCharsets.UTF_8);
			if (lines.isEmpty()) {
				return;
			}

			String headerLine = lines.get(0);
			String[] columns = headerLine.split("[,;]");

			// Show column selection dialog
			JPanel panel = new JPanel(new MigLayout("wrap 1, fillx", "[grow]"));
			panel.add(new JLabel(bundle.getString("dialog.select_columns.msg")), "gapbottom 10");

			javax.swing.JCheckBox[] checkBoxes = new javax.swing.JCheckBox[columns.length];
			for (int i = 0; i < columns.length; i++) {
				checkBoxes[i] = new javax.swing.JCheckBox(columns[i].trim());
				// Pre-select if already in availableColumns
				for (ConfigManager.ColumnConfig cc : availableColumns) {
					if (cc.index == i) {
						checkBoxes[i].setSelected(true);
						break;
					}
				}
				panel.add(checkBoxes[i], "growx");
			}

			int result = JOptionPane.showConfirmDialog(this, new JScrollPane(panel),
					bundle.getString("dialog.select_columns.title"), JOptionPane.OK_CANCEL_OPTION);

			if (result == JOptionPane.OK_OPTION) {
				availableColumns.clear();

				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < checkBoxes.length; i++) {
					if (checkBoxes[i].isSelected()) {
						String colName = columns[i].trim();
						ConfigManager.ColumnConfig cc = new ConfigManager.ColumnConfig();
						cc.name = colName;
						cc.index = i;
						availableColumns.add(cc);

						if (sb.length() > 0) {
							sb.append(", ");
						}
						sb.append(colName);
					}
				}

				updateSelectedColumnsLabel();

				// Reload data with new columns
				loadData(selectedFile.getAbsolutePath());

				// Trigger dynamic text update
				updateTexts();

				saveSettings();
				performSearch();
			}

		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error reading file headers: " + e.getMessage());
		}
	}

	private void updateSelectedColumnsLabel() {
		if (availableColumns.isEmpty()) {
			selectedColumnsLabel.setText(bundle.getString("source.selected_columns") + ": None");
		} else {
			StringBuilder sb = new StringBuilder();
			for (ConfigManager.ColumnConfig cc : availableColumns) {
				if (sb.length() > 0) {
					sb.append(", ");
				}
				sb.append(cc.name);
			}
			selectedColumnsLabel.setText(bundle.getString("source.selected_columns") + ": " + sb.toString());
		}
	}

	private void addNewCriteriaLine() {
		CriteriaLine cl = new CriteriaLine("", () -> performSearch(), this::removeCriteriaLine);
		criteriaLines.add(cl);
		criteriaContainer.add(cl, "growx");
		criteriaContainer.revalidate();
		criteriaContainer.repaint();
		updateTexts();
	}

	private void removeCriteriaLine(CriteriaLine line) {
		criteriaLines.remove(line);
		criteriaContainer.remove(line);

		// Update indices of remaining lines
		for (int i = 0; i < criteriaLines.size(); i++) {
			criteriaLines.get(i).updateIndex(i);
		}

		criteriaContainer.revalidate();
		criteriaContainer.repaint();
		updateTexts();
		saveSettings();
		performSearch();
	}

	private void setupTopPanel(JPanel parent) {
		JPanel mainPanel = new JPanel(new MigLayout("fillx, ins 10, wrap 1", "[grow]"));
		mainPanel.setBackground(Color.WHITE);
		mainPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
				BorderFactory.createEmptyBorder(8, 8, 8, 8)));

		JPanel configHeader = new JPanel(new MigLayout("fillx, ins 0", "[grow][]"));
		configHeader.setOpaque(false);

		criteriaLabel = new JLabel("Search Configuration:");
		criteriaLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
		criteriaLabel.setForeground(new Color(55, 65, 81));
		configHeader.add(criteriaLabel, "growx");

		addCriteriaBtn = new JButton("+ Add Criterion");
		addCriteriaBtn.setPreferredSize(new Dimension(200, 28));
		addCriteriaBtn.addActionListener(e -> addNewCriteriaLine());
		configHeader.add(addCriteriaBtn, "right");

		mainPanel.add(configHeader, "growx, gapbottom 8");

		criteriaContainer = new JPanel(new MigLayout("fillx, ins 0, wrap 1", "[grow]"));
		criteriaContainer.setOpaque(false);
		mainPanel.add(criteriaContainer, "growx, gapbottom 10");


		JPanel bottomBar = new JPanel(new MigLayout("ins 0, fillx", "[][][grow][][][][][][]"));


		JLabel searchLangLabel = new JLabel("Search Lang:");
		bottomBar.add(searchLangLabel);

		langCombo = new JComboBox<>(new String[] { "EN", "FR" });
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
		bottomBar.add(langCombo, "w 80!, h 32!");

		thresholdLabel = new JLabel("Global Threshold:");
		globalThresholdField = new JSpinner(new SpinnerNumberModel(0.3, 0.0, 1.0, 0.05));
		globalThresholdField.putClientProperty(FlatClientProperties.STYLE, "arc: 12;");

		setupDotDecimalSpinner(globalThresholdField, "0.00");
		JFormattedTextField tfThresh = ((JSpinner.DefaultEditor) globalThresholdField.getEditor()).getTextField();
		tfThresh.addActionListener(e -> {
			tfThresh.selectAll();
			performSearch();
		});
		globalThresholdField.addChangeListener(e -> performSearch());
		bottomBar.add(thresholdLabel, "gapleft 15");
		bottomBar.add(globalThresholdField, "w 100!, h 32!");

		limitLabel = new JLabel("Top N Limit:");
		topNField = new JTextField("1000", 5);
		topNField.putClientProperty(FlatClientProperties.STYLE, "arc: 12;");

		topNField.addActionListener(e -> {
			topNField.selectAll();
			performSearch();
		});
		bottomBar.add(limitLabel, "gapleft 15");
		bottomBar.add(topNField, "w 80!, h 32!");

		executeBtn = new JButton("Run Search");
		executeBtn.setBackground(new Color(99, 102, 241)); // Vibrant indigo
		executeBtn.setForeground(Color.WHITE);
		executeBtn.putClientProperty(FlatClientProperties.STYLE, "arc: 8; borderWidth: 0;");

		executeBtn.setFocusPainted(false);
		executeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
		executeBtn.addActionListener(e -> performSearch());
		bottomBar.add(executeBtn, "gapleft 20, w 210!, h 32!");

		csvBtn = new JButton("Export CSV");
		csvBtn.addActionListener(e -> exportToCSV());
		bottomBar.add(csvBtn, "gapleft 10, w 120!, h 32!");

		excelBtn = new JButton("Export Excel");
		excelBtn.addActionListener(e -> exportToExcel());
		bottomBar.add(excelBtn, "gapleft 10, w 120!, h 32!");

		mainPanel.add(bottomBar, "growx");

		parent.add(mainPanel, "growx");
	}

	private void setupCenterPanel() {
		tableModel = new DefaultTableModel(new String[] { "#", "File Row", "First Name", "Spell (FN)", "Phon (FN)",
				"Last Name", "Spell (LN)", "Phon (LN)", "Total Score" }, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		resultTable = new JTable(tableModel);
		resultTable.getTableHeader().setReorderingAllowed(false);
		resultTable.setShowGrid(true);
		resultTable.setGridColor(new Color(243, 244, 246));
		resultTable.setRowHeight(26);
		resultTable.setSelectionBackground(new Color(224, 231, 255));
		resultTable.setSelectionForeground(new Color(30, 30, 30));

		// Modern table header
		resultTable.getTableHeader().setBackground(new Color(249, 250, 251));
		resultTable.getTableHeader().setForeground(new Color(55, 65, 81));

		resultTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));

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
		JPanel loadingPanel = new JPanel(new MigLayout("fill, ins 0", "[grow, center]", "[grow, center]"));
		loadingPanel.setBackground(Color.WHITE);


		JPanel loadingContent = new JPanel(new MigLayout("ins 0, wrap 1, align center", "[center]"));
		loadingContent.setOpaque(false);

		JLabel loadingLabel = new JLabel("Searching...");
		loadingLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
		loadingLabel.setForeground(new Color(99, 102, 241));

		JProgressBar centerSpinner = new JProgressBar();
		centerSpinner.setIndeterminate(true);
		centerSpinner.setPreferredSize(new Dimension(200, 4));

		loadingContent.add(loadingLabel, "gapbottom 10");
		loadingContent.add(centerSpinner, "growx");

		loadingPanel.add(loadingContent);


		// Setup Card Layout
		centerCardLayout = new CardLayout();
		centerPanel = new JPanel(centerCardLayout);

		centerPanel.add(scrollPane, CARD_TABLE);
		centerPanel.add(loadingPanel, CARD_LOADING);

		add(centerPanel, "grow");
	}

	private void setupFooter() {
		JPanel footer = new JPanel(new MigLayout("fillx, ins 0 15 0 15, aligny center", "[grow][]", "[]"));
		footer.setBackground(new Color(17, 24, 39)); // Modern dark slate
		footer.setPreferredSize(new Dimension(getWidth(), 40));
		footer.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(99, 102, 241)),
				BorderFactory.createEmptyBorder(0, 15, 0, 15)));

		// statusLabel intentionally not added to footer

		// Metrics Panel
		JPanel metricPanel = new JPanel(new MigLayout("ins 0, aligny center", "[]20[]", "[]"));
		metricPanel.setOpaque(false);

		totalFoundLabel = new JLabel("");
		totalFoundLabel.setForeground(Color.WHITE);

		totalFoundLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
		metricPanel.add(totalFoundLabel, "aligny center");

		footer.add(metricPanel, "right, aligny center");
		add(footer, "growx");
	}


	private JLabel createClickableMetricLabel(String bundleKey) {
		JLabel label = new JLabel("");
		label.setForeground(Color.WHITE);

		label.setFont(new Font("SansSerif", Font.PLAIN, 12));
		label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

		label.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseEntered(java.awt.event.MouseEvent e) {
				String text = label.getText();
				if (text.contains(":")) {
					label.setText("<html><u>" + text + "</u></html>");
				}
			}

			@Override
			public void mouseExited(java.awt.event.MouseEvent e) {
				String text = label.getText();
				if (text.startsWith("<html>")) {
					label.setText(text.replaceAll("<html><u>|</u></html>", ""));
				}
			}

			@Override
			public void mouseClicked(java.awt.event.MouseEvent e) {
				String text = label.getText();
				if (text.contains(":")) {
					try {
						String valStr = text.substring(text.lastIndexOf(":") + 1).replace("%", "").trim();
						double val = Double.parseDouble(valStr);
						globalThresholdField.setValue(val);
						performSearch();
					} catch (Exception ex) {
					}
				}
			}
		});
		return label;
	}

	private class HighlightRenderer extends javax.swing.table.DefaultTableCellRenderer {
		private final int sourceColIndex; // Index in availableColumns

		public HighlightRenderer(int sourceColIndex) {
			this.sourceColIndex = sourceColIndex;
		}

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if (isSelected) {
				return c;
			}

			c.setBackground(Color.WHITE);

			try {
				int modelRow = table.convertRowIndexToModel(row);
				int dataCol = table.getModel().getColumnCount() - 1;
				Object data = table.getModel().getValueAt(modelRow, dataCol);

				if (data instanceof LineSimResult lsr && sourceColIndex >= 0
						&& sourceColIndex < availableColumns.size()) {
					int csvIdx = availableColumns.get(sourceColIndex).index;
					SimResult[] simResults = lsr.getSimResults();
					if (simResults != null) {
						for (SimResult sr : simResults) {
							if (sr != null && sr.getScore() > 0 && sr.getColumnIndex() == csvIdx) {
								c.setBackground(new java.awt.Color(255, 249, 196)); // Soft Yellow highlight
								break;
							}
						}
					}
				}
			} catch (Exception e) {
				// Fallback to white
			}

			return c;
		}
	}

	private class PercentRenderer extends javax.swing.table.DefaultTableCellRenderer {
		private final java.text.DecimalFormat df = new java.text.DecimalFormat("0.00%");
		private Color backgroundColor = null;

		public PercentRenderer() {
			setHorizontalAlignment(javax.swing.JLabel.RIGHT);
			this.backgroundColor = Color.WHITE;
		}

		public PercentRenderer(Color bgColor) {
			this();
			this.backgroundColor = bgColor;
		}

		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
				boolean hasFocus, int row, int column) {
			java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (value instanceof Number) {
				double d = ((Number) value).doubleValue();
				if (d == 0.0) {
					setText("-");
				} else {
					setText(df.format(d));
				}
			}

			if (!isSelected && backgroundColor != null) {
				c.setBackground(backgroundColor);
			} else if (!isSelected) {
				c.setBackground(table.getBackground());
			}
			return c;
		}
	}

	private void applyTableColumnStyles() {
		if (resultTable == null || resultTable.getColumnCount() == 0) {
			return;
		}

		javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		centerRenderer.setBackground(Color.WHITE);

		javax.swing.table.DefaultTableCellRenderer rightRenderer = new javax.swing.table.DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		rightRenderer.setBackground(Color.WHITE);

		javax.swing.table.DefaultTableCellRenderer leftRenderer = new javax.swing.table.DefaultTableCellRenderer();
		leftRenderer.setHorizontalAlignment(javax.swing.JLabel.LEFT);
		leftRenderer.setBackground(Color.WHITE);

		// Header Renderers
		javax.swing.table.DefaultTableCellRenderer centerHeader = new javax.swing.table.DefaultTableCellRenderer();
		centerHeader.setHorizontalAlignment(javax.swing.JLabel.CENTER);
		centerHeader.setBackground(new Color(232, 234, 246));
		centerHeader.setForeground(new Color(63, 81, 181));
		centerHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
		centerHeader.setBorder(UIManager.getBorder("TableHeader.cellBorder"));

		javax.swing.table.DefaultTableCellRenderer rightHeader = new javax.swing.table.DefaultTableCellRenderer();
		rightHeader.setHorizontalAlignment(javax.swing.JLabel.RIGHT);
		rightHeader.setBackground(new Color(232, 234, 246));
		rightHeader.setForeground(new Color(63, 81, 181));
		rightHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
		rightHeader.setBorder(UIManager.getBorder("TableHeader.cellBorder"));

		javax.swing.table.DefaultTableCellRenderer leftHeader = new javax.swing.table.DefaultTableCellRenderer();
		leftHeader.setHorizontalAlignment(javax.swing.JLabel.LEFT);
		leftHeader.setBackground(new Color(232, 234, 246));
		leftHeader.setForeground(new Color(63, 81, 181));
		leftHeader.setFont(new Font("SansSerif", Font.BOLD, 12));
		leftHeader.setBorder(UIManager.getBorder("TableHeader.cellBorder"));

		int colCount = resultTable.getColumnCount();
		int numAvail = availableColumns.size();

		// Hide the HIDDEN_DATA column
		if (colCount > 0) {
			javax.swing.table.TableColumn dataCol = resultTable.getColumnModel().getColumn(colCount - 1);
			resultTable.getColumnModel().removeColumn(dataCol);
			colCount--; // Adjust effective count for the loop below
		}

		for (int i = 0; i < colCount; i++) {
			javax.swing.table.TableColumn col = resultTable.getColumnModel().getColumn(i);

			if (i == 0 || i == 1) {
				// Table Index (#) and File Row Index
				col.setCellRenderer(centerRenderer);
				col.setHeaderRenderer(centerHeader);
				col.setPreferredWidth(i == 0 ? 40 : 70);
			} else if (i < 2 + numAvail) {
				// Available source columns - with HighlightRenderer
				col.setCellRenderer(new HighlightRenderer(i - 2));
				col.setHeaderRenderer(leftHeader);
				col.setPreferredWidth(120);
			} else if (i == 2 + numAvail) {
				// Total Score column - distinguishing background
				col.setCellRenderer(new PercentRenderer(new Color(232, 234, 246)));
				col.setHeaderRenderer(rightHeader);
				col.setPreferredWidth(100);
			} else {
				// Criteria columns
				int internalIdx = (i - 2 - numAvail - 1) % 3;
				if (internalIdx == 0) {
					// Matched Value column
					col.setCellRenderer(leftRenderer);
					col.setHeaderRenderer(leftHeader);
					col.setPreferredWidth(140);
				} else {
					// Score details (% cols: Spell, Phon)
					col.setCellRenderer(new PercentRenderer());
					col.setHeaderRenderer(rightHeader);
					col.setPreferredWidth(110); // Slightly wider for full labels like Orthographe %
				}
			}
		}
	}

	private void performSearch() {
		if (isInitializing || isCommitting) {
			return;
		}

		// Prevent concurrent searches
		if (executeBtn != null && !executeBtn.isEnabled()) {
			searchPending = true;
			return;
		}

		// Reset pending flag as we are starting a fresh search
		searchPending = false;

		commitSpinners();

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

			// Switch to loading view
			centerCardLayout.show(centerPanel, CARD_LOADING);

			// Update selected indices from current available columns
			lastSelectedIndices = availableColumns.stream().map(cc -> cc.index).toList();

			// Capture parameters for thread
			List<String[]> currentDb = database;
			List<Criteria> criteriaList = new ArrayList<>();
			for (CriteriaLine cl : criteriaLines) {
				if (cl.getCriteria() != null) {
					criteriaList.add(cl.getCriteria());
				}
			}

			double globalThreshold = (Double) globalThresholdField.getValue();
			int topN = Integer.parseInt(topNField.getText());
			String lang = currentLocale.getLanguage();

			// Run search in background
			SwingWorker<SearchResult, Void> worker = new SwingWorker<>() {
				@Override
				protected SearchResult doInBackground() throws Exception {
					return Fuzz.bestMatch(currentDb, criteriaList, lastSelectedIndices, globalThreshold, topN, lang);
				}

				@Override
				protected void done() {
					try {
						SearchResult searchResult = get();
						updateResults(searchResult, criteriaList);
					} catch (Exception e) {
						e.printStackTrace();
						JOptionPane.showMessageDialog(UI.this,
								MessageFormat.format(bundle.getString("dialog.error.search_error"), e.getMessage()));
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
			JOptionPane.showMessageDialog(this,
					MessageFormat.format(bundle.getString("dialog.error.setup_error"), e.getMessage()));
			setCursor(java.awt.Cursor.getDefaultCursor());
			if (executeBtn != null) {
				executeBtn.setEnabled(true);
			}
			centerCardLayout.show(centerPanel, CARD_TABLE);
		}
	}

	private void updateResults(SearchResult searchResult, List<Criteria> criteriaList) {
		this.lastSearchResult = searchResult;
		try {
			List<LineSimResult> results = searchResult.getResults();
			int numCriteria = criteriaList.size();
			int numAvail = availableColumns.size();

			// Expected columns: TableIndex + FileIndex + numAvail + (3 * numCriteria) + Score + HIDDEN_DATA
			int expectedCols = 4 + numAvail + 3 * numCriteria;
			if (tableModel.getColumnCount() != expectedCols) {
				updateTexts();
			}

			tableModel.setRowCount(0);

			// Update Status Bar
			if (totalFoundLabel != null) {
				totalFoundLabel.setText(MessageFormat.format(bundle.getString("status.total"), searchResult.getTotalFound(), searchResult.getTotalResults()));
			}

			int tableRowIndex = 1;
			for (LineSimResult res : results) {
				String[] cand = res.getCandidate();

				Object[] rowValues = new Object[tableModel.getColumnCount()];
				// Column 0: Table Relative Index
				rowValues[0] = tableRowIndex++;

				// Column 1: File Row Index (stored at the end of the candidate array)
				rowValues[1] = (cand != null && cand.length > 0) ? cand[cand.length - 1] : "-";

				int currentCell = 2;
				// Add values for selected columns
				for (ConfigManager.ColumnConfig cc : availableColumns) {
					int idx = cc.index;
					rowValues[currentCell++] = (cand != null && idx >= 0 && idx < cand.length) ? cand[idx] : "";
				}

				// Add Total Score before criteria
				rowValues[currentCell++] = res.getScore();

				SimResult[] simResults = res.getSimResults();

				for (int i = 0; i < numCriteria; i++) {
					SimResult sr = simResults[i];

					if (sr != null && (sr.getScore() > 0 || sr.getValue() != null)) {
						String val = (sr.getValue() != null) ? sr.getValue().trim() : "";
						rowValues[currentCell++] = val.isEmpty() ? "-" : val;
						rowValues[currentCell++] = sr.getSpellingScore();
						rowValues[currentCell++] = sr.getPhoneticScore();
					} else {
						rowValues[currentCell++] = "";
						rowValues[currentCell++] = 0.0;
						rowValues[currentCell++] = 0.0;
					}
				}
				// Hidden data column is last
				rowValues[rowValues.length - 1] = res;

				tableModel.addRow(rowValues);
			}

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
			JOptionPane.showMessageDialog(this,
					MessageFormat.format(bundle.getString("dialog.error.search_error"), e.getMessage()));
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
						bundle.getString("dialog.export.replace_title"), JOptionPane.YES_NO_CANCEL_OPTION,
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
		if (lastSearchResult == null || lastSearchResult.getAllFoundResults() == null) {
			return;
		}

		try (FileWriter out = new FileWriter(file)) {
			String[] originalHeaders = fetchOriginalHeaders();

			// Write Headers (original CSV format)
			for (int i = 0; i < originalHeaders.length; i++) {
				out.write(escapeCSV(originalHeaders[i]));
				if (i < originalHeaders.length - 1) {
					out.write(",");
				}
			}
			out.write("\n");

			// Write Data for ALL found results
			for (LineSimResult res : lastSearchResult.getAllFoundResults()) {
				String[] cand = res.getCandidate();
				// Use the candidate array which contains the original fields (except the last element which is the row index)
				int numFields = originalHeaders.length;
				for (int k = 0; k < numFields; k++) {
					String val = (k < cand.length) ? cand[k] : "";
					out.write(escapeCSV(val));
					if (k < numFields - 1) {
						out.write(",");
					}
				}
				out.write("\n");
			}

			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().open(file);
			}
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					MessageFormat.format(bundle.getString("dialog.export.failed"), e.getMessage()));
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
			// Since file might not be sorted by index if shuffled, we can't stop early
			// unless we track found count.
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
						bundle.getString("dialog.export.replace_title"), JOptionPane.YES_NO_CANCEL_OPTION,
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
		try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fileOut = new FileOutputStream(selectedFile)) {

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

			CellStyle percentStyle = workbook.createCellStyle();
			percentStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
			percentStyle.setAlignment(HorizontalAlignment.RIGHT);

			// Write Data
			for (int i = 0; i < tableModel.getRowCount(); i++) {
				Row row = sheet.createRow(i + 1);
				colIdx = 0;
				int numAvail = availableColumns.size();
				for (int j = 0; j < tableModel.getColumnCount(); j++) {
					Cell cell = row.createCell(colIdx++);
					Object val = tableModel.getValueAt(i, j);
					
					// Handle the HIDDEN_DATA column separately (it's the last one)
					if (j == tableModel.getColumnCount() - 1) {
						int fileRowIdx = Integer.parseInt(tableModel.getValueAt(i, 1).toString());
						String fullLine = originalLines.getOrDefault(fileRowIdx, "");
						cell.setCellValue(fullLine);
						continue;
					}

					if (val instanceof Number) {
						cell.setCellValue(((Number) val).doubleValue());
						
						// Apply percentage format to score columns
						boolean isScoreCol = (j == 2 + numAvail);
						boolean isCriteriaScoreCol = (j > 2 + numAvail) && ((j - 2 - numAvail - 1) % 3 != 0);
						if (isScoreCol || isCriteriaScoreCol) {
							cell.setCellStyle(percentStyle);
						}
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
				sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, // First row (header)
						tableModel.getRowCount(), // Last row
						0, // First column
						totalColumns - 1 // Last column
				));
			}

			// Freeze panes: Keep synthesis columns visible when scrolling horizontally
			// Freeze after all synthesis columns (before original CSV columns start)
			int freezeAfterColumn = tableModel.getColumnCount();
			sheet.createFreezePane(freezeAfterColumn, 1); // Freeze columns and first row (header)

			int hiddenDataColIdx = tableModel.getColumnCount() - 1;
			sheet.setColumnHidden(hiddenDataColIdx, true);

			workbook.write(fileOut);

			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().open(selectedFile);
			}

		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					MessageFormat.format(bundle.getString("dialog.export.failed"), e.getMessage()));
		}
	}

	private class CriteriaLine extends JPanel {
		private JTextField valueField;
		private JComboBox<Criteria.MatchingType> typeCombo;
		private JSpinner minSpellingField;
		private JSpinner minPhoneticField;
		private JSpinner spellingWeightSpinner;
		private JSpinner phoneticWeightSpinner;
		private JLabel minSpellingLabel;
		private JLabel minPhoneticLabel;
		private JLabel criteriaIndexLabel;
		private JLabel valueLabel;
		private JLabel typeLabel;
		private JLabel spellingWeightLabel;
		private JLabel phoneticWeightLabel;
		private final Runnable onEnter;

		private JButton removeBtn;

		public CriteriaLine(String defaultValue, Runnable onEnter,
				java.util.function.Consumer<CriteriaLine> removeAction) {
			this.onEnter = onEnter;
			setLayout(new MigLayout("ins 8 12 8 12, fillx", "[][]15[][]10[][]10[][]10[][]10[][]"));
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


			criteriaIndexLabel = new JLabel("Crit " + (criteriaLines.size() + 1));
			criteriaIndexLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
			add(criteriaIndexLabel, "w 60!");


			valueLabel = new JLabel("Value:");
			add(valueLabel);
			valueField = new JTextField(defaultValue, 12);
			valueField.addActionListener(e -> {
				valueField.selectAll();
				onEnter.run();
			});
			add(valueField, "w 150!, h 32!");

			typeLabel = new JLabel("Type:");
			add(typeLabel);
			typeCombo = new JComboBox<>(Criteria.MatchingType.values());
			typeCombo.setSelectedItem(Criteria.MatchingType.SIMILARITY);
			add(typeCombo, "w 140!, h 32!");

			spellingWeightLabel = new JLabel("Spell W:");
			add(spellingWeightLabel);
			spellingWeightSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 100.0, 0.1));
			setupDotDecimalSpinner(spellingWeightSpinner, "0.0");
			spellingWeightSpinner.addChangeListener(e -> onEnter.run());
			add(spellingWeightSpinner, "w 70!, h 32!");

			phoneticWeightLabel = new JLabel("Phon W:");
			add(phoneticWeightLabel);
			phoneticWeightSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.0, 100.0, 0.1));
			setupDotDecimalSpinner(phoneticWeightSpinner, "0.0");
			phoneticWeightSpinner.addChangeListener(e -> onEnter.run());
			add(phoneticWeightSpinner, "w 70!, h 32!");


			minSpellingLabel = new JLabel("Min Spell:");
			minSpellingField = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.05));
			setupDotDecimalSpinner(minSpellingField, "0.00");
			JFormattedTextField tfS = ((JSpinner.DefaultEditor) minSpellingField.getEditor()).getTextField();
			tfS.addActionListener(e -> {
				tfS.selectAll();
				onEnter.run();
			});
			minSpellingField.addChangeListener(e -> onEnter.run());
			add(minSpellingLabel);
			add(minSpellingField, "w 80!, h 32!");

			minPhoneticLabel = new JLabel("Min Phon:");
			minPhoneticField = new JSpinner(new SpinnerNumberModel(0.8, 0.0, 1.0, 0.05));
			setupDotDecimalSpinner(minPhoneticField, "0.00");
			JFormattedTextField tfP = ((JSpinner.DefaultEditor) minPhoneticField.getEditor()).getTextField();
			tfP.addActionListener(e -> {
				tfP.selectAll();
				onEnter.run();
			});
			minPhoneticField.addChangeListener(e -> onEnter.run());
			add(minPhoneticLabel);
			add(minPhoneticField, "w 80!, h 32!");


			typeCombo.addActionListener(e -> {
				boolean isSimilarity = typeCombo.getSelectedItem() == Criteria.MatchingType.SIMILARITY;
				minSpellingLabel.setVisible(isSimilarity);
				minSpellingField.setVisible(isSimilarity);
				minPhoneticLabel.setVisible(isSimilarity);
				minPhoneticField.setVisible(isSimilarity);
				spellingWeightLabel.setVisible(isSimilarity);
				spellingWeightSpinner.setVisible(isSimilarity);
				phoneticWeightLabel.setVisible(isSimilarity);
				phoneticWeightSpinner.setVisible(isSimilarity);
				revalidate();
				repaint();
				onEnter.run();
			});

			// Trigger initial visibility
			boolean isSimilarity = typeCombo.getSelectedItem() == Criteria.MatchingType.SIMILARITY;
			minSpellingLabel.setVisible(isSimilarity);
			minSpellingField.setVisible(isSimilarity);
			minPhoneticLabel.setVisible(isSimilarity);
			minPhoneticField.setVisible(isSimilarity);
			spellingWeightLabel.setVisible(isSimilarity);
			spellingWeightSpinner.setVisible(isSimilarity);
			phoneticWeightLabel.setVisible(isSimilarity);
			phoneticWeightSpinner.setVisible(isSimilarity);
		}

		public void updateIndex(int index) {
			criteriaIndexLabel.setText("Crit " + (index + 1));
		}

		public boolean isActive() {
			return valueField != null && !valueField.getText().trim().isEmpty();
		}

		public Criteria getCriteria() {
			String val = valueField.getText().trim().toUpperCase();
			if (val.isEmpty()) {
				return null;
			}

			Criteria.MatchingType type = (Criteria.MatchingType) typeCombo.getSelectedItem();
			double sWeight = (Double) spellingWeightSpinner.getValue();
			double pWeight = (Double) phoneticWeightSpinner.getValue();
			double minSpell = (Double) minSpellingField.getValue();
			double minPhon = (Double) minPhoneticField.getValue();

			if (type == Criteria.MatchingType.SIMILARITY) {
				return Criteria.similarity(val, sWeight, pWeight, minSpell, minPhon);
			} else if (type == Criteria.MatchingType.EXACT) {
				return Criteria.exact(val, sWeight, pWeight);
			} else {
				return Criteria.regex(val, sWeight, pWeight);
			}
		}

		public ConfigManager.CriteriaConfig getConfig() {
			ConfigManager.CriteriaConfig cc = new ConfigManager.CriteriaConfig();
			cc.value = valueField.getText();
			cc.type = ((Criteria.MatchingType) typeCombo.getSelectedItem()).name();
			cc.spellingWeight = (Double) spellingWeightSpinner.getValue();
			cc.phoneticWeight = (Double) phoneticWeightSpinner.getValue();
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
			spellingWeightSpinner.setValue(cc.spellingWeight);
			phoneticWeightSpinner.setValue(cc.phoneticWeight);
			minSpellingField.setValue(cc.minSpelling);
			minPhoneticField.setValue(cc.minPhonetic);
		}

		public void updateTexts(ResourceBundle bundle) {
			valueLabel.setText(bundle.getString("search.criteria.value"));
			typeLabel.setText(bundle.getString("search.criteria.type"));
			spellingWeightLabel.setText(bundle.getString("search.criteria.spelling_weight"));
			phoneticWeightLabel.setText(bundle.getString("search.criteria.phonetic_weight"));
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

		public void commitSpinners() {
			try {
				spellingWeightSpinner.commitEdit();
				phoneticWeightSpinner.commitEdit();
				minSpellingField.commitEdit();
				minPhoneticField.commitEdit();
			} catch (Exception e) {
			}
		}

	}

	private void setupDotDecimalSpinner(JSpinner spinner, String pattern) {
		JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, pattern);
		java.text.DecimalFormat format = editor.getFormat();
		java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.US);
		format.setDecimalFormatSymbols(symbols);
		spinner.setEditor(editor);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new UI().setVisible(true);
		});
	}
}
