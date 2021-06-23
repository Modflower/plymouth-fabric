package gay.ampflower.plymouth.database.cache;

import java.lang.annotation.Target;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Target({})
public @interface Match {
    int table() default 0;

    String primary() default "";

    String secondary() default "";
}
