package be.steby.CoreProject.dl.enums;

/**
 * Enum representing supported OAuth2 providers.
 */
public enum OAuthProvider {
    
    GITHUB,
    GOOGLE,
    MICROSOFT;
    
    /**
     * Returns a user-friendly description of the provider.
     *
     * @return the provider description
     */
    public String getDescription() {
        return switch (this) {
            case GITHUB -> "GitHub";
            case GOOGLE -> "Google";
            case MICROSOFT -> "Microsoft";
        };
    }
}