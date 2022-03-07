package gay.ampflower.helium.mixins;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import net.minecraft.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(TagKey.class)
public class MixinTagKey {
    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Redirect(
            method = "<clinit>",
            at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Interners;newStrongInterner()Lcom/google/common/collect/Interner;"),
            require = 0, expect = 1)
    private static Interner<?> plymouth$weakInterner() {
        return Interners.newWeakInterner();
    }
}
