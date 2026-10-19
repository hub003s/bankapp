package bank.model.DAO;

import bank.model.Account;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Runs the SQL for the accounts table. The Connection is passed in so the
// service class can group several steps into one database transaction.
public class AccountDAO {

    // Constructor: this class keeps no data, so the constructor has nothing to set up.
    public AccountDAO() {
    }

    public boolean exists(Connection con, String accountNumber) throws SQLException {
        String sql = "SELECT 1 FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insert(Connection con, String accountNumber, String accountName,
                       BigDecimal balance) throws SQLException {
        String sql = "INSERT INTO accounts (account_number, account_name, balance) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            ps.setString(2, accountName);
            ps.setBigDecimal(3, balance);
            ps.executeUpdate();
        }
    }

    public Account findByNumber(Connection con, String accountNumber) throws SQLException {
        String sql = "SELECT id, account_number, account_name, balance FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(rs.getInt("id"), rs.getString("account_number"),
                            rs.getString("account_name"), rs.getBigDecimal("balance"));
                }
                return null;
            }
        }
    }

    // FOR UPDATE locks the row until commit/rollback, so two users
    // cannot change the same balance at the same time.
    public Account findForUpdate(Connection con, String accountNumber) throws SQLException {
        String sql = "SELECT id, account_number, account_name, balance FROM accounts "
                + "WHERE account_number = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Account(rs.getInt("id"), rs.getString("account_number"),
                            rs.getString("account_name"), rs.getBigDecimal("balance"));
                }
                return null;
            }
        }
    }

    public List<Account> findAll(Connection con) throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = "SELECT id, account_number, account_name, balance FROM accounts ORDER BY account_number";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Account(rs.getInt("id"), rs.getString("account_number"),
                        rs.getString("account_name"), rs.getBigDecimal("balance")));
            }
        }
        return list;
    }

    public void updateBalance(Connection con, String accountNumber, BigDecimal newBalance)
            throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE account_number = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setString(2, accountNumber);
            ps.executeUpdate();
        }
    }
}
