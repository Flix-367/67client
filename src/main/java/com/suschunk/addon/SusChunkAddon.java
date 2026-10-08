package com.suschunk.addon;

import com.mojang.logging.LogUtils;
import com.suschunk.addon.modules.SusChunkFinder;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class SusChunkAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("67Client", Items.AMETHYST_SHARD.getDefaultStack());

    @Override
    public void onInitialize() {
        Modules.get().add(new SusChunkFinder());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "com.suschunk.addon";
    }
}
