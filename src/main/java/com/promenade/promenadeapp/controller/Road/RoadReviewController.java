package com.promenade.promenadeapp.controller.Road;

import com.promenade.promenadeapp.domain.Road.Road;
import com.promenade.promenadeapp.domain.Road.RoadReview;
import com.promenade.promenadeapp.domain.User.User;
import com.promenade.promenadeapp.dto.ResponseDto;
import com.promenade.promenadeapp.dto.Road.RoadReviewRequestDto;
import com.promenade.promenadeapp.dto.Road.RoadReviewResponseDto;
import com.promenade.promenadeapp.service.Road.RoadReviewService;
import com.promenade.promenadeapp.service.Road.RoadService;
import com.promenade.promenadeapp.service.User.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/road/review")
public class RoadReviewController {

    private final RoadReviewService roadReviewService;

    private final UserService userService;

    private final RoadService roadService;

    @GetMapping("/{roadId}")
    public ResponseEntity<?> findByRoadId(@PathVariable Long roadId) {
        List<RoadReview> roadReviews = roadReviewService.findByRoadId(roadId);
        if (roadReviews.isEmpty()) {
            ResponseDto response = ResponseDto.builder()
                    .error("해당 산책로의 리뷰가 없습니다.")
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
        List<RoadReviewResponseDto> responseDtos = roadReviews.stream().map(RoadReviewResponseDto::new).collect(Collectors.toList());
        ResponseDto response = ResponseDto.<RoadReviewResponseDto>builder()
                .data(responseDtos)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roadId}")
    public ResponseEntity postReview(@AuthenticationPrincipal String googleId,
                                     @PathVariable Long roadId, @RequestBody RoadReviewRequestDto requestDto) {
        User userByGoogleId = userService.findByGoogleId(googleId);
        Road roadById = roadService.findById(roadId);

        RoadReview roadReview = RoadReview.builder()
                .id(null) // save하면서 자동 저장
                .score(requestDto.getScore())
                .content(requestDto.getContent())
                .pngPath(requestDto.getPng_path())
                .user(userByGoogleId)
                .road(roadById)
                .build();
        Long reviewId = roadReviewService.save(roadReview);
        log.info("review saved. id="+reviewId);

        return findByRoadId(roadId);
    }
}
