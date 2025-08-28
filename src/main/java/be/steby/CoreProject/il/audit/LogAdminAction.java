package be.steby.CoreProject.il.audit;

import be.steby.CoreProject.dl.enums.actionLogTypes.ActionLogType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

///**
// * Annotation pour marquer les méthodes dont l'exécution doit être journalisée
// * comme une action administrative.
// */
//@Target(ElementType.METHOD)
//@Retention(RetentionPolicy.RUNTIME)
//public @interface LogAdminAction {
//
//    /**
//     * Type d'action à journaliser
//     */
//    ActionLogType actionType() default ActionLogType.ADMIN_USER_UPDATED;
//
//    /**
//     * Description de l'action (facultative)
//     */
//    String description() default "";
//}