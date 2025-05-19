package be.steby.CoreProject.bll.exceptions;

import lombok.Getter;

@Getter
public class CoreProjectException extends RuntimeException {
    /**
     * The {@code detailed message} or object associated with the {@link Exception}.
     */
    private final String message;

    /**
     * The {@code HTTP status code} associated with the {@link Exception}.
     */
    private final int status;

    /**
     * Constructs a new {@link CoreProjectException }with the specified {@code detail message}.
     * The {@code status code} is set to {@code 500 (Internal Server Error)} by default.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     */
    public CoreProjectException(String message) {
        super(message);
        this.message = message;
        this.status = 500;
    }


    /**
     * Constructs a new {@link CoreProjectException} with the specified {@code detail message} and {@code status code}.
     *
     * @param message the {@code detail message} (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param status  the {@code HTTP status code} (which is saved for later retrieval
     *                by the {@link #getStatus()} method).
     */
    public CoreProjectException(String message, int status){
        super(message);
        this.status = status;
        this.message = message;
    }


    /**
     * Returns a {@code string representation} of this {@code exception}.
     * The string includes the {@code class name}, {@code method name}, {@code file name}, {@code line number}, and {@code message}.
     *
     * @return a string representation of this exception.
     */
    @Override
    public String toString() {
        StackTraceElement element = this.getStackTrace()[0];
        return String.format("%s" + " thrown in " + "%s" + "() at " + "%s:%d"  + " with message: " + "%s" ,
                this.getClass().getSimpleName(),
                element.getMethodName(),
                element.getFileName(),
                element.getLineNumber(),
                this.getMessage());
    }
}

