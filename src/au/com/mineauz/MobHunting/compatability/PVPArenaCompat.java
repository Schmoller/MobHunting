package au.com.mineauz.MobHunting.compatability;

import net.slipcor.pvparena.events.PADeathEvent;
import net.slipcor.pvparena.events.PAExitEvent;
import net.slipcor.pvparena.events.PAJoinEvent;
import net.slipcor.pvparena.events.PALeaveEvent;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import au.com.mineauz.MobHunting.MobHunting;

public class PVPArenaCompat implements Listener {
	public PVPArenaCompat() {
		if (isDisabledInConfig()) {
			MobHunting.instance.getLogger().info(
					"Compatability with PvpArena is disabled in config.yml");
		} else {
			Bukkit.getPluginManager().registerEvents(this, MobHunting.instance);
			MobHunting.instance.getLogger().info(
					"Enabling PVPArena Compatability");
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static boolean isDisabledInConfig() {
		return MobHunting.config().disableIntegrationPvpArena;
	}

	public static boolean isEnabledInConfig() {
		return !MobHunting.config().disableIntegrationPvpArena;
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************
	@EventHandler(priority = EventPriority.NORMAL)
	private void onPvpPlayerJoin(PAJoinEvent event) {
		MobHunting.debug("[MH]Player %s joined PVPArena: %s", event.getPlayer()
				.getName(), event.getArena());
		MobArenaHelper.startPlayingMobArena(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	private void onPvpPlayerDeath(PADeathEvent event) {
		MobHunting.debug("[MH]Player %s died in PVPArena: %s", event
				.getPlayer().getName(), event.getArena());
		MobArenaHelper.startPlayingMobArena(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	private void onPvpPlayerLeave(PALeaveEvent event) {
		MobHunting.debug("[MH]Player %s left PVPArena: %s", event.getPlayer()
				.getName(), event.getArena());
		MobArenaHelper.startPlayingMobArena(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	private void onPAExit(PAExitEvent event) {
		MobHunting.debug("[MH]Player %s exit PVPArena: %s", event.getPlayer()
				.getName(), event.getArena());
		MobArenaHelper.startPlayingMobArena(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	private void onPADeath(PADeathEvent event) {
		MobHunting.debug("[MH]Player %s died in PVPArena: %s", event
				.getPlayer().getName(), event.getArena());
		MobArenaHelper.startPlayingMobArena(event.getPlayer());
	}

	// More events at
	// https://github.com/slipcor/pvparena/tree/master/src/net/slipcor/pvparena/events

}
