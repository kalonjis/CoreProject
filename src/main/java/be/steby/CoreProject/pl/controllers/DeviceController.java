package be.steby.CoreProject.pl.controllers;


import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import be.steby.CoreProject.il.device.RequiresDeviceTrustLevel;
import be.steby.CoreProject.pl.models.device.DeviceDTO;
import be.steby.CoreProject.pl.models.device.DeviceTrustLevelForm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    @GetMapping("/current")
    public ResponseEntity<DeviceDTO> getCurrentDeviceInfo(HttpServletRequest request) {
        DeviceDTO deviceDTO = DeviceDTO.fromEntity(deviceService.detectCurrentDevice(request)) ;
        return ResponseEntity.ok(deviceDTO);
    }


    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceDTO> getMyDevice(@PathVariable Long deviceId){
        DeviceDTO deviceDTO = DeviceDTO.fromEntity(deviceService.getMyDevice(deviceId));
        return ResponseEntity.ok(deviceDTO);
    }


    @GetMapping("/my-list")
    public ResponseEntity<List<DeviceDTO>> getDeviceList(HttpServletRequest request) {
        List<DeviceDTO> deviceDTOs = deviceService.getMyDeviceList()
                .stream()
                .map(DeviceDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(deviceDTOs);
    }


    @PatchMapping("/update-trust-level/{deviceId}")
    public ResponseEntity<Void> updateTrustLevel(@PathVariable Long deviceId, @RequestBody DeviceTrustLevelForm form) {
        deviceService.updateTrustLevel(deviceId, form.deviceTrustLevel() );
        return ResponseEntity.noContent().build();
    }



    @PatchMapping("/confirm")
    public ResponseEntity<Void> confirmDevice(@RequestParam String token){
        deviceService.confirmDevice(token);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("/reject")
    public ResponseEntity<Void> rejectDevice(@RequestParam String token){
        deviceService.rejectDevice(token);
        return ResponseEntity.noContent().build();
    }
}

