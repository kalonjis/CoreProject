package be.steby.CoreProject.dl.entities.crm;

import be.steby.CoreProject.dl.entities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * A free-form label that can be applied to {@link Contact}s and {@link Deal}s
 * to enable flexible segmentation without requiring custom fields.
 *
 * <p>Tags are global — a tag is created once and can be applied to any number
 * of contacts or deals. Renaming or deleting a tag updates all associations.</p>
 */
@Entity
@Table(
    name = "crm_tag",
    indexes = { @Index(name = "idx_tag_name", columnList = "name", unique = true) }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
public class Tag extends BaseEntity<Long> {

    /** Unique display name of the tag (e.g. "VIP", "Relance", "Grand compte"). */
    @Column(name = "name", nullable = false, unique = true, length = 50)
    @ToString.Include
    private String name;

    /** Hex color used to render the chip in the UI. Defaults to indigo. */
    @Column(name = "color", nullable = false, length = 7)
    @Builder.Default
    private String color = "#6366f1";
}
