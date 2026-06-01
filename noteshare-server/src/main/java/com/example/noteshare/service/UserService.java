package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.dto.request.UpdateProfileRequest;
import com.example.noteshare.dto.response.UserResponse;
import com.example.noteshare.entity.User;
import com.example.noteshare.repository.NoteRepository;
import com.example.noteshare.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final FollowService followService;
    private final NoteRepository noteRepository;

    public UserService(UserRepository userRepository, FollowService followService, NoteRepository noteRepository) {
        this.userRepository = userRepository;
        this.followService = followService;
        this.noteRepository = noteRepository;
    }

    /**
     * 获取当前用户资料
     */
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserResponse resp = UserResponse.from(user);
        resp.setFollowerCount(followService.getFollowerCount(userId));
        resp.setFollowingCount(followService.getFollowingCount(userId));
        resp.setNoteCount((int) noteRepository.findByAuthorId(userId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        resp.setFollowed(false);
        return resp;
    }

    /**
     * 编辑资料
     */
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (req.getNickname() != null) {
            user.setNickname(req.getNickname());
        }
        if (req.getAvatarUrl() != null) {
            user.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getBio() != null) {
            user.setBio(req.getBio());
        }
        userRepository.save(user);

        UserResponse resp = UserResponse.from(user);
        resp.setFollowerCount(followService.getFollowerCount(userId));
        resp.setFollowingCount(followService.getFollowingCount(userId));
        resp.setNoteCount((int) noteRepository.findByAuthorId(userId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());
        resp.setFollowed(false);
        return resp;
    }

    /**
     * 查看他人资料（含关注/粉丝数、是否已关注）
     */
    public UserResponse getUserProfile(Long targetUserId, Long currentUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserResponse resp = UserResponse.from(user);
        resp.setFollowerCount(followService.getFollowerCount(targetUserId));
        resp.setFollowingCount(followService.getFollowingCount(targetUserId));
        resp.setNoteCount((int) noteRepository.findByAuthorId(targetUserId, org.springframework.data.domain.Pageable.unpaged()).getTotalElements());

        if (currentUserId != null) {
            resp.setFollowed(followService.isFollowing(currentUserId, targetUserId));
        }

        return resp;
    }
}
