package net.kjp12.plymouth.debug.mixins.database;// Created 2021-12-06T23:42:29

import net.kjp12.plymouth.database.DatabaseHelper;
import net.kjp12.plymouth.database.Plymouth;
import net.kjp12.plymouth.debug.database.PlymouthLoggingDelegate;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author KJP12
 * @since ${version}
 **/
@Mixin(DatabaseHelper.class)
public class MixinDatabaseHelper {
    @Shadow
    public static Plymouth database;

    @Redirect(method = "<clinit>", at = @At(value = "FIELD", target = "Lnet/kjp12/plymouth/database/DatabaseHelper;database:Lnet/kjp12/plymouth/database/Plymouth;", opcode = Opcodes.PUTSTATIC))
    private static void debug$redirect$database$store(Plymouth plymouth) {
        database = new PlymouthLoggingDelegate(plymouth);
    }
}
