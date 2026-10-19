package bank;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Opens a JDBC connection (see Oracle's JDBC Basics tutorial: DriverManager.getConnection).
public class DBConnection {

    // XAMPP defaults: MySQL on port 3306, user "root", empty password.
    // Change these only if you set a password in XAMPP.
    private static final String URL =
            "jdbc:mysql://localhost:3306/banking_db?useSSL=false&serverTimezone=Asia/Manila";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Private constructor: this class only has a static method, so nobody should create an object of it.
    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}