package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TienQuyetValidator {

    private TienQuyetValidator() {
    }

    public static void validate(String code, Set<String> prerequisites, Map<String, Set<String>> graph)
            throws BusinessException {
        if (!graph.containsKey(code) || !graph.keySet().containsAll(prerequisites)) {
            throw new BusinessException(ErrorCode.KHONG_TIM_THAY, "Không tìm thấy môn học hoặc môn tiên quyết");
        }
        Set<String> visited = new HashSet<>();
        ArrayDeque<String> pending = new ArrayDeque<>(prerequisites);
        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (current.equals(code)) {
                throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE,
                        "Môn tiên quyết không được tự tham chiếu hoặc tạo thành vòng");
            }
            if (visited.add(current)) {
                for (String next : graph.getOrDefault(current, Set.of())) {
                    pending.push(next);
                }
            }
        }
    }
}
