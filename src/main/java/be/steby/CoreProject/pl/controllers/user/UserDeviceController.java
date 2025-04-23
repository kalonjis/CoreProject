package be.steby.CoreProject.pl.controllers.user;

import be.steby.CoreProject.bll.services.DeviceService;
import be.steby.CoreProject.pl.models.device.DeviceDTO;
import be.steby.CoreProject.pl.models.device.DeviceTrustLevelForm;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/device")
@RequiredArgsConstructor
public class UserDeviceController {

    private final DeviceService deviceService;

    @GetMapping("/current")
    public ResponseEntity<DeviceDTO> getCurrentDeviceInfo(HttpServletRequest request) {
        DeviceDTO deviceDTO = DeviceDTO.fromEntity(deviceService.detectCurrentDevice(request)) ;
        return ResponseEntity.ok(deviceDTO);
    }


    @GetMapping("/my-device/{deviceId}")
    public ResponseEntity<DeviceDTO> getMyDevice(@PathVariable Long deviceId){
        DeviceDTO deviceDTO = DeviceDTO.fromEntity(deviceService.getMyDevice(deviceId));
        return ResponseEntity.ok(deviceDTO);
    }


    @GetMapping("/my-devices-list")
    public ResponseEntity<List<DeviceDTO>> getDeviceList(HttpServletRequest request) {
        List<DeviceDTO> deviceDTOs = deviceService.getMyDeviceList()
                .stream()
                .map(DeviceDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(deviceDTOs);
    }


    @PatchMapping("/confirm")
    public ResponseEntity<Map<String, Object>> confirmDevice(@RequestParam String token){
        Long deviceId = deviceService.confirmDevice(token).getId();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Device confirmed successfully");
        response.put("deviceId", deviceId);

        return ResponseEntity.ok(response);
    }


    @PatchMapping("/reject")
    public ResponseEntity<Void> rejectDevice(@RequestParam String token){
        deviceService.rejectDevice(token);
        return ResponseEntity.noContent().build();
    }

    // Demander un lien de confirmation pour un appareil
    @PostMapping("/request-confirmation")
    public ResponseEntity<Map<String, String>> requestConfirmationLink(
            HttpServletRequest request) {
        deviceService.requestConfirmationLink(request);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Un email avec le lien de confirmation a été envoyé à votre adresse");
        return ResponseEntity.ok(response);
    }

    // Modifier le niveau de confiance d'un appareil (limité à ses propres appareils)
    @PatchMapping("/update-trust-level/{deviceId}")
    public ResponseEntity<Void> updateMyDeviceTrustLevel(
            @PathVariable Long deviceId,
            @RequestBody DeviceTrustLevelForm form) {
        deviceService.updateTrustLevel(deviceId, form.deviceTrustLevel());
        return ResponseEntity.noContent().build();
    }

    // Déconnecter un appareil spécifique
    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> disconnectDevice(@PathVariable Long deviceId) {
        // Logique pour déconnecter un appareil spécifique
        return ResponseEntity.noContent().build();
    }

    // Déconnecter tous les autres appareils
    @PostMapping("/disconnect-all-others")
    public ResponseEntity<Void> disconnectAllOtherDevices(HttpServletRequest request) {
        // Logique pour déconnecter tous les appareils sauf celui actuel
        return ResponseEntity.noContent().build();
    }
}