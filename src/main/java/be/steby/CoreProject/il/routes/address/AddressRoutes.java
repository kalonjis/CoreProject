package be.steby.CoreProject.il.routes.address;

/**
 * Security routes configuration for Address Management domain.
 * 
 * <p>This class defines security routes for user address management including
 * creating, reading, updating, and deleting addresses. Addresses can have different
 * types (RESIDENTIAL, BILLING, SHIPPING) and metadata (default, primary).
 * 
 * <p><b>Domain Responsibilities:</b>
 * <ul>
 *   <li>Address CRUD operations (Create, Read, Update, Delete)</li>
 *   <li>Address type management (RESIDENTIAL, BILLING, SHIPPING, OFFICE)</li>
 *   <li>Address metadata (default, primary, billing-eligible)</li>
 *   <li>Address search and filtering</li>
 * </ul>
 * 
 * <p><b>Address Types:</b>
 * <ul>
 *   <li><b>RESIDENTIAL:</b> Home address</li>
 *   <li><b>BILLING:</b> Billing/invoice address</li>
 *   <li><b>SHIPPING:</b> Delivery address</li>
 *   <li><b>OFFICE:</b> Work/business address</li>
 * </ul>
 * 
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li>All routes require authentication (no public routes)</li>
 *   <li>Users can only manage their own addresses</li>
 *   <li>All operations use cookies - CSRF protection REQUIRED</li>
 *   <li>Soft delete by default (permanent delete available)</li>
 * </ul>
 * 
 * @author Steby Core Team
 * @since 2025-01
 */
public final class AddressRoutes {
    
    // ========== PUBLIC ROUTES ==========
    
    /**
     * No public routes for address management.
     * 
     * <p>All address operations require authentication. Users must be logged in
     * to view or manage their addresses.
     */
    public static final String[] PUBLIC = {
        // No public routes - all address operations require authentication
    };
    
    // ========== AUTHENTICATED ROUTES ==========
    
    /**
     * Authenticated address routes for address management.
     * 
     * <p>These routes allow authenticated users to manage their personal addresses.
     * All use HttpOnly cookies for authentication and MUST have CSRF protection.
     * 
     * <p><b>CRUD Operations:</b>
     * <ul>
     *   <li>GET    /api/profile/addresses - Search/list addresses with query params</li>
     *   <li>POST   /api/profile/addresses - Create new address</li>
     *   <li>GET    /api/profile/addresses/* - Get specific address by publicId</li>
     *   <li>PUT    /api/profile/addresses/* - Update address by publicId</li>
     *   <li>DELETE /api/profile/addresses/* - Soft delete address by publicId</li>
     * </ul>
     * 
     * <p><b>Metadata Operations:</b>
     * <ul>
     *   <li>PATCH  /api/profile/addresses/* /default - Set address as default</li>
     *   <li>PATCH  /api/profile/addresses/* /primary - Set address as primary</li>
     *   <li>PATCH  /api/profile/addresses/* /type - Change address type</li>
     * </ul>
     * 
     * <p><b>Permanent Delete:</b>
     * <ul>
     *   <li>DELETE /api/profile/addresses/* /permanent - Permanently delete address</li>
     * </ul>
     * 
     * <p><b>Query Parameters (GET /addresses):</b>
     * <ul>
     *   <li>?type=BILLING - Filter by address type</li>
     *   <li>?billingEligible=true - Filter billing-eligible addresses</li>
     *   <li>?countryCode=BE - Filter by country</li>
     *   <li>?city=Brussels - Filter by city</li>
     *   <li>?isPrimary=true - Get primary address</li>
     *   <li>?label=Home - Search by label</li>
     *   <li>?active=true - Filter active/inactive</li>
     * </ul>
     * 
     * <p><b>Security:</b>
     * <ul>
     *   <li>✅ All routes use HttpOnly cookies - CSRF protection REQUIRED</li>
     *   <li>✅ Users can only access their own addresses</li>
     *   <li>✅ Soft delete by default (can be recovered)</li>
     *   <li>✅ Permanent delete requires explicit confirmation</li>
     *   <li>✅ Address data validated on creation/update</li>
     * </ul>
     * 
     * <p><b>Use Cases:</b>
     * <ul>
     *   <li>User adds shipping address for e-commerce</li>
     *   <li>User sets default billing address</li>
     *   <li>User updates address after moving</li>
     *   <li>User removes old address (soft delete)</li>
     *   <li>Admin permanently deletes address (GDPR compliance)</li>
     * </ul>
     */
    public static final String[] AUTHENTICATED = {
        "/api/profile/addresses",                    // GET search, POST create
        "/api/profile/addresses/*",                  // GET, PUT, DELETE by publicId
        "/api/profile/addresses/*/default",          // PATCH set as default
        "/api/profile/addresses/*/primary",          // PATCH set as primary
        "/api/profile/addresses/*/type",             // PATCH change type
        "/api/profile/addresses/*/permanent"         // DELETE permanent remove
    };
    
