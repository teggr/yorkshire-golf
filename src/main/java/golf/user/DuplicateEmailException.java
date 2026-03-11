package golf.user;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email already exists: " + email);
    }

    public DuplicateEmailException(String email, Throwable cause) {
        super("An account with email already exists: " + email, cause);
    }
}