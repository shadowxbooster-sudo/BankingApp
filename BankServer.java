// BankServer.java
// Simple single-file Java HTTP server for the Banking App
// Uses only JDK standard library (com.sun.net.httpserver.HttpServer).
// Compile: javac BankServer.java
// Run:     java BankServer
//
// NOTE: This is a demo server for educational use. Authentication is trivial (for demo).
// Data stored in-memory; restarting server resets data.

import com.sun.net.httpserver.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

/* ----------------------
   Main server & endpoints
   ---------------------- */
public class BankServer {
    public static void main(String[] args) throws Exception {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        Bank bank = new Bank("Syllabus Bank");

        // Demo user
        User demo = new User("alice", "alice123", "Alice Wonderland");
        bank.addUser(demo);
        demo.addAccount(new SavingsAccount(demo.generateAccountNo(), demo.getName(), 5000.0));

        // Contexts (endpoints)
        server.createContext("/register", new RegisterHandler(bank));
        server.createContext("/login", new LoginHandler(bank));
        server.createContext("/accounts", new AccountsHandler(bank));
        server.createContext("/savings/deposit", new SavingsDepositHandler(bank));
        server.createContext("/savings/withdraw", new SavingsWithdrawHandler(bank));
        server.createContext("/fd/create", new FDCreateHandler(bank));
        server.createContext("/loan/apply", new LoanApplyHandler(bank));
        server.createContext("/card/issue", new CardIssueHandler(bank));
        server.createContext("/card/charge", new CardChargeHandler(bank));
        server.createContext("/card/pay", new CardPayHandler(bank));
        server.createContext("/credit/mindue", new CreditMinDueHandler(bank));

        server.setExecutor(null);
        System.out.println("BankServer running on port " + port);
        server.start();
    }

    // -------------------------
    // Helper: parse form data (x-www-form-urlencoded)
    // -------------------------
    static Map<String, String> parseForm(InputStream is, String query) throws IOException {
        Map<String,String> map = new HashMap<>();
        String body = "";
        if (is != null) {
            try (Scanner s = new Scanner(is, "UTF-8")) {
                s.useDelimiter("\\A");
                body = s.hasNext() ? s.next() : "";
            }
        }
        String combined = (query == null ? "" : query) + (body.isEmpty() ? "" : "&" + body);
        String[] pairs = combined.split("&");
        for (String p : pairs) {
            if (p.trim().isEmpty()) continue;
            String[] kv = p.split("=",2);
            String k = URLDecoder.decode(kv[0], "UTF-8");
            String v = kv.length>1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
            map.put(k,v);
        }
        return map;
    }

