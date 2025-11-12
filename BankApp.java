import java.util.*;
import java.text.DecimalFormat;

/**
 * BankApp.java
 * Single-file console banking application using OOP concepts from your PPTs.
 *
 * Features:
 * - Register / Login (in-memory)
 * - Savings Account: deposit, withdraw, balance, transactions
 * - Fixed Deposit (FD): create FD with term and interest (simple interest)
 * - Loan: request loan, pay instalments, view outstanding
 * - Cards: DebitCard (withdrawals), CreditCard (charge, pay, outstanding, min due)
 * - Transactions stored per account (inner class Transaction)
 *
 * All classes implemented as inner static classes for single-file delivery.
 *
 * Author: ChatGPT (example project)
 */

public class BankApp {

    private static final Scanner scanner = new Scanner(System.in);
    private static final DecimalFormat df = new DecimalFormat("#0.00");

    // ---------------------------
    // Main entry
    // ---------------------------
    public static void main(String[] args) {
        Bank bank = new Bank("Syllabus Bank");
        seedDemoUsers(bank); // optional demo data
        System.out.println("Welcome to " + bank.getBankName());
        while (true) {
            System.out.println("\n1) Register  2) Login  3) Exit");
            System.out.print("Choose: ");
            String ch = scanner.nextLine().trim();
            if (ch.equals("1")) {
                registerFlow(bank);
            } else if (ch.equals("2")) {
                User user = loginFlow(bank);
                if (user != null) userMenu(bank, user);
            } else if (ch.equals("3")) {
                System.out.println("Thank you. Goodbye!");
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }
    }

    // ---------------------------
    // Registration and Login flows
    // ---------------------------
    private static void registerFlow(Bank bank) {
        System.out.println("\n--- Register ---");
        System.out.print("Enter username: ");
        String username = scanner.nextLine().trim();
        if (bank.getUser(username) != null) {
            System.out.println("Username already exists.");
            return;
        }
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Enter full name: ");
        String name = scanner.nextLine();
        User u = new User(username, password, name);
        bank.addUser(u);
        // create a default savings account
        SavingsAccount sa = new SavingsAccount(u.generateAccountNo(), u.getName(), 0.0);
        u.addAccount(sa);
        System.out.println("Registered. Default savings account created (" + sa.getAccountNo() + ").");
    }

    private static User loginFlow(Bank bank) {
        System.out.println("\n--- Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        User u = bank.authenticate(username, password);
        if (u == null) {
            System.out.println("Invalid credentials.");
        } else {
            System.out.println("Welcome, " + u.getName() + "!");
        }
        return u;
    }

    // ---------------------------
    // User menu
    // ---------------------------
    private static void userMenu(Bank bank, User user) {
        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1) View Accounts  2) Open FD  3) Apply Loan  4) Cards  5) Logout");
            System.out.print("Choose: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1":
                    viewAccountsMenu(user);
                    break;
                case "2":
                    openFDMenu(user);
                    break;
                case "3":
                    applyLoanMenu(user);
                    break;
                case "4":
                    cardsMenu(user);
                    break;
                case "5":
                    System.out.println("Logging out...");
                    return;
                default:
                    System.out.println("Invalid.");
            }
        }
    }

    // ---------------------------
    // Accounts view / operations
    // ---------------------------
    private static void viewAccountsMenu(User user) {
        while (true) {
            System.out.println("\n--- Your Accounts ---");
            user.listAccountsBrief();
            System.out.println("Select account number to manage or 'b' to go back:");
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("b")) return;
            Account acc = user.getAccountByNo(choice);
            if (acc == null) {
                System.out.println("Account not found.");
                continue;
            }
            if (acc instanceof SavingsAccount) {
                manageSavings((SavingsAccount) acc);
            } else if (acc instanceof FDAccount) {
                showFDInfo((FDAccount) acc);
            } else if (acc instanceof LoanAccount) {
                manageLoan((LoanAccount) acc);
            } else if (acc instanceof Card) {
                manageCard((Card) acc);
            } else {
                System.out.println("Unknown account type.");
            }
        }
    }

    private static void manageSavings(SavingsAccount sa) {
        while (true) {
            System.out.println("\n--- Savings Account " + sa.getAccountNo() + " ---");
            System.out.println("Balance: " + df.format(sa.getBalance()));
            System.out.println("1) Deposit 2) Withdraw 3) Transactions 4) Back");
            System.out.print("Choose: ");
            String ch = scanner.nextLine().trim();
            if (ch.equals("1")) {
                System.out.print("Amount to deposit: ");
                double amt = readDouble();
                sa.deposit(amt);
                System.out.println("Deposited. New balance: " + df.format(sa.getBalance()));
            } else if (ch.equals("2")) {
                System.out.print("Amount to withdraw: ");
                double amt = readDouble();
                boolean ok = sa.withdraw(amt);
                if (ok) System.out.println("Withdrawn. New balance: " + df.format(sa.getBalance()));
                else System.out.println("Insufficient funds.");
            } else if (ch.equals("3")) {
                sa.printTransactions();
            } else if (ch.equals("4")) return;
            else System.out.println("Invalid.");
        }
    }

    private static void showFDInfo(FDAccount fd) {
        System.out.println("\n--- Fixed Deposit ---");
        System.out.println("Account No: " + fd.getAccountNo());
        System.out.println("Principal: " + df.format(fd.getPrincipal()));
        System.out.println("Term months: " + fd.getTermMonths());
        System.out.println("Rate (% pa): " + df.format(fd.getInterestRate()));
        System.out.println("Maturity (Simple Interest): " + df.format(fd.calculateMaturityAmount()));
        System.out.println("Transactions:");
        fd.printTransactions();
    }

    private static void manageLoan(LoanAccount loan) {
        while (true) {
            System.out.println("\n--- Loan Account " + loan.getAccountNo() + " ---");
            System.out.println("Principal: " + df.format(loan.getPrincipal()));
            System.out.println("Outstanding: " + df.format(loan.getOutstanding()));
            System.out.println("Rate (% pa): " + df.format(loan.getInterestRate()));
            System.out.println("1) Pay Instalment 2) Transactions 3) Back");
            System.out.print("Choose: ");
            String ch = scanner.nextLine().trim();
            if (ch.equals("1")) {
                System.out.print("Amount to pay: ");
                double amt = readDouble();
                loan.pay(amt);
                System.out.println("Payment done. Outstanding: " + df.format(loan.getOutstanding()));
            } else if (ch.equals("2")) {
                loan.printTransactions();
            } else if (ch.equals("3")) return;
            else System.out.println("Invalid.");
        }
    }

    // ---------------------------
    // FD creation
    // ---------------------------
    private static void openFDMenu(User user) {
        System.out.println("\n--- Open Fixed Deposit (FD) ---");
        System.out.print("Enter principal amount: ");
        double p = readDouble();
        System.out.print("Enter term (months): ");
        int months = readInt();
        System.out.print("Enter annual interest rate (%) (e.g., 6): ");
        double rate = readDouble();
        FDAccount fd = new FDAccount(user.generateAccountNo(), user.getName(), p, months, rate);
        user.addAccount(fd);
        fd.addTransaction("Opened FD", p);
        System.out.println("FD opened. Account no: " + fd.getAccountNo());
        System.out.println("Maturity amount (simple interest): " + df.format(fd.calculateMaturityAmount()));
    }

    // ---------------------------
    // Loan application
    // ---------------------------
    private static void applyLoanMenu(User user) {
        System.out.println("\n--- Apply Loan ---");
        System.out.print("Enter principal requested: ");
        double p = readDouble();
        System.out.print("Enter annual interest rate (%) offered: ");
        double rate = readDouble();
        System.out.print("Enter term months: ");
        int months = readInt();
        LoanAccount loan = new LoanAccount(user.generateAccountNo(), user.getName(), p, rate, months);
        user.addAccount(loan);
        loan.addTransaction("Loan granted", p);
        System.out.println("Loan account created: " + loan.getAccountNo());
        System.out.println("Outstanding: " + df.format(loan.getOutstanding()));
    }

    // ---------------------------
    // Cards menu: issue debit/credit & manage
    // ---------------------------
    private static void cardsMenu(User user) {
        while (true) {
            System.out.println("\n--- Cards ---");
            System.out.println("1) Issue Debit Card 2) Issue Credit Card 3) List Cards 4) Back");
            System.out.print("Choose: ");
            String ch = scanner.nextLine().trim();
            if (ch.equals("1")) {
                System.out.print("Linked savings account no: ");
                String accNo = scanner.nextLine().trim();
                Account acc = user.getAccountByNo(accNo);
                if (acc == null || !(acc instanceof SavingsAccount)) {
                    System.out.println("Savings account not found.");
                    continue;
                }
                DebitCard dc = new DebitCard(user.generateAccountNo(), user.getName(), (SavingsAccount) acc);
                user.addAccount(dc);
                System.out.println("Debit card issued: " + dc.getAccountNo());
            } else if (ch.equals("2")) {
                System.out.print("Credit limit: ");
                double limit = readDouble();
                CreditCard cc = new CreditCard(user.generateAccountNo(), user.getName(), limit);
                user.addAccount(cc);
                System.out.println("Credit card issued: " + cc.getAccountNo());
            } else if (ch.equals("3")) {
                user.listAccountsBrief();
                System.out.println("Enter card account no to manage or 'b' back:");
                String choice = scanner.nextLine().trim();
                if (choice.equalsIgnoreCase("b")) continue;
                Account a = user.getAccountByNo(choice);
                if (a == null || !(a instanceof Card)) {
                    System.out.println("Card not found.");
                    continue;
                }
                manageCard((Card) a);
            } else if (ch.equals("4")) return;
            else System.out.println("Invalid.");
        }
    }

    private static void manageCard(Card card) {
        if (card instanceof DebitCard) {
            DebitCard dc = (DebitCard) card;
            while (true) {
                System.out.println("\n--- Debit Card " + dc.getAccountNo() + " ---");
                System.out.println("Linked Savings balance: " + df.format(dc.getLinkedAccount().getBalance()));
                System.out.println("1) Withdraw using card 2) Transactions 3) Back");
                System.out.print("Choose: ");
                String ch = scanner.nextLine().trim();
                if (ch.equals("1")) {
                    System.out.print("Amount: "); double amt = readDouble();
                    boolean ok = dc.charge(amt);
                    if (ok) System.out.println("Withdrawn. Linked balance: " + df.format(dc.getLinkedAccount().getBalance()));
                    else System.out.println("Insufficient funds.");
                } else if (ch.equals("2")) dc.printTransactions();
                else if (ch.equals("3")) return;
                else System.out.println("Invalid.");
            }
        } else if (card instanceof CreditCard) {
            CreditCard cc = (CreditCard) card;
            while (true) {
                System.out.println("\n--- Credit Card " + cc.getAccountNo() + " ---");
                System.out.println("Outstanding: " + df.format(cc.getOutstanding()));
                System.out.println("Min due (10%): " + df.format(cc.minDue()));
                System.out.println("1) Charge 2) Pay 3) Transactions 4) Back");
                System.out.print("Choose: ");
                String ch = scanner.nextLine().trim();
                if (ch.equals("1")) {
                    System.out.print("Amount to charge: ");
                    double amt = readDouble();
                    boolean ok = cc.charge(amt);
                    if (ok) System.out.println("Charged. Outstanding: " + df.format(cc.getOutstanding()));
                    else System.out.println("Charge would exceed limit.");
                } else if (ch.equals("2")) {
                    System.out.print("Amount to pay: ");
                    double amt = readDouble();
                    cc.pay(amt);
                    System.out.println("Paid. Outstanding: " + df.format(cc.getOutstanding()));
                } else if (ch.equals("3")) cc.printTransactions();
                else if (ch.equals("4")) return;
                else System.out.println("Invalid.");
            }
        } else {
            System.out.println("Unknown card type.");
        }
    }

    // ---------------------------
    // Helper read methods
    // ---------------------------
    private static double readDouble() {
        while (true) {
            try {
                String s = scanner.nextLine().trim();
                return Double.parseDouble(s);
            } catch (Exception e) {
                System.out.print("Invalid. Enter number: ");
            }
        }
    }

    private static int readInt() {
        while (true) {
            try {
                String s = scanner.nextLine().trim();
                return Integer.parseInt(s);
            } catch (Exception e) {
                System.out.print("Invalid. Enter integer: ");
            }
        }
    }

    // ---------------------------
    // Demo data
    // ---------------------------
    private static void seedDemoUsers(Bank bank) {
        User demo = new User("alice", "alice123", "Alice Wonderland");
        bank.addUser(demo);
        SavingsAccount s1 = new SavingsAccount(demo.generateAccountNo(), demo.getName(), 5000.0);
        demo.addAccount(s1);
        s1.deposit(1500.0);
        FDAccount fd = new FDAccount(demo.generateAccountNo(), demo.getName(), 10000.0, 12, 5.5);
        demo.addAccount(fd);
        LoanAccount loan = new LoanAccount(demo.generateAccountNo(), demo.getName(), 20000.0, 7.5, 24);
        demo.addAccount(loan);
        CreditCard cc = new CreditCard(demo.generateAccountNo(), demo.getName(), 5000.0);
        demo.addAccount(cc);
        bank.addUser(new User("bob", "bob123", "Bob Builder"));
    }

    // ---------------------------
    // BANK CLASS
    // ---------------------------
    static class Bank {
        private String bankName;
        private Map<String, User> users = new HashMap<>();

        public Bank(String name) {
            this.bankName = name;
        }

        public String getBankName() { return bankName; }

        public void addUser(User u) { users.put(u.getUsername(), u); }

        public User getUser(String username) { return users.get(username); }

        public User authenticate(String username, String password) {
            User u = users.get(username);
            if (u != null && u.getPassword().equals(password)) return u;
            return null;
        }
    }

    // ---------------------------
    // USER CLASS
    // ---------------------------
    static class User {
        private String username;
        private String password;
        private String name;
        private List<Account> accounts = new ArrayList<>();
        private int accountCounter = 1000;

        public User(String username, String password, String name) {
            this.username = username;
            this.password = password;
            this.name = name;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; } // in real app avoid plain password
        public String getName() { return name; }

        public String generateAccountNo() {
            accountCounter++;
            return username.substring(0, Math.min(3, username.length())).toUpperCase() + "-" + accountCounter;
        }

        public void addAccount(Account a) {
            accounts.add(a);
        }

        public void listAccountsBrief() {
            if (accounts.isEmpty()) {
                System.out.println("(no accounts)");
                return;
            }
            System.out.printf("%-20s %-12s %-12s\n", "Account No", "Type", "Balance/Info");
            for (Account a : accounts) {
                String info = "";
                if (a instanceof SavingsAccount) info = df.format(((SavingsAccount)a).getBalance());
                else if (a instanceof FDAccount) info = "FD Principal " + df.format(((FDAccount)a).getPrincipal());
                else if (a instanceof LoanAccount) info = "Outstanding " + df.format(((LoanAccount)a).getOutstanding());
                else if (a instanceof Card) info = "Card";
                System.out.printf("%-20s %-12s %-12s\n", a.getAccountNo(), a.getClass().getSimpleName(), info);
            }
        }

        public Account getAccountByNo(String accNo) {
            for (Account a : accounts) if (a.getAccountNo().equals(accNo)) return a;
            return null;
        }
    }

    // ---------------------------
    // ACCOUNT (abstract)
    // ---------------------------
    static abstract class Account {
        private String accountNo;
        private String holderName;
        protected List<Transaction> transactions = new ArrayList<>();

        public Account(String accountNo, String holderName) {
            this.accountNo = accountNo;
            this.holderName = holderName;
        }

        public String getAccountNo() { return accountNo; }
        public String getHolderName() { return holderName; }

        public void addTransaction(String desc, double amount) {
            transactions.add(new Transaction(desc, amount));
        }

        public void printTransactions() {
            if (transactions.isEmpty()) {
                System.out.println("(no transactions)");
                return;
            }
            System.out.printf("%-22s %-30s %12s\n", "Timestamp", "Description", "Amount");
            // sort by timestamp (old->new)
            Collections.sort(transactions, new Comparator<Transaction>() {
                public int compare(Transaction a, Transaction b) {
                    return Long.compare(a.timestamp, b.timestamp);
                }
            });
            for (Transaction t : transactions) {
                System.out.printf("%-22s %-30s %12s\n", t.getTimeString(), t.getDescription(), df.format(t.getAmount()));
            }
        }

        // Inner transaction class
        static class Transaction {
            private long timestamp;
            private String description;
            private double amount;

            public Transaction(String description, double amount) {
                this.timestamp = System.currentTimeMillis();
                this.description = description;
                this.amount = amount;
            }

            public String getTimeString() {
                Date d = new Date(timestamp);
                return d.toString();
            }

            public String getDescription() { return description; }
            public double getAmount() { return amount; }
        }
    }

    // ---------------------------
    // SavingsAccount
    // ---------------------------
    static class SavingsAccount extends Account {
        private double balance;

        public SavingsAccount(String accountNo, String holderName, double initial) {
            super(accountNo, holderName);
            this.balance = initial;
            addTransaction("Account opened", initial);
        }

        public double getBalance() { return balance; }

        public void deposit(double amt) {
            if (amt <= 0) return;
            balance += amt;
            addTransaction("Deposit", amt);
        }

        public boolean withdraw(double amt) {
            if (amt <= 0) return false;
            if (amt > balance) return false;
            balance -= amt;
            addTransaction("Withdraw", -amt);
            return true;
        }
    }

    // ---------------------------
    // FDAccount (Fixed Deposit)
    // ---------------------------
    static class FDAccount extends Account {
        private double principal;
        private int termMonths;
        private double interestRate; // annual %

        public FDAccount(String accountNo, String holderName, double principal, int termMonths, double rate) {
            super(accountNo, holderName);
            this.principal = principal;
            this.termMonths = termMonths;
            this.interestRate = rate;
            addTransaction("FD Opened", principal);
        }

        public double getPrincipal() { return principal; }
        public int getTermMonths() { return termMonths; }
        public double getInterestRate() { return interestRate; }

        // Simple interest maturity (principal + principal * r * t)
        public double calculateMaturityAmount() {
            double years = termMonths / 12.0;
            return principal + (principal * (interestRate / 100.0) * years);
        }
    }

    // ---------------------------
    // LoanAccount
    // ---------------------------
    static class LoanAccount extends Account {
        private double principal;
        private double outstanding;
        private double interestRate; // annual %
        private int termMonths;

        public LoanAccount(String accountNo, String holderName, double principal, double rate, int months) {
            super(accountNo, holderName);
            this.principal = principal;
            this.outstanding = principal; // initial
            this.interestRate = rate;
            this.termMonths = months;
            addTransaction("Loan issued", principal);
        }

        public double getPrincipal() { return principal; }
        public double getOutstanding() { return outstanding; }
        public double getInterestRate() { return interestRate; }
        public int getTermMonths() { return termMonths; }

        // Pay amount towards outstanding (simple: reduce outstanding)
        public void pay(double amt) {
            if (amt <= 0) return;
            double payment = Math.min(amt, outstanding);
            outstanding -= payment;
            addTransaction("Loan payment", -payment);
        }
    }

    // ---------------------------
    // Card (abstract)
    // ---------------------------
    static abstract class Card extends Account {
        public Card(String accountNo, String holderName) {
            super(accountNo, holderName);
        }

        // charge (withdraw/charge), pay (for credit)
        public abstract boolean charge(double amt);
        public abstract void pay(double amt);
    }

    // ---------------------------
    // DebitCard
    // ---------------------------
    static class DebitCard extends Card {
        private SavingsAccount linkedAccount;

        public DebitCard(String accountNo, String holderName, SavingsAccount linked) {
            super(accountNo, holderName);
            this.linkedAccount = linked;
            addTransaction("Debit card issued", 0.0);
        }

        public SavingsAccount getLinkedAccount() { return linkedAccount; }

        // charge means withdraw from linked savings
        @Override
        public boolean charge(double amt) {
            if (amt <= 0) return false;
            boolean ok = linkedAccount.withdraw(amt);
            if (ok) addTransaction("Card withdrawal", -amt);
            return ok;
        }

        @Override
        public void pay(double amt) {
            // not applicable for debit card, but implement to deposit into linked
            if (amt <= 0) return;
            linkedAccount.deposit(amt);
            addTransaction("Card deposit", amt);
        }
    }

    // ---------------------------
    // CreditCard
    // ---------------------------
    static class CreditCard extends Card {
        private double creditLimit;
        private double outstanding; // amount owed on card

        public CreditCard(String accountNo, String holderName, double creditLimit) {
            super(accountNo, holderName);
            this.creditLimit = creditLimit;
            this.outstanding = 0.0;
            addTransaction("Credit card issued", 0.0);
        }

        public double getCreditLimit() { return creditLimit; }
        public double getOutstanding() { return outstanding; }

        @Override
        public boolean charge(double amt) {
            if (amt <= 0) return false;
            if (outstanding + amt > creditLimit) return false;
            outstanding += amt;
            addTransaction("Card charge", amt);
            return true;
        }

        @Override
        public void pay(double amt) {
            if (amt <= 0) return;
            double payment = Math.min(amt, outstanding);
            outstanding -= payment;
            addTransaction("Card payment", -payment);
        }

        // Minimum due example: 10% of outstanding or fixed small value
        public double minDue() {
            return Math.max(10.0, outstanding * 0.10);
        }
    }
}