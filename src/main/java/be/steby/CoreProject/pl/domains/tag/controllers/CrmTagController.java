package be.steby.CoreProject.pl.domains.tag.controllers;

import be.steby.CoreProject.bll.domains.crm.tag.services.TagService;
import be.steby.CoreProject.pl.domains.tag.models.requests.CreateTagRequest;
import be.steby.CoreProject.pl.domains.tag.models.requests.UpdateTagRequest;
import be.steby.CoreProject.pl.domains.tag.models.responses.TagResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * REST controller for global CRM tag management.
 *
 * <h3>Base path</h3>
 * <pre>/api/crm/tags</pre>
 */
@RestController
@RequestMapping("/api/crm/tags")
@RequiredArgsConstructor
@Tag(name = "CRM - Tags", description = "Global tag management and assignment to contacts/deals")
public class CrmTagController {

    private final TagService tagService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "List all tags")
    public ResponseEntity<List<TagResponse>> findAll() {
        return ResponseEntity.ok(
                tagService.findAll().stream().map(TagResponse::fromEntity).toList()
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Create a tag")
    public ResponseEntity<TagResponse> create(@RequestBody @Valid CreateTagRequest request) {
        var tag = tagService.create(request);
        return ResponseEntity.created(URI.create("/api/crm/tags/" + tag.getPublicId()))
                .body(TagResponse.fromEntity(tag));
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Update a tag (rename or recolor)")
    public ResponseEntity<TagResponse> update(@PathVariable String publicId,
                                              @RequestBody @Valid UpdateTagRequest request) {
        return ResponseEntity.ok(TagResponse.fromEntity(tagService.update(publicId, request)));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Delete a tag globally (removes from all contacts and deals)")
    public ResponseEntity<Void> delete(@PathVariable String publicId) {
        tagService.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // ─── Contact associations ────────────────────────────────────────────────

    @PostMapping("/{tagPublicId}/contacts/{contactPublicId}")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Add a tag to a contact")
    public ResponseEntity<Void> addToContact(@PathVariable String tagPublicId,
                                             @PathVariable String contactPublicId) {
        tagService.addToContact(tagPublicId, contactPublicId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tagPublicId}/contacts/{contactPublicId}")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Remove a tag from a contact")
    public ResponseEntity<Void> removeFromContact(@PathVariable String tagPublicId,
                                                  @PathVariable String contactPublicId) {
        tagService.removeFromContact(tagPublicId, contactPublicId);
        return ResponseEntity.noContent().build();
    }

    // ─── Deal associations ───────────────────────────────────────────────────

    @PostMapping("/{tagPublicId}/deals/{dealPublicId}")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Add a tag to a deal")
    public ResponseEntity<Void> addToDeal(@PathVariable String tagPublicId,
                                          @PathVariable String dealPublicId) {
        tagService.addToDeal(tagPublicId, dealPublicId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tagPublicId}/deals/{dealPublicId}")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Remove a tag from a deal")
    public ResponseEntity<Void> removeFromDeal(@PathVariable String tagPublicId,
                                               @PathVariable String dealPublicId) {
        tagService.removeFromDeal(tagPublicId, dealPublicId);
        return ResponseEntity.noContent().build();
    }

    // ─── Organisation associations ───────────────────────────────────────────

    @PostMapping("/{tagPublicId}/organisations/{organisationPublicId}")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Add a tag to an organisation")
    public ResponseEntity<Void> addToOrganisation(@PathVariable String tagPublicId,
                                                  @PathVariable String organisationPublicId) {
        tagService.addToOrganisation(tagPublicId, organisationPublicId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{tagPublicId}/organisations/{organisationPublicId}")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL', 'ADMIN')")
    @Operation(summary = "Remove a tag from an organisation")
    public ResponseEntity<Void> removeFromOrganisation(@PathVariable String tagPublicId,
                                                       @PathVariable String organisationPublicId) {
        tagService.removeFromOrganisation(tagPublicId, organisationPublicId);
        return ResponseEntity.noContent().build();
    }
}
