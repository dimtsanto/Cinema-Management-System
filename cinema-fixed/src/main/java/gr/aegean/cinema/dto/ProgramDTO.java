package gr.aegean.cinema.dto;

import gr.aegean.cinema.model.enums.ProgramState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ProgramDTO {

    // ---- CREATE REQUEST ----
    public static class CreateRequest {
        private String name;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;

        public CreateRequest() {}
        public CreateRequest(String name, String description, LocalDate startDate, LocalDate endDate) {
            this.name = name; this.description = description;
            this.startDate = startDate; this.endDate = endDate;
        }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    // ---- UPDATE REQUEST ----
    public static class UpdateRequest {
        private String name;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private List<String> programmerUsernames;
        private List<String> staffUsernames;

        public UpdateRequest() {}
        public UpdateRequest(String name, String description, LocalDate startDate, LocalDate endDate,
                             List<String> programmerUsernames, List<String> staffUsernames) {
            this.name = name; this.description = description;
            this.startDate = startDate; this.endDate = endDate;
            this.programmerUsernames = programmerUsernames;
            this.staffUsernames = staffUsernames;
        }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public List<String> getProgrammerUsernames() { return programmerUsernames; }
        public List<String> getStaffUsernames() { return staffUsernames; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public void setProgrammerUsernames(List<String> programmerUsernames) { this.programmerUsernames = programmerUsernames; }
        public void setStaffUsernames(List<String> staffUsernames) { this.staffUsernames = staffUsernames; }
    }

    // ---- FULL RESPONSE (PROGRAMMER) ----
    public static class FullResponse {
        private Long id;
        private String name;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime creationDate;
        private ProgramState state;
        private String creatorUsername;
        private List<String> programmerUsernames;
        private List<String> staffUsernames;

        public FullResponse() {}
        private FullResponse(Builder b) {
            this.id = b.id; this.name = b.name; this.description = b.description;
            this.startDate = b.startDate; this.endDate = b.endDate;
            this.creationDate = b.creationDate; this.state = b.state;
            this.creatorUsername = b.creatorUsername;
            this.programmerUsernames = b.programmerUsernames;
            this.staffUsernames = b.staffUsernames;
        }
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public LocalDateTime getCreationDate() { return creationDate; }
        public ProgramState getState() { return state; }
        public String getCreatorUsername() { return creatorUsername; }
        public List<String> getProgrammerUsernames() { return programmerUsernames; }
        public List<String> getStaffUsernames() { return staffUsernames; }
        public void setId(Long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String d) { this.description = d; }
        public void setStartDate(LocalDate d) { this.startDate = d; }
        public void setEndDate(LocalDate d) { this.endDate = d; }
        public void setCreationDate(LocalDateTime d) { this.creationDate = d; }
        public void setState(ProgramState s) { this.state = s; }
        public void setCreatorUsername(String u) { this.creatorUsername = u; }
        public void setProgrammerUsernames(List<String> l) { this.programmerUsernames = l; }
        public void setStaffUsernames(List<String> l) { this.staffUsernames = l; }

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private Long id; private String name; private String description;
            private LocalDate startDate; private LocalDate endDate;
            private LocalDateTime creationDate; private ProgramState state;
            private String creatorUsername;
            private List<String> programmerUsernames; private List<String> staffUsernames;
            public Builder id(Long id) { this.id = id; return this; }
            public Builder name(String name) { this.name = name; return this; }
            public Builder description(String d) { this.description = d; return this; }
            public Builder startDate(LocalDate d) { this.startDate = d; return this; }
            public Builder endDate(LocalDate d) { this.endDate = d; return this; }
            public Builder creationDate(LocalDateTime d) { this.creationDate = d; return this; }
            public Builder state(ProgramState s) { this.state = s; return this; }
            public Builder creatorUsername(String u) { this.creatorUsername = u; return this; }
            public Builder programmerUsernames(List<String> l) { this.programmerUsernames = l; return this; }
            public Builder staffUsernames(List<String> l) { this.staffUsernames = l; return this; }
            public FullResponse build() { return new FullResponse(this); }
        }
    }

    // ---- PUBLIC RESPONSE (VISITOR) ----
    public static class PublicResponse {
        private Long id;
        private String name;
        private String description;
        private LocalDate startDate;
        private LocalDate endDate;
        private ProgramState state;
        private List<String> programmerUsernames;

        public PublicResponse() {}
        private PublicResponse(Builder b) {
            this.id = b.id; this.name = b.name; this.description = b.description;
            this.startDate = b.startDate; this.endDate = b.endDate;
            this.state = b.state; this.programmerUsernames = b.programmerUsernames;
        }
        public Long getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public ProgramState getState() { return state; }
        public List<String> getProgrammerUsernames() { return programmerUsernames; }
        public void setId(Long id) { this.id = id; }
        public void setName(String name) { this.name = name; }
        public void setDescription(String d) { this.description = d; }
        public void setStartDate(LocalDate d) { this.startDate = d; }
        public void setEndDate(LocalDate d) { this.endDate = d; }
        public void setState(ProgramState s) { this.state = s; }
        public void setProgrammerUsernames(List<String> l) { this.programmerUsernames = l; }

        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private Long id; private String name; private String description;
            private LocalDate startDate; private LocalDate endDate;
            private ProgramState state; private List<String> programmerUsernames;
            public Builder id(Long id) { this.id = id; return this; }
            public Builder name(String name) { this.name = name; return this; }
            public Builder description(String d) { this.description = d; return this; }
            public Builder startDate(LocalDate d) { this.startDate = d; return this; }
            public Builder endDate(LocalDate d) { this.endDate = d; return this; }
            public Builder state(ProgramState s) { this.state = s; return this; }
            public Builder programmerUsernames(List<String> l) { this.programmerUsernames = l; return this; }
            public PublicResponse build() { return new PublicResponse(this); }
        }
    }

    // ---- ROLE ASSIGN REQUEST ----
    public static class RoleAssignRequest {
        @NotBlank
        private String username;
        public RoleAssignRequest() {}
        public RoleAssignRequest(String username) { this.username = username; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    // ---- STATE TRANSITION REQUEST ----
    public static class StateTransitionRequest {
        @NotNull
        private ProgramState targetState;
        public StateTransitionRequest() {}
        public StateTransitionRequest(ProgramState targetState) { this.targetState = targetState; }
        public ProgramState getTargetState() { return targetState; }
        public void setTargetState(ProgramState targetState) { this.targetState = targetState; }
    }
}
