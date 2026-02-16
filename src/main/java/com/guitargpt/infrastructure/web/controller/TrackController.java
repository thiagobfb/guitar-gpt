package com.guitargpt.infrastructure.web.controller;

import com.guitargpt.domain.model.Track;
import com.guitargpt.domain.port.in.TrackUseCase;
import com.guitargpt.infrastructure.web.dto.request.CreateTrackRequest;
import com.guitargpt.infrastructure.web.dto.request.UpdateTrackRequest;
import com.guitargpt.infrastructure.web.dto.response.TrackResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TrackController {

    private final TrackUseCase trackUseCase;

    public TrackController(TrackUseCase trackUseCase) {
        this.trackUseCase = trackUseCase;
    }

    @PostMapping("/projects/{projectId}/tracks")
    public ResponseEntity<TrackResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTrackRequest request) {
        Track track = new Track();
        track.setName(request.name());
        track.setType(request.type());
        track.setDescription(request.description());
        Track created = trackUseCase.create(projectId, track);
        return ResponseEntity.status(HttpStatus.CREATED).body(TrackResponse.from(created));
    }

    @GetMapping("/projects/{projectId}/tracks")
    public ResponseEntity<List<TrackResponse>> findByProjectId(@PathVariable UUID projectId) {
        List<TrackResponse> tracks = trackUseCase.findByProjectId(projectId).stream()
                .map(TrackResponse::from)
                .toList();
        return ResponseEntity.ok(tracks);
    }

    @GetMapping("/tracks/{id}")
    public ResponseEntity<TrackResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(TrackResponse.from(trackUseCase.findById(id)));
    }

    @PutMapping("/tracks/{id}")
    public ResponseEntity<TrackResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTrackRequest request) {
        Track track = new Track();
        track.setName(request.name());
        track.setType(request.type());
        track.setDescription(request.description());
        Track updated = trackUseCase.update(id, track);
        return ResponseEntity.ok(TrackResponse.from(updated));
    }

    @DeleteMapping("/tracks/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        trackUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
