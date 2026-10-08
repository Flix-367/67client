package com.suschunk.addon.modules;

import com.suschunk.addon.SusChunkAddon;
import com.suschunk.addon.suschunk.ServerLightCache;
import com.suschunk.addon.suschunk.SusChunkRenderer;
import com.suschunk.addon.suschunk.SusChunkScanner;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class SusChunkFinder extends Module {
    public enum Notify { Chat, Off }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSignals = settings.createGroup("Signals");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    public final Setting<Integer> sensitivity = sgGeneral.add(new IntSetting.Builder()
        .name("sensitivity").description("Higher = stricter: more weighted evidence before a chunk flags.")
        .defaultValue(3).range(1, 10).sliderRange(1, 10).build());
    public final Setting<Integer> scanSpeed = sgGeneral.add(new IntSetting.Builder()
        .name("scan-speed").description("Chunks scanned per tick.")
        .defaultValue(10).range(2, 24).sliderRange(2, 24).build());
    public final Setting<Notify> notifications = sgGeneral.add(new EnumSetting.Builder<Notify>()
        .name("notifications").description("Announce each new zone once, with coords and distance.")
        .defaultValue(Notify.Chat).build());

    // Signals
    public final Setting<Boolean> amethyst = sgSignals.add(new BoolSetting.Builder()
        .name("amethyst").description("Hidden grown clusters via server block light 5 - the strongest signal.").defaultValue(true).build());
    public final Setting<Boolean> kelp = sgSignals.add(new BoolSetting.Builder()
        .name("kelp").description("Fully grown / unusually tall kelp columns.").defaultValue(true).build());
    public final Setting<Boolean> bamboo = sgSignals.add(new BoolSetting.Builder()
        .name("bamboo").description("Fully grown, max-height bamboo.").defaultValue(true).build());
    public final Setting<Boolean> berries = sgSignals.add(new BoolSetting.Builder()
        .name("berries").description("Sweet berry bushes at max growth stage.").defaultValue(true).build());
    public final Setting<Boolean> vines = sgSignals.add(new BoolSetting.Builder()
        .name("vines").description("Vines grown far down from their support.").defaultValue(true).build());
    public final Setting<Boolean> dripstone = sgSignals.add(new BoolSetting.Builder()
        .name("dripstone").description("Dripstone spikes longer than natural generation.").defaultValue(true).build());

    // Render
    public final Setting<Integer> renderY = sgRender.add(new IntSetting.Builder()
        .name("render-y").description("Height the flat chunk highlights render at.")
        .defaultValue(100).range(-64, 320).sliderRange(-64, 320).build());
    public final Setting<SettingColor> color = sgRender.add(new ColorSetting.Builder()
        .name("color").description("Highlight color.").defaultValue(new SettingColor(190, 90, 255, 255)).build());
    public final Setting<Integer> fillOpacity = sgRender.add(new IntSetting.Builder()
        .name("fill-opacity").description("Fill opacity of the chunk quads.")
        .defaultValue(90).range(0, 255).sliderRange(0, 255).build());
    public final Setting<Boolean> outline = sgRender.add(new BoolSetting.Builder()
        .name("outline").description("Crisp border along the chunk edges.").defaultValue(true).build());
    public final Setting<Integer> outlineOpacity = sgRender.add(new IntSetting.Builder()
        .name("outline-opacity").description("Border opacity.")
        .defaultValue(200).range(0, 255).sliderRange(0, 255).visible(outline::get).build());
    public final Setting<Boolean> smartMode = sgRender.add(new BoolSetting.Builder()
        .name("smart-mode").description("Merge nearby flags into zones with a centroid marker.").defaultValue(true).build());
    public final Setting<Integer> mergeRadius = sgRender.add(new IntSetting.Builder()
        .name("merge-radius").description("Flags within this many chunks merge into one zone.")
        .defaultValue(3).range(1, 8).sliderRange(1, 8).visible(smartMode::get).build());
    public final Setting<Boolean> centroidMarker = sgRender.add(new BoolSetting.Builder()
        .name("centroid-marker").description("Mark each zone's weighted centre - the likely base spot.")
        .defaultValue(true).visible(smartMode::get).build());

    public final SusChunkScanner scanner = new SusChunkScanner(this);

    public SusChunkFinder() {
        super(SusChunkAddon.CATEGORY, "sus-chunk-finder", "Finds long-loaded chunks (bases) via amethyst light and plant growth.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        scanner.tick();
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        SusChunkRenderer.render(event, this);
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        ServerLightCache.get().clear();
        scanner.clear();
        SusChunkRenderer.reset();
    }

    @Override
    public void onDeactivate() {
        scanner.clear();
        SusChunkRenderer.reset();
    }
}
