package com.test.voting.dto;

import com.test.voting.model.enums.VoteType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private String cpf;
    private VoteType vote;
}
