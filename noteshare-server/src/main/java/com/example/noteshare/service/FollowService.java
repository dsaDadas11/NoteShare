package com.example.noteshare.service;

import com.example.noteshare.common.BusinessException;
import com.example.noteshare.common.ErrorCode;
import com.example.noteshare.entity.Follow;
import com.example.noteshare.repository.FollowRepository;
import com.example.noteshare.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public FollowService(FollowRepository followRepository, UserRepository userRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BusinessException(ErrorCode.FOLLOW_SELF);
        }
        if (!userRepository.existsById(followeeId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        try {
            followRepository.saveAndFlush(follow);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.FOLLOW_ALREADY);
        }
    }

    @Transactional
    public void unfollow(Long followerId, Long followeeId) {
        int deleted = followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.FOLLOW_NOT_FOUND);
        }
    }

    public long getFollowingCount(Long userId) {
        return followRepository.countByFollowerId(userId);
    }

    public long getFollowerCount(Long userId) {
        return followRepository.countByFolloweeId(userId);
    }

    public boolean isFollowing(Long followerId, Long followeeId) {
        return followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
    }
}
