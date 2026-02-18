package com.test.voting.repository;

import com.test.voting.dto.ResultResponse;
import com.test.voting.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Long> {

    boolean existsBySessionIdAndCpf(Long sessionId, String cpf);

    @Query(value = """
        SELECT 
            COALESCE(SUM(CASE WHEN v.tp_vote = 0 THEN 1 ELSE 0 END), 0) AS yes,
            COALESCE(SUM(CASE WHEN v.tp_vote = 1 THEN 1 ELSE 0 END), 0) AS no,
            COUNT(v.id_vote) AS total
        FROM vote v
        WHERE v.session_id = :sessionId
        """, nativeQuery = true)
    ResultResponse.VoteCount countVotesBySessionId(@Param("sessionId") Long sessionId);
}
