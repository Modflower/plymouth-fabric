package net.kjp12.plymouth.common;// Created 2021-03-27T22:44:51

/**
 * @author Ampflower
 * @since 0.0.0
 */
public interface InjectableInteractionManager {
    void setManager(InteractionManagerInjection manager);

    InteractionManagerInjection getManager();
}
