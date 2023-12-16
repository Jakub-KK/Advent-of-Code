package dev.aoc.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotates solution parsing methods */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SolutionParser {
    public int partNumber();
    public String solutionName() default "default";
}
