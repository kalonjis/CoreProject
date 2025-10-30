package be.steby.CoreProject.bll.domains.auth.exceptions;

/**
 * Exception thrown when an unsupported or invalid OAuth2 provider is requested.
 * Returns HTTP 400 (Bad Request) as it indicates a client error with invalid input.
 */
public class InvalidOAuth2ProviderException extends AuthenticationException {

    /**
     * Creates a new exception for an invalid OAuth2 provider.
     *
     * @param message The error message describing the invalid provider
     */
    public InvalidOAuth2ProviderException(String message) {
        super(message, 400);
    }

    /**
     * Factory method for unsupported provider.
     *
     * @param provider The unsupported provider name
     * @return A new exception instance
     */
    public static InvalidOAuth2ProviderException unsupportedProvider(String provider) {
        return new InvalidOAuth2ProviderException(
            "Unsupported OAuth2 provider: " + provider + ". Supported providers: GITHUB, GOOGLE, MICROSOFT"
        );
    }
}