package io.github.cputnama11y.smallcommission;

import io.github.cputnama11y.smallcommission.handler.ShopHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmallCommission implements ClientModInitializer {
    public static final String MOD_ID = "smallcommission";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        var handler = new ShopHandler();
        ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register(handler);
        ClientEntityEvents.ENTITY_LOAD.register(handler);
        ClientEntityEvents.ENTITY_UNLOAD.register(handler);
        ClientCommandRegistrationCallback.EVENT.register(handler);
    }
}