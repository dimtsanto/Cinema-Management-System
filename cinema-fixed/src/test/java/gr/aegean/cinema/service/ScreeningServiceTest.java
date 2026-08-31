package gr.aegean.cinema.service;

import gr.aegean.cinema.dto.ScreeningDTO;
import gr.aegean.cinema.exception.BadRequestException;
import gr.aegean.cinema.exception.ForbiddenException;
import gr.aegean.cinema.model.entity.Program;
import gr.aegean.cinema.model.entity.Screening;
import gr.aegean.cinema.model.entity.User;
import gr.aegean.cinema.model.enums.ProgramRole;
import gr.aegean.cinema.model.enums.ProgramState;
import gr.aegean.cinema.model.enums.ScreeningState;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScreeningServiceTest {

    @Mock private ScreeningRepository screeningRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserProgramRoleRepository roleRepository;

    @InjectMocks private ScreeningService screeningService;

    private User submitter;
    private User programmer;
    private User staffUser;
    private Program program;
    private Screening screening;

    @BeforeEach
    void setUp() {
        submitter  = User.builder().id(1L).username("submitter1").build();
        programmer = User.builder().id(2L).username("prog1").build();
        staffUser  = User.builder().id(3L).username("staff1").build();

        program = Program.builder()
                .id(10L).name("Season 2025")
                .state(ProgramState.SUBMISSION)
                .creator(programmer)
                .build();

        screening = Screening.builder()
                .id(100L)
                .program(program)
                .submitter(submitter)
                .state(ScreeningState.CREATED)
                .filmTitle("The Matrix")
                .filmCast("Keanu Reeves")
                .filmGenre("Sci-Fi")
                .filmDurationMinutes(136)
                .auditoriumName("Hall A")
                .startTime(LocalDateTime.of(2025, 3, 10, 18, 0))
                .endTime(LocalDateTime.of(2025, 3, 10, 20, 20))
                .build();
    }

    // --------- CREATE ---------

    @Test
    @DisplayName("createScreening: programmer cannot submit to own program")
    void createScreening_programmerSubmits_throwsForbidden() {
        when(programRepository.findById(10L)).thenReturn(Optional.of(program));
        when(userRepository.findByUsername("prog1")).thenReturn(Optional.of(programmer));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        ScreeningDTO.CreateRequest req = ScreeningDTO.CreateRequest.builder()
                .programId(10L).filmTitle("Film").build();

        assertThatThrownBy(() -> screeningService.createScreening(req, "prog1"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("PROGRAMMER cannot submit");
    }

    // --------- SUBMIT ---------

    @Test
    @DisplayName("submitScreening: success when program is SUBMISSION and screening is complete")
    void submitScreening_success() {
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("submitter1")).thenReturn(Optional.of(submitter));
        when(screeningRepository.save(any())).thenReturn(screening);

        screeningService.submitScreening(100L, "submitter1");
        verify(screeningRepository).save(argThat(s -> s.getState() == ScreeningState.SUBMITTED));
    }

    @Test
    @DisplayName("submitScreening: fails when program not in SUBMISSION state")
    void submitScreening_wrongProgramState_throwsBadRequest() {
        program.setState(ProgramState.REVIEW);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("submitter1")).thenReturn(Optional.of(submitter));

        assertThatThrownBy(() -> screeningService.submitScreening(100L, "submitter1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SUBMISSION state");
    }

    @Test
    @DisplayName("submitScreening: fails when screening is incomplete (no auditorium)")
    void submitScreening_incompleteScreening_throwsBadRequest() {
        screening.setAuditoriumName(null);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("submitter1")).thenReturn(Optional.of(submitter));

        assertThatThrownBy(() -> screeningService.submitScreening(100L, "submitter1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("submitScreening: fails when end time - start time < film duration")
    void submitScreening_endTimeTooEarly_throwsBadRequest() {
        screening.setEndTime(LocalDateTime.of(2025, 3, 10, 19, 0));
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("submitter1")).thenReturn(Optional.of(submitter));

        assertThatThrownBy(() -> screeningService.submitScreening(100L, "submitter1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("film duration");
    }

    // --------- WITHDRAW ---------

    @Test
    @DisplayName("withdrawScreening: success — CREATED screening is deleted")
    void withdrawScreening_success() {
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("submitter1")).thenReturn(Optional.of(submitter));

        screeningService.withdrawScreening(100L, "submitter1");
        verify(screeningRepository).delete(screening);
    }

    @Test
    @DisplayName("withdrawScreening: non-submitter cannot withdraw")
    void withdrawScreening_notSubmitter_throwsForbidden() {
        User other = User.builder().id(99L).username("other").build();
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> screeningService.withdrawScreening(100L, "other"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("withdrawScreening: fails when screening is not in CREATED state")
    void withdrawScreening_notCreated_throwsBadRequest() {
        screening.setState(ScreeningState.SUBMITTED);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("submitter1")).thenReturn(Optional.of(submitter));

        assertThatThrownBy(() -> screeningService.withdrawScreening(100L, "submitter1"))
                .isInstanceOf(BadRequestException.class);
    }

    // --------- ASSIGN HANDLER ---------

    @Test
    @DisplayName("assignHandler: fails when program not in ASSIGNMENT state")
    void assignHandler_wrongState_throwsBadRequest() {
        program.setState(ProgramState.REVIEW);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("prog1")).thenReturn(Optional.of(programmer));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        assertThatThrownBy(() -> screeningService.assignHandler(100L, "staff1", "prog1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("ASSIGNMENT state");
    }

    @Test
    @DisplayName("assignHandler: fails when user is not a STAFF of the program")
    void assignHandler_notStaff_throwsBadRequest() {
        program.setState(ProgramState.ASSIGNMENT);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("prog1")).thenReturn(Optional.of(programmer));
        when(userRepository.findByUsername("staff1")).thenReturn(Optional.of(staffUser));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);
        when(roleRepository.existsByUserIdAndProgramIdAndRole(3L, 10L, ProgramRole.STAFF)).thenReturn(false);

        assertThatThrownBy(() -> screeningService.assignHandler(100L, "staff1", "prog1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not a STAFF member");
    }

    // --------- REVIEW ---------

    @Test
    @DisplayName("reviewScreening: fails when program not in REVIEW state")
    void reviewScreening_wrongState_throwsBadRequest() {
        program.setState(ProgramState.SCHEDULING);
        screening.setHandler(staffUser);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("staff1")).thenReturn(Optional.of(staffUser));

        ScreeningDTO.ReviewRequest req = new ScreeningDTO.ReviewRequest(8, "Good");
        assertThatThrownBy(() -> screeningService.reviewScreening(100L, req, "staff1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("REVIEW state");
    }

    @Test
    @DisplayName("reviewScreening: non-handler cannot review")
    void reviewScreening_notHandler_throwsForbidden() {
        program.setState(ProgramState.REVIEW);
        screening.setHandler(staffUser);
        User other = User.builder().id(99L).username("other").build();
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

        ScreeningDTO.ReviewRequest req = new ScreeningDTO.ReviewRequest(8, "Good");
        assertThatThrownBy(() -> screeningService.reviewScreening(100L, req, "other"))
                .isInstanceOf(ForbiddenException.class);
    }

    // --------- REJECT ---------

    @Test
    @DisplayName("rejectScreening: in DECISION, only APPROVED can be rejected")
    void rejectScreening_decision_notApproved_throwsBadRequest() {
        program.setState(ProgramState.DECISION);
        screening.setState(ScreeningState.SUBMITTED);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("prog1")).thenReturn(Optional.of(programmer));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        ScreeningDTO.RejectionRequest req = new ScreeningDTO.RejectionRequest("Not suitable");
        assertThatThrownBy(() -> screeningService.rejectScreening(100L, req, "prog1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("APPROVED");
    }

    @Test
    @DisplayName("rejectScreening: in SCHEDULING, only REVIEWED can be rejected")
    void rejectScreening_scheduling_notReviewed_throwsBadRequest() {
        program.setState(ProgramState.SCHEDULING);
        screening.setState(ScreeningState.SUBMITTED);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("prog1")).thenReturn(Optional.of(programmer));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        ScreeningDTO.RejectionRequest req = new ScreeningDTO.RejectionRequest("Not suitable");
        assertThatThrownBy(() -> screeningService.rejectScreening(100L, req, "prog1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("REVIEWED");
    }

    // --------- FINAL SUBMIT ---------

    @Test
    @DisplayName("finalSubmit: fails when program not in FINAL_PUBLICATION state")
    void finalSubmit_wrongProgramState_throwsBadRequest() {
        program.setState(ProgramState.DECISION);
        screening.setState(ScreeningState.APPROVED);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("submitter1")).thenReturn(Optional.of(submitter));

        assertThatThrownBy(() -> screeningService.finalSubmit(100L, "submitter1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("FINAL_PUBLICATION");
    }

    @Test
    @DisplayName("finalSubmit: fails when screening not APPROVED")
    void finalSubmit_notApproved_throwsBadRequest() {
        program.setState(ProgramState.FINAL_PUBLICATION);
        screening.setState(ScreeningState.SUBMITTED);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("submitter1")).thenReturn(Optional.of(submitter));

        assertThatThrownBy(() -> screeningService.finalSubmit(100L, "submitter1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("APPROVED");
    }

    // --------- ACCEPT ---------

    @Test
    @DisplayName("acceptScreening: fails when screening not finally submitted")
    void acceptScreening_notFinallySubmitted_throwsBadRequest() {
        program.setState(ProgramState.DECISION);
        screening.setState(ScreeningState.APPROVED);
        // finallySubmitted defaults to false
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("prog1")).thenReturn(Optional.of(programmer));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        assertThatThrownBy(() -> screeningService.acceptScreening(100L, "prog1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("acceptScreening: fails when program not in DECISION state")
    void acceptScreening_wrongState_throwsBadRequest() {
        program.setState(ProgramState.SCHEDULING);
        screening.setState(ScreeningState.APPROVED);
        when(screeningRepository.findById(100L)).thenReturn(Optional.of(screening));
        when(userRepository.findByUsername("prog1")).thenReturn(Optional.of(programmer));
        when(roleRepository.existsByUserIdAndProgramIdAndRole(2L, 10L, ProgramRole.PROGRAMMER)).thenReturn(true);

        assertThatThrownBy(() -> screeningService.acceptScreening(100L, "prog1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("DECISION state");
    }
}
