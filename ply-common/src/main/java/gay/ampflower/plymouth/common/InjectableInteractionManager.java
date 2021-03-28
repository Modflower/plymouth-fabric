package gay.ampflower.plymouth.common;

/**
 * @author Ampflower
 * @since 0.0.0
 */
public interface InjectableInteractionManager {
    void setManager(InteractionManagerInjection manager);

    InteractionManagerInjection getManager();
}
