package me.khajiitos.servercountryflags.common.config;

public @interface Constraints {
    int minValue() default 0;
    int maxValue() default 100;
}
