package net.kjp12.plymouth.tracker.glue;// Created 2021-13-06T06:41:49

import net.kjp12.plymouth.database.Target;

/**
 * @author KJP12
 * @since ${version}
 **/
public interface ITargetInjectable {
    void plymouth$injectTarget(Target target);
}
