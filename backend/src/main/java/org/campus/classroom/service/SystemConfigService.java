package org.campus.classroom.service;

import org.campus.classroom.vo.SystemConfigVO;

import java.util.List;

public interface SystemConfigService {
    List<SystemConfigVO> listConfigs(String category);

    SystemConfigVO updateConfig(String key, String value);

    int getMaxSingleReservationMinutes();

    int getDailyReservationLimitMinutes();

    int getCheckInEarlyMinutes();

    int getCheckInGraceMinutes();

    int getCheckInRewardScore();

    int getNoShowDeductionScore();

    int getSeatReservationAdvanceHours(Integer creditScore);
}
