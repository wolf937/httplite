package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Params
 *
 * @author alexclin  16/1/20 21:46
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Params {
    boolean encoded() default false;
}
