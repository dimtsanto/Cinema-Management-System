package gr.aegean.cinema.repository;

import gr.aegean.cinema.model.entity.Screening;
import gr.aegean.cinema.model.enums.ScreeningState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long>,
        JpaSpecificationExecutor<Screening> {

    List<Screening> findByProgramId(Long programId);

    List<Screening> findByProgramIdAndState(Long programId, ScreeningState state);

    List<Screening> findBySubmitterId(Long submitterId);

    List<Screening> findByHandlerId(Long handlerId);

    /**
     * Search screenings within a program with optional AND filters.
     */
    @Query("""
            SELECT s FROM Screening s
            WHERE s.program.id = :programId
              AND (:filmTitle IS NULL OR LOWER(s.filmTitle) LIKE LOWER(CONCAT('%',:filmTitle,'%')))
              AND (:cast IS NULL OR LOWER(s.filmCast) LIKE LOWER(CONCAT('%',:cast,'%')))
              AND (:genre IS NULL OR LOWER(s.filmGenre) LIKE LOWER(CONCAT('%',:genre,'%')))
              AND (:startDate IS NULL OR s.startTime >= :startDate)
              AND (:endDate IS NULL OR s.startTime <= :endDate)
            ORDER BY s.filmGenre ASC, s.filmTitle ASC
            """)
    List<Screening> searchScreenings(
            @Param("programId") Long programId,
            @Param("filmTitle") String filmTitle,
            @Param("cast") String cast,
            @Param("genre") String genre,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Auto-reject all APPROVED screenings that were NOT finally submitted,
     * when program transitions to DECISION state.
     */
    @Modifying
    @Query("""
            UPDATE Screening s
            SET s.state = 'REJECTED', s.rejectionReason = 'Auto-rejected: not finally submitted before DECISION'
            WHERE s.program.id = :programId
              AND s.state = 'APPROVED'
              AND s.finallySubmitted = false
            """)
    int autoRejectNotFinallySubmitted(@Param("programId") Long programId);
}
