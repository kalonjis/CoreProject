package be.steby.CoreProject.dl.enums.crm;

/**
 * Role of a {@link be.steby.CoreProject.dl.entities.crm.Contact} on a
 * {@link be.steby.CoreProject.dl.entities.crm.Deal}.
 *
 * <p>A deal can involve several contacts with distinct responsibilities.
 * Exactly one contact per deal must carry the {@code isPrimary} flag
 * (managed on {@code DealContactRole}); the role describes <em>why</em>
 * that person is involved.</p>
 */
public enum ContactRole {

    /** Final decision-maker — signs or approves the contract. */
    DECISION_MAKER,

    /** Influences the decision without having final authority. */
    INFLUENCER,

    /** Legally signs the contract (may differ from the decision-maker). */
    SIGNER,

    /** Technical evaluator or implementer. */
    TECHNICAL,

    /** End-user of the product or service. */
    USER,

    /** Any other involvement not covered by the values above. */
    OTHER
}
