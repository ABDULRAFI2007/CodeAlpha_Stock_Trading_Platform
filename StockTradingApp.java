import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
// -------------------- STOCK CLASS --------------------
class Stock implements Serializable {
    private final String symbol;
    private final String name;
    private double price;

    public Stock(String symbol, String name, double price) {
        this.symbol = symbol;
        this.name = name;
        this.price = price;
    }

    public void updatePrice() {
        double changePercent = (Math.random() * 10) - 5; // -5% to +5%
        price += price * (changePercent / 100);
        price = Math.round(price * 100.0) / 100.0;
    }

    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getPrice() { return price; }
}

// -------------------- TRANSACTION CLASS --------------------
class Transaction implements Serializable {
    private final String stockSymbol;
    private final int quantity;
    private final double price;
    private final String type;
    private final LocalDateTime timestamp;

    public Transaction(String stockSymbol, int quantity, double price, String type) {
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.price = price;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return timestamp + " | " + type + " | " + stockSymbol +
                " | Qty: " + quantity + " | Price: $" + price;
    }
}

// -------------------- PORTFOLIO CLASS --------------------
class Portfolio implements Serializable {
    private final Map<String, Integer> holdings = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public double buyStock(Stock stock, int quantity) {
        double cost = stock.getPrice() * quantity;
        holdings.put(stock.getSymbol(),
                holdings.getOrDefault(stock.getSymbol(), 0) + quantity);
        transactions.add(new Transaction(stock.getSymbol(), quantity,
                stock.getPrice(), "BUY"));
        return cost;
    }

    public double sellStock(Stock stock, int quantity) throws Exception {
        int owned = holdings.getOrDefault(stock.getSymbol(), 0);
        if (owned < quantity)
            throw new Exception("Not enough shares to sell.");

        double gain = stock.getPrice() * quantity;
        holdings.put(stock.getSymbol(), owned - quantity);
        transactions.add(new Transaction(stock.getSymbol(), quantity,
                stock.getPrice(), "SELL"));
        return gain;
    }

    public double calculateValue(Market market) {
        double total = 0;
        for (String symbol : holdings.keySet()) {
            int qty = holdings.get(symbol);
            total += market.getStock(symbol).getPrice() * qty;
        }
        return Math.round(total * 100.0) / 100.0;
    }

    public void showTransactions() {
        for (Transaction t : transactions)
            System.out.println(t);
    }

    public Map<String, Integer> getHoldings() {
        return holdings;
    }
}

// -------------------- USER CLASS --------------------
class User implements Serializable {
    private final String name;
    private double balance;
    private final Portfolio portfolio;

    public User(String name, double balance) {
        this.name = name;
        this.balance = balance;
        this.portfolio = new Portfolio();
    }

    public void buy(Stock stock, int quantity) throws Exception {
        double cost = stock.getPrice() * quantity;
        if (balance < cost)
            throw new Exception("Insufficient balance.");
        balance -= portfolio.buyStock(stock, quantity);
    }

    public void sell(Stock stock, int quantity) throws Exception {
        balance += portfolio.sellStock(stock, quantity);
    }

    public String getName() { return name; }
    public double getBalance() { return balance; }
    public Portfolio getPortfolio() { return portfolio; }
}

// -------------------- MARKET CLASS --------------------
class Market {
    private final Map<String, Stock> stocks = new HashMap<>();

    public void addStock(Stock stock) {
        stocks.put(stock.getSymbol(), stock);
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }

    public void displayMarket() {
        System.out.println("\n--- Market Data ---");
        for (Stock s : stocks.values()) {
            System.out.println(s.getSymbol() + " - " +
                    s.getName() + " : $" + s.getPrice());
        }
    }

    public void simulatePriceChanges() {
        for (Stock s : stocks.values())
            s.updatePrice();
    }
}

// -------------------- MAIN APPLICATION --------------------
public class StockTradingApp {

    private static final String FILE_NAME = "portfolio.dat";

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        Market market = new Market();
        market.addStock(new Stock("AAPL", "Apple Inc.", 150));
        market.addStock(new Stock("GOOGL", "Alphabet Inc.", 2800));
        market.addStock(new Stock("TSLA", "Tesla Inc.", 700));

        User user = loadUser();
        if (user == null)
            user = new User("Alice", 10000);

        while (true) {
            market.simulatePriceChanges();
            market.displayMarket();

            System.out.println("\nBalance: $" + user.getBalance());
            System.out.println("1. Buy");
            System.out.println("2. Sell");
            System.out.println("3. Portfolio Value");
            System.out.println("4. Transactions");
            System.out.println("5. Save & Exit");

            int choice = scanner.nextInt();

            try {
                switch (choice) {
                    case 1 -> {
                        System.out.print("Enter symbol: ");
                        String buySymbol = scanner.next();
                        System.out.print("Enter quantity: ");
                        int buyQty = scanner.nextInt();
                        user.buy(market.getStock(buySymbol), buyQty);
                    }
                    case 2 -> {
                        System.out.print("Enter symbol: ");
                        String sellSymbol = scanner.next();
                        System.out.print("Enter quantity: ");
                        int sellQty = scanner.nextInt();
                        user.sell(market.getStock(sellSymbol), sellQty);
                    }
                    case 3 -> {
                        double value = user.getPortfolio().calculateValue(market);
                        System.out.println("Portfolio Value: $" + value);
                    }
                    case 4 -> user.getPortfolio().showTransactions();
                    case 5 -> {
                        saveUser(user);
                        System.out.println("Portfolio saved. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid choice.");
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // -------- FILE SAVE --------
    private static void saveUser(User user) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(user);
        } catch (IOException e) {
            System.out.println("Error saving file.");
        }
    }

    // -------- FILE LOAD --------
    private static User loadUser() {
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            return (User) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }
}