package gay.ampflower.plymouth.debug.mixins.database;

import gay.ampflower.plymouth.database.DatabaseHelper;
import gay.ampflower.plymouth.database.Plymouth;
import gay.ampflower.plymouth.debug.database.PlymouthLoggingDelegate;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Ampflower
 * @since ${version}
 **/
@Mixin(DatabaseHelper.class)
public class MixinDatabaseHelper {
    @Shadow
    public static Plymouth database;

    @Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Lgay/ampflower/plymouth/database/DatabaseHelper;database:Lgay/ampflower/plymouth/database/Plymouth;", opcode = Opcodes.PUTSTATIC))
    private static void debug$redirect$database$store(Plymouth plymouth) {
        database = new PlymouthLoggingDelegate(plymouth);
    }
}
