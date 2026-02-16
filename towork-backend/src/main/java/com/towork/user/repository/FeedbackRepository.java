package com.towork.user.repository;

import com.towork.user.entity.Feedback;
import com.towork.user.entity.FeedbackDirection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByMissionIdAndAuthorUserId(Long missionId, Long authorUserId);

    List<Feedback> findByMissionId(Long missionId);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.targetUserId = :targetUserId")
    Double findAverageRatingByTargetUserId(Long targetUserId);

    Long countByTargetUserId(Long targetUserId);

    List<Feedback> findByTargetUserIdAndDirection(Long targetUserId, FeedbackDirection direction);

    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.targetUserId = :targetUserId AND f.direction = :direction")
    Double findAverageRatingByTargetUserIdAndDirection(Long targetUserId, FeedbackDirection direction);

    Long countByTargetUserIdAndDirection(Long targetUserId, FeedbackDirection direction);
}
