package com.towork.user.repository;

import com.towork.user.entity.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FreelancerRepository extends JpaRepository<Freelancer, Long> {

    Optional<Freelancer> findByEmail(String email);

    List<Freelancer> findByIsVerified(Boolean isVerified);

    List<Freelancer> findByIsAvailable(Boolean isAvailable);

    List<Freelancer> findByTitleContainingIgnoreCase(String title);

    List<Freelancer> findByCity(String city);

    List<Freelancer> findByCountry(String country);

    List<Freelancer> findByHourlyRateBetween(BigDecimal minRate, BigDecimal maxRate);

    List<Freelancer> findByRatingGreaterThanEqual(BigDecimal minRating);

    @Query("SELECT f FROM Freelancer f WHERE f.isActive = true AND f.isVerified = true AND f.isAvailable = true")
    List<Freelancer> findActiveAvailableFreelancers();

    @Query("SELECT f FROM Freelancer f WHERE f.isActive = true AND f.isVerified = true ORDER BY f.rating DESC")
    List<Freelancer> findTopRatedFreelancers();

    @Query("SELECT COUNT(f) FROM Freelancer f WHERE f.isVerified = true")
    Long countVerifiedFreelancers();

    @Query("SELECT f FROM Freelancer f WHERE f.email = :email AND f.isActive = true")
    Optional<Freelancer> findActiveByEmail(@Param("email") String email);
}
