package be.steby.CoreProject.pl.controllers.admin;


import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.il.device.RequiresDeviceTrustLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;



@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
//@RequiresDeviceTrustLevel(DeviceTrustLevel.HIGHLY_TRUSTED)
@RequestMapping("/api/admin/device")
public class AdminDeviceController {

    private final DeviceService deviceService;



//    @GetMapping("/device/list/user/{id}")
//    public ResponseEntity<List<Device>> getUserDevices(@PathVariable Long id) {
//        List<Device> devices = deviceService.getUserDevices(id);
//        return ResponseEntity.ok(devices);
//    }


    @GetMapping("/count-total")
    public ResponseEntity<Map<String,Long>>getTotalDevices(){
        Map<String, Long> response = new HashMap<>();
        Long totalDevices = deviceService.getTotalDevices();
        response.put("totalDevices", totalDevices);
        return ResponseEntity.ok(response);
    }





}

