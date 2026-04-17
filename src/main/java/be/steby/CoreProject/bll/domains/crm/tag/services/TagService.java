package be.steby.CoreProject.bll.domains.crm.tag.services;

import be.steby.CoreProject.dl.entities.crm.Tag;
import be.steby.CoreProject.pl.domains.tag.models.requests.CreateTagRequest;
import be.steby.CoreProject.pl.domains.tag.models.requests.UpdateTagRequest;

import java.util.List;

/**
 * Service contract for managing CRM {@link Tag} entities.
 *
 * <p>Tags are cross-cutting labels that can be attached to contacts, deals, and organisations.
 * All association operations are bidirectional and persisted immediately.</p>
 */
public interface TagService {

    /**
     * Returns all tags ordered by name ascending.
     *
     * @return a list of all existing tags
     */
    List<Tag> findAll();

    /**
     * Creates a new tag from the given request.
     *
     * @param request the creation payload (name, optional color)
     * @return the persisted tag
     */
    Tag create(CreateTagRequest request);

    /**
     * Updates the name and/or color of an existing tag.
     *
     * @param publicId the public UUID of the tag to update
     * @param request  the update payload (nullable fields — only non-blank values are applied)
     * @return the updated tag
     * @throws be.steby.CoreProject.bll.domains.crm.tag.exceptions.TagNotFoundException if no tag exists for {@code publicId}
     */
    Tag update(String publicId, UpdateTagRequest request);

    /**
     * Deletes a tag and removes it from all associated contacts, deals, and organisations.
     *
     * @param publicId the public UUID of the tag to delete
     * @throws be.steby.CoreProject.bll.domains.crm.tag.exceptions.TagNotFoundException if no tag exists for {@code publicId}
     */
    void delete(String publicId);

    /**
     * Attaches a tag to a contact.
     *
     * @param tagPublicId     the public UUID of the tag
     * @param contactPublicId the public UUID of the contact
     */
    void addToContact(String tagPublicId, String contactPublicId);

    /**
     * Detaches a tag from a contact.
     *
     * @param tagPublicId     the public UUID of the tag
     * @param contactPublicId the public UUID of the contact
     */
    void removeFromContact(String tagPublicId, String contactPublicId);

    /**
     * Attaches a tag to a deal.
     *
     * @param tagPublicId  the public UUID of the tag
     * @param dealPublicId the public UUID of the deal
     */
    void addToDeal(String tagPublicId, String dealPublicId);

    /**
     * Detaches a tag from a deal.
     *
     * @param tagPublicId  the public UUID of the tag
     * @param dealPublicId the public UUID of the deal
     */
    void removeFromDeal(String tagPublicId, String dealPublicId);

    /**
     * Attaches a tag to an organisation.
     *
     * @param tagPublicId            the public UUID of the tag
     * @param organisationPublicId   the public UUID of the organisation
     */
    void addToOrganisation(String tagPublicId, String organisationPublicId);

    /**
     * Detaches a tag from an organisation.
     *
     * @param tagPublicId            the public UUID of the tag
     * @param organisationPublicId   the public UUID of the organisation
     */
    void removeFromOrganisation(String tagPublicId, String organisationPublicId);
}
