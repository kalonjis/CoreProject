package be.steby.CoreProject.bll.domains.organisation.services;

import be.steby.CoreProject.bll.domains.device.services.DeviceService;
import be.steby.CoreProject.bll.domains.organisation.events.OrganisationCreatedEvent;
import be.steby.CoreProject.bll.domains.organisation.events.OrganisationMergedEvent;
import be.steby.CoreProject.bll.domains.organisation.events.OrganisationUpdatedEvent;
import be.steby.CoreProject.bll.domains.organisation.exceptions.OrganisationMergeException;
import be.steby.CoreProject.bll.domains.organisation.exceptions.OrganisationNameAlreadyExistsException;
import be.steby.CoreProject.bll.domains.organisation.exceptions.OrganisationNotFoundException;
import be.steby.CoreProject.bll.domains.organisation.models.OrganisationCreateRequest;
import be.steby.CoreProject.bll.domains.organisation.models.OrganisationFilterRequest;
import be.steby.CoreProject.bll.domains.organisation.models.OrganisationMergeRequest;
import be.steby.CoreProject.bll.domains.organisation.models.OrganisationUpdateRequest;
import be.steby.CoreProject.dal.repositories.AddressRepository;
import be.steby.CoreProject.dal.repositories.crm.ContactRepository;
import be.steby.CoreProject.dal.repositories.crm.OrganisationRepository;
import be.steby.CoreProject.dal.specifications.crm.OrganisationSpecification;
import be.steby.CoreProject.dl.entities.Address;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.dl.entities.crm.Contact;
import be.steby.CoreProject.dl.entities.crm.Organisation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link OrganisationService}.
 *
 * <h3>Name uniqueness</h3>
 * <p>Organisation names are enforced as unique (case-insensitive) using
 * {@code OrganisationRepository.existsByNameIgnoreCase}. Checked on
 * creation and on name change during update.</p>
 *
 * <h3>Merge strategy</h3>
 * <p>The merge operation reassigns all contacts from the source to the target
 * organisation, copies missing fields, then removes the source.
 * TODO: replace hard-delete with soft-archive once an {@code archived} boolean
 * field is added to the {@link Organisation} entity.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrganisationServiceImpl implements OrganisationService {

    private final OrganisationRepository organisationRepository;
    private final AddressRepository addressRepository;
    private final ContactRepository contactRepository;
    private final DeviceService deviceService;
    private final ApplicationEventPublisher eventPublisher;

    // =========================================================================
    // Lookup
    // =========================================================================

    @Override
    public Organisation getById(Long id) {
        return organisationRepository.findById(id)
                .orElseThrow(() -> OrganisationNotFoundException.byId(id));
    }

    @Override
    public Organisation getByPublicId(String publicId) {
        return organisationRepository.findByPublicId(publicId)
                .orElseThrow(() -> OrganisationNotFoundException.byPublicId(publicId));
    }

    @Override
    public Organisation getByName(String name) {
        return organisationRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> OrganisationNotFoundException.byName(name));
    }

    @Override
    public Page<Organisation> findAll(OrganisationFilterRequest filter, Pageable pageable) {
        Specification<Organisation> spec = OrganisationSpecification.nameContains(filter.keyword())
                .and(OrganisationSpecification.hasIndustry(filter.industry()))
                .and(OrganisationSpecification.hasSize(filter.size()))
                .and(OrganisationSpecification.inCountry(filter.countryCode()));

        return organisationRepository.findAll(spec, pageable);
    }

    // =========================================================================
    // Creation
    // =========================================================================

    @Override
    @Transactional
    public Organisation create(OrganisationCreateRequest request, User actor) {
        log.debug("Creating organisation — name: {}, by: {}", request.name(), actor.getUsername());

        if (organisationRepository.existsByNameIgnoreCase(request.name())) {
            throw OrganisationNameAlreadyExistsException.forName(request.name());
        }

        Address address = resolveAddress(request.addressPublicId());

        Organisation organisation = Organisation.builder()
                .name(request.name().trim())
                .website(request.website())
                .industry(request.industry())
                .size(request.size())
                .phone(request.phone())
                .address(address)
                .notes(request.notes())
                .build();

        Organisation saved = organisationRepository.save(organisation);
        log.info("Organisation created — publicId: {}, by: {}", saved.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new OrganisationCreatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Update
    // =========================================================================

    @Override
    @Transactional
    public Organisation update(String publicId, OrganisationUpdateRequest request, User actor) {
        log.debug("Updating organisation — publicId: {}, by: {}", publicId, actor.getUsername());

        Organisation organisation = getByPublicId(publicId);

        if (request.name() != null && !request.name().equalsIgnoreCase(organisation.getName())) {
            if (organisationRepository.existsByNameIgnoreCase(request.name())) {
                throw OrganisationNameAlreadyExistsException.onUpdate(request.name(), publicId);
            }
            organisation.setName(request.name().trim());
        }
        if (request.website()  != null) organisation.setWebsite(request.website());
        if (request.industry() != null) organisation.setIndustry(request.industry());
        if (request.size()     != null) organisation.setSize(request.size());
        if (request.phone()    != null) organisation.setPhone(request.phone());
        if (request.notes()    != null) organisation.setNotes(request.notes());
        if (request.addressPublicId() != null) {
            organisation.setAddress(resolveAddress(request.addressPublicId()));
        }

        Organisation saved = organisationRepository.save(organisation);
        log.info("Organisation updated — publicId: {}, by: {}", publicId, actor.getUsername());

        eventPublisher.publishEvent(new OrganisationUpdatedEvent(
                saved, actor, deviceService.detectAndRegisterDevice(actor)));
        return saved;
    }

    // =========================================================================
    // Merge
    // =========================================================================

    @Override
    @Transactional
    public Organisation merge(OrganisationMergeRequest request, User actor) {
        if (request.sourcePublicId().equals(request.targetPublicId())) {
            throw OrganisationMergeException.sameOrganisation(request.sourcePublicId());
        }

        log.debug("Merging organisation {} into {}, by: {}",
                request.sourcePublicId(), request.targetPublicId(), actor.getUsername());

        Organisation source = getByPublicId(request.sourcePublicId());
        Organisation target = getByPublicId(request.targetPublicId());

        // Copy non-null fields from source to target where target is blank
        if (target.getWebsite()  == null && source.getWebsite()  != null) target.setWebsite(source.getWebsite());
        if (target.getIndustry() == null && source.getIndustry() != null) target.setIndustry(source.getIndustry());
        if (target.getSize()     == null && source.getSize()     != null) target.setSize(source.getSize());
        if (target.getPhone()    == null && source.getPhone()    != null) target.setPhone(source.getPhone());
        if (target.getAddress()  == null && source.getAddress()  != null) target.setAddress(source.getAddress());
        if (target.getNotes()    == null && source.getNotes()    != null) target.setNotes(source.getNotes());

        // Reassign all contacts from source to target
        List<Contact> contacts = contactRepository.findByOrganisationId(source.getId());
        contacts.forEach(c -> c.setOrganisation(target));
        contactRepository.saveAll(contacts);
        log.debug("Reassigned {} contact(s) from organisation {} to {}",
                contacts.size(), source.getPublicId(), target.getPublicId());

        Organisation savedTarget = organisationRepository.save(target);

        // TODO: replace with soft-archive once Organisation entity has an `archived` boolean field
        organisationRepository.delete(source);

        log.info("Organisation merged — source {} removed, target {} updated, by: {}",
                source.getPublicId(), target.getPublicId(), actor.getUsername());

        eventPublisher.publishEvent(new OrganisationMergedEvent(
                savedTarget, source, actor, deviceService.detectAndRegisterDevice(actor)));
        return savedTarget;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Resolves an address entity from its public UUID.
     * Returns {@code null} if {@code publicId} is null (no address).
     *
     * @param addressPublicId the public UUID of the address, or {@code null}
     * @return the address entity, or {@code null}
     * @throws IllegalArgumentException if the publicId is provided but yields no result
     */
    private Address resolveAddress(String addressPublicId) {
        if (addressPublicId == null) return null;
        return addressRepository.findByPublicId(addressPublicId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Address not found with publicId: " + addressPublicId));
    }
}
