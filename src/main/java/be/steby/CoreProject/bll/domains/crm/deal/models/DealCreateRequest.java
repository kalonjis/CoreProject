package be.steby.CoreProject.bll.domains.crm.deal.models;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * BLL request model for creating a new {@link be.steby.CoreProject.dl.entities.crm.Deal}.
 *
 * <p>Used when a commercial encodes a deal directly. Pipeline, stage, contact,
 * and assigned commercial must all be provided by their public UUIDs — the
 * service layer resolves them to entity references before persisting.</p>
 *
 * <h3>Stage validation</h3>
 * <p>The service layer validates that {@code stagePublicId} belongs to the
 * pipeline identified by {@code pipelinePublicId}. Mismatched stage/pipeline
 * combinations are rejected with a {@code DealStageNotInPipelineException}.</p>
 *
 * <h3>Organisation</h3>
 * <p>{@code organisationPublicId} is optional — a deal can involve an
 * independent contact without a company.</p>
 *
 * @param title                 short descriptive title of the deal (required)
 * @param amount                estimated contract value (optional)
 * @param currency              ISO 4217 currency code, defaults to "EUR" if null (optional)
 * @param pipelinePublicId      public UUID of the pipeline (required)
 * @param stagePublicId         public UUID of the initial pipeline step (required)
 * @param contactPublicId       public UUID of the primary contact (required)
 * @param organisationPublicId  public UUID of the associated organisation (optional)
 * @param assignedToPublicId    public UUID of the assigned commercial (required)
 * @param expectedCloseDate     expected closing date for forecasting (optional)
 * @param notes                 internal notes for the commercial team (optional)
 */
public record DealCreateRequest(
        String title,
        BigDecimal amount,
        String currency,
        String pipelinePublicId,
        String stagePublicId,
        String contactPublicId,
        String organisationPublicId,
        String assignedToPublicId,
        LocalDate expectedCloseDate,
        String notes
) {}
