package com.emblemix.team;

import com.emblemix.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "team_members")
public class TeamMember {

    @EmbeddedId
    private TeamMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teamId")
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamRole role = TeamRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    public TeamMember() {}

    public TeamMember(Team team, User user, TeamRole role) {
        this.id = new TeamMemberId(team.getId(), user.getId());
        this.team = team;
        this.user = user;
        this.role = role != null ? role : TeamRole.MEMBER;
        this.joinedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.joinedAt == null) {
            this.joinedAt = Instant.now();
        }
    }

    public TeamMemberId getId() {
        return id;
    }

    public void setId(TeamMemberId id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
}
