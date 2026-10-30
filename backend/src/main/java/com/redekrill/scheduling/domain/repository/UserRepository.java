package com.redekrill.scheduling.domain.repository;

import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    PageResult<User> findAll(int page, int size);

    User save(User user);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
