package be.steby.CoreProject.pl.domains.user.controllers;

import be.steby.CoreProject.dal.repositories.UserRepository;
import be.steby.CoreProject.dl.enums.UserRole;
import be.steby.CoreProject.pl.domains.user.models.responses.CommercialSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing user lookups needed by CRM operations.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/users</pre>
 *
 * <h3>Security</h3>
 * <p>Requires {@code COMMERCIAL} or {@code ADMIN} authority.</p>
 */
@RestController
@RequestMapping("/api/crm/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
@Slf4j
@Tag(name = "CRM - Users", description = "User lookups for CRM assignment")
public class CrmUserController {

    private final UserRepository userRepository;

    /**
     * Returns all active commercials for assignment dropdowns.
     *
     * <p><strong>Endpoint:</strong> GET /api/crm/users/commercials</p>
     *
     * @return list of active commercials (publicId, firstName, lastName, username)
     */
    @GetMapping("/commercials")
    @Operation(summary = "List commercials", description = "Returns all active users with COMMERCIAL role")
    public ResponseEntity<List<CommercialSummaryResponse>> getCommercials() {
        List<CommercialSummaryResponse> result = userRepository.findActiveByRole(UserRole.COMMERCIAL)
                .stream()
                .map(CommercialSummaryResponse::from)
                .toList();

        log.debug("CRM commercials list — {} results", result.size());
        return ResponseEntity.ok(result);
    }
}
