package be.steby.CoreProject.il.routes;

/**
 * Utility methods for security routes manipulation.
 * 
 * <p>This class provides helper methods for working with security route arrays,
 * primarily used by domain-specific security route configurations and the
 * central aggregator.
 * 
 * <p><b>Usage Example:</b>
 * <pre>
 * String[] allRoutes = SecurityRoutes.concatenate(
 *     AuthRoutes.PUBLIC,
 *     AccountRoutes.PUBLIC,
 *     PasswordRoutes.PUBLIC
 * );
 * </pre>
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public final class SecurityRoutes {
    
    /**
     * Concatenates multiple String arrays into a single array.
     * 
     * <p>This method efficiently combines multiple route arrays into one,
     * preserving the order of elements. It's used by the SecurityRoutesAggregator
     * to combine domain-specific routes.
     * 
     * <p><b>Performance:</b> Uses System.arraycopy for optimal performance.
     * 
     * @param arrays Variable number of String arrays to concatenate
     * @return A new array containing all elements from input arrays in order
     * @throws NullPointerException if arrays parameter is null
     * 
     * @example
     * <pre>
     * String[] auth = {"/api/auth/login", "/api/auth/logout"};
     * String[] account = {"/api/account/signup"};
     * String[] combined = SecurityRoutes.concatenate(auth, account);
     * // Result: ["/api/auth/login", "/api/auth/logout", "/api/account/signup"]
     * </pre>
     */
    public static String[] concatenate(String[]... arrays) {
        if (arrays == null) {
            throw new NullPointerException("Arrays parameter cannot be null");
        }
        
        // Calculate total length
        int totalLength = 0;
        for (String[] array : arrays) {
            if (array != null) {
                totalLength += array.length;
            }
        }
        
        // Create result array and copy elements
        String[] result = new String[totalLength];
        int currentIndex = 0;
        
        for (String[] array : arrays) {
            if (array != null && array.length > 0) {
                System.arraycopy(array, 0, result, currentIndex, array.length);
                currentIndex += array.length;
            }
        }
        
        return result;
    }
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private SecurityRoutes() {
        throw new UnsupportedOperationException("Utility class - cannot be instantiated");
    }
}