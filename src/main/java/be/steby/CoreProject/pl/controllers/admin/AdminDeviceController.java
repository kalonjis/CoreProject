package be.steby.CoreProject.pl.controllers.admin;


import be.steby.CoreProject.bll.services.AdminService;
import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.dl.entities.User;
import be.steby.CoreProject.pl.models.device.DeviceDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
//@RequiresDeviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
@RequestMapping("/api/admin/device")
public class AdminDeviceController {

    private final DeviceService deviceService;
    private final AdminService adminService;



    @GetMapping("/list/user/{id}")
    public ResponseEntity<List<DeviceDTO>> getUserDevices(@PathVariable Long id) {
        User user = adminService.getUserById(id);
        List<DeviceDTO> devicesDTO = deviceService.getUserDevice(user).stream()
                .map(DeviceDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(devicesDTO);
    }


    @GetMapping("/count-total")
    public ResponseEntity<Map<String,Long>>getTotalDevices(){
        Map<String, Long> response = new HashMap<>();
        Long totalDevices = deviceService.getTotalDevices();
        response.put("totalDevices", totalDevices);
        return ResponseEntity.ok(response);
    }





}

