package be.steby.CoreProject.dl.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@MappedSuperclass
@Getter
@EntityListeners(AuditingEntityListener.class)
public class BaseEntity<T extends Serializable> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private T id;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    @Setter
    private Instant createdAt;

    @LastModifiedBy
    @Column(insertable = false)
    private String updatedBy;

    @Column(nullable = false)
    @UpdateTimestamp
    @Setter
    private Instant updatedAt;
}