    // -------------------------
    // Helper: write JSON response & CORS
    // -------------------------
    static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.add("Content-Type","application/json; charset=utf-8");
        h.add("Access-Control-Allow-Origin","*");
        h.add("Access-Control-Allow-Methods","GET,POST,OPTIONS");
        h.add("Access-Control-Allow-Headers","Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, resp.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(resp);
        }
    }

    // -------------------------
    // Handlers
    // -------------------------
    static class RegisterHandler implements HttpHandler {
        private Bank bank;
        RegisterHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String username = f.get("username");
            String password = f.get("password");
            String name = f.getOrDefault("name", username);
            if (username==null || password==null) {
                sendJson(ex,400, "{\"ok\":false,\"error\":\"username & password required\"}");
                return;
            }
            synchronized(bank) {
                if (bank.getUser(username) != null) {
                    sendJson(ex,409, "{\"ok\":false,\"error\":\"username exists\"}");
                    return;
                }
                User u = new User(username, password, name);
                bank.addUser(u);
                // create default savings account
                SavingsAccount sa = new SavingsAccount(u.generateAccountNo(), u.getName(), 0.0);
                u.addAccount(sa);
                sendJson(ex,200, "{\"ok\":true,\"msg\":\"registered\",\"defaultAccount\":\""+sa.getAccountNo()+"\"}");
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        private Bank bank;
        LoginHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String username = f.get("username");
            String password = f.get("password");
            if (username==null || password==null) {
                sendJson(ex,400, "{\"ok\":false,\"error\":\"username & password required\"}");
                return;
            }
            User u = bank.authenticate(username,password);
            if (u==null) sendJson(ex,401, "{\"ok\":false,\"error\":\"invalid\"}");
            else sendJson(ex,200, "{\"ok\":true,\"token\":\""+u.getUsername()+"\",\"name\":\""+u.getName()+"\"}");
        }
    }

    static class AccountsHandler implements HttpHandler {
        private Bank bank;
        AccountsHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(null, ex.getRequestURI().getQuery());
            String t = f.get("token");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            StringBuilder sb = new StringBuilder();
            sb.append("{\"ok\":true,\"accounts\":[");
            boolean first=true;
            for (Account a : u.getAccounts()) {
                if (!first) sb.append(",");
                first=false;
                sb.append(a.toJson());
            }
            sb.append("]}");
            sendJson(ex,200,sb.toString());
        }
    }

    static class SavingsDepositHandler implements HttpHandler {
        private Bank bank;
        SavingsDepositHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String t = f.get("token"), acc = f.get("accountNo"), amtS = f.get("amount");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            Account a = u.getAccountByNo(acc);
            if (!(a instanceof SavingsAccount)) { sendJson(ex,400,"{\"ok\":false}"); return; }
            double amt = Double.parseDouble(amtS);
            ((SavingsAccount)a).deposit(amt);
            sendJson(ex,200,"{\"ok\":true,\"balance\":"+((SavingsAccount)a).getBalance()+"}");
        }
    }

    static class SavingsWithdrawHandler implements HttpHandler {
        private Bank bank;
        SavingsWithdrawHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String t = f.get("token"), acc = f.get("accountNo"), amtS = f.get("amount");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            Account a = u.getAccountByNo(acc);
            if (!(a instanceof SavingsAccount)) { sendJson(ex,400,"{\"ok\":false}"); return; }
            double amt = Double.parseDouble(amtS);
            boolean ok = ((SavingsAccount)a).withdraw(amt);
            sendJson(ex, ok ? 200 : 400, "{\"ok\":"+ok+",\"balance\":"+((SavingsAccount)a).getBalance()+"}");
        }
    }

    static class FDCreateHandler implements HttpHandler {
        private Bank bank;
        FDCreateHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String t = f.get("token"), amtS = f.get("principal"), monthsS = f.get("months"), rateS = f.get("rate");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            double principal = Double.parseDouble(amtS);
            int months = Integer.parseInt(monthsS);
            double rate = Double.parseDouble(rateS);
            FDAccount fd = new FDAccount(u.generateAccountNo(), u.getName(), principal, months, rate);
            u.addAccount(fd);
            sendJson(ex,200,"{\"ok\":true,\"accountNo\":\""+fd.getAccountNo()+"\",\"maturity\":"+fd.calculateMaturityAmount()+"}");
        }
    }

    static class LoanApplyHandler implements HttpHandler {
        private Bank bank;
        LoanApplyHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String t = f.get("token"), amtS = f.get("principal"), rateS = f.get("rate"), monthsS = f.get("months");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            double principal = Double.parseDouble(amtS);
            double rate = Double.parseDouble(rateS);
            int months = Integer.parseInt(monthsS);
            LoanAccount loan = new LoanAccount(u.generateAccountNo(), u.getName(), principal, rate, months);
            u.addAccount(loan);
            sendJson(ex,200,"{\"ok\":true,\"accountNo\":\""+loan.getAccountNo()+"\",\"outstanding\":"+loan.getOutstanding()+"}");
        }
    }

    static class CardIssueHandler implements HttpHandler {
        private Bank bank;
        CardIssueHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String t = f.get("token"), kind = f.get("type"), linked = f.get("linked"), limitS = f.get("limit");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            if ("debit".equalsIgnoreCase(kind)) {
                Account la = u.getAccountByNo(linked);
                if (!(la instanceof SavingsAccount)) { sendJson(ex,400,"{\"ok\":false}"); return; }
                DebitCard dc = new DebitCard(u.generateAccountNo(), u.getName(), (SavingsAccount)la);
                u.addAccount(dc);
                sendJson(ex,200,"{\"ok\":true,\"accountNo\":\""+dc.getAccountNo()+"\"}");
                return;
            } else if ("credit".equalsIgnoreCase(kind)) {
                double limit = Double.parseDouble(limitS);
                CreditCard cc = new CreditCard(u.generateAccountNo(), u.getName(), limit);
                u.addAccount(cc);
                sendJson(ex,200,"{\"ok\":true,\"accountNo\":\""+cc.getAccountNo()+"\"}");
                return;
            } else {
                sendJson(ex,400,"{\"ok\":false}");
                return;
            }
        }
    }

    static class CardChargeHandler implements HttpHandler {
        private Bank bank;
        CardChargeHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String t = f.get("token"), accNo = f.get("accountNo"), amtS = f.get("amount");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            Account a = u.getAccountByNo(accNo);
            double amt = Double.parseDouble(amtS);
            if (a instanceof Card) {
                boolean ok = ((Card)a).charge(amt);
                sendJson(ex, ok ? 200 : 400, "{\"ok\":"+ok+"}");
            } else {
                sendJson(ex,400,"{\"ok\":false}");
            }
        }
    }

    static class CardPayHandler implements HttpHandler {
        private Bank bank;
        CardPayHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(ex.getRequestBody(), ex.getRequestURI().getQuery());
            String t = f.get("token"), accNo = f.get("accountNo"), amtS = f.get("amount");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            Account a = u.getAccountByNo(accNo);
            double amt = Double.parseDouble(amtS);
            if (a instanceof Card) {
                ((Card)a).pay(amt);
                sendJson(ex,200,"{\"ok\":true}");
            } else {
                sendJson(ex,400,"{\"ok\":false}");
            }
        }
    }

    static class CreditMinDueHandler implements HttpHandler {
        private Bank bank;
        CreditMinDueHandler(Bank b){ bank=b; }
        public void handle(HttpExchange ex) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex,204,"{}"); return; }
            Map<String,String> f = parseForm(null, ex.getRequestURI().getQuery());
            String t = f.get("token"), accNo = f.get("accountNo");
            User u = bank.getUser(t);
            if (u==null) { sendJson(ex,401,"{\"ok\":false}"); return; }
            Account a = u.getAccountByNo(accNo);
            if (a instanceof CreditCard) {
                CreditCard cc = (CreditCard)a;
                sendJson(ex,200,"{\"ok\":true,\"outstanding\":"+cc.getOutstanding()+",\"minDue\":"+cc.minDue()+"}");
            } else {
                sendJson(ex,400,"{\"ok\":false}");
            }
        }
    }

    // -------------------------
    // Domain model classes (Bank, User, Account, SavingsAccount, FDAccount, LoanAccount, Card, DebitCard, CreditCard)
    // Implemented using OOP concepts taught (constructors, encapsulation, inheritance, inner classes)
    // -------------------------
    static class Bank {
        private String name;
        private Map<String,User> users = new HashMap<>();
        public Bank(String n){ this.name=n; }
        public void addUser(User u){ users.put(u.getUsername(), u); }
        public User getUser(String username){ return users.get(username); }
        public User authenticate(String username, String password){
            User u = users.get(username);
            if (u!=null && u.getPassword().equals(password)) return u;
            return null;
        }
    }

    static class User {
        private String username, password, name;
        private List<Account> accounts = new ArrayList<>();
        private int counter = 1000;
        public User(String username, String password, String name) {
            this.username=username; this.password=password; this.name=name;
        }
        public String getUsername(){ return username; }
        public String getPassword(){ return password; }
        public String getName(){ return name; }
        public void addAccount(Account a){ accounts.add(a); }
        public List<Account> getAccounts(){ return accounts; }
        public Account getAccountByNo(String no){
            for (Account a : accounts) if (a.getAccountNo().equals(no)) return a;
            return null;
        }
        public String generateAccountNo(){
            counter++;
            String p = username.length()>=3 ? username.substring(0,3).toUpperCase() : username.toUpperCase();
            return p + "-" + counter;
        }
    }

    static abstract class Account {
        private String accountNo;
        private String holderName;
        protected List<Transaction> transactions = new ArrayList<>();
        public Account(String accountNo, String holderName) {
            this.accountNo = accountNo;
            this.holderName = holderName;
        }
        public String getAccountNo(){ return accountNo; }
        public String getHolderName(){ return holderName; }
        public void addTransaction(String desc, double amount){ transactions.add(new Transaction(desc, amount)); }
        public String toJson(){
            // minimal JSON for account metadata
            String t = this.getClass().getSimpleName();
            return "{\"accountNo\":\""+accountNo+"\",\"type\":\""+t+"\"}";
        }
        // inner transaction class
        static class Transaction {
            long ts; String desc; double amount;
            Transaction(String d, double a){ ts=System.currentTimeMillis(); desc=d; amount=a; }
        }
    }

    static class SavingsAccount extends Account {
        private double balance;
        public SavingsAccount(String no, String holderName, double initial) {
            super(no,holderName); this.balance=initial; addTransaction("opened", initial);
        }
        public synchronized void deposit(double amt) { if (amt<=0) return; balance += amt; addTransaction("deposit", amt); }
        public synchronized boolean withdraw(double amt) {
            if (amt<=0 || amt>balance) return false;
            balance -= amt; addTransaction("withdraw", -amt); return true;
        }
        public double getBalance(){ return balance; }
        public String toJson() {
            return "{\"accountNo\":\""+getAccountNo()+"\",\"type\":\"SavingsAccount\",\"balance\":"+getBalance()+"}";
        }
    }

    static class FDAccount extends Account {
        private double principal; private int termMonths; private double rate;
        public FDAccount(String no, String holderName, double principal, int months, double rate) {
            super(no,holderName); this.principal=principal; this.termMonths=months; this.rate=rate; addTransaction("fd_opened", principal);
        }
        public double calculateMaturityAmount(){
            double yrs = termMonths/12.0;
            return principal + principal*(rate/100.0)*yrs; // simple interest
        }
        public double getPrincipal(){ return principal; }
        public String toJson() {
            return "{\"accountNo\":\""+getAccountNo()+"\",\"type\":\"FDAccount\",\"principal\":"+getPrincipal()+"}";
        }
    }

    static class LoanAccount extends Account {
        private double principal; private double outstanding; private double rate; private int months;
        public LoanAccount(String no, String holder, double principal, double rate, int months) {
            super(no,holder); this.principal=principal; this.outstanding=principal; this.rate=rate; this.months=months; addTransaction("loan_granted", principal);
        }
        public synchronized void pay(double amt) { if (amt<=0) return; double p = Math.min(amountOrZero(amt), outstanding); outstanding -= p; addTransaction("loan_payment", -p); }
        private double amountOrZero(double a) { return a>0?a:0; }
        public double getOutstanding(){ return outstanding; }
        public String toJson() {
            return "{\"accountNo\":\""+getAccountNo()+"\",\"type\":\"LoanAccount\",\"outstanding\":"+getOutstanding()+"}";
        }
    }

    static abstract class Card extends Account {
        public Card(String no, String holder){ super(no,holder); }
        public abstract boolean charge(double amt);
        public abstract void pay(double amt);
    }

    static class DebitCard extends Card {
        private SavingsAccount linked;
        public DebitCard(String no, String holder, SavingsAccount linked) { super(no,holder); this.linked = linked; addTransaction("debit_issued",0); }
        public boolean charge(double amt) { boolean ok = linked.withdraw(amt); if (ok) addTransaction("card_withdraw",-amt); return ok; }
        public void pay(double amt) { if (amt<=0) return; linked.deposit(amt); addTransaction("card_deposit", amt); }
    }

    static class CreditCard extends Card {
        private double limit; private double outstanding;
        public CreditCard(String no, String holder, double limit){ super(no,holder); this.limit=limit; this.outstanding=0; addTransaction("credit_issued",0); }
        public synchronized boolean charge(double amt) {
            if (amt<=0) return false;
            if (outstanding + amt > limit) return false;
            outstanding += amt; addTransaction("card_charge", amt); return true;
        }
        public synchronized void pay(double amt) { if (amt<=0) return; double p = Math.min(amt, outstanding); outstanding -= p; addTransaction("card_pay", -p); }
        public double getOutstanding(){ return outstanding; }
        public double minDue(){ return Math.max(10.0, outstanding*0.10); }
        public String toJson() {
            return "{\"accountNo\":\""+getAccountNo()+"\",\"type\":\"CreditCard\",\"outstanding\":"+getOutstanding()+",\"limit\":"+limit+"}";
        }
    }
}
