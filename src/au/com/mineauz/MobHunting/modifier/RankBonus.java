package au.com.mineauz.MobHunting.modifier;

import java.util.Iterator;
import java.util.Map.Entry;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import au.com.mineauz.MobHunting.DamageInformation;
import au.com.mineauz.MobHunting.HuntData;
import au.com.mineauz.MobHunting.Messages;
import au.com.mineauz.MobHunting.MobHunting;

public class RankBonus implements IModifier {

	@Override
	public String getName() {
		return ChatColor.GRAY + Messages.getString("bonus.rank.name");
	}

	@Override
	public double getMultiplier(LivingEntity deadEntity, Player killer,
			HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if (!killer.isOp()) {
			Iterator<Entry<String, String>> ranks = MobHunting.config().rankMultiplier
					.entrySet().iterator();
			while (ranks.hasNext()) {
				Entry<String, String> rank = ranks.next();
				if (!rank.getKey().equalsIgnoreCase("mobhunting")
						&& !rank.getKey().equalsIgnoreCase(
								"mobhunting.multiplier"))
					if (killer.hasPermission(rank.getKey())) {
						return Double.valueOf(rank.getValue());
					}
			}
		} else if (MobHunting.config().rankMultiplier
				.containsKey("mobhunting.multiplier.op"))
			return Double.valueOf(MobHunting.config().rankMultiplier
					.get("mobhunting.multiplier.op"));
		return 1;
	}

	@Override
	public boolean doesApply(LivingEntity deadEntity, Player killer,
			HuntData data, DamageInformation extraInfo,
			EntityDamageByEntityEvent lastDamageCause) {
		if (!killer.isOp()) {
			Iterator<Entry<String, String>> ranks = MobHunting.config().rankMultiplier
					.entrySet().iterator();
			while (ranks.hasNext()) {
				Entry<String, String> rank = ranks.next();
				if (!rank.getKey().equalsIgnoreCase("mobhunting")
						&& !rank.getKey().equalsIgnoreCase(
								"mobhunting.multiplier"))
					if (killer.hasPermission(rank.getKey())) {
						MobHunting.debug("RankMultiplier Key=%s Value=%s",
								rank.getKey(), rank.getValue());
						return true;
					}
			}
		} else if (MobHunting.config().rankMultiplier
				.containsKey("mobhunting.multiplier.op")) {
			MobHunting.debug(
					"RankMultiplier Key=mobhunting.multiplier.op Value=%s",
					MobHunting.config().rankMultiplier
							.get("mobhunting.multiplier.op"));
			return true;
		}
		MobHunting.debug("%s has no Rank Multiplier", killer.getName());
		return false;
	}
}
