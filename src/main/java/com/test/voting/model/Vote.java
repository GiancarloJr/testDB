package com.test.voting.model;

import java.io.Serial;
import java.io.Serializable;

import com.test.voting.converter.VoteTypeConverter;
import com.test.voting.model.enums.VoteType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vote")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vote implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vote_seq")
    @SequenceGenerator(name = "vote_seq", sequenceName = "vote_id_seq")
    @Column(name = "id_vote", unique = true, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(name= "cd_cpf",nullable = false, length = 11)
    private String cpf;

    @Convert(converter = VoteTypeConverter.class)
    @Column(name = "tp_vote",nullable = false)
    private VoteType vote;

    @Column(name = "dt_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
