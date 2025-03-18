package be.steby.CoreProject.il.device;

import be.steby.CoreProject.dl.enums.DeviceTrustLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresDeviceTrustLevel {
    DeviceTrustLevel value() default DeviceTrustLevel.BASIC;
}
