package com.hpfxd.spectatorplus.paper.sync.handler;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.hpfxd.spectatorplus.paper.SpectatorPlugin;
import com.hpfxd.spectatorplus.paper.sync.packet.ClientboundFoodSyncPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class FoodSyncHandler implements Listener {
    private static final String PERMISSION = "spectatorplus.sync.food";
    private static final long UPDATE_INTERVAL_TICKS = 20; // roughly every second

    private final SpectatorPlugin plugin;

    public FoodSyncHandler(SpectatorPlugin plugin) {
        this.plugin = plugin;

        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::broadcastPeriodicUpdates, UPDATE_INTERVAL_TICKS, UPDATE_INTERVAL_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof final Player player) {
            final Player forced = this.plugin.getSyncController().getForcedSyncPlayer().orElse(null);

            if (forced != null && !forced.equals(player)) {
                return;
            }

            final Player dataPlayer = forced != null ? forced : player;
            final int foodLevel = forced != null ? dataPlayer.getFoodLevel() : event.getFoodLevel();
            this.sendHungerUpdate(dataPlayer, foodLevel, dataPlayer.getSaturation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStartSpectatingEntity(PlayerStartSpectatingEntityEvent event) {
        final Player spectator = event.getPlayer();

        if (event.getNewSpectatorTarget() instanceof final Player target && spectator.hasPermission(PERMISSION)) {
            final Player dataPlayer = this.plugin.getSyncController().resolveDataPlayer(target);
            this.plugin.getSyncController().sendPacket(spectator, new ClientboundFoodSyncPacket(dataPlayer.getUniqueId(), dataPlayer.getFoodLevel(), dataPlayer.getSaturation()));
        }
    }

    private void broadcastPeriodicUpdates() {
        final Player forced = this.plugin.getSyncController().getForcedSyncPlayer().orElse(null);

        if (forced != null) {
            this.broadcastCurrentState(forced);
            return;
        }

        for (final Player player : Bukkit.getOnlinePlayers()) {
            this.broadcastCurrentState(player);
        }
    }

    private void broadcastCurrentState(Player dataPlayer) {
        this.sendHungerUpdate(dataPlayer, dataPlayer.getFoodLevel(), dataPlayer.getSaturation());
    }

    private void sendHungerUpdate(Player dataPlayer, int foodLevel, float saturation) {
        this.plugin.getSyncController().broadcastPacketToSpectators(dataPlayer, PERMISSION, new ClientboundFoodSyncPacket(dataPlayer.getUniqueId(), foodLevel, saturation));
    }
}
