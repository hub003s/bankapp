package bank.model.DAO;

import bank.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Runs the SQL for the transactions table.
public class TransactionDAO {

    // Constructor: this class keeps no data, so the constructor has nothing to set up.
    public TransactionDAO() {
    }

    // Builds the next reference: TXN-001, TXN-002, ...
    private String nextReference(Connection con) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return String.format("TXN-%03d", rs.getInt(1) + 1);
        }
    }

    public void insert(Connection con, String accountNumber, String type, BigDecimal amount,
                       BigDecimal balanceAfter, String referenceAccount) throws SQLException {
        String reference = nextReference(con);
        String sql = "INSERT INTO transactions (transaction_reference, account_number, "
                + "transaction_type, amount, balance, reference_account) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reference);
            ps.setString(2, accountNumber);
            ps.setString(3, type);
            ps.setBigDecimal(4, amount);
            ps.setBigDecimal(5, balanceAfter);
            ps.setString(6, referenceAccount);
            ps.executeUpdate();
        }
    }

    public List<Transaction> findByAccount(Connection con, String accountNumber)
            throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT transaction_reference, account_number, transaction_type, amount, "
                + "balance, reference_account, created_at FROM transactions "
                + "WHERE account_number = ? ORDER BY id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Transaction(
                            rs.getString("transaction_reference"),
                            rs.getString("account_number"),
                            rs.getString("transaction_type"),
                            rs.getBigDecimal("amount"),
                            rs.getBigDecimal("balance"),
                            rs.getString("reference_account"),
                            rs.getTimestamp("created_at")));
                }
            }
        }
        return list;
    }
}
