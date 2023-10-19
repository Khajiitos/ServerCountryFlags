package me.khajiitos.servercountryflags.common.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigEntry {
    String[] stringValues() default {};
    boolean stringValuesTranslatable() default true;
    String configCategory() default "servercountryflags.config.category.miscellaneous";
    Constraints[] constraints() default {};
}
