package gr.aegean.cinema.repository;

import gr.aegean.cinema.model.entity.UserProgramRole;
import gr.aegean.cinema.model.enums.ProgramRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProgramRoleRepository extends JpaRepository<UserProgramRole, Long> {

    Optional<UserProgramRole> findByUserIdAndProgramId(Long userId, Long programId);

    boolean existsByUserIdAndProgramId(Long userId, Long programId);

    boolean existsByUserIdAndProgramIdAndRole(Long userId, Long programId, ProgramRole role);

    List<UserProgramRole> findByProgramIdAndRole(Long programId, ProgramRole role);

    List<UserProgramRole> findByUserId(Long userId);

    @Query("SELECT upr FROM UserProgramRole upr WHERE upr.program.id = :programId")
    List<UserProgramRole> findAllByProgramId(@Param("programId") Long programId);
}
