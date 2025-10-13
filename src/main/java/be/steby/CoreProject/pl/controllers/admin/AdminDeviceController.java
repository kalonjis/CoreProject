//package be.steby.CoreProject.pl.controllers.admin;
//
//
//import be.steby.CoreProject.bll.domains.admin.services.AdminService;
//import be.steby.CoreProject.bll.domains.device.services.DeviceService;
//import be.steby.CoreProject.dl.entities.User;
//import be.steby.CoreProject.pl.models.device.DeviceDTO;
//import be.steby.CoreProject.pl.models.device.DeviceTrustLevelForm;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//
//
//@RestController
//@RequiredArgsConstructor
//@PreAuthorize("hasAuthority('ADMIN')")
//@RequestMapping("/api/admin/device")
//public class AdminDeviceController {
//    private final DeviceService deviceService;
//    private final AdminService adminService;
//
//    // Obtenir tous les appareils d'un utilisateur spécifique
//    @GetMapping("/list/user/{userId}")
//    public ResponseEntity<List<DeviceDTO>> getUserDevices(@PathVariable Long userId) {
//        User user = adminService.getUserById(userId);
//        List<DeviceDTO> devicesDTO = deviceService.getUserDevice(user)
//                .stream()
//                .map(DeviceDTO::fromEntity)
//                .toList();
//        return ResponseEntity.ok(devicesDTO);
//    }
//
//    // Obtenir les détails d'un appareil spécifique (n'importe quel utilisateur)
//    @GetMapping("/{deviceId}")
//    public ResponseEntity<DeviceDTO> getDeviceById(@PathVariable Long deviceId) {
//        DeviceDTO deviceDTO = DeviceDTO.fromEntity(deviceService.getDeviceById(deviceId));
//        return ResponseEntity.ok(deviceDTO);
//    }
//
//    // Obtenir le nombre total d'appareils dans le système
//    @GetMapping("/count-total")
//    public ResponseEntity<Map<String, Long>> getTotalDevices() {
//        Map<String, Long> response = new HashMap<>();
//        response.put("totalDevices", deviceService.getTotalDevices());
//        return ResponseEntity.ok(response);
//    }
//
//    // Forcer la mise à jour du niveau de confiance d'un appareil
//    @PatchMapping("/force-update-trust-level/{deviceId}")
//    public ResponseEntity<Void> forceUpdateDeviceTrustLevel(
//            @PathVariable Long deviceId,
//            @RequestBody DeviceTrustLevelForm form,
//            HttpServletRequest request) {
//        deviceService.updateTrustLevel(deviceId, form.deviceTrustLevel(), request);
//        return ResponseEntity.noContent().build();
//    }
//
//    // Blacklister un appareil
//    @PatchMapping("/blacklist/{deviceId}")
//    public ResponseEntity<Void> blacklistDevice(@PathVariable Long deviceId) {
//        // Logique pour blacklister un appareil
//        return ResponseEntity.noContent().build();
//    }
//
//    // Retirer un appareil de la blacklist
//    @PatchMapping("/whitelist/{deviceId}")
//    public ResponseEntity<Void> whitelistDevice(@PathVariable Long deviceId) {
//        // Logique pour retirer un appareil de la blacklist
//        return ResponseEntity.noContent().build();
//    }
//
//    // Obtenir les statistiques d'appareils
//    @GetMapping("/stats")
//    public ResponseEntity<Map<String, Object>> getDeviceStatistics() {
//        // Logique pour obtenir des statistiques sur les appareils
//        Map<String, Object> stats = new HashMap<>();
//        stats.put("totalDevices", deviceService.getTotalDevices());
//        stats.put("confirmedDevices", 0); // Implémentation à compléter
//        stats.put("blacklistedDevices", 0); // Implémentation à compléter
//        return ResponseEntity.ok(stats);
//    }
//
//    // Forcer la déconnexion d'un appareil
//    @DeleteMapping("/force-disconnect/{deviceId}")
//    public ResponseEntity<Void> forceDisconnectDevice(@PathVariable Long deviceId) {
//        // Logique pour forcer la déconnexion d'un appareil
//        return ResponseEntity.noContent().build();
//    }
//}
//
