package io.github.cputnama11y.smallcommission.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.text.Text;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.cputnama11y.smallcommission.utils.DataResultUtils.exceptionToResult;

public class ShopHandler implements ClientEntityEvents.Load, ClientBlockEntityEvents.Load, ClientEntityEvents.Unload, ClientBlockEntityEvents.Unload, ClientCommandRegistrationCallback {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();
    private static final Codec<List<ShopData>> CODEC = ShopData.CODEC.listOf();
    private final List<ShopData> shopEntities = new ArrayList<>();

    @Override
    public void onLoad(Entity entity, ClientWorld world) {
        getShopEntity(entity).ifPresent(data -> {
            if (!shopEntities.contains(data))
                shopEntities.add(data);
        });
    }


    @Override
    public void onLoad(BlockEntity blockEntity, ClientWorld world) {
        getShopEntity(blockEntity).ifPresent(data -> {
            if (!shopEntities.contains(data))
                shopEntities.add(data);
        });
    }

    @Override
    public void onUnload(Entity entity, ClientWorld world) {
        shopEntities.removeIf(entry -> entry.entity == entity);
    }

    @Override
    public void onUnload(BlockEntity blockEntity, ClientWorld world) {
        shopEntities.removeIf(entry -> entry.blockEntity == blockEntity);
    }

    @Override
    public void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess
            registryAccess) {
        final var output = FabricLoader.getInstance().getGameDir().resolve("./output.json");
        var mainNode = ClientCommandManager.literal("shop-logger")
                .executes(context -> {
                    exceptionToResult(
                            () -> new FileWriter(output.toFile())
                    ).flatMap(
                                    writer -> CODEC.encodeStart(JsonOps.INSTANCE, shopEntities)
                                            .flatMap(jsonElement -> exceptionToResult(
                                                    () -> GSON.toJson(jsonElement, writer)
                                            ))
                                            .ifError(ignored -> exceptionToResult(writer::close).mapError(err -> "Error Closing File: " + err).getOrThrow())
                                            .ifSuccess(ignored -> exceptionToResult(writer::close).mapError(err -> "Error Closing File: " + err).getOrThrow())
                            )
                            .ifError(System.out::println)
                            .ifError(err -> context.getSource().sendFeedback(Text.literal(err.message())))
                            .ifSuccess(ignored -> context.getSource().sendFeedback(Text.literal("Successfully wrote to " + shopEntities.size() + "shops" + output)));
                    return 1;
                })
                .build();
        dispatcher.getRoot().addChild(mainNode);
    }

    private static Optional<ShopData> getShopEntity(Entity entity) {
        if (entity == null || entity.isRemoved() || entity.getWorld() == null || !(entity instanceof ItemFrameEntity itemFrameEntity))
            return Optional.empty();
        BlockPos pos = entity.getBlockPos().down();
        World world = entity.getWorld();
        return world.getBlockEntity(pos, BlockEntityType.SIGN)
                .map(blockEntity -> new ShopData(itemFrameEntity, blockEntity));
    }

    private static Optional<ShopData> getShopEntity(BlockEntity entity) {
        if (entity == null || entity.isRemoved() || entity.getWorld() == null || !(entity instanceof SignBlockEntity signBlockEntity))
            return Optional.empty();
        BlockPos pos = entity.getPos().up();
        World world = entity.getWorld();
        return world.getEntitiesByType(
                        TypeFilter.instanceOf(ItemFrameEntity.class),
                        Box.enclosing(
                                pos,
                                pos.add(1, 1, 1)
                        ),
                        itemFrameEntity -> true
                )
                .stream()
                .findFirst()
                .map(itemFrameEntity -> new ShopData(itemFrameEntity, signBlockEntity));
    }

    private record ShopData(ItemFrameEntity entity, SignBlockEntity blockEntity) {
        private static final Codec<NbtElement> NBT_ELEMENT_CODEC = Codec.PASSTHROUGH.xmap(
                dynamic -> dynamic.convert(NbtOps.INSTANCE).into(
                        Dynamic::getValue
                ),
                nbt -> new Dynamic<>(
                        NbtOps.INSTANCE,
                        nbt.copy()
                )
        );

        private static final Codec<ItemFrameEntity> ITEM_FRAME_ENTITY_CODEC = NBT_ELEMENT_CODEC.flatXmap(
                nbtElement -> DataResult.error(() -> "Not implemented"),
                itemFrameEntity -> exceptionToResult(() -> itemFrameEntity.writeNbt(new NbtCompound()))
                        .mapError(err -> "Failed to serialize ItemFrameEntity")
        );

        private static final Codec<SignBlockEntity> SIGN_BLOCK_ENTITY_CODEC = NBT_ELEMENT_CODEC.flatXmap(
                nbtElement -> DataResult.error(() -> "Not implemented"),
                signBlockEntity -> Optional.of(signBlockEntity)
                        .map(BlockEntity::getWorld)
                        .map(World::getRegistryManager)
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "SignBlockEntity has no world"))
                        .flatMap(registryManager -> exceptionToResult(
                                () -> signBlockEntity.createNbtWithIdentifyingData(registryManager))
                        )
                        .mapError(err -> "Failed to serialize SignBlockEntity: " + err)
        );
        public static final Codec<ShopData> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        ITEM_FRAME_ENTITY_CODEC.fieldOf("item_frame")
                                .forGetter(ShopData::entity),
                        SIGN_BLOCK_ENTITY_CODEC.fieldOf("sign")
                                .forGetter(ShopData::blockEntity)
                ).apply(instance, ShopData::new)
        );
    }
}
