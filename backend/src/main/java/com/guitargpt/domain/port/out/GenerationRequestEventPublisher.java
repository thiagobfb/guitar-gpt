package com.guitargpt.domain.port.out;

import com.guitargpt.domain.model.GenerationRequest;

public interface GenerationRequestEventPublisher {

    void publish(GenerationRequest generationRequest);
}
