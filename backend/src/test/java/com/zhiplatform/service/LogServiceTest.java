package com.zhiplatform.service;

import com.zhiplatform.entity.mysql.OperationLog;
import com.zhiplatform.repository.mysql.OperationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private OperationLogRepository logRepo;

    private LogService logService;

    @BeforeEach
    void setUp() {
        logService = new LogService(logRepo);
    }

    @Test
    void log_savesOperationLog() {
        logService.log(1L, "admin", "登录", "用户", "127.0.0.1");
        verify(logRepo).save(org.mockito.ArgumentMatchers.any(OperationLog.class));
    }

    @Test
    void getAllLogs_returnsList() {
        when(logRepo.findAllByOrderByCreateTimeDesc()).thenReturn(List.of(new OperationLog(), new OperationLog()));
        assertEquals(2, logService.getAllLogs().size());
    }

    @Test
    void getUserLogs_returnsUserLogs() {
        when(logRepo.findByUserIdOrderByCreateTimeDesc(1L)).thenReturn(List.of(new OperationLog()));
        assertEquals(1, logService.getUserLogs(1L).size());
    }
}
