package bank;


import bank.exception.InsufficientFundsException;
import bank.model.Account;
import bank.model.Transaction;
import bank.service.BankService;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final BankService service = new BankService();
    private static final Scanner scanner = new Scanner(System.in, "UTF-8");

    public static void main(String[] args) throws Exception {
        // Needed so the peso sign (₱) prints correctly in the console.
        System.setOut(new PrintStream(System.out, true, "UTF-8"));

        int choice = -1;
        while (choice != 0) {
            showMenu();
            String input = scanner.nextLine().trim();
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number from the menu.\n");
                continue;
            }

            switch (choice) {
                case 1: createAccount(); break;
                case 2: balanceInquiry(); break;
                case 3: listAccounts(); break;
                case 4: deposit(); break;
                case 5: withdraw(); break;
                case 6: transfer(); break;
                case 7: transactionHistory(); break;
                case 0: System.out.println("Thank you for using the Banking Application."); break;
                default: System.out.println("Invalid choice.\n");
            }
            waitForEnter();
        }
    }

    private static void showMenu() {
        System.out.println("====================================");
        System.out.println("BANKING APPLICATION");
        System.out.println("====================================");
        System.out.println("1. Create Account");
        System.out.println("2. Balance Inquiry");
        System.out.println("3. List Accounts");
        System.out.println("4. Deposit");
        System.out.println("5. Withdraw");
        System.out.println("6. Transfer");
        System.out.println("7. Transaction History");
        System.out.println("0. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void createAccount() {
        try {
            System.out.print("Account Number: ");
            String number = scanner.nextLine();
            System.out.print("Account Name: ");
            String name = scanner.nextLine();
            BigDecimal deposit = readAmount("Initial Deposit: ");

            Account a = service.createAccount(number, name, deposit);
            System.out.println();
            System.out.println("Account Number: " + a.getAccountNumber());
            System.out.println("Account Name: " + a.getAccountName());
            System.out.println("Initial Deposit: " + peso(a.getBalance()));
            System.out.println("Account created successfully!\n");
        } catch (Exception e) {
            showError(e);
        }
    }

    private static void balanceInquiry() {
        try {
            System.out.print("Account Number: ");
            Account a = service.getAccount(scanner.nextLine());
            System.out.println();
            System.out.println("Account Number: " + a.getAccountNumber());
            System.out.println("Account Holder: " + a.getAccountName());
            System.out.println("Current Balance: " + peso(a.getBalance()) + "\n");
        } catch (Exception e) {
            showError(e);
        }
    }

    private static void listAccounts() {
        try {
            List<Account> accounts = service.listAccounts();
            System.out.println();
            for (Account a : accounts) {
                System.out.printf("%-10s %-25s %s%n", a.getAccountNumber(),
                        a.getAccountName(), peso(a.getBalance()));
            }
            System.out.println("Total Accounts: " + accounts.size() + "\n");
        } catch (Exception e) {
            showError(e);
        }
    }

    private static void deposit() {
        try {
            System.out.print("Account Number: ");
            String number = scanner.nextLine();
            BigDecimal amount = readAmount("Deposit Amount: ");

            BigDecimal previous = service.deposit(number, amount);
            System.out.println();
            System.out.println("Previous Balance: " + peso(previous));
            System.out.println("Deposit Amount: " + peso(amount));
            System.out.println("New Balance: " + peso(previous.add(amount)));
            System.out.println("Deposit successful!\n");
        } catch (Exception e) {
            showError(e);
        }
    }

    private static void withdraw() {
        try {
            System.out.print("Account Number: ");
            String number = scanner.nextLine();
            BigDecimal amount = readAmount("Withdrawal Amount: ");

            BigDecimal previous = service.withdraw(number, amount);
            System.out.println();
            System.out.println("Previous Balance: " + peso(previous));
            System.out.println("Withdrawal Amount: " + peso(amount));
            System.out.println("New Balance: " + peso(previous.subtract(amount)));
            System.out.println("Withdrawal successful!\n");
        } catch (Exception e) {
            showError(e);
        }
    }

    private static void transfer() {
        try {
            System.out.print("From Account: ");
            String from = scanner.nextLine();
            System.out.print("To Account: ");
            String to = scanner.nextLine();
            BigDecimal amount = readAmount("Amount: ");

            service.transfer(from, to, amount);
            System.out.println();
            System.out.println("From Account: " + from.trim());
            System.out.println("To Account: " + to.trim());
            System.out.println("Amount: " + peso(amount));
            System.out.println("Transfer successful!\n");
        } catch (Exception e) {
            showError(e);
        }
    }

    private static void transactionHistory() {
        try {
            System.out.print("Account Number: ");
            String number = scanner.nextLine();
            List<Transaction> list = service.getHistory(number);
            System.out.println();
            System.out.println(
                    addSpaces("REF", 12) +
                            addSpaces("TYPE", 12) +
                            addSpaces("AMOUNT", 16) +
                            addSpaces("DATE", 22) +
                            addSpaces("REF ACCT", 14) +
                            "BALANCE"
            );

            System.out.println("----------------------------------------------------------------------------------------");

            if (list.isEmpty()) {
                System.out.println("No transactions found.");
            } else {
                for (Transaction t : list) {

                    String ref = t.getReferenceAccount();

                    if (ref == null) {
                        ref = "-";
                    }

                    String date = t.getCreatedAt().toString()
                            .replace("T", " ")
                            .substring(0, 19);

                    System.out.println(
                            addSpaces(t.getReference(), 12) +
                                    addSpaces(t.getType(), 12) +
                                    addSpaces(peso(t.getAmount()), 16) +
                                    addSpaces(date, 22) +
                                    addSpaces(ref, 14) +
                                    peso(t.getBalanceAfter())
                    );
                }
            }

            System.out.println();

        } catch (Exception e) {
            showError(e);
        }
    }

    private static String addSpaces(String text, int length) {
        while (text.length() < length) {
            text = text + " ";
        }

        return text;
    }
    // Reads a money value; throws NumberFormatException if it is not a number.
    private static BigDecimal readAmount(String prompt) {
        System.out.print(prompt);
        return new BigDecimal(scanner.nextLine().trim().replace(",", ""));
    }

    private static String peso(BigDecimal value) {
        return "₱" + String.format("%,.2f", value);
    }

    private static void showError(Exception e) {
        if (e instanceof NumberFormatException) {
            System.out.println("Error: Please enter a valid number.\n");
        } else if (e instanceof IllegalArgumentException || e instanceof InsufficientFundsException) {
            System.out.println("Error: " + e.getMessage() + "\n");
        } else if (e instanceof SQLException) {
            System.out.println("Database error: " + e.getMessage() + "\n");
        } else {
            System.out.println("Unexpected error: " + e.getMessage() + "\n");
        }
    }
    private static void waitForEnter() {
        System.out.println("\nPress Enter to return to main menu...");
        scanner.nextLine();
    }
}