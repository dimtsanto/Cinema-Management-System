package gr.aegean.cinema.controller;

import gr.aegean.cinema.dto.ProgramDTO;
import gr.aegean.cinema.model.enums.ProgramState;
import gr.aegean.cinema.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    // -------- CREATE --------

    /**
     * POST /api/programs
     * Create a new program. Caller becomes PROGRAMMER.
     */
    @PostMapping
    public ResponseEntity<ProgramDTO.FullResponse> createProgram(
            @Valid @RequestBody ProgramDTO.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(programService.createProgram(request, userDetails.getUsername()));
    }

    // -------- UPDATE --------

    /**
     * PUT /api/programs/{id}
     * Update program fields. Only PROGRAMMER.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProgramDTO.FullResponse> updateProgram(
            @PathVariable Long id,
            @RequestBody ProgramDTO.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(programService.updateProgram(id, request, userDetails.getUsername()));
    }

    // -------- SEARCH --------

    /**
     * GET /api/programs/search
     * Search programs. Anonymous users see only ANNOUNCED.
     */
    @GetMapping("/search")
    public ResponseEntity<List<?>> searchPrograms(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String filmTitle,
            @RequestParam(required = false) String auditorium,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(programService.searchPrograms(
                name, description, startDate, endDate, filmTitle, auditorium, username));
    }

    /**
     * GET /api/programs/announced
     * Public: list all ANNOUNCED programs.
     */
    @GetMapping("/announced")
    public ResponseEntity<List<?>> getAnnouncedPrograms() {
        return ResponseEntity.ok(programService.searchPrograms(
                null, null, null, null, null, null, null));
    }

    // -------- VIEW --------

    /**
     * GET /api/programs/{id}
     * View program details (role-filtered).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> viewProgram(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(programService.viewProgram(id, username));
    }

    // -------- DELETE --------

    /**
     * DELETE /api/programs/{id}
     * Delete program (PROGRAMMER only, CREATED state).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        programService.deleteProgram(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // -------- STATE TRANSITION --------

    /**
     * PATCH /api/programs/{id}/state
     * Transition program to next state.
     */
    @PatchMapping("/{id}/state")
    public ResponseEntity<ProgramDTO.FullResponse> transitionState(
            @PathVariable Long id,
            @RequestBody ProgramDTO.StateTransitionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(programService.transitionState(
                id, request.getTargetState(), userDetails.getUsername()));
    }

    // -------- ADD PROGRAMMER --------

    /**
     * POST /api/programs/{id}/programmers
     * Add a user as PROGRAMMER.
     */
    @PostMapping("/{id}/programmers")
    public ResponseEntity<Void> addProgrammer(
            @PathVariable Long id,
            @Valid @RequestBody ProgramDTO.RoleAssignRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        programService.addProgrammer(id, request.getUsername(), userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // -------- ADD STAFF --------

    /**
     * POST /api/programs/{id}/staff
     * Add a user as STAFF.
     */
    @PostMapping("/{id}/staff")
    public ResponseEntity<Void> addStaff(
            @PathVariable Long id,
            @Valid @RequestBody ProgramDTO.RoleAssignRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        programService.addStaff(id, request.getUsername(), userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
