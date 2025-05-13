package be.steby.CoreProject.il.device;

import be.steby.CoreProject.bll.exceptions.DeviceTrustLevelException;
import be.steby.CoreProject.bll.utils.device.DeviceSecurityEvaluator;
import be.steby.CoreProject.dl.enums.DeviceTrustLevel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class DeviceTrustLevelAspect {

    private final DeviceSecurityEvaluator deviceSecurityEvaluator;



    @Around("@annotation(be.steby.CoreProject.il.device.RequiresDeviceTrustLevel) || @within(be.steby.CoreProject.il.device.RequiresDeviceTrustLevel)")
    public Object checkDeviceTrustLevel(ProceedingJoinPoint joinPoint) throws Throwable {
        // Obtenir l'authentification actuelle
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Obtenir la requête HTTP
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        // Déterminer le niveau de confiance requis à partir de l'annotation
        DeviceTrustLevel requiredLevel = getRequiredTrustLevel(joinPoint);

        // Vérifier si l'appareil a le niveau de confiance requis
        if (!deviceSecurityEvaluator.hasRequiredTrustLevel(authentication, request, requiredLevel)) {
            throw new DeviceTrustLevelException("Device trust level insufficient for this operation");
        }

        return joinPoint.proceed();
    }

    private DeviceTrustLevel getRequiredTrustLevel(ProceedingJoinPoint joinPoint) {
        // Essayer d'obtenir l'annotation au niveau de la méthode
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequiresDeviceTrustLevel methodAnnotation = method.getAnnotation(RequiresDeviceTrustLevel.class);
        if (methodAnnotation != null) {
            return methodAnnotation.value();
        }

        // Si non trouvé, essayer au niveau de la classe
        RequiresDeviceTrustLevel classAnnotation = method.getDeclaringClass().getAnnotation(RequiresDeviceTrustLevel.class);
        if (classAnnotation != null) {
            return classAnnotation.value();
        }

        // Par défaut
        return DeviceTrustLevel.BASIC;
    }
}