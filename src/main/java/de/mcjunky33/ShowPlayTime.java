package de.mcjunky33;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ShowPlayTime implements ClientModInitializer {

    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("timer_config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ModData data = new ModData();
    private String currentId = null;
    private int tickCounter = 0;

    private static class WorldState {
        long playtimeSeconds = -1;
        long timerSeconds = 0;
        boolean timerRunning = false;
        boolean timerBackwards = false;
    }

    private static class ModData {
        Map<String, WorldState> worldData = new HashMap<>();
        boolean showPlaytime = false;
        boolean showTimer = true;
        boolean playSounds = true;
        List<Integer> colorList = new ArrayList<>(List.of(0xFFFFFF));
        boolean isBold = true;
        boolean isItalic = false;
        boolean isUnderlined = false;
        int yOffset = 52;

        void resetVisuals() {
            this.colorList = new ArrayList<>(List.of(0xFFFFFF));
            this.isBold = true;
            this.isItalic = false;
            this.isUnderlined = false;
            this.yOffset = 52;
        }
    }

    @Override
    public void onInitializeClient() {
        loadData();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.getSingleplayerServer() != null) {
                currentId = "local_" + client.getSingleplayerServer().getWorldData().getLevelName();
            } else if (client.getCurrentServer() != null) {
                currentId = "server_" + client.getCurrentServer().ip.replace(":", "_");
            }

            if (currentId != null) {
                data.worldData.putIfAbsent(currentId, new WorldState());
                WorldState state = data.worldData.get(currentId);

                if (state.playtimeSeconds == -1) {
                    if (client.player != null && client.player.getStats() != null) {
                        int ticksPlayed = client.player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
                        state.playtimeSeconds = ticksPlayed / 20;
                    } else {
                        state.playtimeSeconds = 0;
                    }
                    saveData();
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (currentId != null && data.worldData.containsKey(currentId)) {
                data.worldData.get(currentId).timerRunning = false;
                saveData();
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.isPaused() || currentId == null) return;
            WorldState state = data.worldData.get(currentId);
            tickCounter++;
            if (tickCounter >= 20) {
                if (state.playtimeSeconds < 0) state.playtimeSeconds = 0;
                state.playtimeSeconds++;

                if (state.timerRunning) {
                    if (state.timerBackwards) {
                        if (state.timerSeconds > 0) state.timerSeconds--;
                        else {
                            state.timerRunning = false;
                            playSoundById("minecraft:entity.villager.celebrate", 1.0f);
                            client.player.displayClientMessage(Component.translatable("text.showplaytime.timer_finished"), false);
                        }
                    } else {
                        state.timerSeconds++;
                    }
                }
                tickCounter = 0;
                saveData();
            }
        });

        HudRenderCallback.EVENT.register((guiGraphics, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.options.hideGui || client.player == null || currentId == null) return;
            WorldState state = data.worldData.get(currentId);

            String textToRender = "";
            if (data.showTimer && (state.timerRunning || state.timerSeconds > 0)) {
                textToRender = state.timerRunning ? formatRawTime(state.timerSeconds) : "paused";
            } else if (data.showPlaytime) {
                textToRender = formatRawTime(state.playtimeSeconds);
            }

            if (!textToRender.isEmpty()) {
                Component finalComp = createAnimatedGradientText(textToRender);
                int x = guiGraphics.guiWidth() / 2 - client.font.width(finalComp) / 2;
                int y = guiGraphics.guiHeight() - data.yOffset;
                guiGraphics.drawString(client.font, finalComp, x, y, 0xFFFFFFFF, true);
            }
        });

        registerCommands();
    }

    private void playSoundById(String id, float pitch) {
        Minecraft client = Minecraft.getInstance();
        if (data.playSounds && client.player != null && client.level != null) {
            ResourceLocation loc = ResourceLocation.parse(id);
            Optional<SoundEvent> sound = BuiltInRegistries.SOUND_EVENT.getOptional(loc);
            sound.ifPresent(e -> client.level.playSound(client.player, client.player.getX(), client.player.getY(), client.player.getZ(), e, SoundSource.MASTER, 1.0f, pitch));
        }
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // --- /playtime ---
            dispatcher.register(ClientCommandManager.literal("playtime")
                    .executes(c -> {
                        long time = data.worldData.get(currentId).playtimeSeconds;
                        playSoundById("minecraft:entity.experience_orb.pickup", 1.2f);
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.playtime_current", formatRawTime(time)));
                        return 1;
                    })
                    .then(ClientCommandManager.literal("show").then(ClientCommandManager.argument("v", BoolArgumentType.bool()).executes(c -> {
                        data.showPlaytime = BoolArgumentType.getBool(c, "v");
                        playSoundById(data.showPlaytime ? "minecraft:entity.experience_orb.pickup" : "minecraft:block.note_block.bass", 1.2f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.config_updated", "Playtime HUD", data.showPlaytime));
                        return 1;
                    })))
            );

            // --- /timer ---
            dispatcher.register(ClientCommandManager.literal("timer")
                    .then(ClientCommandManager.literal("start").executes(c -> {
                        WorldState s = data.worldData.get(currentId);
                        if (s.timerRunning) {
                            playSoundById("minecraft:block.note_block.bass", 0.5f);
                            c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_already_running"));
                            return 0;
                        }
                        s.timerRunning = true;
                        playSoundById("minecraft:entity.player.levelup", 1.0f);
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_started"));
                        saveData();
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("add").then(ClientCommandManager.argument("h", IntegerArgumentType.integer(0)).then(ClientCommandManager.argument("m", IntegerArgumentType.integer(0, 59)).then(ClientCommandManager.argument("s", IntegerArgumentType.integer(0, 59)).executes(c -> {
                        long toAdd = (IntegerArgumentType.getInteger(c, "h") * 3600L) + (IntegerArgumentType.getInteger(c, "m") * 60L) + IntegerArgumentType.getInteger(c, "s");
                        data.worldData.get(currentId).timerSeconds += toAdd;
                        playSoundById("minecraft:entity.experience_orb.pickup", 1.2f);
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_added", formatRawTime(toAdd)));
                        saveData();
                        return 1;
                    })))))
                    .then(ClientCommandManager.literal("set").then(ClientCommandManager.argument("h", IntegerArgumentType.integer(0)).then(ClientCommandManager.argument("m", IntegerArgumentType.integer(0, 59)).then(ClientCommandManager.argument("s", IntegerArgumentType.integer(0, 59)).executes(c -> {
                        WorldState s = data.worldData.get(currentId);
                        s.timerSeconds = (IntegerArgumentType.getInteger(c, "h") * 3600L) + (IntegerArgumentType.getInteger(c, "m") * 60L) + IntegerArgumentType.getInteger(c, "s");
                        playSoundById("minecraft:entity.experience_orb.pickup", 1.2f);
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_current", formatRawTime(s.timerSeconds)));
                        saveData();
                        return 1;
                    })))))
                    .then(ClientCommandManager.literal("pause").executes(c -> {
                        WorldState s = data.worldData.get(currentId);
                        if (!s.timerRunning) {
                            playSoundById("minecraft:block.note_block.bass", 0.5f);
                            c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_not_running"));
                            return 0;
                        }
                        s.timerRunning = false;
                        playSoundById("minecraft:block.note_block.bass", 0.5f);
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_paused"));
                        saveData();
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("resume").executes(c -> {
                        WorldState s = data.worldData.get(currentId);
                        if (s.timerRunning || s.timerSeconds == 0) {
                            playSoundById("minecraft:block.note_block.bass", 0.5f);
                            c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_no_resume"));
                            return 0;
                        }
                        s.timerRunning = true;
                        playSoundById("minecraft:entity.player.levelup", 1.2f);
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_resumed"));
                        saveData();
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("stop").executes(c -> {
                        WorldState s = data.worldData.get(currentId);
                        s.timerRunning = false; s.timerSeconds = 0;
                        playSoundById("minecraft:block.note_block.bass", 0.5f);
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.timer_stopped"));
                        saveData();
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("backwards").then(ClientCommandManager.argument("v", BoolArgumentType.bool()).executes(c -> {
                        data.worldData.get(currentId).timerBackwards = BoolArgumentType.getBool(c, "v");
                        playSoundById(data.worldData.get(currentId).timerBackwards ? "minecraft:entity.experience_orb.pickup" : "minecraft:block.note_block.bass", 1.2f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.config_updated", "Backwards", data.worldData.get(currentId).timerBackwards));
                        return 1;
                    })))
                    .then(ClientCommandManager.literal("show").then(ClientCommandManager.argument("v", BoolArgumentType.bool()).executes(c -> {
                        data.showTimer = BoolArgumentType.getBool(c, "v");
                        playSoundById(data.showTimer ? "minecraft:entity.experience_orb.pickup" : "minecraft:block.note_block.bass", 1.2f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.config_updated", "Timer HUD", data.showTimer));
                        return 1;
                    })))
            );

            // --- /timerlook ---
            dispatcher.register(ClientCommandManager.literal("timerlook")
                    .then(ClientCommandManager.literal("reset").executes(c -> {
                        data.resetVisuals();
                        playSoundById("minecraft:block.note_block.bass", 0.5f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.look_reset"));
                        return 1;
                    }))
                    .then(ClientCommandManager.literal("bold").then(ClientCommandManager.argument("v", BoolArgumentType.bool()).executes(c -> {
                        data.isBold = BoolArgumentType.getBool(c, "v");
                        playSoundById(data.isBold ? "minecraft:entity.experience_orb.pickup" : "minecraft:block.note_block.bass", 1.2f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.config_updated", "Bold", data.isBold));
                        return 1;
                    })))
                    .then(ClientCommandManager.literal("italic").then(ClientCommandManager.argument("v", BoolArgumentType.bool()).executes(c -> {
                        data.isItalic = BoolArgumentType.getBool(c, "v");
                        playSoundById(data.isItalic ? "minecraft:entity.experience_orb.pickup" : "minecraft:block.note_block.bass", 1.2f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.config_updated", "Italic", data.isItalic));
                        return 1;
                    })))
                    .then(ClientCommandManager.literal("underlined").then(ClientCommandManager.argument("v", BoolArgumentType.bool()).executes(c -> {
                        data.isUnderlined = BoolArgumentType.getBool(c, "v");
                        playSoundById(data.isUnderlined ? "minecraft:entity.experience_orb.pickup" : "minecraft:block.note_block.bass", 1.2f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.config_updated", "Underlined", data.isUnderlined));
                        return 1;
                    })))
                    .then(ClientCommandManager.literal("ypos").then(ClientCommandManager.argument("v", IntegerArgumentType.integer(0, 1000)).executes(c -> {
                        data.yOffset = IntegerArgumentType.getInteger(c, "v");
                        playSoundById("minecraft:entity.experience_orb.pickup", 1.2f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.config_updated", "Y-Offset", data.yOffset));
                        return 1;
                    })))
                    .then(ClientCommandManager.literal("mutesound").then(ClientCommandManager.argument("v", BoolArgumentType.bool()).executes(c -> {
                        data.playSounds = !BoolArgumentType.getBool(c, "v");
                        playSoundById("minecraft:entity.experience_orb.pickup", 1.2f);
                        saveData();
                        c.getSource().sendFeedback(Component.translatable("text.showplaytime.config_updated", "Mute Sounds", !data.playSounds));
                        return 1;
                    })))
                    .then(ClientCommandManager.literal("color")
                            .then(ClientCommandManager.literal("add").then(ClientCommandManager.argument("c", ColorArgument.color()).executes(c -> {
                                ChatFormatting cf = c.getArgument("c", ChatFormatting.class);
                                if (data.colorList.size() == 1 && data.colorList.get(0) == 0xFFFFFF) data.colorList.clear();
                                if (cf.getColor() != null) {
                                    data.colorList.add(cf.getColor());
                                    playSoundById("minecraft:entity.experience_orb.pickup", 1.2f);
                                    saveData();
                                    c.getSource().sendFeedback(Component.translatable("text.showplaytime.color_added", cf.getName()));
                                }
                                return 1;
                            })))
                            .then(ClientCommandManager.literal("remove").then(ClientCommandManager.argument("c", ColorArgument.color()).suggests((context, builder) -> {
                                List<String> activeColors = new ArrayList<>();
                                for (ChatFormatting cf : ChatFormatting.values()) {
                                    if (cf.getColor() != null && data.colorList.contains(cf.getColor())) activeColors.add(cf.getName());
                                }
                                return SharedSuggestionProvider.suggest(activeColors, builder);
                            }).executes(c -> {
                                ChatFormatting cf = c.getArgument("c", ChatFormatting.class);
                                data.colorList.remove(cf.getColor());
                                if (data.colorList.isEmpty()) data.colorList.add(0xFFFFFF);
                                playSoundById("minecraft:block.note_block.bass", 0.5f);
                                saveData();
                                c.getSource().sendFeedback(Component.translatable("text.showplaytime.color_removed", cf.getName()));
                                return 1;
                            })))
                            .then(ClientCommandManager.literal("clear").executes(c -> {
                                data.colorList = new ArrayList<>(List.of(0xFFFFFF));
                                playSoundById("minecraft:block.note_block.bass", 0.5f);
                                saveData();
                                c.getSource().sendFeedback(Component.translatable("text.showplaytime.color_cleared"));
                                return 1;
                            }))
                    )
            );
        });
    }

    private String formatRawTime(long t) {
        if (t <= 0) return "0s";
        long y = t / 31536000, M = (t % 31536000) / 2592000, w = (t % 2592000) / 604800, d = (t % 604800) / 86400, h = (t % 86400) / 3600, m = (t % 3600) / 60, s = t % 60;
        StringBuilder sb = new StringBuilder();
        if (y > 0) sb.append(y).append("y "); if (M > 0) sb.append(M).append("M "); if (w > 0) sb.append(w).append("w "); if (d > 0) sb.append(d).append("d "); if (h > 0) sb.append(h).append("h "); if (m > 0) sb.append(m).append("m "); if (s > 0) sb.append(s).append("s");
        return sb.toString().trim();
    }

    private Component createAnimatedGradientText(String text) {
        MutableComponent res = Component.empty();
        double offset = (System.currentTimeMillis() % 3000L) / 3000.0;
        for (int i = 0; i < text.length(); i++) {
            int color = interpolateColors(data.colorList, (float)(((float)i/text.length() + offset) % 1.0));
            res.append(Component.literal(String.valueOf(text.charAt(i))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(data.isBold).withItalic(data.isItalic).withUnderlined(data.isUnderlined)));
        }
        return res;
    }

    private int interpolateColors(List<Integer> colors, float ratio) {
        if (colors.size() == 1) return colors.get(0);
        float section = ratio * colors.size();
        int i = (int)section % colors.size(), next = (i+1) % colors.size();
        float t = section - (int)section;
        int c1 = colors.get(i), c2 = colors.get(next);
        return ((int)(((c1>>16)&0xFF)*(1-t)+((c2>>16)&0xFF)*t)<<16) | ((int)(((c1>>8)&0xFF)*(1-t)+((c2>>8)&0xFF)*t)<<8) | (int)((c1&0xFF)*(1-t)+(c2&0xFF)*t);
    }

    private void loadData() { try { if (Files.exists(CONFIG_PATH)) { try (Reader r = Files.newBufferedReader(CONFIG_PATH)) { data = GSON.fromJson(r, ModData.class); if(data==null) data=new ModData(); } } } catch (Exception e) {} }
    private void saveData() { try { Files.createDirectories(CONFIG_PATH.getParent()); try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) { GSON.toJson(data, w); } } catch (Exception e) {} }
}