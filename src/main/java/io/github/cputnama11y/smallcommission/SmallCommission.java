package io.github.cputnama11y.smallcommission;

import io.github.cputnama11y.smallcommission.handler.ShopHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SmallCommission implements ClientModInitializer {
    public static final String MOD_ID = "smallcommission";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        List<Event<? super ShopHandler>> events = List.of(ClientEntityEvents.ENTITY_LOAD, ClientBlockEntityEvents.BLOCK_ENTITY_LOAD, ClientCommandRegistrationCallback.EVENT);
        var handler = new ShopHandler();
        events.forEach(event -> event.register(handler));
    }
}