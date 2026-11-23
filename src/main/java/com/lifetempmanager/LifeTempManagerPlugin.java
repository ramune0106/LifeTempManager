package com.lifetempmanager;

import com.lifetempmanager.armor.ArmorInspector;
import com.lifetempmanager.command.LifeTempCommand;
import com.lifetempmanager.config.ConfigManager;
import com.lifetempmanager.core.TemperatureManager;
import com.lifetempmanager.listener.PlayerLifecycleListener;
import com.lifetempmanager.mythic.MythicMobsHook;
import com.lifetempmanager.storage.TemperatureStorage;
import com.lifetempmanager.ui.BossBarController;
import com.lifetempmanager.ui.WarningController;
import com.lifetempmanager.zone.ZoneManager;
import com.lifetempmanager.zone.ZoneWandListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LifeTempManager メインプラグインクラス。
 * Paper 1.21.11 (v1_21_R7) / MythicMobs 5.14+ 対応。
 */
public class LifeTempManagerPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ArmorInspector armorInspector;
    private TemperatureStorage storage;
    private BossBarController bossBarController;
    private WarningController warningController;
    private TemperatureManager temperatureManager;
    private ZoneManager zoneManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.armorInspector = new ArmorInspector(this, configManager);
        this.storage = new TemperatureStorage(this);
        this.bossBarController = new BossBarController(configManager);
        this.warningController = new WarningController(configManager);
        this.temperatureManager = new TemperatureManager(
                this, configManager, armorInspector, storage, bossBarController, warningController
        );
        this.zoneManager = new ZoneManager(this, configManager, temperatureManager);

        // 各種リスナー登録
        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(temperatureManager), this);
        Bukkit.getPluginManager().registerEvents(new ZoneWandListener(zoneManager, configManager), this);

        // MythicMobs 連携登録
        if (Bukkit.getPluginManager().getPlugin("MythicMobs") != null) {
            MythicMobsHook mythicHook = new MythicMobsHook(this, configManager, temperatureManager);
            armorInspector.setExternalItemReader(mythicHook);
            Bukkit.getPluginManager().registerEvents(mythicHook, this);
            getLogger().info("MythicMobs 連携を有効化しました (tempadd / tempset / tempreset / tempinrange / NBT防具解析)。");
        } else {
            getLogger().warning("MythicMobs が見つかりませんでした。MythicMobs 連携機能は無効状態で起動します。");
        }

        // コマンド登録
        PluginCommand cmd = getCommand("lifetemp");
        if (cmd != null) {
            LifeTempCommand executor = new LifeTempCommand(
                    configManager, temperatureManager, armorInspector, zoneManager, this::reloadAll
            );
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        // 定期タスク開始
        temperatureManager.start();
        zoneManager.start();

        getLogger().info("LifeTempManager v" + getPluginMeta().getVersion() + " を有効化しました。");
    }

    @Override
    public void onDisable() {
        if (zoneManager != null) {
            zoneManager.shutdown();
        }
        if (temperatureManager != null) {
            temperatureManager.shutdown();
        }
        getLogger().info("LifeTempManager を無効化しました。");
    }

    public void reloadAll() {
        configManager.reload();
        zoneManager.load();
        temperatureManager.restartAutoSave();
    }

    public TemperatureManager getTemperatureManager() {
        return temperatureManager;
    }

    public ArmorInspector getArmorInspector() {
        return armorInspector;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }
}
