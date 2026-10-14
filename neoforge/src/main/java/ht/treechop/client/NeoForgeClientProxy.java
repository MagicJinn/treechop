package ht.treechop.client;

import ht.treechop.client.model.NeoForgeChoppedLogBlockStateModel;
import net.neoforged.bus.api.IEventBus;

public class NeoForgeClientProxy {
    public static void init(IEventBus modBus) {
        modBus.addListener(NeoForgeChoppedLogBlockStateModel::overrideBlockStateModels);
    }
}
