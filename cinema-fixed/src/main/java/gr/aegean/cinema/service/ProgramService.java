package gr.aegean.cinema.service;

import gr.aegean.cinema.dto.ProgramDTO;
import gr.aegean.cinema.exception.*;
import gr.aegean.cinema.model.entity.Program;
import gr.aegean.cinema.model.entity.User;
import gr.aegean.cinema.model.entity.UserProgramRole;
import gr.aegean.cinema.model.enums.ProgramRole;
import gr.aegean.cinema.model.enums.ProgramState;
import gr.aegean.cinema.repository.ProgramRepository;
import gr.aegean.cinema.repository.ScreeningRepository;
import gr.aegean.cinema.repository.UserProgramRoleRepository;
import gr.aegean.cinema.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProgramService {

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final UserProgramRoleRepository roleRepository;
    private final ScreeningRepository screeningRepository;

    // ==================== PROGRAM CREATION ====================

    @Transactional
    public ProgramDTO.FullResponse createProgram(ProgramDTO.CreateRequest request,
                                                  String currentUsername) {
        if (programRepository.existsByName(request.getName())) {
            throw new ConflictException("Program name already exists: " + request.getName());
        }

        User creator = getUserByUsername(currentUsername);

        Program program = Program.builder()
                .name(request.getName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .creator(creator)
                .state(ProgramState.CREATED)
                .build();

        programRepository.save(program);

        UserProgramRole role = UserProgramRole.builder()
                .user(creator)
                .program(program)
                .role(ProgramRole.PROGRAMMER)
                .build();
        roleRepository.save(role);

        log.info("Program created [id={}] by user [{}]", program.getId(), currentUsername);
        return toFullResponse(program);
    }

    // ==================== PROGRAM UPDATE ====================

    @Transactional
    public ProgramDTO.FullResponse updateProgram(Long programId,
                                                  ProgramDTO.UpdateRequest request,
                                                  String currentUsername) {
        Program program = getProgramById(programId);
        assertIsProgrammer(currentUsername, program);
        assertNotAnnounced(program);

        if (request.getName() != null) {
            if (programRepository.existsByNameAndIdNot(request.getName(), programId)) {
                throw new ConflictException("Program name already taken: " + request.getName());
            }
            program.setName(request.getName());
        }
        if (request.getDescription() != null) program.setDescription(request.getDescription());
        if (request.getStartDate() != null) program.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) program.setEndDate(request.getEndDate());

        // Handle programmer removals — creator can NEVER be removed
        if (request.getProgrammerUsernames() != null) {
            User creator = program.getCreator();
            boolean creatorIncluded = request.getProgrammerUsernames().contains(creator.getUsername());
            if (!creatorIncluded) {
                throw new BadRequestException("The program creator cannot be removed from the PROGRAMMER set");
            }
            // Remove programmers not in the new list (except creator)
            List<UserProgramRole> currentProgrammers = roleRepository
                    .findByProgramIdAndRole(programId, ProgramRole.PROGRAMMER);
            for (UserProgramRole upr : currentProgrammers) {
                if (!request.getProgrammerUsernames().contains(upr.getUser().getUsername())) {
                    roleRepository.delete(upr);
                }
            }
            // Add new programmers
            for (String username : request.getProgrammerUsernames()) {
                User u = getUserByUsername(username);
                if (!roleRepository.existsByUserIdAndProgramIdAndRole(u.getId(), programId, ProgramRole.PROGRAMMER)) {
                    if (roleRepository.existsByUserIdAndProgramId(u.getId(), programId)) {
                        throw new ConflictException(username + " already has a different role in this program");
                    }
                    roleRepository.save(UserProgramRole.builder()
                            .user(u).program(program).role(ProgramRole.PROGRAMMER).build());
                }
            }
        }

        // Handle staff updates — only allowed before ASSIGNMENT
        if (request.getStaffUsernames() != null) {
            if (program.getState().ordinal() >= ProgramState.ASSIGNMENT.ordinal()) {
                throw new BadRequestException("Cannot modify STAFF after SUBMISSION state");
            }
            List<UserProgramRole> currentStaff = roleRepository
                    .findByProgramIdAndRole(programId, ProgramRole.STAFF);
            for (UserProgramRole upr : currentStaff) {
                if (!request.getStaffUsernames().contains(upr.getUser().getUsername())) {
                    roleRepository.delete(upr);
                }
            }
            for (String username : request.getStaffUsernames()) {
                User u = getUserByUsername(username);
                if (!roleRepository.existsByUserIdAndProgramIdAndRole(u.getId(), programId, ProgramRole.STAFF)) {
                    if (roleRepository.existsByUserIdAndProgramId(u.getId(), programId)) {
                        throw new ConflictException(username + " already has a different role in this program");
                    }
                    roleRepository.save(UserProgramRole.builder()
                            .user(u).program(program).role(ProgramRole.STAFF).build());
                }
            }
        }

        programRepository.save(program);
        log.info("Program updated [id={}] by [{}]", programId, currentUsername);
        return toFullResponse(program);
    }

    // ==================== ADD PROGRAMMER ====================

    @Transactional
    public void addProgrammer(Long programId, String usernameToAdd, String currentUsername) {
        Program program = getProgramById(programId);
        assertIsProgrammer(currentUsername, program);
        assertNotAnnounced(program);

        User targetUser = getUserByUsername(usernameToAdd);
        if (roleRepository.existsByUserIdAndProgramIdAndRole(
                targetUser.getId(), programId, ProgramRole.PROGRAMMER)) {
            throw new ConflictException(usernameToAdd + " is already a PROGRAMMER of this program");
        }
        if (roleRepository.existsByUserIdAndProgramId(targetUser.getId(), programId)) {
            throw new ConflictException(usernameToAdd + " already has a role in this program");
        }

        roleRepository.save(UserProgramRole.builder()
                .user(targetUser).program(program).role(ProgramRole.PROGRAMMER).build());
        log.info("User [{}] added as PROGRAMMER to program [{}]", usernameToAdd, programId);
    }

    // ==================== ADD STAFF ====================

    @Transactional
    public void addStaff(Long programId, String usernameToAdd, String currentUsername) {
        Program program = getProgramById(programId);
        assertIsProgrammer(currentUsername, program);

        // Staff set frozen after SUBMISSION state
        if (program.getState().ordinal() >= ProgramState.ASSIGNMENT.ordinal()) {
            throw new BadRequestException("Cannot modify STAFF after SUBMISSION state");
        }

        User targetUser = getUserByUsername(usernameToAdd);
        if (roleRepository.existsByUserIdAndProgramId(targetUser.getId(), programId)) {
            throw new ConflictException(usernameToAdd + " already has a role in this program");
        }

        roleRepository.save(UserProgramRole.builder()
                .user(targetUser).program(program).role(ProgramRole.STAFF).build());
        log.info("User [{}] added as STAFF to program [{}]", usernameToAdd, programId);
    }

    // ==================== PROGRAM SEARCH ====================

    @Transactional(readOnly = true)
    public List<?> searchPrograms(String name, String description,
                                   LocalDate startDate, LocalDate endDate,
                                   String filmTitle, String auditorium,
                                   String currentUsername) {
        List<Program> programs = programRepository.searchPrograms(
                name, description, startDate, endDate, filmTitle, auditorium);

        // Unauthenticated: only ANNOUNCED programs, sorted by date then name
        if (currentUsername == null) {
            return programs.stream()
                    .filter(p -> p.getState() == ProgramState.ANNOUNCED)
                    .sorted(java.util.Comparator
                            .comparing(Program::getStartDate)
                            .thenComparing(Program::getName))
                    .map(this::toPublicResponse)
                    .collect(Collectors.toList());
        }

        User user = getUserByUsername(currentUsername);

        return programs.stream().map(program -> {
            boolean isProgrammer = roleRepository.existsByUserIdAndProgramIdAndRole(
                    user.getId(), program.getId(), ProgramRole.PROGRAMMER);
            boolean isStaff = roleRepository.existsByUserIdAndProgramIdAndRole(
                    user.getId(), program.getId(), ProgramRole.STAFF);
            boolean isSubmitter = roleRepository.existsByUserIdAndProgramIdAndRole(
                    user.getId(), program.getId(), ProgramRole.SUBMITTER);

            if (isProgrammer) {
                return (Object) toFullResponse(program);
            } else if (isStaff || isSubmitter) {
                // Staff and submitters see announced or their own programs (public info)
                return (Object) toPublicResponse(program);
            } else if (program.getState() == ProgramState.ANNOUNCED) {
                return (Object) toPublicResponse(program);
            } else {
                return null;
            }
        }).filter(r -> r != null)
          .sorted((a, b) -> {
              // Sort by date then name
              LocalDate dateA = a instanceof ProgramDTO.FullResponse ?
                      ((ProgramDTO.FullResponse) a).getStartDate() :
                      ((ProgramDTO.PublicResponse) a).getStartDate();
              LocalDate dateB = b instanceof ProgramDTO.FullResponse ?
                      ((ProgramDTO.FullResponse) b).getStartDate() :
                      ((ProgramDTO.PublicResponse) b).getStartDate();
              String nameA = a instanceof ProgramDTO.FullResponse ?
                      ((ProgramDTO.FullResponse) a).getName() :
                      ((ProgramDTO.PublicResponse) a).getName();
              String nameB = b instanceof ProgramDTO.FullResponse ?
                      ((ProgramDTO.FullResponse) b).getName() :
                      ((ProgramDTO.PublicResponse) b).getName();
              int dateCmp = dateA.compareTo(dateB);
              return dateCmp != 0 ? dateCmp : nameA.compareTo(nameB);
          })
          .collect(Collectors.toList());
    }

    // ==================== PROGRAM VIEW ====================

    @Transactional(readOnly = true)
    public Object viewProgram(Long programId, String currentUsername) {
        Program program = getProgramById(programId);

        if (currentUsername == null) {
            if (program.getState() != ProgramState.ANNOUNCED) {
                throw new ForbiddenException("Program is not publicly visible yet");
            }
            return toPublicResponse(program);
        }

        User user = getUserByUsername(currentUsername);
        boolean isProgrammer = roleRepository.existsByUserIdAndProgramIdAndRole(
                user.getId(), programId, ProgramRole.PROGRAMMER);

        if (isProgrammer) return toFullResponse(program);

        if (program.getState() != ProgramState.ANNOUNCED) {
            throw new ForbiddenException("Program is not publicly visible yet");
        }
        return toPublicResponse(program);
    }

    // ==================== PROGRAM DELETE ====================

    @Transactional
    public void deleteProgram(Long programId, String currentUsername) {
        Program program = getProgramById(programId);
        assertIsProgrammer(currentUsername, program);

        if (program.getState() != ProgramState.CREATED) {
            throw new BadRequestException("Program can only be deleted in CREATED state");
        }
        programRepository.delete(program);
        log.info("Program [{}] deleted by [{}]", programId, currentUsername);
    }

    // ==================== STATE TRANSITION ====================

    @Transactional
    public ProgramDTO.FullResponse transitionState(Long programId,
                                                    ProgramState targetState,
                                                    String currentUsername) {
        Program program = getProgramById(programId);
        assertIsProgrammer(currentUsername, program);

        validateStateTransition(program.getState(), targetState);

        // AUTO-REJECT: when transitioning to DECISION, auto-reject all APPROVED
        // screenings that were not finally submitted (as required by the spec)
        if (targetState == ProgramState.DECISION) {
            int rejected = screeningRepository.autoRejectNotFinallySubmitted(programId);
            if (rejected > 0) {
                log.info("Auto-rejected {} screenings in program [{}] on DECISION transition",
                        rejected, programId);
            }
        }

        program.setState(targetState);
        programRepository.save(program);
        log.info("Program [{}] state -> {} by [{}]", programId, targetState, currentUsername);
        return toFullResponse(program);
    }

    // ==================== HELPERS ====================

    private void validateStateTransition(ProgramState current, ProgramState target) {
        boolean valid = switch (current) {
            case CREATED           -> target == ProgramState.SUBMISSION;
            case SUBMISSION        -> target == ProgramState.ASSIGNMENT;
            case ASSIGNMENT        -> target == ProgramState.REVIEW;
            case REVIEW            -> target == ProgramState.SCHEDULING;
            case SCHEDULING        -> target == ProgramState.FINAL_PUBLICATION;
            case FINAL_PUBLICATION -> target == ProgramState.DECISION;
            case DECISION          -> target == ProgramState.ANNOUNCED;
            case ANNOUNCED         -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    "Invalid state transition: " + current + " -> " + target);
        }
    }

    private void assertIsProgrammer(String username, Program program) {
        User user = getUserByUsername(username);
        boolean isProgrammer = roleRepository.existsByUserIdAndProgramIdAndRole(
                user.getId(), program.getId(), ProgramRole.PROGRAMMER);
        if (!isProgrammer) {
            throw new ForbiddenException("Only a PROGRAMMER of this program can perform this action");
        }
    }

    private void assertNotAnnounced(Program program) {
        if (program.getState() == ProgramState.ANNOUNCED) {
            throw new BadRequestException("Program is already ANNOUNCED and cannot be modified");
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

    public ProgramDTO.FullResponse toFullResponse(Program p) {
        List<UserProgramRole> roles = roleRepository.findAllByProgramId(p.getId());
        List<String> programmers = roles.stream()
                .filter(r -> r.getRole() == ProgramRole.PROGRAMMER)
                .map(r -> r.getUser().getUsername())
                .collect(Collectors.toList());
        List<String> staff = roles.stream()
                .filter(r -> r.getRole() == ProgramRole.STAFF)
                .map(r -> r.getUser().getUsername())
                .collect(Collectors.toList());

        return ProgramDTO.FullResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .creationDate(p.getCreationDate())
                .state(p.getState())
                .creatorUsername(p.getCreator().getUsername())
                .programmerUsernames(programmers)
                .staffUsernames(staff)
                .build();
    }

    private ProgramDTO.PublicResponse toPublicResponse(Program p) {
        List<String> programmers = roleRepository.findByProgramIdAndRole(p.getId(), ProgramRole.PROGRAMMER)
                .stream().map(r -> r.getUser().getUsername()).collect(Collectors.toList());
        return ProgramDTO.PublicResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .state(p.getState())
                .programmerUsernames(programmers)
                .build();
    }
}
