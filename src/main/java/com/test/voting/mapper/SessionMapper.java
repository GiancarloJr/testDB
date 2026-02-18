package com.test.voting.mapper;

import com.test.voting.dto.SessionResponse;
import com.test.voting.dto.SessionRequest;
import com.test.voting.model.enums.SessionStatus;
import com.test.voting.model.Session;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface SessionMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expirationTime", ignore = true)
    @Mapping(target = "status", ignore = true)
    Session toEntity(SessionRequest request);
    
    @Mapping(source = "id", target = "sessionId")
    @Mapping(source = "createdAt", target = "creationTime")
    SessionResponse toResponse(Session session);

    @AfterMapping
    default void setDefaultValues(@MappingTarget Session session) {

        if (session.getCreatedAt() == null) {
            session.setCreatedAt(LocalDateTime.now());
        }

        if (session.getVotingTimeMinutes() == null) {
            session.setVotingTimeMinutes(1);
        }

        if (session.getExpirationTime() == null) {
            session.setExpirationTime(
                    session.getCreatedAt().plusMinutes(session.getVotingTimeMinutes())
            );
        }

        if (session.getStatus() == null) {
            session.setStatus(SessionStatus.OPEN);
        }
    }

}
