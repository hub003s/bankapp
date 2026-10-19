

package bank.service;

import bank.model.DAO.AccountDAO;
import bank.model.DAO.TransactionDAO;
import bank.DBConnection;
import bank.exception.InsufficientFundsException;
import bank.model.Account;
import bank.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Business rules and validation of the Banking Application.
 *
 * Every money operation (create, deposit, withdraw, transfer) is ONE database transaction:
 *   1. con.setAutoCommit(false)  - start a transaction
 *   2. do all the steps          - update balances, save transaction records
 *   3. con.commit()              - save everything
 *      con.rollback()            - if any step fails, undo everything
 */
public class BankService {

    private final AccountDAO accountDAO;
    private final TransactionDAO transactionDAO;

    // Constructor: creates the two DAO objects this service needs.
    public BankService() {
        this.accountDAO = new AccountDAO();
        this.transactionDAO = new TransactionDAO();
    }

    // =====================================================================
    // 1. CREATE ACCOUNT
    // =====================================================================

    // Overloading: same method name as below, but with only two parameters.
    // A new account without an initial deposit starts with a balance of zero.
    public Account createAccount(String accountNumber, String accountName) throws SQLException {
        return createAccount(accountNumber, accountName, BigDecimal.ZERO);
    }

    public Account createAccount(String accountNumber, String accountName, BigDecimal initialDeposit)
            throws SQLException {

        // Validation (before touching the database)
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number must not be empty.");
        }
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name must not be empty.");
        }
        if (initialDeposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial deposit must be zero or greater.");
        }

        accountNumber = accountNumber.trim();
        accountName = accountName.trim();

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (accountDAO.exists(con, accountNumber)) {
                    throw new IllegalArgumentException("Account number already exists.");
                }

                accountDAO.insert(con, accountNumber, accountName, initialDeposit);

                // The initial deposit is recorded as the first transaction of the account.
                if (initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
                    transactionDAO.insert(con, accountNumber, "DEPOSIT",
                            initialDeposit, initialDeposit, null);
                }

                con.commit();
                return accountDAO.findByNumber(con, accountNumber);

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // =====================================================================
    // 2. BALANCE INQUIRY
    // =====================================================================
    public Account getAccount(String accountNumber) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return findExistingAccount(con, accountNumber, "Account");
        }
    }

    // =====================================================================
    // 3. LIST ACCOUNTS
    // =====================================================================
    public List<Account> listAccounts() throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return accountDAO.findAll(con);
        }
    }

    // =====================================================================
    // 4. DEPOSIT
    // =====================================================================

    // Overloading: same method name, but the amount is a double instead of a BigDecimal.
    public BigDecimal deposit(String accountNumber, double amount) throws SQLException {
        return deposit(accountNumber, BigDecimal.valueOf(amount));
    }

    // Returns the PREVIOUS balance. The new balance is previous balance + amount.
    public BigDecimal deposit(String accountNumber, BigDecimal amount) throws SQLException {
        checkAmountIsPositive(amount);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Account account = findExistingAccountForUpdate(con, accountNumber, "Account");

                BigDecimal previousBalance = account.getBalance();
                BigDecimal newBalance = previousBalance.add(amount);

                accountDAO.updateBalance(con, account.getAccountNumber(), newBalance);
                transactionDAO.insert(con, account.getAccountNumber(), "DEPOSIT",
                        amount, newBalance, null);

                con.commit();
                return previousBalance;

            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // =====================================================================
    // 5. WITHDRAW
    // =====================================================================

    // Overloading: same method name, but the amount is a double.
    public BigDecimal withdraw(String accountNumber, double amount)
            throws SQLException, InsufficientFundsException {
        return withdraw(accountNumber, BigDecimal.valueOf(amount));
    }

    // Returns the PREVIOUS balance. The new balance is previous balance - amount.
    public BigDecimal withdraw(String accountNumber, BigDecimal amount)
            throws SQLException, InsufficientFundsException {
        checkAmountIsPositive(amount);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                Account account = findExistingAccountForUpdate(con, accountNumber, "Account");

                BigDecimal previousBalance = account.getBalance();
                checkEnoughBalance(previousBalance, amount);

                BigDecimal newBalance = previousBalance.subtract(amount);

                accountDAO.updateBalance(con, account.getAccountNumber(), newBalance);
                transactionDAO.insert(con, account.getAccountNumber(), "WITHDRAW",
                        amount, newBalance, null);

                con.commit();
                return previousBalance;

            } catch (SQLException | RuntimeException | InsufficientFundsException e) {
                con.rollback();
                throw e;
            }
        }
    }

    // =====================================================================
    // 6. TRANSFER
    // =====================================================================
    // Both balance updates and both transaction records succeed together, or none of them do.
    public void transfer(String fromNumber, String toNumber, BigDecimal amount)
            throws SQLException, InsufficientFundsException {

        fromNumber = fromNumber.trim();
        toNumber = toNumber.trim();

        if (fromNumber.equals(toNumber)) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }
        checkAmountIsPositive(amount);

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                // Always lock the accounts in the same order (smaller number first)
                // so two transfers at the same time cannot block each other forever.
                Account from;
                Account to;
                if (fromNumber.compareTo(toNumber) < 0) {
                    from = findExistingAccountForUpdate(con, fromNumber, "Source account");
                    to = findExistingAccountForUpdate(con, toNumber, "Destination account");
                } else {
                    to = findExistingAccountForUpdate(con, toNumber, "Destination account");
                    from = findExistingAccountForUpdate(con, fromNumber, "Source account");
                }

                checkEnoughBalance(from.getBalance(), amount);

                BigDecimal fromNewBalance = from.getBalance().subtract(amount);
                BigDecimal toNewBalance = to.getBalance().add(amount);

                // Step 1 and 2: update both balances
                accountDAO.updateBalance(con, fromNumber, fromNewBalance);
                accountDAO.updateBalance(con, toNumber, toNewBalance);

                // Step 3 and 4: save one transaction record for each account
                transactionDAO.insert(con, fromNumber, "TRANSFER", amount, fromNewBalance, toNumber);
                transactionDAO.insert(con, toNumber, "TRANSFER", amount, toNewBalance, fromNumber);

                con.commit();

            } catch (SQLException | RuntimeException | InsufficientFundsException e) {
                con.rollback();   // nothing is saved if any step above failed
                throw e;
            }
        }
    }

    // =====================================================================
    // 7. TRANSACTION HISTORY
    // =====================================================================
    public List<Transaction> getHistory(String accountNumber) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            Account account = findExistingAccount(con, accountNumber, "Account");
            return transactionDAO.findByAccount(con, account.getAccountNumber());
        }
    }

    // =====================================================================
    // HELPER METHODS (small methods that remove repeated code)
    // =====================================================================

    // Finds an account. If it does not exist, stops with a clear error message.
    private Account findExistingAccount(Connection con, String accountNumber, String label)
            throws SQLException {
        Account account = accountDAO.findByNumber(con, accountNumber.trim());
        if (account == null) {
            throw new IllegalArgumentException(label + " " + accountNumber.trim() + " does not exist.");
        }
        return account;
    }

    // Same as above, but also locks the row so nobody else can change it until commit/rollback.
    private Account findExistingAccountForUpdate(Connection con, String accountNumber, String label)
            throws SQLException {
        Account account = accountDAO.findForUpdate(con, accountNumber.trim());
        if (account == null) {
            throw new IllegalArgumentException(label + " " + accountNumber.trim() + " does not exist.");
        }
        return account;
    }

    // Rule: the amount of a deposit, withdrawal or transfer must be greater than zero.
    private void checkAmountIsPositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }

    // Rule: a withdrawal or transfer must not be more than the available balance.
    private void checkEnoughBalance(BigDecimal balance, BigDecimal amount)
            throws InsufficientFundsException {
        if (amount.compareTo(balance) > 0) {
            throw new InsufficientFundsException("Insufficient balance. Available: " + balance);
        }
    }
}