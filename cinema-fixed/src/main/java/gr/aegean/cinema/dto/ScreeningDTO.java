package gr.aegean.cinema.dto;

import gr.aegean.cinema.model.enums.ScreeningState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ScreeningDTO {

    // ---- CREATE REQUEST ----
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "Program ID is required")
        private Long programId;

        // Film & auditorium info may be provided at creation or later update
        private String filmTitle;
        private String filmCast;
        private String filmGenre;
        private Integer filmDurationMinutes;
        private String auditoriumName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    // ---- UPDATE REQUEST ----
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String filmTitle;
        private String filmCast;
        private String filmGenre;
        private Integer filmDurationMinutes;
        private String auditoriumName;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    // ---- HANDLER ASSIGNMENT REQUEST ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HandlerAssignRequest {
        @NotBlank
        private String staffUsername;
    }

    // ---- REVIEW REQUEST ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRequest {
        @NotNull
        @Min(0)
        @Max(10)
        private Integer score;

        @NotBlank
        private String comments;
    }

    // ---- APPROVAL REQUEST ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalRequest {
        private String conditionalNotes; // optional
    }

    // ---- REJECTION REQUEST ----
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectionRequest {
        @NotBlank
        private String rejectionReason;
    }

    // ---- FULL RESPONSE (for PROGRAMMER / SUBMITTER / STAFF handler) ----
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullResponse {
        private Long id;
        private Long programId;
        private ScreeningState state;
        private LocalDateTime creationDate;
        private String submitterUsername;
        private String handlerUsername;

        private String filmTitle;
        private String filmCast;
        private String filmGenre;
        private Integer filmDurationMinutes;
        private String auditoriumName;

        private LocalDateTime startTime;
        private LocalDateTime endTime;

        private Integer reviewScore;
        private String reviewComments;
        private String approvalNotes;
        private String rejectionReason;
        private boolean finallySubmitted;
    }

    // ---- PUBLIC RESPONSE (for VISITOR - announced programs only) ----
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicResponse {
        private Long id;
        private String filmTitle;
        private String filmGenre;
        private LocalDateTime startTime;
        private String auditoriumName;
    }
}
