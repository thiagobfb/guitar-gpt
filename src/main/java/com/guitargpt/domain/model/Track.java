package com.guitargpt.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

    private UUID id;
    private UUID projectId;
    private String name;
    private TrackType type;
    private String description;
    private LocalDateTime createdAt;
}
