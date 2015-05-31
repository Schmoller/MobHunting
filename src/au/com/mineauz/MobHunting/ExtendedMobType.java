package au.com.mineauz.MobHunting;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Rabbit;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Skeleton.SkeletonType;

public enum ExtendedMobType {
	// Giant is unsupported by in the original game and Giants can only be
	// spawnwed through plugins.
	Giant("GIANT", 100),
	// Minecraft 1.7
	Slime("SLIME", 100), MagmaCube("MAGMA_CUBE", 100), Ghast("GHAST", 80), Blaze(
			"BLAZE", 80), Creeper("CREEPER", 100), Enderman("ENDERMAN", 100), Silverfish(
			"SILVERFISH", 100), Skeleton("SKELETON", 100), WitherSkeleton(
			"SKELETON", 80), Spider("SPIDER", 100), CaveSpider("CAVE_SPIDER",
			100), Witch("WITCH", 80), Wither("WITHER", 20), Zombie("ZOMBIE",
			100), ZombiePigman("PIG_ZOMBIE", 100), BonusMob("UNKNOWN", 20),
	// Minecraft 1.8 Entity's
	Endermite("ENDERMITE", 100), Guardian("GUARDIAN", 100), KillerRabbit(
			"RABBIT", 100), PvpPlayer("PLAYER", 100);

	private String mType;
	private int mMax;

	private ExtendedMobType(String type, int max) {
		mType = type;
		mMax = max;
	}

	public String getEntType() {
		return mType;
	}

	public int getMax() {
		return mMax;
	}

	public boolean matches(Entity ent) {
		// test if MC 1.8 classes exists
		try {
			Class cls = Class.forName("org.bukkit.entity.Rabbit");
			if (this == KillerRabbit)
				// return ent instanceof Rabbit && (((Rabbit)
				// ent).getRabbitType().equals(Rabbit.Type.THE_KILLER_BUNNY));
				return ent instanceof Rabbit
						&& (((Rabbit) ent).getRabbitType()) == Rabbit.Type.THE_KILLER_BUNNY;

		} catch (ClassNotFoundException e) {
			// not MC 1.8
		}
		if (this == WitherSkeleton)
			return ent instanceof Skeleton
					&& ((Skeleton) ent).getSkeletonType() == SkeletonType.WITHER;
		else if (this == Skeleton)
			return ent instanceof Skeleton
					&& ((Skeleton) ent).getSkeletonType() == SkeletonType.NORMAL;
		else if (this == BonusMob)
			return ent.hasMetadata("MH:hasBonus"); //$NON-NLS-1$
		else {
			return ent.getType().toString() == mType;
		}
	}

	public String getName() {
		return Messages.getString("mobs." + name() + ".name"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static ExtendedMobType fromEntity(Entity entity) {
		for (ExtendedMobType type : values()) {
			if (type.matches(entity))
				return type;
		}

		return null;
	}
}
