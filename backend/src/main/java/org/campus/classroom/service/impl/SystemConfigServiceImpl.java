package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.entity.SystemConfig;
import org.campus.classroom.enums.CreditLevel;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.SystemConfigMapper;
import org.campus.classroom.service.SystemConfigService;
import org.campus.classroom.vo.SystemConfigVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigServiceImpl implements SystemConfigService {
    private static final String RESERVATION_MAX_SINGLE_MINUTES = "reservation.max_single_minutes";
    private static final String RESERVATION_DAILY_MAX_MINUTES = "reservation.daily_max_minutes";
    private static final String ATTENDANCE_CHECK_IN_EARLY_MINUTES = "attendance.check_in_early_minutes";
    private static final String ATTENDANCE_CHECK_IN_GRACE_MINUTES = "attendance.check_in_grace_minutes";
    private static final String CREDIT_CHECK_IN_REWARD = "credit.check_in_reward";
    private static final String CREDIT_NO_SHOW_DEDUCTION = "credit.no_show_deduction";
    private static final String CREDIT_LEVEL_A_MIN_SCORE = "credit.level_a_min_score";
    private static final String CREDIT_LEVEL_B_MIN_SCORE = "credit.level_b_min_score";
    private static final String CREDIT_LEVEL_A_ADVANCE_HOURS = "credit.level_a_advance_hours";
    private static final String CREDIT_LEVEL_B_ADVANCE_HOURS = "credit.level_b_advance_hours";
    private static final String CREDIT_LEVEL_C_ADVANCE_HOURS = "credit.level_c_advance_hours";
    private static final String UI_RESERVATION_START_TIME = "ui.reservation_start_time";
    private static final String UI_RESERVATION_END_TIME = "ui.reservation_end_time";

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public List<SystemConfigVO> listConfigs(String category) {
        return systemConfigMapper.selectList(normalize(category))
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public SystemConfigVO updateConfig(String key, String value) {
        SystemConfig config = getConfig(key);
        if (!Integer.valueOf(1).equals(config.getEditable())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该配置项不允许修改");
        }

        String normalizedValue = normalizeByType(value, config.getValueType());
        int updatedRows = systemConfigMapper.updateValue(key, normalizedValue);
        if (updatedRows != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "配置更新失败，请刷新后重试");
        }
        return toVO(getConfig(key));
    }

    @Override
    public int getMaxSingleReservationMinutes() {
        return getIntConfig(RESERVATION_MAX_SINGLE_MINUTES, 180);
    }

    @Override
    public int getDailyReservationLimitMinutes() {
        return getIntConfig(RESERVATION_DAILY_MAX_MINUTES, 540);
    }

    @Override
    public int getCheckInEarlyMinutes() {
        return getIntConfig(ATTENDANCE_CHECK_IN_EARLY_MINUTES, 10);
    }

    @Override
    public int getCheckInGraceMinutes() {
        return getIntConfig(ATTENDANCE_CHECK_IN_GRACE_MINUTES, 15);
    }

    @Override
    public int getCheckInRewardScore() {
        return getIntConfig(CREDIT_CHECK_IN_REWARD, 1);
    }

    @Override
    public int getNoShowDeductionScore() {
        return getIntConfig(CREDIT_NO_SHOW_DEDUCTION, 2);
    }

    @Override
    public int getSeatReservationAdvanceHours(Integer creditScore) {
        Map<String, SystemConfig> configMap = systemConfigMapper.selectByKeys(List.of(
                CREDIT_LEVEL_A_MIN_SCORE,
                CREDIT_LEVEL_B_MIN_SCORE,
                CREDIT_LEVEL_A_ADVANCE_HOURS,
                CREDIT_LEVEL_B_ADVANCE_HOURS,
                CREDIT_LEVEL_C_ADVANCE_HOURS
        )).stream().collect(Collectors.toMap(SystemConfig::getConfigKey, Function.identity()));

        int normalizedScore = CreditLevel.normalizeScore(creditScore);
        int levelAMinScore = getIntValue(configMap.get(CREDIT_LEVEL_A_MIN_SCORE), 85, CREDIT_LEVEL_A_MIN_SCORE);
        int levelBMinScore = getIntValue(configMap.get(CREDIT_LEVEL_B_MIN_SCORE), 60, CREDIT_LEVEL_B_MIN_SCORE);
        int levelAAdvanceHours = getIntValue(configMap.get(CREDIT_LEVEL_A_ADVANCE_HOURS), 24, CREDIT_LEVEL_A_ADVANCE_HOURS);
        int levelBAdvanceHours = getIntValue(configMap.get(CREDIT_LEVEL_B_ADVANCE_HOURS), 12, CREDIT_LEVEL_B_ADVANCE_HOURS);
        int levelCAdvanceHours = getIntValue(configMap.get(CREDIT_LEVEL_C_ADVANCE_HOURS), 6, CREDIT_LEVEL_C_ADVANCE_HOURS);

        if (normalizedScore >= levelAMinScore) {
            return levelAAdvanceHours;
        }
        if (normalizedScore >= levelBMinScore) {
            return levelBAdvanceHours;
        }
        return levelCAdvanceHours;
    }

    @Override
    public LocalTime getReservationStartTime() {
        return getTimeConfig(UI_RESERVATION_START_TIME, LocalTime.of(7, 0));
    }

    @Override
    public LocalTime getReservationEndTime() {
        return getTimeConfig(UI_RESERVATION_END_TIME, LocalTime.of(22, 30));
    }

    private int getIntConfig(String key, int defaultValue) {
        return getIntValue(systemConfigMapper.selectByKey(key), defaultValue, key);
    }

    private LocalTime getTimeConfig(String key, LocalTime defaultValue) {
        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        try {
            return LocalTime.parse(config.getConfigValue().trim());
        } catch (Exception ex) {
            log.warn("[系统配置读取失败] configKey={}, configValue={}, 使用默认值={}",
                    key, config.getConfigValue(), defaultValue);
            return defaultValue;
        }
    }

    private int getIntValue(SystemConfig config, int defaultValue, String key) {
        if (config == null || !StringUtils.hasText(config.getConfigValue())) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (NumberFormatException ex) {
            log.warn("[系统配置读取失败] configKey={}, configValue={}, 使用默认值={}",
                    key, config.getConfigValue(), defaultValue);
            return defaultValue;
        }
    }

    private SystemConfig getConfig(String key) {
        SystemConfig config = systemConfigMapper.selectByKey(key);
        if (config == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "配置项不存在");
        }
        return config;
    }

    private String normalizeByType(String value, String valueType) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "配置值不能为空");
        }
        String trimmed = value.trim();
        String safeType = valueType == null ? "STRING" : valueType.trim().toUpperCase(Locale.ROOT);
        try {
            return switch (safeType) {
                case "INT" -> String.valueOf(Integer.parseInt(trimmed));
                case "BOOLEAN" -> normalizeBoolean(trimmed);
                case "TIME" -> LocalTime.parse(trimmed).toString();
                default -> trimmed;
            };
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "配置值格式不正确");
        }
    }

    private String normalizeBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return "true";
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return "false";
        }
        throw new IllegalArgumentException("invalid boolean");
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private SystemConfigVO toVO(SystemConfig config) {
        SystemConfigVO configVO = new SystemConfigVO();
        BeanUtils.copyProperties(config, configVO);
        return configVO;
    }
}
