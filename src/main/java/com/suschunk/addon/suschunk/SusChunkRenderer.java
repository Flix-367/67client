package com.suschunk.addon.suschunk;

import com.suschunk.addon.modules.SusChunkFinder;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class SusChunkRenderer {
    private static final float FADE_IN_SECONDS = 0.3F;
    private static final float FADE_OUT_SECONDS = 0.45F;
    private static final float MOVE_RATE = 7.0F;
    private static final Map<Long, ChunkFade> chunkFades = new HashMap<>();
    private static final Map<Long, ZoneFade> zoneFades = new HashMap<>();
    private static long lastFrameNanos;

    private SusChunkRenderer() {
    }

    public static void reset() {
        chunkFades.clear();
        zoneFades.clear();
    }

    public static void render(Render3DEvent event, SusChunkFinder module) {
        SusChunkScanner scanner = module.scanner;
        double y = module.renderY.get();
        SettingColor base = module.color.get();
        float fillAlpha = module.fillOpacity.get() / 255.0F;
        float outlineAlpha = module.outlineOpacity.get() / 255.0F;
        boolean outline = module.outline.get();
        boolean smart = module.smartMode.get();
        int threshold = scanner.threshold();
        updateTargets(scanner, smart, threshold);
        float dt = frameDelta();
        float move = 1.0F - (float) Math.exp(-dt * MOVE_RATE);

        Iterator<Map.Entry<Long, ChunkFade>> chunks = chunkFades.entrySet().iterator();
        while (chunks.hasNext()) {
            Map.Entry<Long, ChunkFade> entry = chunks.next();
            ChunkFade fade = entry.getValue();
            fade.alpha += fade.flagged ? dt / FADE_IN_SECONDS : -dt / FADE_OUT_SECONDS;
            fade.alpha = Math.clamp(fade.alpha, 0.0F, 1.0F);
            if (!fade.flagged && fade.alpha <= 0.0F) {
                chunks.remove();
                continue;
            }
            double x0 = ChunkPos.getPackedX(entry.getKey()) * 16.0;
            double z0 = ChunkPos.getPackedZ(entry.getKey()) * 16.0;
            drawQuad(event, x0, z0, x0 + 16.0, z0 + 16.0, y, base,
                fillAlpha * fade.tier * fade.alpha, outline ? outlineAlpha * fade.alpha : 0.0F);
        }

        Iterator<ZoneFade> zones = zoneFades.values().iterator();
        while (zones.hasNext()) {
            ZoneFade zone = zones.next();
            zone.alpha += zone.flagged ? dt / FADE_IN_SECONDS : -dt / FADE_OUT_SECONDS;
            zone.alpha = Math.clamp(zone.alpha, 0.0F, 1.0F);
            if (!zone.flagged && zone.alpha <= 0.0F) {
                zones.remove();
                continue;
            }
            zone.centerX += (zone.targetX - zone.centerX) * move;
            zone.centerZ += (zone.targetZ - zone.centerZ) * move;
            zone.size += (zone.targetSize - zone.size) * move;
            double half = zone.size / 2.0;
            drawQuad(event, zone.centerX - half, zone.centerZ - half, zone.centerX + half, zone.centerZ + half, y, base,
                fillAlpha * zone.tier * zone.alpha, outline ? outlineAlpha * zone.alpha : 0.0F);
            if (module.centroidMarker.get()) {
                Color marker = withAlpha(base, 0.95F * zone.alpha);
                double r = 2.0;
                event.renderer.box(zone.centerX - r, y, zone.centerZ - r, zone.centerX + r, y + 6.0, zone.centerZ + r,
                    marker, marker, ShapeMode.Lines, 0);
            }
        }
    }

    private static Color withAlpha(SettingColor c, float alpha) {
        return new Color(c.r, c.g, c.b, Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F));
    }

    private static void drawQuad(Render3DEvent event, double x0, double z0, double x1, double z1, double y,
                                 SettingColor base, float fill, float border) {
        if (fill > 0.004F) {
            event.renderer.quadHorizontal(x0, y, z0, x1, z1, withAlpha(base, fill));
        }
        if (border > 0.004F) {
            Color c = withAlpha(base, border);
            event.renderer.line(x0, y, z0, x1, y, z0, c);
            event.renderer.line(x0, y, z1, x1, y, z1, c);
            event.renderer.line(x0, y, z0, x0, y, z1, c);
            event.renderer.line(x1, y, z0, x1, y, z1, c);
        }
    }

    private static void updateTargets(SusChunkScanner scanner, boolean smart, int threshold) {
        for (ChunkFade fade : chunkFades.values()) fade.flagged = false;
        for (ZoneFade zone : zoneFades.values()) zone.flagged = false;

        if (smart) {
            for (SusChunkScanner.Zone zone : scanner.zones()) {
                if (zone.members().size() == 1) {
                    long key = zone.members().iterator().next();
                    ChunkFade fade = chunkFades.computeIfAbsent(key, k -> new ChunkFade());
                    fade.flagged = true;
                    fade.tier = confidence(zone.maxScore(), threshold);
                } else {
                    long key = Long.MAX_VALUE;
                    for (long member : zone.members()) key = Math.min(key, member);

                    ZoneFade fade = zoneFades.get(key);
                    if (fade == null) {
                        fade = new ZoneFade(zone.centroidX(), zone.centroidZ());
                        zoneFades.put(key, fade);
                    }
                    fade.flagged = true;
                    fade.targetX = zone.centroidX();
                    fade.targetZ = zone.centroidZ();
                    fade.targetSize = Math.min(48.0F, 16.0F + (zone.members().size() - 1) * 8.0F);
                    fade.tier = confidence(zone.maxScore(), threshold);
                }
            }
        } else {
            for (SusChunkScanner.Flag flag : scanner.flags()) {
                ChunkFade fade = chunkFades.computeIfAbsent(flag.chunkKey(), k -> new ChunkFade());
                fade.flagged = true;
                fade.tier = 1.0F;
            }
        }
    }

    private static float confidence(double score, int threshold) {
        if (threshold <= 0) return 1.0F;
        float over = (float) ((score - threshold) / (threshold * 2.0));
        return 0.55F + 0.45F * Math.clamp(over, 0.0F, 1.0F);
    }

    private static float frameDelta() {
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0.016F : (float) (now - lastFrameNanos) / 1.0E9F;
        lastFrameNanos = now;
        return Math.min(dt, 0.1F);
    }

    private static final class ChunkFade {
        float alpha;
        float tier = 1.0F;
        boolean flagged;
    }

    private static final class ZoneFade {
        double centerX, centerZ, targetX, targetZ;
        float size = 16.0F, targetSize = 16.0F, alpha, tier = 1.0F;
        boolean flagged;

        ZoneFade(double x, double z) {
            this.centerX = this.targetX = x;
            this.centerZ = this.targetZ = z;
        }
    }
}
