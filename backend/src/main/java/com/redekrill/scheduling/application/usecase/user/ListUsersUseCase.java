package com.redekrill.scheduling.application.usecase.user;

import com.redekrill.scheduling.application.dto.user.UserResult;
import com.redekrill.scheduling.application.usecase.Pagination;
import com.redekrill.scheduling.domain.model.PageResult;
import com.redekrill.scheduling.domain.repository.UserRepository;

public class ListUsersUseCase {

    private final UserRepository userRepository;

    public ListUsersUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public PageResult<UserResult> execute(int page, int size) {
        PageResult<com.redekrill.scheduling.domain.model.User> result =
                userRepository.findAll(Pagination.page(page), Pagination.size(size));

        return PageResult.of(
                result.content().stream().map(UserResult::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}
