package com.test.voting.model;

import java.io.Serial;
import java.io.Serializable;

import com.test.voting.converter.SessionStatusConverter;
import com.test.voting.model.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_session", unique = true, nullable = false)
    private Long id;

    @Column(name = "nm_description", nullable = false)
    private String description;

    @Column(name = "dt_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "qt_voting_time_minutes", nullable = false)
    private Integer votingTimeMinutes;

    @Column(name = "dt_expiration_time")
    private LocalDateTime expirationTime;

    @Convert(converter = SessionStatusConverter.class)
    @Column(name = "tp_status", nullable = false)
    private SessionStatus status;

    public boolean isExpired() {
        return expirationTime != null && expirationTime.isBefore(LocalDateTime.now());
    }

}
