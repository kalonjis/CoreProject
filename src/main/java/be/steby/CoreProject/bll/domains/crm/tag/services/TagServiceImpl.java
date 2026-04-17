package be.steby.CoreProject.bll.domains.crm.tag.services;

import be.steby.CoreProject.bll.domains.crm.tag.exceptions.TagNotFoundException;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.DealRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dal.repositories.crm.TagRepository;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Deal;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import be.steby.CoreProject.dl.entities.crm.Tag;
import be.steby.CoreProject.pl.domains.tag.models.requests.CreateTagRequest;
import be.steby.CoreProject.pl.domains.tag.models.requests.UpdateTagRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default implementation of {@link TagService}.
 *
 * <p>Manages the full lifecycle of {@link be.steby.CoreProject.dl.entities.crm.Tag} entities,
 * including creation, update, deletion, and association with contacts, deals, and organisations.
 * Deletion cascades to all join tables via native bulk-delete queries before removing the tag itself.</p>
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository          tagRepository;
    private final ContactRepository      contactRepository;
    private final DealRepository         dealRepository;
    private final OrganisationRepository organisationRepository;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<Tag> findAll() {
        return tagRepository.findAllByOrderByNameAsc();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Tag create(CreateTagRequest request) {
        Tag tag = Tag.builder()
                .name(request.name().trim())
                .color(request.color() != null ? request.color() : "#6366f1")
                .build();
        return tagRepository.save(tag);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Tag update(String publicId, UpdateTagRequest request) {
        Tag tag = findByPublicId(publicId);
        if (request.name()  != null && !request.name().isBlank())  tag.setName(request.name().trim());
        if (request.color() != null && !request.color().isBlank()) tag.setColor(request.color());
        return tagRepository.save(tag);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(String publicId) {
        Tag tag = findByPublicId(publicId);
        tagRepository.removeFromAllContacts(tag.getId());
        tagRepository.removeFromAllDeals(tag.getId());
        tagRepository.removeFromAllOrganisations(tag.getId());
        tagRepository.delete(tag);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void addToContact(String tagPublicId, String contactPublicId) {
        Tag     tag     = findByPublicId(tagPublicId);
        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactPublicId));
        contact.getTags().add(tag);
        contactRepository.save(contact);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void removeFromContact(String tagPublicId, String contactPublicId) {
        Tag     tag     = findByPublicId(tagPublicId);
        Contact contact = contactRepository.findByPublicId(contactPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactPublicId));
        contact.getTags().remove(tag);
        contactRepository.save(contact);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void addToDeal(String tagPublicId, String dealPublicId) {
        Tag  tag  = findByPublicId(tagPublicId);
        Deal deal = dealRepository.findByPublicId(dealPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealPublicId));
        deal.getTags().add(tag);
        dealRepository.save(deal);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void removeFromDeal(String tagPublicId, String dealPublicId) {
        Tag  tag  = findByPublicId(tagPublicId);
        Deal deal = dealRepository.findByPublicId(dealPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Deal not found: " + dealPublicId));
        deal.getTags().remove(tag);
        dealRepository.save(deal);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void addToOrganisation(String tagPublicId, String organisationPublicId) {
        Tag          tag  = findByPublicId(tagPublicId);
        Organisation org  = organisationRepository.findByPublicId(organisationPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + organisationPublicId));
        org.getTags().add(tag);
        organisationRepository.save(org);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void removeFromOrganisation(String tagPublicId, String organisationPublicId) {
        Tag          tag  = findByPublicId(tagPublicId);
        Organisation org  = organisationRepository.findByPublicId(organisationPublicId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + organisationPublicId));
        org.getTags().remove(tag);
        organisationRepository.save(org);
    }

    /**
     * Resolves a {@link Tag} by its public UUID or throws {@link TagNotFoundException}.
     *
     * @param publicId the public UUID of the tag
     * @return the matching tag entity
     * @throws TagNotFoundException if no tag exists for the given {@code publicId}
     */
    private Tag findByPublicId(String publicId) {
        return tagRepository.findByPublicId(publicId)
                .orElseThrow(() -> new TagNotFoundException(publicId));
    }
}
