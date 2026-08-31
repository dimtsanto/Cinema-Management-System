package gr.aegean.cinema.model.entity;

import gr.aegean.cinema.model.enums.ScreeningState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "screenings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Screening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScreeningState state = ScreeningState.CREATED;

    // --- Program association ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    // --- Submitter ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id", nullable = false)
    private User submitter;

    // --- Handler (STAFF assigned during ASSIGNMENT phase) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handler_id")
    private User handler;

    // --- Film Information ---
    @Column(length = 300)
    private String filmTitle;

    @Column(length = 1000)
    private String filmCast;

    @Column(length = 300)
    private String filmGenre;

    /** Duration in minutes */
    private Integer filmDurationMinutes;

    // --- Auditorium ---
    @Column(length = 200)
    private String auditoriumName;

    // --- Scheduling ---
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // --- Review (filled during REVIEW phase) ---
    private Integer reviewScore;

    @Column(length = 2000)
    private String reviewComments;

    // --- Approval / Rejection ---
    @Column(length = 2000)
    private String approvalNotes;

    @Column(length = 2000)
    private String rejectionReason;

    /** True after final submission (FINAL_PUBLICATION phase) — details are frozen */
    @Builder.Default
    private boolean finallySubmitted = false;
}
