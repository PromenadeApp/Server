package com.promenade.promenadeapp.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoadRepository extends JpaRepository<UserRoad, Long> {

    List<UserRoad> findByUserGoogleId(String googleId);

    List<UserRoad> findByUserId(Long userId);

}
