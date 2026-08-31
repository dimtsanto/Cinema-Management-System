package gr.aegean.cinema.service;

import gr.aegean.cinema.dto.ProgramDTO;
import gr.aegean.cinema.exception.BadRequestException;
import gr.aegean.cinema.exception.ConflictException;
import gr.aegean.cinema.exception.ForbiddenException;
import gr.aegean.cinema.model.entity.Program;
import gr.aegean.cinema.model.entity.User;
import gr.aegean.cinema.model.entity.UserProgramRole;
import gr.aegean.cinema.model.enums.ProgramRole;
import gr.aegean.cinema.model.enums.ProgramState;
import gr.aegean.cinema.repository.ProgramRepository;
import gr.aegean.cinema.repository.ScreeningRepository;
import gr.aegean.cinema.repository.UserProgramRoleRepository;
import gr.aegean.cinema.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock private ProgramRepository programRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserProgramRoleRepository roleRepository;
    @Mock private ScreeningRepository screeningRepository;

    @InjectMocks private ProgramService programService;

    private User creator;
    private Program program;

    @BeforeEach
    void setUp() {
        creator = User.builder().id(1L).username("alice").fullName("Alice A").build();
        program = Program.builder()
                .id(10L).name("Spring Film Season 2025")
                .description("A great season")
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 3, 31))
                .creator(creator)
                .state(ProgramState.CREATED)
                .build();
    }

    // --------- CREATE ---------

    @Test
    @DisplayName("createProgram: success — creator becomes PROGRAMMER")
    void createProgram_success() {
        when(programRepository.existsByName(any())).thenReturn(false);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(programRepository.save(any())).thenReturn(program);
        when(roleRepository.findAllByProgramId(any())).thenReturn(List.of(
                UserProgramRole.builder().user(creator).program(program)
                        .role(ProgramRole.PROGRAMMER).build()));

        ProgramDTO.CreateRequest req = new ProgramDTO.CreateRequest(
                "Spring Film Season 2025", "A great season",
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31));

        ProgramDTO.FullResponse response = programService.createProgram(req, "alice");

        assertThat(response.getName()).isEqualTo("Spring Film Season 2025");
        assertThat(response.getState()).isEqualTo(ProgramState.CREATED);
        verify(roleRepository).save(any(UserProgramRole.class));
    }

    @Test
    @DisplayName("createProgram: duplicate name throws ConflictException")
    void createProgram_duplicateName_throwsConflict() {
        when(programRepository.existsByName("Spring Film Season 2025")).thenReturn(true);

        ProgramDTO.CreateRequest req = new ProgramDTO.CreateRequest(
                "Spring Film Season 2025", "desc",
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31));

        assertThatThrownBy(() -> programService.createProgram(req, "alice"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    // --------- UPDATE ---------

    @Test
    @DisplayName("updateProgram: removing creator from programmers throws BadRequest")
    void updateProgram_removeCreator_throwsBadRequest() {
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        ProgramDTO.UpdateRequest req = new ProgramDTO.UpdateRequest(
                null, null, null, null, List.of("bob"), null);

        assertThatThrownBy(() -> programService.updateProgram(10L, req, "alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("creator cannot be removed");
    }

    @Test
    @DisplayName("updateProgram: non-programmer cannot update")
    void updateProgram_notProgrammer_throwsForbidden() {
        User bob = User.builder().id(2L).username("bob").build();
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(false);

        ProgramDTO.UpdateRequest req = new ProgramDTO.UpdateRequest("Name", null, null, null, null, null);
        assertThatThrownBy(() -> programService.updateProgram(10L, req, "bob"))
                .isInstanceOf(ForbiddenException.class);
    }

    // --------- DELETE ---------

    @Test
    @DisplayName("deleteProgram: success when CREATED state and user is PROGRAMMER")
    void deleteProgram_success() {
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        programService.deleteProgram(10L, "alice");
        verify(programRepository).delete(program);
    }

    @Test
    @DisplayName("deleteProgram: fails when program not in CREATED state")
    void deleteProgram_notCreatedState_throwsBadRequest() {
        program.setState(ProgramState.SUBMISSION);
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        assertThatThrownBy(() -> programService.deleteProgram(10L, "alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CREATED state");
    }

    @Test
    @DisplayName("deleteProgram: fails when user is not PROGRAMMER")
    void deleteProgram_notProgrammer_throwsForbidden() {
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(
                User.builder().id(2L).username("bob").build()));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(false);

        assertThatThrownBy(() -> programService.deleteProgram(10L, "bob"))
                .isInstanceOf(ForbiddenException.class);
    }

    // --------- STATE TRANSITION ---------

    @Test
    @DisplayName("transitionState: CREATED -> SUBMISSION is valid")
    void transitionState_createdToSubmission_success() {
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);
        when(programRepository.save(any())).thenReturn(program);
        when(roleRepository.findAllByProgramId(any())).thenReturn(List.of());

        ProgramDTO.FullResponse resp = programService.transitionState(10L, ProgramState.SUBMISSION, "alice");
        assertThat(resp).isNotNull();
        verify(programRepository).save(argThat(p -> p.getState() == ProgramState.SUBMISSION));
    }

    @Test
    @DisplayName("transitionState: FINAL_PUBLICATION -> DECISION triggers auto-reject")
    void transitionState_toDecision_autoRejectsScreenings() {
        program.setState(ProgramState.FINAL_PUBLICATION);
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);
        when(screeningRepository.autoRejectNotFinallySubmitted(10L)).thenReturn(2);
        when(programRepository.save(any())).thenReturn(program);
        when(roleRepository.findAllByProgramId(any())).thenReturn(List.of());

        programService.transitionState(10L, ProgramState.DECISION, "alice");
        verify(screeningRepository).autoRejectNotFinallySubmitted(10L);
    }

    @Test
    @DisplayName("transitionState: rollback (SUBMISSION -> CREATED) is rejected")
    void transitionState_rollback_throwsBadRequest() {
        program.setState(ProgramState.SUBMISSION);
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        assertThatThrownBy(() -> programService.transitionState(10L, ProgramState.CREATED, "alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid state transition");
    }

    @Test
    @DisplayName("transitionState: skipping state (CREATED -> REVIEW) is rejected")
    void transitionState_skipState_throwsBadRequest() {
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        assertThatThrownBy(() -> programService.transitionState(10L, ProgramState.REVIEW, "alice"))
                .isInstanceOf(BadRequestException.class);
    }

    // --------- ADD PROGRAMMER ---------

    @Test
    @DisplayName("addProgrammer: success — user is added")
    void addProgrammer_success() {
        User bob = User.builder().id(2L).username("bob").build();
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(false);
        when(roleRepository.existsByUserIdAndProgramId(2L, 10L)).thenReturn(false);

        programService.addProgrammer(10L, "bob", "alice");
        verify(roleRepository).save(argThat(r -> r.getRole() == ProgramRole.PROGRAMMER));
    }

    @Test
    @DisplayName("addProgrammer: already PROGRAMMER throws ConflictException")
    void addProgrammer_alreadyProgrammer_throwsConflict() {
        User bob = User.builder().id(2L).username("bob").build();
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        assertThatThrownBy(() -> programService.addProgrammer(10L, "bob", "alice"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a PROGRAMMER");
    }

    // --------- ADD STAFF ---------

    @Test
    @DisplayName("addStaff: success in CREATED state")
    void addStaff_success() {
        User bob = User.builder().id(2L).username("bob").build();
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);
        when(roleRepository.existsByUserIdAndProgramId(2L, 10L)).thenReturn(false);

        programService.addStaff(10L, "bob", "alice");
        verify(roleRepository).save(argThat(r -> r.getRole() == ProgramRole.STAFF));
    }

    @Test
    @DisplayName("addStaff: fails after SUBMISSION (staff set frozen)")
    void addStaff_afterSubmission_throwsBadRequest() {
        program.setState(ProgramState.ASSIGNMENT);
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        assertThatThrownBy(() -> programService.addStaff(10L, "bob", "alice"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("frozen");
    }

    @Test
    @DisplayName("addStaff: user with existing role cannot be added as staff")
    void addStaff_userAlreadyHasRole_throwsConflict() {
        User bob = User.builder().id(2L).username("bob").build();
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(creator));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(1L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);
        when(roleRepository.existsByUserIdAndProgramId(2L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> programService.addStaff(10L, "bob", "alice"))
                .isInstanceOf(ConflictException.class);
    }
}
