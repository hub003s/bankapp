package bank.model;

import java.math.BigDecimal;

/**
 * Represents one row of the "accounts" table.
 */
public class Account {

    private int id;
    private String accountNumber;
    private String accountName;
    private BigDecimal balance;

    // Constructor 1: used when we read an existing account from the database.
    public Account(int id, String accountNumber, String accountName, BigDecimal balance) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.balance = balance;
    }

    // Constructor 2 (overloaded constructor): used for a new account that has no id yet.
    // It calls Constructor 1 with an id of 0.
    public Account(String accountNumber, String accountName, BigDecimal balance) {
        this(0, accountNumber, accountName, balance);
    }

    public int getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountName() {
        return accountName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    // Overriding: replaces the toString() method that every class inherits from Object.
    @Override
    public String toString() {
        return accountNumber + " - " + accountName + " (Balance: " + balance + ")";
    }
}

