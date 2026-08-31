package gr.aegean.cinema.controller;

import gr.aegean.cinema.dto.ScreeningDTO;
import gr.aegean.cinema.service.ScreeningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/screenings")
@RequiredArgsConstructor
public class ScreeningController {

    private final ScreeningService screeningService;

    // -------- CREATE --------

    /**
     * POST /api/screenings
     * Create a screening. USER becomes SUBMITTER.
     */
    @PostMapping
    public ResponseEntity<ScreeningDTO.FullResponse> createScreening(
            @Valid @RequestBody ScreeningDTO.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(screeningService.createScreening(request, userDetails.getUsername()));
    }

    // -------- UPDATE --------

    /**
     * PUT /api/screenings/{id}
     * Update a screening (SUBMITTER only, CREATED state).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScreeningDTO.FullResponse> updateScreening(
            @PathVariable Long id,
            @RequestBody ScreeningDTO.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(screeningService.updateScreening(id, request, userDetails.getUsername()));
    }

    // -------- SUBMIT --------

    /**
     * PATCH /api/screenings/{id}/submit
     * Submit a complete screening to the program.
     */
    @PatchMapping("/{id}/submit")
    public ResponseEntity<ScreeningDTO.FullResponse> submitScreening(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(screeningService.submitScreening(id, userDetails.getUsername()));
    }

    // -------- WITHDRAW --------

    /**
     * DELETE /api/screenings/{id}
     * Withdraw (delete) a CREATED screening.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> withdrawScreening(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        screeningService.withdrawScreening(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // -------- ASSIGN HANDLER --------

    /**
     * PATCH /api/screenings/{id}/handler
     * Assign a STAFF member as handler (PROGRAMMER only, ASSIGNMENT phase).
     */
    @PatchMapping("/{id}/handler")
    public ResponseEntity<ScreeningDTO.FullResponse> assignHandler(
            @PathVariable Long id,
            @Valid @RequestBody ScreeningDTO.HandlerAssignRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(screeningService.assignHandler(
                id, request.getStaffUsername(), userDetails.getUsername()));
    }

    // -------- REVIEW --------

    /**
     * PATCH /api/screenings/{id}/review
     * Provide a review (assigned STAFF only, REVIEW phase).
     */
    @PatchMapping("/{id}/review")
    public ResponseEntity<ScreeningDTO.FullResponse> reviewScreening(
            @PathVariable Long id,
            @Valid @RequestBody ScreeningDTO.ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(screeningService.reviewScreening(id, request, userDetails.getUsername()));
    }

    // -------- APPROVE --------

    /**
     * PATCH /api/screenings/{id}/approve
     * Approve a screening (PROGRAMMER only, SCHEDULING phase).
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ScreeningDTO.FullResponse> approveScreening(
            @PathVariable Long id,
            @RequestBody(required = false) ScreeningDTO.ApprovalRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (request == null) request = new ScreeningDTO.ApprovalRequest();
        return ResponseEntity.ok(screeningService.approveScreening(id, request, userDetails.getUsername()));
    }

    // -------- REJECT --------

    /**
     * PATCH /api/screenings/{id}/reject
     * Reject a screening (PROGRAMMER only).
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<ScreeningDTO.FullResponse> rejectScreening(
            @PathVariable Long id,
            @Valid @RequestBody ScreeningDTO.RejectionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(screeningService.rejectScreening(id, request, userDetails.getUsername()));
    }

    // -------- FINAL SUBMIT --------

    /**
     * PATCH /api/screenings/{id}/final-submit
     * Final submission of approved screening (SUBMITTER only, FINAL_PUBLICATION phase).
     */
    @PatchMapping("/{id}/final-submit")
    public ResponseEntity<ScreeningDTO.FullResponse> finalSubmit(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(screeningService.finalSubmit(id, userDetails.getUsername()));
    }

    // -------- ACCEPT --------

    /**
     * PATCH /api/screenings/{id}/accept
     * Accept into schedule = SCHEDULED (PROGRAMMER only, DECISION phase).
     */
    @PatchMapping("/{id}/accept")
    public ResponseEntity<ScreeningDTO.FullResponse> acceptScreening(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(screeningService.acceptScreening(id, userDetails.getUsername()));
    }

    // -------- SEARCH --------

    /**
     * GET /api/screenings/search?programId=X&filmTitle=...
     * Search screenings within a program.
     */
    @GetMapping("/search")
    public ResponseEntity<List<?>> searchScreenings(
            @RequestParam Long programId,
            @RequestParam(required = false) String filmTitle,
            @RequestParam(required = false) String cast,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(screeningService.searchScreenings(
                programId, filmTitle, cast, genre, startDate, endDate, username));
    }

    // -------- VIEW --------

    /**
     * GET /api/screenings/{id}
     * View screening details (role-filtered).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> viewScreening(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(screeningService.viewScreening(id, username));
    }
}
