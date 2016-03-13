package au.com.mineauz.MobHunting.compatability;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

import au.com.mineauz.MobHunting.MobHunting;
import au.com.mineauz.MobHunting.MobRewardData;
import au.com.mineauz.MobHunting.MobPlugins;
//import net.elseland.xikage.MythicMobs.API.Events.MythicMobCustomSkillEvent;
//import net.elseland.xikage.MythicMobs.API.Events.MythicMobDeathEvent;
//import net.elseland.xikage.MythicMobs.API.Events.MythicMobSkillEvent;
//import net.elseland.xikage.MythicMobs.API.Events.MythicMobSpawnEvent;
//import net.elseland.xikage.MythicMobs.Mobs.MythicMob;
import net.elseland.xikage.MythicMobs.API.Bukkit.Events.*;

public class MythicMobsCompat implements Listener {

	private static boolean supported = false;
	private static Plugin mPlugin;
	private static HashMap<String, MobRewardData> mMobRewardData = new HashMap<String, MobRewardData>();
	private File file = new File(MobHunting.instance.getDataFolder(),
			"mythicmobs-rewards.yml");
	private YamlConfiguration config = new YamlConfiguration();

	public MythicMobsCompat() {
		if (isDisabledInConfig()) {
			MobHunting.instance.getLogger().info(
					"Compatability with MythicMobs is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin("MythicMobs");

			Bukkit.getPluginManager().registerEvents(this, MobHunting.instance);

			MobHunting.instance.getLogger().info(
					"Enabling Compatability with MythicMobs ("
							+ getMythicMobs().getDescription().getVersion()
							+ ")");
			// API:
			// http://xikage.elseland.net/viewgit/?a=tree&p=MythicMobs&h=dec796decd1ef71fdd49aed69aef85dc7d82b1c1&hb=ffeb51fb84e882365846a30bd2b9753716faf51e&f=MythicMobs/src/net/elseland/xikage/MythicMobs/API
			supported = true;

			loadMythicMobsData();
			saveMythicMobsData();
		}
	}

	// **************************************************************************
	// LOAD & SAVE
	// **************************************************************************
	public void loadMythicMobsData() {
		try {
			if (!file.exists())
				return;

			config.load(file);
			for (String key : config.getKeys(false)) {
				ConfigurationSection section = config
						.getConfigurationSection(key);
				MobRewardData mob = new MobRewardData();
				mob.read(section);
				mob.setMobType(key);
				mMobRewardData.put(key, mob);
			}
			MobHunting.debug("Loaded %s MythicMobs", mMobRewardData.size());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void loadMythicMobsData(String key) {
		try {
			if (!file.exists())
				return;

			config.load(file);
			ConfigurationSection section = config.getConfigurationSection(key);
			MobRewardData mob = new MobRewardData();
			mob.read(section);
			mob.setMobType(key);
			mMobRewardData.put(key, mob);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void saveMythicMobsData() {
		try {
			config.options()
					.header("This a extra MobHunting config data for the MythicMobs on your server.");

			if (mMobRewardData.size() > 0) {

				int n = 0;
				for (String str : mMobRewardData.keySet()) {
					ConfigurationSection section = config.createSection(str);
					mMobRewardData.get(str).save(section);
					n++;
				}

				if (n != 0) {
					MobHunting
							.debug("Saving Mobhunting extra MythicMobs data.");
					config.save(file);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveMythicMobsData(String key) {
		try {
			if (mMobRewardData.containsKey(key)) {
				ConfigurationSection section = config.createSection(key);
				mMobRewardData.get(key).save(section);
				MobHunting.debug("Saving Mobhunting extra MythicMobs data.");
				config.save(file);
			} else {
				MobHunting.debug(
						"ERROR! MythicMobs ID (%s) is not found in mNPCData",
						key);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static Plugin getMythicMobs() {
		return mPlugin;
	}

	public static boolean isMythicMobsSupported() {
		return supported;
	}
	
	public static boolean isMythicMob(LivingEntity mob){
		return mob.hasMetadata("MH:MythicMob");
	}
	
	public static String getMythicMobType(LivingEntity mob){
		List<MetadataValue> data = mob.getMetadata("MH:MythicMob");
		MetadataValue value = data.get(0);
		return ((MobRewardData) value.value()).getMobType();		
	}

	public static HashMap<String, MobRewardData> getMobRewardData() {
		return mMobRewardData;
	}

	public static boolean isDisabledInConfig() {
		return MobHunting.config().disableIntegrationMythicmobs;
	}

	public static boolean isEnabledInConfig() {
		return !MobHunting.config().disableIntegrationMythicmobs;
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onMythicMobDeathEvent(MythicMobDeathEvent event) {
		// if (event.getKiller() != null)
		// MobHunting
		// .debug("MythicMob Death event, killer is %s, mobname=%s, Mobname=%s",
		// event.getKiller().getName(), event.MobName,
		// event.getMobType().MobName);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onMythicMobSpawnEvent(MythicMobSpawnEvent event) {
		MobHunting.debug("MythicMob spawn event: MinecraftMobtype=%s MythicMobType=%s", event
				.getLivingEntity().getType(), event.getMobType().MobName);

		if (mMobRewardData != null
				&& !mMobRewardData.containsKey(event.getMobType().MobName)) {
			MobHunting.debug("New MythicMobType found=%s,%s", event
					.getMobType().MobName, event.getMobType().getDisplayName());
			mMobRewardData.put(event.getMobType().MobName, new MobRewardData(
					MobPlugins.MobPluginNames.MythicMobs, event.getMobType().MobName, 
					event.getMobType().getDisplayName(), "10",
					"give {player} iron_sword 1", "You got an Iron sword.",
					100, 100));
			saveMythicMobsData(event.getMobType().MobName);
		}

		event.getLivingEntity().setMetadata(
				"MH:MythicMob",
				new FixedMetadataValue(mPlugin,
						mMobRewardData.get(event.getMobType().MobName)));
				//new FixedMetadataValue(mPlugin,event.getMobType().MobName));

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onMythicMobSkillEvent(MythicMobSkillEvent event) {
		// MobHunting.debug("MythicMob Skill event");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onMythicMobCustomSkillEvent(MythicMobCustomSkillEvent event) {
		// MobHunting.debug("MythicMob Custom Skill event");
	}

}
