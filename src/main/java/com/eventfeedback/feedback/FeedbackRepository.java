package com.eventfeedback.feedback;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.eventfeedback.event.EventStats;

public interface FeedbackRepository extends JpaRepository<Feedback, Long>, JpaSpecificationExecutor<Feedback> {

    List<Feedback> findByEventIdOrderBySubmittedAtDescIdDesc(Long eventId);

    @Query("""
            select new com.eventfeedback.event.EventStats(f.event.id, count(f), avg(f.overallRating))
            from Feedback f
            group by f.event.id""")
    List<EventStats> statsPerEvent();

    @Query("""
            select new com.eventfeedback.event.EventStats(f.event.id, count(f), avg(f.overallRating))
            from Feedback f
            where f.event.id = :eventId
            group by f.event.id""")
    Optional<EventStats> statsForEvent(Long eventId);

    @Query("select avg(f.overallRating) from Feedback f")
    Double averageOverallRating();

    long countByOverallRatingGreaterThanEqual(int rating);

    long countByWouldRecommendTrue();

    long countByStatus(FeedbackStatus status);
}
