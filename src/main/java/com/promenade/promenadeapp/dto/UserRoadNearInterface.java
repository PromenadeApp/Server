package com.promenade.promenadeapp.dto;

public interface UserRoadNearInterface {

    double getD(); // 현위치로부터의 거리

    Long getId();

    Long getUserId();

    String getTrail_name();

    String getDescription();

    double getDistance(); // 산책로 총 길이

    String getPicture();

    String getStart_addr();

    double getStart_lat();

    double getStart_lng();

}
