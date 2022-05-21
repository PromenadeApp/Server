package com.promenade.promenadeapp.controller.User;

import com.promenade.promenadeapp.domain.User.User;
import com.promenade.promenadeapp.domain.User.UserRoad;
import com.promenade.promenadeapp.domain.User.UserRoadHashtag;
import com.promenade.promenadeapp.domain.User.UserRoadPath;
import com.promenade.promenadeapp.dto.*;
import com.promenade.promenadeapp.service.User.UserRoadHashtagService;
import com.promenade.promenadeapp.service.User.UserRoadPathService;
import com.promenade.promenadeapp.service.User.UserRoadService;
import com.promenade.promenadeapp.service.User.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/user/road")
public class UserRoadController {

    private final UserService userService;

    private final UserRoadService userRoadService;

    private final UserRoadPathService userRoadPathService;

    private final UserRoadHashtagService userRoadHashtagService;

    @GetMapping
    public ResponseEntity<?> getUserRoads(@AuthenticationPrincipal String googleId) {
        List<UserRoad> userRoads = userRoadService.findByUserGoogleId(googleId);
        if (userRoads.isEmpty()) {
            ResponseDto response = ResponseDto.builder()
                    .error("사용자의 커스텀 산책로가 없습니다. userGoogleId = " + googleId)
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
        // UserRoad에 Hashtag 추가해서 응답해주기
        List<UserRoadResponseDto> responseDtos = userRoadHashtagService.addHashtagRoads(userRoads);
        ResponseDto<UserRoadResponseDto> response = ResponseDto.<UserRoadResponseDto>builder()
                .data(responseDtos)
                .build();

        return ResponseEntity.ok().body(response);
    }

    @PostMapping
    public ResponseEntity<?> saveUserRoad(@AuthenticationPrincipal String googleId,
                                          @RequestBody UserRoadRequestDto requestDto) {
        try {
            User foundUserByGoogleId = userService.findByGoogleId(googleId);

            // 1. request 정보에 따라 UserRoad 엔티티에 저장하기 (trailPoints x)
            UserRoad userRoad = UserRoad.builder()
                    .id(null) // saveUserRoad()에서 자동 추가
                    .trailName(requestDto.getTrailName())
                    .description(requestDto.getDescription())
                    .distance(requestDto.getDistance())
                    .startAddr(requestDto.getStartAddr())
                    .startLat(requestDto.getTrailPoints().get(0).get(0))
                    .startLng(requestDto.getTrailPoints().get(0).get(1))
                    .user(foundUserByGoogleId) // user 추가
                    .build();
            UserRoad savedUserRoad = userRoadService.saveUserRoad(userRoad);

            // 2. request 정보에 따라 UserRoadPath 엔티티에 저장하기 (trailPoints o)
            List<List<Double>> points = requestDto.getTrailPoints();
            if (points == null) {
                userRoadService.deleteUserRoad(savedUserRoad);
                ResponseDto response = ResponseDto.builder().error("산책로 경로 정보(points)가 없습니다.").build();
                return ResponseEntity.badRequest().body(response);
            }
            for (List<Double> point : points) {
                log.debug("lat, lng = " + point.toString());
                UserRoadPath tmpUserRoadPath = UserRoadPath.builder()
                        .lat(point.get(0))
                        .lng(point.get(1))
                        .userRoad(savedUserRoad)
                        .build();
                Long pathId = userRoadPathService.save(tmpUserRoadPath);// id 자동 추가
                log.debug("UserRoadPath is saved. id = {}", pathId);
            }

            // 3. request 정보에 따라 UserRoadHashTag 엔티티에 저장하기
            List<String> hashtags = requestDto.getHashtag();
            System.out.println(hashtags);
            if (hashtags != null && !hashtags.isEmpty()) {
                for (String hashtag : hashtags) {
                    UserRoadHashtag userRoadHashtag = UserRoadHashtag.builder()
                            .userRoad(userRoad)
                            .hashtag(hashtag)
                            .build();
                    Long hashtagId = userRoadHashtagService.save(userRoadHashtag);
                    log.debug("Hashtag is saved. id = " + hashtagId + ". hashtag = " + hashtag);
                }
            }

            // 사용자의 모든 산책로 응답 (userRoadPath 제외. path는 새로 요청 했을 시에만)
            List<UserRoad> userRoads = userRoadService.findByUserGoogleId(googleId); // 인증된 사용자의 산책로 리스트

            // UserRoad에 Hashtag 추가해서 응답해주기
            List<UserRoadResponseDto> responseDtos = userRoadHashtagService.addHashtagRoads(userRoads);
            ResponseDto<UserRoadResponseDto> response = ResponseDto.<UserRoadResponseDto>builder()
                    .data(responseDtos)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ResponseDto response = ResponseDto.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserRoad(@AuthenticationPrincipal String googleId,
                                            @PathVariable Long id) {
        try {
            Long userId = userService.getUserIdByGoogleId(googleId);
            UserRoad userRoad = userRoadService.findById(id);
            if (userId != userRoad.getUser().getId()) {
                ResponseDto response = ResponseDto.builder()
                        .error("요청한 산책로 id가 로그인한 당신의 산책로가 아닙니다. roadId = " + id)
                        .build();
                return ResponseEntity.badRequest().body(response);
            }

            List<UserRoad> userRoads = userRoadService.deleteUserRoad(userRoad);

            List<UserRoadResponseDto> responseDtos = userRoadHashtagService.addHashtagRoads(userRoads);

            ResponseDto<UserRoadResponseDto> response = ResponseDto.<UserRoadResponseDto>builder()
                    .data(responseDtos)
                    .build();

            return ResponseEntity.ok().body(response);

        } catch (Exception e) {
            ResponseDto response = ResponseDto.builder().error(e.getMessage()).build();
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/paths")
    public ResponseEntity<?> findByUserRoadId(@AuthenticationPrincipal String googleId,
                                                       @PathVariable Long id) {
        List<UserRoad> foundUserRoads = userRoadService.findByUserGoogleId(googleId);
        List<Long> roadIds = foundUserRoads.stream().map(road -> road.getId()).collect(Collectors.toList());

        if (!roadIds.contains(id)) {
            ResponseDto response = ResponseDto.builder().error("접근 가능한 산책로가 아닙니다. id=" + id).build();
            return ResponseEntity.badRequest().body(response);
        }

        ResponseDto<UserRoadPathResponse> response = ResponseDto.<UserRoadPathResponse>builder()
                .data(userRoadPathService.findByUserRoadId(id))
                .build();
        return ResponseEntity.ok(response);

    }

    @GetMapping("/{id}/share")
    public ResponseEntity<?> shareUserRoad(@AuthenticationPrincipal String googleId,
                                           @PathVariable Long id) {
        List<UserRoad> foundUserRoads = userRoadService.findByUserGoogleId(googleId);
        List<Long> roadIds = foundUserRoads.stream().map(road -> road.getId()).collect(Collectors.toList());

        if (!roadIds.contains(id)) {
            ResponseDto response = ResponseDto.builder().error("접근 가능한 산책로가 아닙니다. id=" + id).build();
            return ResponseEntity.badRequest().body(response);
        }

        UserRoad userRoad = userRoadService.updateShared(id);

        ResponseDto response = ResponseDto.builder()
                .data(Arrays.asList("shared changed. " + "shared: " + userRoad.isShared()))
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nearRoads")
    public ResponseEntity<?> getNearRoads(@RequestParam double lat, @RequestParam double lng) {
        List<UserRoadNearInterface> nearRoads = userRoadService.findNearUserRoads(lat, lng);
        if (nearRoads.isEmpty()) {
            ResponseDto response = ResponseDto.builder()
                    .error("주변에 공유된 사용자 산책로가 없습니다.")
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
        ResponseDto response = ResponseDto.<UserRoadNearInterface>builder()
                .data(nearRoads)
                .build();
        return ResponseEntity.ok(response);
    }

}
