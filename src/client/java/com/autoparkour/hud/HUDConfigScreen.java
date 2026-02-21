package com.autoparkour.hud;

import com.autoparkour.AutoParkourMod;
import com.autoparkour.config.ModConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Arrays;

public class HUDConfigScreen extends Screen {

    private final Screen parent;
    private final ModConfig config;

    public HUDConfigScreen(Screen parent, ModConfig config) {
        super(Text.literal("AutoParkour Configuration"));
        this.parent = parent;
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("AutoParkour Configuration"))
                .setSavingRunnable(this::saveConfig);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Parkour"), config.isParkourEnabled())
                .setDefaultValue(false)
                .setSaveConsumer(config::setParkourEnabled)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Sprint"), config.isAutoSprintEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(config::setAutoSprintEnabled)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Jump"), config.isAutoJumpEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(config::setAutoJumpEnabled)
                .build());

        general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Debug Mode"), config.isDebugMode())
                .setDefaultValue(false)
                .setSaveConsumer(config::setDebugMode)
                .build());

        ConfigCategory movement = builder.getOrCreateCategory(Text.literal("Movement"));

        movement.addEntry(entryBuilder.startBooleanToggle(Text.literal("Allow Diagonal Movement"), config.isAllowDiagonalMovement())
                .setDefaultValue(false)
                .setSaveConsumer(config::setAllowDiagonalMovement)
                .build());

        movement.addEntry(entryBuilder.startBooleanToggle(Text.literal("Prefer Straight Directions"), config.isPreferStraightDirections())
                .setDefaultValue(true)
                .setTooltip(Text.literal("Сначала выбирать прямые направления, потом диагонали"))
                .setSaveConsumer(config::setPreferStraightDirections)
                .build());

        movement.addEntry(entryBuilder.startBooleanToggle(Text.literal("Prevent Overshoot"), config.isPreventOvershoot())
                .setDefaultValue(true)
                .setSaveConsumer(config::setPreventOvershoot)
                .build());

        movement.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Look at Target"), config.isAutoLookAtTarget())
                .setDefaultValue(false)
                .setSaveConsumer(config::setAutoLookAtTarget)
                .build());

        movement.addEntry(entryBuilder.startDoubleField(Text.literal("Look Speed"), config.getLookSpeed())
                .setDefaultValue(5.0)
                .setMin(1.0)
                .setMax(20.0)
                .setSaveConsumer(config::setLookSpeed)
                .build());

        ConfigCategory blocks = builder.getOrCreateCategory(Text.literal("Blocks"));

        blocks.addEntry(entryBuilder.startBooleanToggle(Text.literal("Use Block Whitelist"), config.isUseBlockWhitelist())
                .setDefaultValue(false)
                .setSaveConsumer(config::setUseBlockWhitelist)
                .build());

        blocks.addEntry(entryBuilder.startStrList(Text.literal("Allowed Blocks"), config.getAllowedBlocks())
                .setDefaultValue(Arrays.asList(
                    "minecraft:stone",
                    "minecraft:cobblestone",
                    "minecraft:dirt",
                    "minecraft:grass_block",
                    "minecraft:sand",
                    "minecraft:sandstone",
                    "minecraft:oak_planks",
                    "minecraft:spruce_planks"
                ))
                .setSaveConsumer(config::setAllowedBlocks)
                .build());
        // Добавьте после списка Allowed Blocks (примерно строка 150)
blocks.addEntry(entryBuilder.startBooleanToggle(Text.literal("Use Block Ignore List"), config.isUseIgnoreList())
        .setDefaultValue(true)
        .setTooltip(Text.literal("Игнорировать указанные блоки"))
        .setSaveConsumer(config::setUseIgnoreList)
        .build());

