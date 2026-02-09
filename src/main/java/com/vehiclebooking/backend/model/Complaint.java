package com.vehiclebooking.backend.model;

import com.vehiclebooking.backend.model.enums.ComplaintStatus;
import com.vehiclebooking.backend.model.enums.ComplaintType;
import com.vehiclebooking.backend.model.enums.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "complaints")
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "complainant_id", nullable = false)
    private User complainant;

    @Enumerated(EnumType.STRING)
    private ComplaintType type; // USER_COMPLAINT, DRIVER_COMPLAINT

    private String issue;

    @Enumerated(EnumType.STRING)
    private Severity severity; // LOW, MEDIUM, HIGH

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status; // OPEN, RESOLVED

    private String resolutionNotes;

    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = ComplaintStatus.OPEN;
        if (severity == null)
            severity = Severity.MEDIUM;
    }
}
