package be.steby.CoreProject.il.configs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class CustomAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    // Vous pouvez injecter des services ici si nécessaire
    // private final ErrorNotificationService errorNotificationService;

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... params) {
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        String paramValues = Arrays.toString(params);

        log.error("Exception non gérée dans la méthode async '{}' avec paramètres: {}",
                methodName, paramValues, throwable);

        // Classification de l'erreur selon le nom de la méthode/classe
        if (methodName.contains("Mailer")) {
            handleEmailError(throwable, methodName, params);
        } else if (methodName.contains("ActivityLog")) {
            handleLogError(throwable, methodName, params);
        } else {
            handleGenericError(throwable, methodName, params);
        }
    }

    private void handleEmailError(Throwable throwable, String methodName, Object... params) {
        log.error("Erreur lors de l'envoi d'email dans {}: {}", methodName, throwable.getMessage());
        // Vous pourriez implémenter une logique de retry ici
        // Ou notifier les administrateurs d'une autre manière
    }

    private void handleLogError(Throwable throwable, String methodName, Object... params) {
        log.error("Erreur lors de la journalisation dans {}: {}", methodName, throwable.getMessage());
        // Les erreurs de log sont moins critiques, peut-être juste les logger
    }

    private void handleGenericError(Throwable throwable, String methodName, Object... params) {
        log.error("Erreur async générique dans {}: {}", methodName, throwable.getMessage());
    }
}
