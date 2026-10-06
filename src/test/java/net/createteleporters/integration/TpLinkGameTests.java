package net.createteleporters.integration;

import net.createteleporters.init.CreateteleportersModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("createteleporters")
@PrefixGameTestTemplate(false)
public final class TpLinkGameTests {
    @GameTest(template = "train_test")
    public static void targetingUsesLoadedChunksAndClickedHand(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(0, 70, 0));
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setYRot(-90); player.setYHeadRot(-90); player.setXRot(0);
        BlockPos target = base.east(4).above();
        for (int x = 0; x <= 4; x++) level.setBlock(base.east(x).above(), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(target, Blocks.STONE.defaultBlockState(), 2);
        for (Item item : new Item[] { CreateteleportersModItems.TP_LINK.get(), CreateteleportersModItems.ADV_TPLINK.get() }) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = new ItemStack(item);
                CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString("marker", "preserved"));
                ItemStack other = new ItemStack(Items.STONE);
                player.setItemInHand(hand, stack);
                player.setItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, other);
                player.setPos(base.getX() + .5, base.getY(), base.getZ() + .5);
                item.use(level, player, hand);
                CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                check(data.getDouble("xpo") == target.getX() + .5 && data.getDouble("ypo") == target.getY() + .9
                    && data.getDouble("zpo") == target.getZ() + .5 && data.getDouble("yawpo") == -90, "Save the hit block and yaw");
                check(data.getString("marker").equals("preserved") && !other.has(DataComponents.CUSTOM_DATA), "Update only the clicked hand and preserve other tags");
                check(item == CreateteleportersModItems.ADV_TPLINK.get()
                    ? data.getString("dimension").equals(level.dimension().location().toString()) : !data.contains("dimension"), "Advanced link saves the dimension");

                
                int chunkX = base.getX() >> 4, chunkZ = base.getZ() >> 4;
                while (level.getChunkSource().getChunkNow(chunkX, chunkZ) != null && chunkX < (base.getX() >> 4) + 160) chunkX++;
                check(level.getChunkSource().getChunkNow(chunkX, chunkZ) == null, "Fixture needs an unloaded chunk along the ray");
                player.setPos((chunkX - 1) * 16 + 14.5, base.getY() + 20, base.getZ() + .5);
                int loadedBefore = level.getChunkSource().getLoadedChunksCount();
                item.use(level, player, hand);
                check(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().equals(data), "A miss must preserve the saved destination");
                check(level.getChunkSource().getLoadedChunksCount() == loadedBefore && level.getChunkSource().getChunkNow(chunkX, chunkZ) == null,
                    "Distant targeting must not load or generate chunks");
                player.setXRot(-90);
                item.use(level, player, hand);
                check(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().equals(data), "Looking into the sky must preserve the destination");
                check(level.getChunkSource().getLoadedChunksCount() == loadedBefore, "Sky targeting must not load chunks");
                player.setXRot(0);
            }
        }
        level.removeBlock(target, false);
        helper.succeed();
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
