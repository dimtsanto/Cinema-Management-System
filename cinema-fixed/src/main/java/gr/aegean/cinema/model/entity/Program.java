package gr.aegean.cinema.model.entity;

import gr.aegean.cinema.model.enums.ProgramState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "programs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProgramState state = ProgramState.CREATED;

    // Creator (always remains a PROGRAMMER)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    // All screenings belonging to this program
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Screening> screenings = new ArrayList<>();

    // All user-role assignments for this program
    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProgramRole> userRoles = new ArrayList<>();
}
