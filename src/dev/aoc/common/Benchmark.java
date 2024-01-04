package dev.aoc.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotates "day" class to perform benchmarks instead of usual run. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Benchmark {
    public boolean run() default true;
    public int cycles() default 10;
    public String inputSuffix() default "\\/\\/DEFAULT\\/\\/";
    /** Use 0 to benchmark both parts */
    public int partNumber() default 0;
    public String solutionName() default "default";
}
