package com.robotgryphon.compactcrafting.field;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.robotgryphon.compactcrafting.CompactCrafting;
import dev.compactmods.compactcrafting.api.field.IActiveWorldFields;
import dev.compactmods.compactcrafting.api.field.IMiniaturizationField;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

public class ActiveWorldFields implements IActiveWorldFields {

    private World level;

    /**
     * Holds a set of miniaturization fields that are active, referenced by their center point.
     */
    private final HashMap<BlockPos, IMiniaturizationField> fields;
    private final HashMap<BlockPos, LazyOptional<IMiniaturizationField>> laziness;

    public ActiveWorldFields() {
        this.fields = new HashMap<>();
        this.laziness = new HashMap<>();
    }

    public ActiveWorldFields(World level) {
        this();
        this.level = level;
    }


    @Override
    public void setLevel(World level) {
        this.level = level;
    }

    @Override
    public Stream<IMiniaturizationField> getFields() {
        return fields.values().stream();
    }

    public void tickFields() {
        Set<IMiniaturizationField> loaded = fields.values().stream()
                .filter(IMiniaturizationField::isLoaded)
                .collect(Collectors.toSet());

        if(loaded.isEmpty())
            return;

        CompactCrafting.LOGGER.trace("Loaded count ({}): {}", level.dimension().location(), loaded.size());
        loaded.forEach(IMiniaturizationField::tick);
    }

    public void registerField(IMiniaturizationField field) {
        field.setLevel(level);

        BlockPos center = field.getCenter();
        fields.put(center, field);

        LazyOptional<IMiniaturizationField> lazy = LazyOptional.of(() -> field);
        laziness.put(center, lazy);

        lazy.addListener(lo -> {
            lo.ifPresent(this::unregisterField);
        });
    }

    public void unregisterField(BlockPos center) {
        fields.remove(center);
        laziness.remove(center);
    }

    public void unregisterField(IMiniaturizationField field) {
        BlockPos center = field.getCenter();
        unregisterField(center);
    }

    public LazyOptional<IMiniaturizationField> getLazy(BlockPos center) {
        return laziness.getOrDefault(center, LazyOptional.empty());
    }

    @Override
    public Optional<IMiniaturizationField> get(BlockPos center) {
        return Optional.ofNullable(fields.getOrDefault(center, null));
    }

    @Override
    public boolean hasActiveField(BlockPos center) {
        return fields.containsKey(center);
    }

    @Override
    public Stream<IMiniaturizationField> getFields(ChunkPos chunk) {
        return fields.entrySet()
                .stream()
                .filter(p -> new ChunkPos(p.getKey()).equals(chunk))
                .map(Map.Entry::getValue);
    }
}
