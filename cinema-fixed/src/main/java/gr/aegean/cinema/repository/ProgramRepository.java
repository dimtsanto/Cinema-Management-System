package gr.aegean.cinema.repository;

import gr.aegean.cinema.model.entity.Program;
import gr.aegean.cinema.model.enums.ProgramState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long>,
        JpaSpecificationExecutor<Program> {

    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    Optional<Program> findByName(String name);

    List<Program> findByState(ProgramState state);

    /**
     * Search programs with optional AND filters.
     * Null parameters are ignored (treated as "match all").
     */
    @Query("""
            SELECT DISTINCT p FROM Program p
            WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%',:name,'%')))
              AND (:description IS NULL OR LOWER(p.description) LIKE LOWER(CONCAT('%',:description,'%')))
              AND (:startDate IS NULL OR p.startDate >= :startDate)
              AND (:endDate IS NULL OR p.endDate <= :endDate)
              AND (:filmTitle IS NULL OR EXISTS (
                    SELECT s FROM Screening s WHERE s.program = p
                    AND LOWER(s.filmTitle) LIKE LOWER(CONCAT('%',:filmTitle,'%'))))
              AND (:auditorium IS NULL OR EXISTS (
                    SELECT s FROM Screening s WHERE s.program = p
                    AND LOWER(s.auditoriumName) LIKE LOWER(CONCAT('%',:auditorium,'%'))))
            ORDER BY p.startDate ASC, p.name ASC
            """)
    List<Program> searchPrograms(
            @Param("name") String name,
            @Param("description") String description,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("filmTitle") String filmTitle,
            @Param("auditorium") String auditorium
    );

    @Query("""
            SELECT p FROM Program p
            WHERE p.state = 'ANNOUNCED'
            ORDER BY p.startDate ASC, p.name ASC
            """)
    List<Program> findAnnouncedPrograms();
}
