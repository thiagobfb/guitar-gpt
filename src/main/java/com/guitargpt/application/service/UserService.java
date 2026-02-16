package com.guitargpt.application.service;

import com.guitargpt.domain.exception.BusinessRuleException;
import com.guitargpt.domain.exception.ResourceNotFoundException;
import com.guitargpt.domain.model.User;
import com.guitargpt.domain.port.in.UserUseCase;
import com.guitargpt.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService implements UserUseCase {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User create(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new BusinessRuleException("Email already in use: " + user.getEmail());
        }
        user.setId(UUID.randomUUID());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User update(UUID id, User user) {
        User existing = findById(id);
        if (userRepository.existsByEmailAndIdNot(user.getEmail(), id)) {
            throw new BusinessRuleException("Email already in use: " + user.getEmail());
        }
        existing.setName(user.getName());
        existing.setEmail(user.getEmail());
        existing.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        findById(id);
        userRepository.deleteById(id);
    }
}