blocks.addEntry(entryBuilder.startStrList(Text.literal("Ignored Blocks"), config.getIgnoredBlocks())
        .setDefaultValue(Arrays.asList(
            "minecraft:air",
            "minecraft:cave_air",
            "minecraft:void_air",
            "minecraft:water",
            "minecraft:lava",
            "minecraft:fire",
            "minecraft:soul_fire",
            "minecraft:cactus",
            "minecraft:sweet_berry_bush",
            "minecraft:magma_block",
            "minecraft:campfire",
            "minecraft:soul_campfire"
        ))
        .setTooltip(Text.literal("Блоки, которые будут игнорироваться при паркуре"))
        .setSaveConsumer(config::setIgnoredBlocks)
        .build());

        ConfigCategory scanning = builder.getOrCreateCategory(Text.literal("Scanning"));

        scanning.addEntry(entryBuilder.startIntSlider(Text.literal("Scan Range"), config.getMaxScanRange(), 2, 12)
                .setDefaultValue(6)
                .setTextGetter(value -> Text.literal(value + " blocks"))
                .setSaveConsumer(config::setMaxScanRange)
                .build());

        scanning.addEntry(entryBuilder.startIntSlider(Text.literal("Min Jump Distance"), config.getMinJumpDistance(), 1, 5)
                .setDefaultValue(2)
                .setTextGetter(value -> Text.literal(value + " blocks"))
                .setSaveConsumer(config::setMinJumpDistance)
                .build());

        scanning.addEntry(entryBuilder.startIntSlider(Text.literal("Max Jump Distance"), config.getMaxJumpDistance(), 2, 8)
                .setDefaultValue(4)
                .setTextGetter(value -> Text.literal(value + " blocks"))
                .setSaveConsumer(config::setMaxJumpDistance)
                .build());

        scanning.addEntry(entryBuilder.startIntSlider(Text.literal("Block Placement Delay"), config.getBlockPlacementDelay(), 0, 10)
                .setDefaultValue(2)
                .setTextGetter(value -> Text.literal(value + " ticks"))
                .setSaveConsumer(config::setBlockPlacementDelay)
                .build());

        ConfigCategory safety = builder.getOrCreateCategory(Text.literal("Safety"));

        safety.addEntry(entryBuilder.startBooleanToggle(Text.literal("Safe Fall"), config.isSafeFallEnabled())
                .setDefaultValue(true)
                .setSaveConsumer(config::setSafeFallEnabled)
                .build());

        safety.addEntry(entryBuilder.startIntSlider(Text.literal("Fall Damage Threshold"), config.getFallDamageThreshold(), 1, 10)
                .setDefaultValue(3)
                .setTextGetter(value -> Text.literal(value + " blocks"))
                .setSaveConsumer(config::setFallDamageThreshold)
                .build());

        safety.addEntry(entryBuilder.startBooleanToggle(Text.literal("Avoid Lava"), config.isAvoidLava())
                .setDefaultValue(true)
                .setSaveConsumer(config::setAvoidLava)
                .build());

        safety.addEntry(entryBuilder.startBooleanToggle(Text.literal("Avoid Void"), config.isAvoidVoid())
                .setDefaultValue(true)
                .setSaveConsumer(config::setAvoidVoid)
                .build());

        ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD"));

        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("HUD Visible"), config.isHudVisible())
                .setDefaultValue(true)
                .setSaveConsumer(config::setHudVisible)
                .build());

        hud.addEntry(entryBuilder.startIntSlider(Text.literal("HUD X Position"), config.getHudX(), 0, 1920)
                .setDefaultValue(10)
                .setSaveConsumer(config::setHudX)
                .build());

        hud.addEntry(entryBuilder.startIntSlider(Text.literal("HUD Y Position"), config.getHudY(), 0, 1080)
                .setDefaultValue(10)
                .setSaveConsumer(config::setHudY)
                .build());

        hud.addEntry(entryBuilder.startIntSlider(Text.literal("HUD Opacity"), config.getHudOpacity(), 0, 255)
                .setDefaultValue(180)
                .setSaveConsumer(config::setHudOpacity)
                .build());

        hud.addEntry(entryBuilder.startIntSlider(Text.literal("HUD Scale (%)"), config.getHudScale(), 50, 200)
                .setDefaultValue(100)
                .setSaveConsumer(config::setHudScale)
                .build());

        hud.addEntry(entryBuilder.startStrField(Text.literal("HUD Color (Hex)"), config.getHudColor())
                .setDefaultValue("#00FF00")
                .setSaveConsumer(config::setHudColor)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Coordinates"), config.isHudShowCoordinates())
                .setDefaultValue(true)
                .setSaveConsumer(config::setHudShowCoordinates)
                .build());

        hud.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Target Block"), config.isHudShowTargetBlock())
                .setDefaultValue(true)
                .setSaveConsumer(config::setHudShowTargetBlock)
                .build());

        ConfigCategory keys = builder.getOrCreateCategory(Text.literal("Key Bindings"));

        keys.addEntry(entryBuilder.startStrField(Text.literal("Toggle Parkour Key"), config.getKeyToggleParkour())
                .setDefaultValue("key.keyboard.p")
                .setSaveConsumer(config::setKeyToggleParkour)
                .build());

        keys.addEntry(entryBuilder.startStrField(Text.literal("Toggle Sprint Key"), config.getKeyToggleSprint())
                .setDefaultValue("key.keyboard.k")
                .setSaveConsumer(config::setKeyToggleSprint)
                .build());

        keys.addEntry(entryBuilder.startStrField(Text.literal("Toggle HUD Key"), config.getKeyToggleHUD())
                .setDefaultValue("key.keyboard.h")
                .setSaveConsumer(config::setKeyToggleHUD)
                .build());

        keys.addEntry(entryBuilder.startStrField(Text.literal("Toggle Jump Key"), config.getKeyToggleJump())
                .setDefaultValue("key.keyboard.j")
                .setSaveConsumer(config::setKeyToggleJump)
                .build());

        keys.addEntry(entryBuilder.startStrField(Text.literal("Open Config Key"), config.getKeyOpenConfig())
                .setDefaultValue("key.keyboard.o")
                .setSaveConsumer(config::setKeyOpenConfig)
                .build());

        builder.setDoesConfirmSave(true);
        client.setScreen(builder.build());
    }

    private void saveConfig() {
        AutoParkourMod.getInstance().getConfigManager().saveConfig();
        if (parent != null) {
            parent.init(client, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        }
    }
}
