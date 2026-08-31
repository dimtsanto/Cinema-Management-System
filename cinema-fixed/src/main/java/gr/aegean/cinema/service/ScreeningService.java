package gr.aegean.cinema.service;

import gr.aegean.cinema.dto.ScreeningDTO;
import gr.aegean.cinema.exception.*;
import gr.aegean.cinema.model.entity.Program;
import gr.aegean.cinema.model.entity.Screening;
import gr.aegean.cinema.model.entity.User;
import gr.aegean.cinema.model.entity.UserProgramRole;
import gr.aegean.cinema.model.enums.ProgramRole;
import gr.aegean.cinema.model.enums.ProgramState;
import gr.aegean.cinema.model.enums.ScreeningState;
import gr.aegean.cinema.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final UserProgramRoleRepository roleRepository;

    // ==================== CREATE ====================

    @Transactional
    public ScreeningDTO.FullResponse createScreening(ScreeningDTO.CreateRequest request,
                                                      String currentUsername) {
        Program program = getProgramById(request.getProgramId());
        User submitter = getUserByUsername(currentUsername);

        // Programmers cannot submit to their own program
        if (roleRepository.existsByUserIdAndProgramIdAndRole(
                submitter.getId(), program.getId(), ProgramRole.PROGRAMMER)) {
            throw new ForbiddenException("A PROGRAMMER cannot submit screenings to their own program");
        }

        Screening screening = Screening.builder()
                .program(program)
                .submitter(submitter)
                .state(ScreeningState.CREATED)
                .filmTitle(request.getFilmTitle())
                .filmCast(request.getFilmCast())
                .filmGenre(request.getFilmGenre())
                .filmDurationMinutes(request.getFilmDurationMinutes())
                .auditoriumName(request.getAuditoriumName())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        screeningRepository.save(screening);

        // Assign SUBMITTER role if not yet set
        if (!roleRepository.existsByUserIdAndProgramId(submitter.getId(), program.getId())) {
            roleRepository.save(UserProgramRole.builder()
                    .user(submitter).program(program).role(ProgramRole.SUBMITTER).build());
        }

        log.info("Screening [{}] created by [{}] in program [{}]",
                screening.getId(), currentUsername, program.getId());
        return toFullResponse(screening);
    }

    // ==================== UPDATE ====================

    @Transactional
    public ScreeningDTO.FullResponse updateScreening(Long screeningId,
                                                      ScreeningDTO.UpdateRequest request,
                                                      String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        assertIsSubmitter(currentUsername, screening);

        if (screening.getState() != ScreeningState.CREATED) {
            throw new BadRequestException("Screening can only be updated in CREATED state");
        }

        if (request.getFilmTitle() != null) screening.setFilmTitle(request.getFilmTitle());
        if (request.getFilmCast() != null) screening.setFilmCast(request.getFilmCast());
        if (request.getFilmGenre() != null) screening.setFilmGenre(request.getFilmGenre());
        if (request.getFilmDurationMinutes() != null)
            screening.setFilmDurationMinutes(request.getFilmDurationMinutes());
        if (request.getAuditoriumName() != null) screening.setAuditoriumName(request.getAuditoriumName());
        if (request.getStartTime() != null) screening.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) screening.setEndTime(request.getEndTime());

        screeningRepository.save(screening);
        return toFullResponse(screening);
    }

    // ==================== SUBMIT ====================

    @Transactional
    public ScreeningDTO.FullResponse submitScreening(Long screeningId, String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        assertIsSubmitter(currentUsername, screening);

        if (screening.getState() != ScreeningState.CREATED) {
            throw new BadRequestException("Only CREATED screenings can be submitted");
        }
        if (screening.getProgram().getState() != ProgramState.SUBMISSION) {
            throw new BadRequestException("Submissions only allowed while program is in SUBMISSION state");
        }
        assertScreeningComplete(screening);

        screening.setState(ScreeningState.SUBMITTED);
        screeningRepository.save(screening);
        log.info("Screening [{}] submitted", screeningId);
        return toFullResponse(screening);
    }

    // ==================== WITHDRAW ====================

    @Transactional
    public void withdrawScreening(Long screeningId, String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        assertIsSubmitter(currentUsername, screening);

        if (screening.getState() != ScreeningState.CREATED) {
            throw new BadRequestException("Only CREATED screenings can be withdrawn");
        }
        screeningRepository.delete(screening);
        log.info("Screening [{}] withdrawn and deleted", screeningId);
    }

    // ==================== ASSIGN HANDLER ====================

    @Transactional
    public ScreeningDTO.FullResponse assignHandler(Long screeningId,
                                                    String staffUsername,
                                                    String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        Program program = screening.getProgram();
        assertIsProgrammer(currentUsername, program);

        if (program.getState() != ProgramState.ASSIGNMENT) {
            throw new BadRequestException("Handler assignment only allowed in ASSIGNMENT state");
        }

        User staff = getUserByUsername(staffUsername);
        if (!roleRepository.existsByUserIdAndProgramIdAndRole(
                staff.getId(), program.getId(), ProgramRole.STAFF)) {
            throw new BadRequestException(staffUsername + " is not a STAFF member of this program");
        }

        screening.setHandler(staff);
        screeningRepository.save(screening);
        log.info("Handler [{}] assigned to screening [{}]", staffUsername, screeningId);
        return toFullResponse(screening);
    }

    // ==================== REVIEW ====================

    @Transactional
    public ScreeningDTO.FullResponse reviewScreening(Long screeningId,
                                                      ScreeningDTO.ReviewRequest request,
                                                      String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        Program program = screening.getProgram();

        if (program.getState() != ProgramState.REVIEW) {
            throw new BadRequestException("Reviews only allowed in REVIEW state");
        }

        User reviewer = getUserByUsername(currentUsername);
        if (screening.getHandler() == null ||
                !screening.getHandler().getId().equals(reviewer.getId())) {
            throw new ForbiddenException("Only the assigned handler can review this screening");
        }

        screening.setReviewScore(request.getScore());
        screening.setReviewComments(request.getComments());
        screening.setState(ScreeningState.REVIEWED);
        screeningRepository.save(screening);
        log.info("Screening [{}] reviewed by [{}]", screeningId, currentUsername);
        return toFullResponse(screening);
    }

    // ==================== APPROVE ====================

    @Transactional
    public ScreeningDTO.FullResponse approveScreening(Long screeningId,
                                                       ScreeningDTO.ApprovalRequest request,
                                                       String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        assertIsProgrammer(currentUsername, screening.getProgram());

        if (screening.getProgram().getState() != ProgramState.SCHEDULING) {
            throw new BadRequestException("Approval only allowed in SCHEDULING state");
        }
        if (screening.getState() != ScreeningState.REVIEWED) {
            throw new BadRequestException("Only REVIEWED screenings can be approved");
        }

        screening.setState(ScreeningState.APPROVED);
        if (request.getConditionalNotes() != null)
            screening.setApprovalNotes(request.getConditionalNotes());
        screeningRepository.save(screening);
        log.info("Screening [{}] approved", screeningId);
        return toFullResponse(screening);
    }

    // ==================== REJECT ====================

    @Transactional
    public ScreeningDTO.FullResponse rejectScreening(Long screeningId,
                                                      ScreeningDTO.RejectionRequest request,
                                                      String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        assertIsProgrammer(currentUsername, screening.getProgram());

        ProgramState ps = screening.getProgram().getState();
        if (ps != ProgramState.SCHEDULING && ps != ProgramState.DECISION) {
            throw new BadRequestException("Rejection only allowed in SCHEDULING or DECISION state");
        }
        // In SCHEDULING: must be REVIEWED (not yet approved/rejected)
        if (ps == ProgramState.SCHEDULING && screening.getState() != ScreeningState.REVIEWED) {
            throw new BadRequestException("In SCHEDULING, only REVIEWED screenings can be rejected");
        }
        // In DECISION: must be APPROVED (and not yet finally submitted or already rejected)
        if (ps == ProgramState.DECISION && screening.getState() != ScreeningState.APPROVED) {
            throw new BadRequestException("In DECISION, only APPROVED screenings can be rejected");
        }

        screening.setState(ScreeningState.REJECTED);
        screening.setRejectionReason(request.getRejectionReason());
        screeningRepository.save(screening);
        log.info("Screening [{}] rejected", screeningId);
        return toFullResponse(screening);
    }

    // ==================== FINAL SUBMISSION ====================

    @Transactional
    public ScreeningDTO.FullResponse finalSubmit(Long screeningId, String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        assertIsSubmitter(currentUsername, screening);

        if (screening.getProgram().getState() != ProgramState.FINAL_PUBLICATION) {
            throw new BadRequestException("Final submission only allowed in FINAL_PUBLICATION state");
        }
        if (screening.getState() != ScreeningState.APPROVED) {
            throw new BadRequestException("Only APPROVED screenings can be finally submitted");
        }

        screening.setFinallySubmitted(true);
        screeningRepository.save(screening);
        log.info("Screening [{}] finally submitted", screeningId);
        return toFullResponse(screening);
    }

    // ==================== ACCEPT (SCHEDULE) ====================

    @Transactional
    public ScreeningDTO.FullResponse acceptScreening(Long screeningId, String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        assertIsProgrammer(currentUsername, screening.getProgram());

        if (screening.getProgram().getState() != ProgramState.DECISION) {
            throw new BadRequestException("Acceptance only allowed in DECISION state");
        }
        if (screening.getState() != ScreeningState.APPROVED || !screening.isFinallySubmitted()) {
            throw new BadRequestException("Only APPROVED and finally submitted screenings can be accepted");
        }

        screening.setState(ScreeningState.SCHEDULED);
        screeningRepository.save(screening);
        log.info("Screening [{}] accepted/SCHEDULED", screeningId);
        return toFullResponse(screening);
    }

    // ==================== AUTO-REJECT ====================

    @Transactional
    public int autoRejectNotFinallySubmitted(Long programId) {
        int count = screeningRepository.autoRejectNotFinallySubmitted(programId);
        log.info("Auto-rejected {} screenings in program [{}]", count, programId);
        return count;
    }

    // ==================== SEARCH ====================

    @Transactional(readOnly = true)
    public List<?> searchScreenings(Long programId, String filmTitle, String cast,
                                     String genre, LocalDateTime startDate,
                                     LocalDateTime endDate, String currentUsername) {
        Program program = getProgramById(programId);
        List<Screening> allScreenings = screeningRepository.searchScreenings(
                programId, null, null, null, startDate, endDate);

        // Multi-word AND matching: each word in the query must appear in the field
        List<Screening> screenings = allScreenings.stream().filter(s -> {
            if (filmTitle != null) {
                String title = s.getFilmTitle() != null ? s.getFilmTitle().toLowerCase() : "";
                for (String word : filmTitle.trim().toLowerCase().split("\\s+")) {
                    if (!title.contains(word)) return false;
                }
            }
            if (cast != null) {
                String c = s.getFilmCast() != null ? s.getFilmCast().toLowerCase() : "";
                for (String word : cast.trim().toLowerCase().split("\\s+")) {
                    if (!c.contains(word)) return false;
                }
            }
            if (genre != null) {
                String g = s.getFilmGenre() != null ? s.getFilmGenre().toLowerCase() : "";
                for (String word : genre.trim().toLowerCase().split("\\s+")) {
                    if (!g.contains(word)) return false;
                }
            }
            return true;
        }).sorted(java.util.Comparator
                .comparing((Screening s) -> s.getFilmGenre() != null ? s.getFilmGenre() : "")
                .thenComparing(s -> s.getFilmTitle() != null ? s.getFilmTitle() : ""))
          .collect(java.util.stream.Collectors.toList());

        // Visitor / unauthenticated: only ANNOUNCED programs, public info
        if (currentUsername == null) {
            if (program.getState() != ProgramState.ANNOUNCED) {
                throw new ForbiddenException("Program not publicly visible");
            }
            return screenings.stream()
                    .filter(s -> s.getState() == ScreeningState.SCHEDULED)
                    .map(this::toPublicResponse)
                    .collect(Collectors.toList());
        }

        User user = getUserByUsername(currentUsername);
        boolean isProgrammer = roleRepository.existsByUserIdAndProgramIdAndRole(
                user.getId(), programId, ProgramRole.PROGRAMMER);
        if (isProgrammer) {
            return screenings.stream().map(this::toFullResponse).collect(Collectors.toList());
        }

        // Authenticated non-programmer: apply role-based filter
        return screenings.stream().map(s -> {
            // SUBMITTER sees own
            if (s.getSubmitter().getId().equals(user.getId())) return toFullResponse(s);
            // STAFF sees assigned
            if (s.getHandler() != null && s.getHandler().getId().equals(user.getId()))
                return toFullResponse(s);
            // Otherwise public info (if ANNOUNCED)
            if (program.getState() == ProgramState.ANNOUNCED
                    && s.getState() == ScreeningState.SCHEDULED)
                return toPublicResponse(s);
            return null;
        }).filter(r -> r != null).collect(Collectors.toList());
    }

    // ==================== VIEW ====================

    @Transactional(readOnly = true)
    public Object viewScreening(Long screeningId, String currentUsername) {
        Screening screening = getScreeningById(screeningId);
        Program program = screening.getProgram();

        if (currentUsername == null) {
            if (program.getState() != ProgramState.ANNOUNCED
                    || screening.getState() != ScreeningState.SCHEDULED) {
                throw new ForbiddenException("Not visible to visitors");
            }
            return toPublicResponse(screening);
        }

        User user = getUserByUsername(currentUsername);
        if (roleRepository.existsByUserIdAndProgramIdAndRole(
                user.getId(), program.getId(), ProgramRole.PROGRAMMER))
            return toFullResponse(screening);
        if (screening.getSubmitter().getId().equals(user.getId()))
            return toFullResponse(screening);
        if (screening.getHandler() != null && screening.getHandler().getId().equals(user.getId()))
            return toFullResponse(screening);

        if (program.getState() == ProgramState.ANNOUNCED
                && screening.getState() == ScreeningState.SCHEDULED)
            return toPublicResponse(screening);

        throw new ForbiddenException("You do not have access to this screening");
    }

    // ==================== HELPERS ====================

    private void assertScreeningComplete(Screening s) {
        if (s.getFilmTitle() == null || s.getAuditoriumName() == null
                || s.getFilmDurationMinutes() == null || s.getEndTime() == null) {
            throw new BadRequestException(
                    "Screening must have film title, auditorium, duration and end time before submission");
        }
        if (s.getStartTime() != null && s.getEndTime() != null) {
            long minutesDiff = java.time.Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
            if (minutesDiff < s.getFilmDurationMinutes()) {
                throw new BadRequestException(
                        "End time - start time must be >= film duration (" +
                        s.getFilmDurationMinutes() + " min)");
            }
        }
    }

    private void assertIsSubmitter(String username, Screening screening) {
        User user = getUserByUsername(username);
        if (!screening.getSubmitter().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the SUBMITTER of this screening can perform this action");
        }
    }

    private void assertIsProgrammer(String username, Program program) {
        User user = getUserByUsername(username);
        if (!roleRepository.existsByUserIdAndProgramIdAndRole(
                user.getId(), program.getId(), ProgramRole.PROGRAMMER)) {
            throw new ForbiddenException("Only a PROGRAMMER of this program can perform this action");
        }
    }

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private Program getProgramById(Long id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found: " + id));
    }

    private Screening getScreeningById(Long id) {
        return screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screening not found: " + id));
    }

    private ScreeningDTO.FullResponse toFullResponse(Screening s) {
        return ScreeningDTO.FullResponse.builder()
                .id(s.getId())
                .programId(s.getProgram().getId())
                .state(s.getState())
                .creationDate(s.getCreationDate())
                .submitterUsername(s.getSubmitter().getUsername())
                .handlerUsername(s.getHandler() != null ? s.getHandler().getUsername() : null)
                .filmTitle(s.getFilmTitle())
                .filmCast(s.getFilmCast())
                .filmGenre(s.getFilmGenre())
                .filmDurationMinutes(s.getFilmDurationMinutes())
                .auditoriumName(s.getAuditoriumName())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .reviewScore(s.getReviewScore())
                .reviewComments(s.getReviewComments())
                .approvalNotes(s.getApprovalNotes())
                .rejectionReason(s.getRejectionReason())
                .finallySubmitted(s.isFinallySubmitted())
                .build();
    }

    private ScreeningDTO.PublicResponse toPublicResponse(Screening s) {
        return ScreeningDTO.PublicResponse.builder()
                .id(s.getId())
                .filmTitle(s.getFilmTitle())
                .filmGenre(s.getFilmGenre())
                .startTime(s.getStartTime())
                .auditoriumName(s.getAuditoriumName())
                .build();
    }
}
