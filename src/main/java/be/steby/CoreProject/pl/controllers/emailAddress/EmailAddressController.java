package be.steby.CoreProject.pl.controllers.emailAddress;

import be.steby.CoreProject.bll.domains.emailAddress.services.EmailAddressService;
import be.steby.CoreProject.pl.models.user.ChangeEmailForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/email-address-change/")
public class EmailAddressController {

    private final EmailAddressService emailAddressService;

    @PostMapping("request")
    public ResponseEntity<Void> changeEmailRequest(@Valid @RequestBody ChangeEmailForm form, HttpServletRequest request){
        emailAddressService.changeEmailRequest(form, request);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("cancel")
    public ResponseEntity<Void>cancelEmailChange(@RequestParam String token, HttpServletRequest request){
        emailAddressService.cancelEmailChange(token, request);
        return ResponseEntity.noContent().build();
    }


    @PatchMapping("verification")
    public ResponseEntity<Void>changeEmailVerification(@RequestParam String token, HttpServletRequest request){
        emailAddressService.changeEmailVerification(token, request);
        return ResponseEntity.noContent().build();
    }


    @PutMapping("confirmation")
    public ResponseEntity<Void>changeEmailConfirmation(@RequestParam String token, HttpServletRequest request){
        emailAddressService.confirmEmail(token, request);
        return ResponseEntity.noContent().build();
    }
}
