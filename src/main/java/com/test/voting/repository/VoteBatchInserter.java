package com.test.voting.repository;

import com.test.voting.dto.VoteMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class VoteBatchInserter {

    private final JdbcTemplate jdbcTemplate;

    private static final String SQL = """
        INSERT INTO vote (session_id, cd_cpf, tp_vote, dt_created_at)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (session_id, cd_cpf) DO NOTHING
    """;

    @Transactional
    public int[] insertBatch(List<VoteMessage> votes) {
        Timestamp now = Timestamp.from(Instant.now());

        return jdbcTemplate.batchUpdate(SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                VoteMessage msg = votes.get(i);
                ps.setLong(1, msg.getSessionId());
                ps.setString(2, msg.getCpf());
                ps.setInt(3, msg.getVote().getCode());
                ps.setTimestamp(4, now);
            }

            @Override
            public int getBatchSize() {
                return votes.size();
            }
        });
    }
}