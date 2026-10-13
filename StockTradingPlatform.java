import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class StockTradingPlatform extends JFrame {

    private static final String SAVE_FILE = "stock_portfolio.dat";

    private double cash = 100000.0;

    private final Map<String, Stock> stocks = new LinkedHashMap<>();
    private final Map<String, Integer> holdings = new LinkedHashMap<>();
    private final Map<String, Double> averageCost = new LinkedHashMap<>();
    private final Set<String> favorites = new LinkedHashSet<>();
    private final List<String[]> history = new ArrayList<>();

    private DefaultTableModel marketModel;
    private DefaultTableModel portfolioModel;
    private DefaultTableModel favoritesModel;
    private DefaultTableModel historyModel;

    private JTable marketTable;
    private JTable portfolioTable;
    private JTable favoritesTable;
    private JTable historyTable;

    private JLabel cashLabel;
    private JLabel portfolioLabel;
    private JLabel profitLabel;
    private JLabel statusLabel;
    private JLabel selectedLabel;

    private JTextField searchField;
    private JTextField quantityField;

    private String selectedSymbol = "AAPL";
    private Chart chart;

    private final Color background = new Color(15, 23, 42);
    private final Color panelColor = new Color(30, 41, 59);
    private final Color accent = new Color(56, 189, 248);
    private final Color green = new Color(52, 211, 153);
    private final Color red = new Color(248, 113, 113);
    private final Color textColor = new Color(226, 232, 240);
    private final Color gridColor = new Color(51, 65, 85);

    private static class Stock {
        String symbol;
        String name;
        double price;
        double change;

        Stock(String symbol, String name, double price, double change) {
            this.symbol = symbol;
            this.name = name;
            this.price = price;
            this.change = change;
        }
    }

    public StockTradingPlatform() {
        setTitle("Stock Trading Platform");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 780);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);

        getContentPane().setBackground(background);

        initializeStocks();
        buildUI();
        loadSaved(false);
        refreshAll();

        new javax.swing.Timer(5000, e -> refreshPrices()).start();
    }

    private void initializeStocks() {
        stocks.put("AAPL", new Stock("AAPL", "Apple Inc.", 225.50, 1.25));
        stocks.put("GOOGL", new Stock("GOOGL", "Alphabet Inc.", 182.30, 0.85));
        stocks.put("MSFT", new Stock("MSFT", "Microsoft Corp.", 425.70, 1.65));
        stocks.put("AMZN", new Stock("AMZN", "Amazon.com", 218.40, -0.45));
        stocks.put("TSLA", new Stock("TSLA", "Tesla Inc.", 335.20, -1.20));
        stocks.put("NVDA", new Stock("NVDA", "NVIDIA Corp.", 180.60, 2.10));
        stocks.put("INFY", new Stock("INFY", "Infosys Ltd.", 25.80, 0.65));
        stocks.put("TCS", new Stock("TCS", "Tata Consultancy", 42.50, -0.30));
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(background);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(background);

        JLabel title = new JLabel("STOCK TRADING PLATFORM");
        title.setForeground(accent);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));

        JLabel subtitle = label("Virtual trading simulator");

        header.add(title, BorderLayout.WEST);
        header.add(subtitle, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setBackground(background);
        root.add(center, BorderLayout.CENTER);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 10, 10));
        metrics.setBackground(background);
        metrics.setPreferredSize(new Dimension(0, 85));

        JPanel cashPanel = metricPanel("AVAILABLE CASH");
        JPanel valuePanel = metricPanel("PORTFOLIO VALUE");
        JPanel profitPanel = metricPanel("UNREALIZED PROFIT / LOSS");

        cashLabel = (JLabel) cashPanel.getClientProperty("valueLabel");
        portfolioLabel = (JLabel) valuePanel.getClientProperty("valueLabel");
        profitLabel = (JLabel) profitPanel.getClientProperty("valueLabel");

        metrics.add(cashPanel);
        metrics.add(valuePanel);
        metrics.add(profitPanel);

        center.add(metrics, BorderLayout.NORTH);

        JPanel mainArea = new JPanel(new GridLayout(1, 2, 10, 10));
        mainArea.setBackground(background);
        center.add(mainArea, BorderLayout.CENTER);

        JPanel marketPanel = panel("MARKET WATCH");
        marketPanel.setLayout(new BorderLayout(5, 7));

        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBackground(panelColor);

        searchField = new JTextField();
        styleField(searchField);

        JButton searchButton = button("Search", accent);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        marketPanel.add(searchPanel, BorderLayout.NORTH);

        marketModel = model("Symbol", "Company", "Price (₹)", "Change (%)");
        marketTable = table(marketModel);

        marketPanel.add(darkScroll(marketTable), BorderLayout.CENTER);

        JPanel marketButtons = new JPanel(new GridLayout(1, 2, 5, 5));
        marketButtons.setBackground(panelColor);

        JButton favoriteButton = button("☆ Add to Watchlist", accent);
        JButton viewChartButton = button("View Chart", accent);

        marketButtons.add(favoriteButton);
        marketButtons.add(viewChartButton);

        marketPanel.add(marketButtons, BorderLayout.SOUTH);
        mainArea.add(marketPanel);

        JPanel rightArea = new JPanel(new GridLayout(2, 1, 10, 10));
        rightArea.setBackground(background);
        mainArea.add(rightArea);

        JPanel chartPanel = panel("PRICE CHART");
        chartPanel.setLayout(new BorderLayout());

        chart = new Chart();
        chartPanel.add(chart, BorderLayout.CENTER);
        rightArea.add(chartPanel);

        JPanel tradePanel = panel("PLACE ORDER");
        tradePanel.setLayout(new GridLayout(4, 2, 8, 8));

        selectedLabel = label("Selected stock: " + selectedSymbol);
        quantityField = new JTextField("1");
        styleField(quantityField);

        tradePanel.add(label("Stock"));
        tradePanel.add(selectedLabel);
        tradePanel.add(label("Quantity"));
        tradePanel.add(quantityField);

        JButton buyButton = button("BUY STOCK", green);
        JButton sellButton = button("SELL STOCK", red);
        JButton refreshButton = button("Refresh Prices", accent);
        JButton saveButton = button("Save Portfolio", accent);

        tradePanel.add(buyButton);
        tradePanel.add(sellButton);
        tradePanel.add(refreshButton);
        tradePanel.add(saveButton);

        rightArea.add(tradePanel);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setBackground(background);
        bottom.setPreferredSize(new Dimension(0, 220));
        center.add(bottom, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(panelColor);
        tabs.setForeground(textColor);
        tabs.setOpaque(true);

        portfolioModel = model(
                "Symbol", "Quantity", "Avg Cost",
                "Current Price", "Market Value", "Profit / Loss");

        portfolioTable = table(portfolioModel);

        favoritesModel = model("Symbol", "Company", "Price (₹)", "Change (%)");
        favoritesTable = table(favoritesModel);

        historyModel = model("Time", "Action", "Symbol", "Quantity", "Price (₹)");
        historyTable = table(historyModel);

        tabs.addTab("Portfolio", darkScroll(portfolioTable));
        tabs.addTab("Watchlist", darkScroll(favoritesTable));
        tabs.addTab("Transactions", darkScroll(historyTable));

        bottom.add(tabs, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(background);

        statusLabel = label("Ready to trade");

        JPanel footerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        footerButtons.setBackground(background);

        JButton loadButton = button("Load Portfolio", accent);
        JButton exportButton = button("Export CSV", accent);
        JButton resetButton = button("Reset Portfolio", red);

        footerButtons.add(loadButton);
        footerButtons.add(exportButton);
        footerButtons.add(resetButton);

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(footerButtons, BorderLayout.EAST);

        bottom.add(footer, BorderLayout.SOUTH);

        marketTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && marketTable.getSelectedRow() >= 0) {
                int row = marketTable.convertRowIndexToModel(
                        marketTable.getSelectedRow());

                selectedSymbol = String.valueOf(marketModel.getValueAt(row, 0));
                selectedLabel.setText("Selected stock: " + selectedSymbol);
                chart.repaint();
            }
        });

        searchButton.addActionListener(e -> filterMarket());
        searchField.addActionListener(e -> filterMarket());

        favoriteButton.addActionListener(e -> toggleFavorite());

        viewChartButton.addActionListener(e -> {
            if (marketTable.getSelectedRow() >= 0) {
                int row = marketTable.convertRowIndexToModel(
                        marketTable.getSelectedRow());

                selectedSymbol = String.valueOf(marketModel.getValueAt(row, 0));
                selectedLabel.setText("Selected stock: " + selectedSymbol);
                chart.repaint();
            } else {
                warn("Select a stock from Market Watch first.");
            }
        });

        buyButton.addActionListener(e -> trade(true));
        sellButton.addActionListener(e -> trade(false));
        refreshButton.addActionListener(e -> refreshPrices());
        saveButton.addActionListener(e -> savePortfolio());

        loadButton.addActionListener(e -> {
            loadSaved(true);
            refreshAll();
        });

        exportButton.addActionListener(e -> exportCSV());
        resetButton.addActionListener(e -> resetPortfolio());
    }

    private JPanel metricPanel(String heading) {
        JPanel p = new JPanel(new BorderLayout(5, 5));
        p.setBackground(panelColor);
        p.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JLabel name = label(heading);
        name.setFont(new Font("SansSerif", Font.BOLD, 11));

        JLabel value = label("₹0.00");
        value.setFont(new Font("SansSerif", Font.BOLD, 20));
        value.setForeground(accent);

        p.add(name, BorderLayout.NORTH);
        p.add(value, BorderLayout.CENTER);
        p.putClientProperty("valueLabel", value);

        return p;
    }

    private JPanel panel(String title) {
        JPanel p = new JPanel();
        p.setBackground(panelColor);

        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(gridColor),
                        title, 0, 0,
                        new Font("SansSerif", Font.BOLD, 12), accent),
                BorderFactory.createEmptyBorder(7, 7, 7, 7)));

        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(textColor);
        return l;
    }

    private DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);

        t.setBackground(panelColor);
        t.setForeground(textColor);
        t.setGridColor(gridColor);
        t.setSelectionBackground(new Color(71, 85, 105));
        t.setSelectionForeground(Color.WHITE);
        t.setRowHeight(27);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.setFillsViewportHeight(true);
        t.setOpaque(true);
        t.setShowGrid(true);

        t.getTableHeader().setBackground(gridColor);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setOpaque(true);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        t.setAutoCreateRowSorter(true);

        return t;
    }

    private JScrollPane darkScroll(JTable table) {
        JScrollPane scroll = new JScrollPane(table);

        scroll.setBackground(panelColor);
        scroll.setOpaque(true);
        scroll.getViewport().setBackground(panelColor);
        scroll.getViewport().setOpaque(true);

        scroll.getVerticalScrollBar().setBackground(panelColor);
        scroll.getHorizontalScrollBar().setBackground(panelColor);

        return scroll;
    }

    private JButton button(String text, Color color) {
        JButton b = new JButton(text);
        b.setBackground(color);
        b.setForeground(background);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void styleField(JTextField field) {
        field.setBackground(background);
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(gridColor),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
    }

    private String money(double amount) {
        return String.format(Locale.US, "₹%,.2f", amount);
    }

    private String change(double amount) {
        return String.format(Locale.US, "%+.2f%%", amount);
    }

    private void filterMarket() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        marketModel.setRowCount(0);

        for (Stock s : stocks.values()) {
            if (s.symbol.toLowerCase(Locale.ROOT).contains(query)
                    || s.name.toLowerCase(Locale.ROOT).contains(query)) {
                marketModel.addRow(new Object[]{
                        s.symbol, s.name, money(s.price), change(s.change)
                });
            }
        }
    }

    private void toggleFavorite() {
        int row = marketTable.getSelectedRow();

        if (row < 0) {
            warn("Select a stock from Market Watch first.");
            return;
        }

        row = marketTable.convertRowIndexToModel(row);
        String symbol = String.valueOf(marketModel.getValueAt(row, 0));

        if (favorites.contains(symbol)) {
            favorites.remove(symbol);
            statusLabel.setText(symbol + " removed from watchlist.");
        } else {
            favorites.add(symbol);
            statusLabel.setText(symbol + " added to watchlist.");
        }

        refreshFavorites();
    }

    private void trade(boolean buy) {
        Stock stock = stocks.get(selectedSymbol);

        if (stock == null) {
            warn("Select a stock first.");
            return;
        }

        int quantity;

        try {
            quantity = Integer.parseInt(quantityField.getText().trim());

            if (quantity <= 0) {
                warn("Quantity must be greater than zero.");
                return;
            }
        } catch (NumberFormatException ex) {
            warn("Enter a valid whole-number quantity.");
            return;
        }

        int owned = holdings.getOrDefault(selectedSymbol, 0);
        double total = stock.price * quantity;

        if (buy) {
            if (total > cash) {
                warn("Insufficient cash. Required: " + money(total));
                return;
            }

            double oldAverage = averageCost.getOrDefault(selectedSymbol, 0.0);
            double newAverage =
                    (oldAverage * owned + total) / (owned + quantity);

            cash -= total;
            holdings.put(selectedSymbol, owned + quantity);
            averageCost.put(selectedSymbol, newAverage);

            record("BUY", selectedSymbol, quantity, stock.price);
            statusLabel.setText("Bought " + quantity + " shares of " + selectedSymbol);

        } else {
            if (quantity > owned) {
                warn("You only own " + owned + " shares of " + selectedSymbol);
                return;
            }

            cash += total;
            int remaining = owned - quantity;

            if (remaining == 0) {
                holdings.remove(selectedSymbol);
                averageCost.remove(selectedSymbol);
            } else {
                holdings.put(selectedSymbol, remaining);
            }

            record("SELL", selectedSymbol, quantity, stock.price);
            statusLabel.setText("Sold " + quantity + " shares of " + selectedSymbol);
        }

        refreshAll();
    }

    private void record(String action, String symbol, int quantity, double price) {
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date());

        history.add(new String[]{
                time, action, symbol,
                String.valueOf(quantity),
                String.format(Locale.US, "%.2f", price)
        });
    }

    private void refreshPrices() {
        Random random = new Random();

        for (Stock stock : stocks.values()) {
            double movement = (random.nextDouble() - 0.48) * 1.5;
            stock.price = Math.max(1.0, stock.price * (1 + movement / 100.0));
            stock.change = movement;
        }

        refreshAll();
        statusLabel.setText("Simulated prices refreshed.");
    }

    private void refreshAll() {
        refreshMarket();
        refreshPortfolio();
        refreshFavorites();
        refreshHistory();
        refreshMetrics();

        if (chart != null) {
            chart.repaint();
        }
    }

    private void refreshMarket() {
        String query = searchField == null
                ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        marketModel.setRowCount(0);

        for (Stock s : stocks.values()) {
            if (s.symbol.toLowerCase(Locale.ROOT).contains(query)
                    || s.name.toLowerCase(Locale.ROOT).contains(query)) {
                marketModel.addRow(new Object[]{
                        s.symbol, s.name, money(s.price), change(s.change)
                });
            }
        }
    }

    private void refreshPortfolio() {
        portfolioModel.setRowCount(0);

        for (String symbol : holdings.keySet()) {
            Stock stock = stocks.get(symbol);
            int quantity = holdings.getOrDefault(symbol, 0);

            if (stock == null || quantity <= 0) {
                continue;
            }

            double avg = averageCost.getOrDefault(symbol, 0.0);
            double value = stock.price * quantity;
            double pnl = (stock.price - avg) * quantity;

            portfolioModel.addRow(new Object[]{
                    symbol, quantity, money(avg), money(stock.price),
                    money(value), money(pnl)
            });
        }
    }

    private void refreshFavorites() {
        favoritesModel.setRowCount(0);

        for (String symbol : favorites) {
            Stock stock = stocks.get(symbol);

            if (stock != null) {
                favoritesModel.addRow(new Object[]{
                        stock.symbol, stock.name,
                        money(stock.price), change(stock.change)
                });
            }
        }
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);

        for (String[] tx : history) {
            historyModel.addRow(new Object[]{
                    tx.length > 0 ? tx[0] : "",
                    tx.length > 1 ? tx[1] : "",
                    tx.length > 2 ? tx[2] : "",
                    tx.length > 3 ? tx[3] : "",
                    tx.length > 4 ? tx[4] : ""
            });
        }
    }

    private void refreshMetrics() {
        double marketValue = 0;
        double pnl = 0;

        for (String symbol : holdings.keySet()) {
            Stock stock = stocks.get(symbol);
            int quantity = holdings.getOrDefault(symbol, 0);

            if (stock != null) {
                marketValue += stock.price * quantity;
                pnl += (stock.price -
                        averageCost.getOrDefault(symbol, 0.0)) * quantity;
            }
        }

        cashLabel.setText(money(cash));
        portfolioLabel.setText(money(marketValue));
        profitLabel.setText(money(pnl));
        profitLabel.setForeground(pnl >= 0 ? green : red);
    }

    private void savePortfolio() {
        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(SAVE_FILE))) {

            out.writeDouble(cash);
            out.writeObject(new HashMap<>(holdings));
            out.writeObject(new HashMap<>(averageCost));
            out.writeObject(new HashSet<>(favorites));
            out.writeObject(new ArrayList<>(history));

            statusLabel.setText("Portfolio saved.");
            JOptionPane.showMessageDialog(this, "Portfolio saved successfully.");

        } catch (IOException ex) {
            warn("Save failed: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadSaved(boolean notify) {
        File file = new File(SAVE_FILE);

        if (!file.exists()) {
            if (notify) {
                warn("No saved portfolio found yet.");
            }
            return;
        }

        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(file))) {

            cash = in.readDouble();

            holdings.clear();
            holdings.putAll((Map<String, Integer>) in.readObject());

            averageCost.clear();
            averageCost.putAll((Map<String, Double>) in.readObject());

            favorites.clear();
            favorites.addAll((Set<String>) in.readObject());

            history.clear();

            Object savedHistory = in.readObject();

            if (savedHistory instanceof List<?>) {
                for (Object item : (List<?>) savedHistory) {
                    if (item instanceof String[]) {
                        history.add((String[]) item);
                    } else if (item instanceof String) {
                        String[] parts = ((String) item).split(",", -1);
                        history.add(new String[]{
                                parts.length > 0 ? parts[0] : "",
                                parts.length > 1 ? parts[1] : "",
                                parts.length > 2 ? parts[2] : "",
                                parts.length > 3 ? parts[3] : "",
                                parts.length > 4 ? parts[4] : ""
                        });
                    }
                }
            }

            if (notify) {
                statusLabel.setText("Portfolio loaded.");
                JOptionPane.showMessageDialog(this, "Portfolio loaded successfully.");
            }

        } catch (Exception ex) {
            if (notify) {
                warn("Could not load portfolio: " + ex.getMessage());
            }
        }
    }

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("stock_transactions.csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                        new FileOutputStream(chooser.getSelectedFile()),
                        StandardCharsets.UTF_8))) {

            writer.println("Time,Action,Symbol,Quantity,Price");

            for (String[] tx : history) {
                for (int i = 0; i < 5; i++) {
                    if (i > 0) {
                        writer.print(",");
                    }

                    String value = i < tx.length ? tx[i] : "";
                    value = value.replace("\"", "\"\"");
                    writer.print("\"" + value + "\"");
                }

                writer.println();
            }

            statusLabel.setText("CSV exported.");
            JOptionPane.showMessageDialog(this, "CSV exported successfully.");

        } catch (IOException ex) {
            warn("CSV export failed: " + ex.getMessage());
        }
    }

    private void resetPortfolio() {
        int answer = JOptionPane.showConfirmDialog(
                this,
                "Reset cash, holdings, watchlist and transaction history?",
                "Confirm Reset",
                JOptionPane.YES_NO_OPTION);

        if (answer == JOptionPane.YES_OPTION) {
            cash = 100000.0;
            holdings.clear();
            averageCost.clear();
            favorites.clear();
            history.clear();

            refreshAll();
            statusLabel.setText("Portfolio reset.");
        }
    }

    private void warn(String message) {
        JOptionPane.showMessageDialog(
                this, message,
                "Stock Trading Platform",
                JOptionPane.WARNING_MESSAGE);
    }

    private class Chart extends JPanel {
        Chart() {
            setPreferredSize(new Dimension(300, 160));
            setBackground(background);
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2.setColor(gridColor);

            for (int i = 1; i < 5; i++) {
                g2.drawLine(10, i * h / 5, w - 10, i * h / 5);
            }

            for (int i = 1; i < 7; i++) {
                g2.drawLine(i * w / 7, 10, i * w / 7, h - 10);
            }

            Stock stock = stocks.get(selectedSymbol);

            if (stock != null && w > 40 && h > 40) {
                g2.setColor(stock.change >= 0 ? green : red);
                g2.setStroke(new BasicStroke(2.5f));

                int previousX = 10;
                int previousY = h / 2;
                int points = 35;

                for (int i = 1; i < points; i++) {
                    int x = 10 + i * (w - 20) / (points - 1);
                    int y = (int) (h / 2
                            - Math.sin(i * 0.5) * h * 0.18
                            - Math.cos(i * 0.21) * h * 0.12);

                    y = Math.max(15, Math.min(h - 15, y));

                    g2.drawLine(previousX, previousY, x, y);
                    previousX = x;
                    previousY = y;
                }

                g2.setColor(Color.WHITE);
                g2.drawString(
                        selectedSymbol + "  " + money(stock.price),
                        12, 20);
            }

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StockTradingPlatform app = new StockTradingPlatform();
            app.setVisible(true);
        });
    }
}