package waterbending;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import tools.Abilities;
import tools.AvatarState;
import tools.ConfigManager;
import tools.TempBlock;
import tools.Tools;

public class Melt {

	private static final int defaultrange = FreezeMelt.defaultrange;
	private static final int defaultradius = FreezeMelt.defaultradius;
	private static final int defaultevaporateradius = 3;
	private static final int seaLevel = ConfigManager.seaLevel;

	private static final byte full = 0x0;

	public Melt(Player player) {
		int range = (int) Tools.waterbendingNightAugment(defaultrange,
				player.getWorld());
		int radius = (int) Tools.waterbendingNightAugment(defaultradius,
				player.getWorld());

		if (AvatarState.isAvatarState(player)) {
			range = AvatarState.getValue(range);
			radius = AvatarState.getValue(radius);
		}
		boolean evaporate = false;
		Location location = Tools.getTargetedLocation(player, range);
		if (Tools.isWater(player.getTargetBlock(null, range))
				&& !(player.getEyeLocation().getBlockY() <= 62)) {
			evaporate = true;
			radius = (int) Tools.waterbendingNightAugment(
					defaultevaporateradius, player.getWorld());
		}
		for (Block block : Tools.getBlocksAroundPoint(location, radius)) {
			if (evaporate) {
				if (block.getY() > seaLevel)
					evaporate(player, block);
			} else {
				melt(player, block);
			}
		}
	}

	public static void melt(Player player, Block block) {
		if (Tools.isRegionProtectedFromBuild(player, Abilities.PhaseChange,
				block.getLocation()))
			return;
		if (!Wave.canThaw(block)) {
			Wave.thaw(block);
			return;
		}
		if (Tools.isMeltable(block) && !TempBlock.isTempBlock(block)
				&& WaterManipulation.canPhysicsChange(block)) {
			if (block.getType() == Material.SNOW) {
				block.setType(Material.AIR);
				return;
			}
			if (FreezeMelt.frozenblocks.containsKey(block)) {
				FreezeMelt.thaw(block);
			} else {
				block.setType(Material.WATER);
				block.setData(full);
			}
		}
	}

	public static void evaporate(Player player, Block block) {
		if (Tools.isRegionProtectedFromBuild(player, Abilities.PhaseChange,
				block.getLocation()))
			return;
		if (Tools.isWater(block) && !TempBlock.isTempBlock(block)
				&& WaterManipulation.canPhysicsChange(block)) {
			block.setType(Material.AIR);
			block.getWorld().playEffect(block.getLocation(), Effect.SMOKE, 1);
		}
	}

}
