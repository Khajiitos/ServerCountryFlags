package me.khajiitos.servercountryflags.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigEntry {
    String name() default "";
    String description() default "";
    String[] stringValues() default {};
    String configCategory() default "Miscellaneous";
    Constraints[] constraints() default {};
}
