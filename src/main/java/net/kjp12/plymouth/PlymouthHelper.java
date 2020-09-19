package net.kjp12.plymouth;

import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

final class PlymouthHelper {
    static final Set<Property<?>> bannedProperties = Set.of(Properties.WATERLOGGED);

    static <K, V> String toJson(Map<K, V> map, Set<K> banned, Function<K, String> kString, Function<V, String> vString) {
        var properties = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (banned.contains(k)) return;
            properties.append('"').append(kString.apply(k).replace("\"", "\\\"").replace("\\", "\\\\")).append("\":\"").append(vString.apply(v).replace("\"", "\\\"").replace("\\", "\\\\")).append("\",");
        });
        return (properties.length() == 1 ? properties.append('}') : properties.replace(properties.length() - 1, properties.length(), "}")).toString();
    }
}
