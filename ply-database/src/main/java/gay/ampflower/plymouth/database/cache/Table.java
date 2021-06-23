package gay.ampflower.plymouth.database.cache;

import java.lang.annotation.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Tables.class)
public @interface Table {
    int table() default 0;

    String value();

    Match match() default @Match;
}
