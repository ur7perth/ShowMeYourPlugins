package com.example.serverinfo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Set;
import java.util.TreeSet;

public class ServerInfoClient implements ClientModInitializer {

    private static final Set<String> IGNORED = Set.of("minecraft", "bukkit", "spigot", "paper");

    @Override
    public void onInitializeClient() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String m = message.trim();
            if (m.equalsIgnoreCase("?pl")) {
                showPlugins();
                return false; // لا يرسل الرسالة للسيرفر
            }
            if (m.equalsIgnoreCase("?ver")) {
                showVersion();
                return false;
            }
            return true;
        });
    }

    private static void msg(Text text) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(text, false);
        }
    }

    private static void showPlugins() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return;

        CommandDispatcher<CommandSource> dispatcher = handler.getCommandDispatcher();
        Set<String> plugins = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (CommandNode<CommandSource> node : dispatcher.getRoot().getChildren()) {
            String name = node.getName();
            int i = name.indexOf(':');
            if (i > 0) {
                String ns = name.substring(0, i);
                if (!IGNORED.contains(ns.toLowerCase())) {
                    plugins.add(ns);
                }
            }
        }

        if (plugins.isEmpty()) {
            msg(Text.literal("[ServerInfo] لم يتم العثور على بلوقنات (قد يكون السيرفر يخفيها).")
                    .formatted(Formatting.RED));
            return;
        }

        msg(Text.literal("[ServerInfo] البلوقنات (" + plugins.size() + "):")
                .formatted(Formatting.GREEN));
        msg(Text.literal(String.join(", ", plugins)).formatted(Formatting.YELLOW));
    }

    private static void showVersion() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayNetworkHandler handler = mc.getNetworkHandler();
        if (handler == null) return;

        String brand = handler.getBrand();
        ServerInfo info = mc.getCurrentServerEntry();
        String version = (info != null && info.version != null) ? info.version.getString() : "غير معروف";

        String combined = ((brand == null ? "" : brand) + " " + version).toLowerCase();
        String type = detectType(combined);

        msg(Text.literal("[ServerInfo] النوع: ").formatted(Formatting.GREEN)
                .append(Text.literal(type).formatted(Formatting.YELLOW)));
        msg(Text.literal("[ServerInfo] Brand: ").formatted(Formatting.GREEN)
                .append(Text.literal(brand == null ? "غير معروف" : brand).formatted(Formatting.YELLOW)));
        msg(Text.literal("[ServerInfo] الاصدار: ").formatted(Formatting.GREEN)
                .append(Text.literal(version).formatted(Formatting.YELLOW)));
    }

    private static String detectType(String s) {
        String[] types = {"purpur", "folia", "pufferfish", "paper", "spigot", "bukkit",
                "velocity", "bungeecord", "waterfall", "fabric", "forge", "neoforge", "vanilla"};
        for (String t : types) {
            if (s.contains(t)) return t.substring(0, 1).toUpperCase() + t.substring(1);
        }
        return "غير معروف";
    }
}
