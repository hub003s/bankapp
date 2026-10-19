package bank.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Represents one row of the "transactions" table.
 */
public class Transaction {

    private String reference;          // example: TXN-001
    private String accountNumber;
    private String type;               // DEPOSIT, WITHDRAW or TRANSFER
    private BigDecimal amount;
    private BigDecimal balanceAfter;   // balance of the account after this transaction
    private String referenceAccount;   // the other account in a transfer (null if none)
    private Timestamp createdAt;

    public Transaction(String reference, String accountNumber, String type,
                       BigDecimal amount, BigDecimal balanceAfter,
                       String referenceAccount, Timestamp createdAt) {
        this.reference = reference;
        this.accountNumber = accountNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.referenceAccount = referenceAccount;
        this.createdAt = createdAt;
    }

    public String getReference() {
        return reference;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getReferenceAccount() {
        return referenceAccount;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    // Overriding: replaces the toString() method inherited from Object.
    @Override
    public String toString() {
        return reference + " " + type + " " + amount + " (balance: " + balanceAfter + ")";
    }
}