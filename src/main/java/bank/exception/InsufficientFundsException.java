package bank.exception;

// Thrown when a withdrawal or transfer is more than the available balance.
public class InsufficientFundsException extends Exception {

    private static final long serialVersionUID = 1L;

    // Constructor: passes the message up to the parent class (Exception).
    public InsufficientFundsException(String message) {
        super(message);
    }
}