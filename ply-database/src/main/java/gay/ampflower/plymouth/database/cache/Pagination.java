package gay.ampflower.plymouth.database.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Pagination {
    Value sort() default @Value({});

    String limit() default "";

    String offset() default "";
}