    // ========== CSRF CONFIGURATION ==========
    
    /**
     * Routes that bypass CSRF protection.
     * 
     * <p><b>⚠️ CRITICAL SECURITY WARNING:</b>
     * <ul>
     *   <li>❌ NO address routes should EVER bypass CSRF protection</li>
     *   <li>❌ All operations use HttpOnly cookies for authentication</li>
     *   <li>❌ All operations are state-changing (even GET can have side effects)</li>
     * </ul>
     * 
     * <p><b>Why Address Routes MUST NEVER Bypass CSRF:</b>
     * <ul>
     *   <li>All use cookie-based authentication</li>
     *   <li>Create/Update/Delete are state-changing operations</li>
     *   <li>Address data is sensitive (PII - Personally Identifiable Information)</li>
     *   <li>CSRF attack could:
     *     <ul>
     *       <li>Change user's billing address → financial fraud</li>
     *       <li>Add malicious addresses</li>
     *       <li>Delete legitimate addresses</li>
     *       <li>Modify shipping addresses → package theft</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * <p><b>⚠️ PRODUCTION CRITICAL:</b>
     * The old SecurityConstants has ALL address routes in CSRF_IGNORE for development.
     * This is an EXTREME SECURITY RISK and MUST be removed before production.
     * 
     * <p><b>Current Development Exception (REMOVE IN PROD):</b>
     * <pre>
     * // ⚠️ EXTREME SECURITY RISK - ALL ADDRESS ROUTES IN CSRF_IGNORE
     * // This allows CSRF attacks on address management
     * // MUST BE REMOVED BEFORE PRODUCTION
     * ADDRESS_AUTHENTICATED_ROUTES  // Currently in old SecurityConstants CSRF_IGNORE
     * </pre>
     * 
     * <p><b>Production Checklist:</b>
     * <ol>
     *   <li>✅ Verify NO address routes are in CSRF_IGNORE</li>
     *   <li>✅ Ensure CSRF protection is active for all address operations</li>
     *   <li>✅ Test address CRUD with CSRF tokens</li>
     *   <li>✅ Remove ADDRESS_AUTHENTICATED_ROUTES from SecurityRoutesAggregator.CSRF_IGNORE</li>
     * </ol>
     * 
     * <p><b>Correct Configuration:</b>
     * This array should ALWAYS be empty. No address routes bypass CSRF.
     */
    public static final String[] CSRF_IGNORE = {
        // ✅ CORRECT - Empty array
        // NO address routes should EVER bypass CSRF protection
        
        // ❌ NEVER ADD:
        // "/api/profile/addresses"           - CSRF REQUIRED
        // "/api/profile/addresses/*"         - CSRF REQUIRED
        // "/api/profile/addresses/*/default" - CSRF REQUIRED
        // "/api/profile/addresses/*/primary" - CSRF REQUIRED
        // "/api/profile/addresses/*/type"    - CSRF REQUIRED
        // "/api/profile/addresses/*/permanent" - CSRF REQUIRED
    };
    
    /**
     * Private constructor to prevent instantiation.
     * 
     * @throws UnsupportedOperationException if instantiation is attempted
     */
    private AddressRoutes() {
        throw new UnsupportedOperationException("Configuration class - cannot be instantiated");
    }
}