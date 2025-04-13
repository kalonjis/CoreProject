package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.dl.entities.ConnectionLog;
import be.steby.CoreProject.dl.entities.Device;
import be.steby.CoreProject.dl.entities.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ConnectionLogService {

    ConnectionLog logLoginAttempt(
            User user,
            Device device,
            boolean successful,
            String failureReason,
            HttpServletRequest request
    );


    ConnectionLog logLogout(User user, Device device, HttpServletRequest request);


    ConnectionLog logSecurityAction(
            User user,
            Device device,
            String actionType,
            boolean successful,
            String details,
            HttpServletRequest request
    );

    Page<ConnectionLog> getUserConnectionHistory(User user, Pageable pageable);


    List<ConnectionLog> getRecentLoginAttempts(User user);

    void cleanupOldLogs();




}
