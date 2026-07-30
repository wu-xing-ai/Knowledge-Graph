package com.zhiplatform.service;

import com.zhiplatform.entity.mysql.OperationLog;
import com.zhiplatform.repository.mysql.OperationLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogService {

    private final OperationLogRepository logRepo;

    public LogService(OperationLogRepository logRepo) {
        this.logRepo = logRepo;
    }

    public void log(Long userId, String username, String action, String module, String ip) {
        OperationLog log = new OperationLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setModule(module);
        log.setIp(ip);
        logRepo.save(log);
    }

    public List<OperationLog> getAllLogs() {
        return logRepo.findAllByOrderByCreateTimeDesc();
    }

    public List<OperationLog> getUserLogs(Long userId) {
        return logRepo.findByUserIdOrderByCreateTimeDesc(userId);
    }
}
