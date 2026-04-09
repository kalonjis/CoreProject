package be.steby.CoreProject.bll.domains.lead.models;

import be.steby.CoreProject.dl.entities.crm.Lead;

/**
 * BLL read model for the lead detail view.
 *
 * <p>Wraps the lead entity with optional deduplication context: if a Contact
 * with the same email already exists in the CRM, its public UUID is provided
 * so the frontend can disable the "Convert" action and show a direct link.</p>
 *
 * @param lead                    the lead entity
 * @param existingContactPublicId public UUID of an existing Contact sharing this
 *                                lead's email address, or {@code null} if none
 */
public record LeadDetailModel(Lead lead, String existingContactPublicId) {}
