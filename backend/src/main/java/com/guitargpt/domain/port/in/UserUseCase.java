package com.guitargpt.domain.port.in;

import com.guitargpt.domain.model.User;

import java.util.List;
import java.util.UUID;

public interface UserUseCase {

    User create(User user);

    User findById(UUID id);

    List<User> findAll();

    User update(UUID id, User user);

    void delete(UUID id);
}
