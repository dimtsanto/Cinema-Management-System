package gr.aegean.cinema.model.entity;

import gr.aegean.cinema.model.enums.ProgramRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_program_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "program_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProgramRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProgramRole role;
}
