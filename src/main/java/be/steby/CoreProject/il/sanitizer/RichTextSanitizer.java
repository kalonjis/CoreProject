package be.steby.CoreProject.il.sanitizer;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * HTML sanitizer for rich-text fields produced by TipTap on the frontend.
 *
 * <p>Uses Jsoup's allowlist-based approach: only the HTML elements and attributes
 * that TipTap's whitelisted extensions can legitimately produce are allowed through.
 * Everything else — including {@code <script>}, {@code javascript:} URLs,
 * {@code on*} event handlers, and any unknown tag — is stripped before the
 * content reaches the email template.</p>
 *
 * <h3>Allowed elements</h3>
 * <ul>
 *   <li>{@code <p>}, {@code <br>} — paragraphs and line breaks (StarterKit)</li>
 *   <li>{@code <strong>}, {@code <em>} — inline formatting (StarterKit)</li>
 *   <li>{@code <ul>}, {@code <ol>}, {@code <li>} — lists (StarterKit)</li>
 *   <li>{@code <a href>} — hyperlinks (extension-link), {@code https://} and
 *       {@code mailto:} schemes only; {@code javascript:} is automatically
 *       blocked by Jsoup's URL scheme enforcement</li>
 * </ul>
 *
 * <h3>Why a server-side sanitizer?</h3>
 * <p>TipTap's extension whitelist is client-side only and can be bypassed by a
 * crafted HTTP request. This sanitizer ensures the guarantee holds regardless
 * of what the client sends, without trusting the frontend exclusively.</p>
 *
 * <h3>Usage scope</h3>
 * <p>Designed for outbound email bodies (CRM outreach). Reusable for any domain
 * that stores or forwards rich-text HTML: support-ticket replies, internal notes, etc.</p>
 */
@Component
public class RichTextSanitizer {

    /**
     * Allowlist matching exactly what TipTap's configured extensions produce.
     * Protocols are restricted to {@code https} and {@code mailto} for {@code href}.
     */
    private static final Safelist CRM_SAFELIST = Safelist.none()
            .addTags("p", "br", "strong", "em", "ul", "ol", "li", "a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "https", "mailto");

    /**
     * Sanitizes an HTML string produced by TipTap, stripping any element or
     * attribute not in the CRM allowlist.
     *
     * <p>The output is safe to inject via {@code th:utext} into a Thymeleaf
     * email template. An empty or blank input returns an empty string.</p>
     *
     * @param html the raw HTML string from the client (may be {@code null})
     * @return sanitized HTML, never {@code null}
     */
    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, CRM_SAFELIST);
    }
}
