package net.kjp12.plymouth.debug.misc;// Created 2022-03-12T22:53:20

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

import java.io.IOError;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;


/**
 * @author KJP12
 * @since ${version}
 **/
public class MiscDebugClient {

    public static void initialise() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var misc = literal("misc")
                    .then(literal("regdump")
                            .executes(src -> {
                                try {
                                    var regpath = Path.of("registry-" + System.nanoTime());
                                    Files.createDirectories(regpath);

                                    var iterableRegistries = Registry.REGISTRIES.getIndexedEntries();

                                    System.err.print("""
                                            \s=== Plymouth: Debug - Start of CLIENT Registry Dump ===
                                            The contents should be usable as a CSV file separated by start & end headers.
                                                                                    
                                                                                    
                                            """);

                                    int or = 0;
                                    int tor = 0;
                                    for (int i = 0, l = iterableRegistries.size(); i < l; i++) {
                                        var registry = iterableRegistries.get(i);
                                        if (registry == null) {
                                            System.err.printf("Registry @ ID %d missing. Continuing.\n\n", i);
                                            or++;
                                            l++;
                                            continue;
                                        }
                                        var value = registry.value();

                                        if (value == null) {
                                            System.err.printf("Registry `%s` @ ID %d is missing an implementation. Continuing.\n\n", registry.getKey(), i);
                                            continue;
                                        }

                                        System.err.printf(" === Registry `%s` (`%s` -> class: `%s`, hash: %08x) @ ID %d ===\n"
                                                        + "\"Registry ID\",\t\"Identifier\",\t\"JVM Backing Class\",\t\"JVM Identity Hash\"\n",
                                                value, registry.getKey(), value.getClass(), System.identityHashCode(value), i);

                                        var iterableRegistry = value.getIndexedEntries();

                                        var path = regpath.resolve(registry.getKey().map(key -> key.getValue().toString()).orElse("plymouth-debug/unknown-registry").replace(':', '/') + ".csv");

                                        Files.createDirectories(path.getParent());

                                        int xor = 0;
                                        try (var fos = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                                             var ps = new PrintStream(fos)) {
                                            ps.println("\"Registry ID\",\t\"Identifier\",\t\"JVM Backing Class\",\t\"JVM Identity Hash\"");

                                            for (int x = 0, y = iterableRegistry.size(); x < y; x++) {
                                                var entry = iterableRegistry.get(x);

                                                if (entry == null) {
                                                    System.err.printf("\"%d\",\t\"NOT PRESENT\",\t\"NOT PRESENT\",\t\"NOT PRESENT\"\n", x);
                                                    ps.printf("\"%d\",\t\"NOT PRESENT\",\t\"NOT PRESENT\",\t\"NOT PRESENT\"\n", x);
                                                    xor++;
                                                    y++;
                                                } else {
                                                    var subValue = entry.value();
                                                    System.err.printf("\"%d\",\t\"%s\",\t\"%s\",\t\"%08x\"\n",
                                                            x,
                                                            entry.getKey().map(RegistryKey::toString).orElse("MISSING"),
                                                            subValue == null ? "MISSING" : subValue.getClass(),
                                                            subValue == null ? -1 : System.identityHashCode(subValue));
                                                    ps.printf("\"%d\",\t\"%s\",\t\"%s\",\t\"%08x\"\n",
                                                            x,
                                                            entry.getKey().map(RegistryKey::toString).orElse("MISSING"),
                                                            subValue == null ? "MISSING" : subValue.getClass(),
                                                            subValue == null ? -1 : System.identityHashCode(subValue));

                                                    {
                                                        int mapBack = ((Registry<Object>) value).getRawId(subValue);
                                                        if (mapBack != x) {
                                                            throw new AssertionError("Expected raw ID " + x + ", got " + mapBack);
                                                        }
                                                    }

                                                    Identifier expected = entry.getKey().orElseThrow().getValue();

                                                    {
                                                        Identifier mapBack = ((Registry<Object>) value).getId(subValue);
                                                        if (mapBack != expected) {
                                                            throw new AssertionError("Expected ID " + expected + ", got " + mapBack);
                                                        }
                                                    }

                                                    {
                                                        Object obj = value.get(expected);
                                                        if (obj != subValue) {
                                                            throw new AssertionError("Identifier: Expected object " + subValue + ", got " + obj);
                                                        }
                                                    }

                                                    {
                                                        Object obj = value.get(x);
                                                        if (obj != subValue) {
                                                            throw new AssertionError("Raw ID: Expected object " + subValue + ", got " + obj);
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        tor += xor;
                                        System.err.printf(" === End of Registry `%s` (Over-read by %d entries) ===\n\n", registry.getKey(), xor);
                                    }

                                    System.err.printf(" === End of Registry Dump, over-read by %d entries and %d sub-entries.\n\n", or, tor);
                                } catch (IOException ioe) {
                                    throw new IOError(ioe);
                                }

                                return Command.SINGLE_SUCCESS;
                            }));

            dispatcher.register(literal("pdbc").then(misc));
        });
    }
}
