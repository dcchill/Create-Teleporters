package net.createteleporters.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.procedures.QuantumPortalBlockOnTickUpdateProcedure;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class QuantumPortalBlockBlock extends IronBarsBlock {
	private static final int TRAIN_PORTAL_SEARCH_RADIUS = 24;
	private static final int PORTAL_BASE_SEARCH_RADIUS = 23;
	private static final String PORTAL_COLOR_TAG = "portalColor";
	public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
	private static final Map<String, String> BOUND_TRAIN_TRACK_REFRESH_KEYS = new ConcurrentHashMap<>();

	public QuantumPortalBlockBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.GLASS).strength(-1, 3600000).noOcclusion().hasPostProcess((bs, br, bp) -> true).emissiveRendering((bs, br, bp) -> true).isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.defaultBlockState().setValue(COLOR, DyeColor.PURPLE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(COLOR);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(stack.getItem() instanceof DyeItem dyeItem)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (world.isClientSide()) {
			return ItemInteractionResult.SUCCESS;
		}

		DyeColor color = dyeItem.getDyeColor();
		BlockPos basePos = findPortalBase(world, pos);
		if (basePos == null) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}

		boolean changed = setPortalColor(world, basePos, color);
		if (world instanceof ServerLevel serverLevel) {
			changed |= setLinkedPortalColor(serverLevel, basePos, color);
		}
		if (changed && !player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		return ItemInteractionResult.SUCCESS;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		world.scheduleTick(pos, this, 3);
		scheduleAdjacentCreateTracks(world, pos);
	}

	@Override
	public void tick(BlockState blockstate, ServerLevel world, BlockPos pos, RandomSource random) {
		super.tick(blockstate, world, pos, random);
		QuantumPortalBlockOnTickUpdateProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
		scheduleAdjacentCreateTracks(world, pos);
		world.scheduleTick(pos, this, 3);
	}

	private static void scheduleAdjacentCreateTracks(Level world, BlockPos pos) {
		if (!(world instanceof ServerLevel serverLevel)) {
			return;
		}

		String readyLinkKey = getReadyLinkedPortalRefreshKey(serverLevel, pos);
		if (readyLinkKey == null || !hasAdjacentCreateTrack(serverLevel, pos)) {
			return;
		}

		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos trackPos = pos.relative(direction);
			BlockState trackState = serverLevel.getBlockState(trackPos);
			Block trackBlock = trackState.getBlock();
			if (!isCreateTrackBlock(trackBlock)) {
				continue;
			}

			String boundTrackKey = getBoundCreatePortalTrackKey(serverLevel, trackPos);
			String trackStateKey = boundTrackKey == null ? "<unbound>" : boundTrackKey;
			if (shouldRefreshBoundPortalTrack(serverLevel, pos, trackPos, readyLinkKey, trackStateKey)) {
				serverLevel.scheduleTick(trackPos, trackBlock, 1);
			}
		}
	}

	private static boolean hasAdjacentCreateTrack(Level world, BlockPos pos) {
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			if (isCreateTrackBlock(world.getBlockState(pos.relative(direction)).getBlock())) {
				return true;
			}
		}

		return false;
	}

	private static boolean isCreateTrackBlock(Block block) {
		try {
			return Class.forName("com.simibubi.create.content.trains.track.ITrackBlock").isInstance(block);
		} catch (ClassNotFoundException | LinkageError ignored) {
			return false;
		}
	}

	private static String getReadyLinkedPortalRefreshKey(ServerLevel sourceLevel, BlockPos portalPos) {
		BlockPos min = portalPos.offset(-TRAIN_PORTAL_SEARCH_RADIUS, -TRAIN_PORTAL_SEARCH_RADIUS, -TRAIN_PORTAL_SEARCH_RADIUS);
		BlockPos max = portalPos.offset(TRAIN_PORTAL_SEARCH_RADIUS, TRAIN_PORTAL_SEARCH_RADIUS, TRAIN_PORTAL_SEARCH_RADIUS);

		for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
			if (!sourceLevel.getBlockState(cursor).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get())) {
				continue;
			}

			BlockEntity blockEntity = sourceLevel.getBlockEntity(cursor);
			if (blockEntity == null) {
				continue;
			}

			CompoundTag nbt = blockEntity.getPersistentData();
			if (!nbt.getBoolean("isLinked") || !nbt.getBoolean("portalActive")) {
				continue;
			}
			if (!isPortalInteriorBlock(cursor, portalPos, nbt)) {
				continue;
			}
			if (isLinkedTargetActive(sourceLevel, nbt)) {
				return cursor.asLong() + "|" + nbt.getString("linkedDim") + "|" + nbt.getDouble("linkedX") + "|" + nbt.getDouble("linkedY") + "|" + nbt.getDouble("linkedZ");
			}
		}

		return null;
	}

	private static String getBoundCreatePortalTrackKey(Level world, BlockPos trackPos) {
		try {
			BlockEntity blockEntity = world.getBlockEntity(trackPos);
			if (blockEntity == null || !Class.forName("com.simibubi.create.content.trains.track.TrackBlockEntity").isInstance(blockEntity)) {
				return null;
			}

			Object boundLocation = blockEntity.getClass().getField("boundLocation").get(blockEntity);
			return boundLocation == null ? null : boundLocation.toString();
		} catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | LinkageError ignored) {
			return null;
		}
	}

	private static boolean shouldRefreshBoundPortalTrack(ServerLevel level, BlockPos portalPos, BlockPos trackPos, String readyLinkKey, String boundTrackKey) {
		String trackRefreshKey = level.dimension().location() + "|" + portalPos.asLong() + "|" + trackPos.asLong();
		String refreshValue = readyLinkKey + "|" + boundTrackKey;
		String previousValue = BOUND_TRAIN_TRACK_REFRESH_KEYS.put(trackRefreshKey, refreshValue);
		return !refreshValue.equals(previousValue);
	}

	private static boolean isLinkedTargetActive(ServerLevel sourceLevel, CompoundTag nbt) {
		ResourceLocation targetDimLoc = ResourceLocation.tryParse(nbt.getString("linkedDim").trim());
		if (targetDimLoc == null) {
			return false;
		}

		ResourceKey<net.minecraft.world.level.Level> targetDim = ResourceKey.create(Registries.DIMENSION, targetDimLoc);
		ServerLevel targetLevel = sourceLevel.getServer().getLevel(targetDim);
		if (targetLevel == null) {
			return false;
		}

		BlockPos targetBasePos = BlockPos.containing(
			nbt.getDouble("linkedX"),
			nbt.getDouble("linkedY"),
			nbt.getDouble("linkedZ")
		);
		BlockEntity targetBlockEntity = targetLevel.getBlockEntity(targetBasePos);
		return targetBlockEntity != null && targetBlockEntity.getPersistentData().getBoolean("portalActive");
	}

	private static boolean isPortalInteriorBlock(BlockPos basePos, BlockPos portalPos, CompoundTag nbt) {
		if (!nbt.contains("portalHeight") || !nbt.contains("portalMinExtent") || !nbt.contains("portalMaxExtent")) {
			return false;
		}

		int portalHeight = nbt.getInt("portalHeight");
		int interiorMin = nbt.getInt("portalMinExtent") + 1;
		int interiorMax = nbt.getInt("portalMaxExtent") - 1;
		String rotation = nbt.getString("rotation");
		int dy = portalPos.getY() - basePos.getY();

		if (dy < 1 || dy > (portalHeight - 1)) {
			return false;
		}

		if ("east".equals(rotation) || "west".equals(rotation)) {
			int dz = portalPos.getZ() - basePos.getZ();
			return portalPos.getX() == basePos.getX() && dz >= interiorMin && dz <= interiorMax;
		}

		int dx = portalPos.getX() - basePos.getX();
		return portalPos.getZ() == basePos.getZ() && dx >= interiorMin && dx <= interiorMax;
	}

	public static String getStoredPortalColorName(BlockEntity controller) {
		if (controller == null) {
			return DyeColor.PURPLE.getName();
		}
		String storedColor = controller.getPersistentData().getString(PORTAL_COLOR_TAG);
		for (DyeColor color : DyeColor.values()) {
			if (color.getName().equals(storedColor)) {
				return storedColor;
			}
		}
		return DyeColor.PURPLE.getName();
	}

	private static BlockPos findPortalBase(Level world, BlockPos portalPos) {
		for (int yOffset = 1; yOffset < PORTAL_BASE_SEARCH_RADIUS; yOffset++) {
			int baseY = portalPos.getY() - yOffset;
			for (int horizontalOffset = -PORTAL_BASE_SEARCH_RADIUS; horizontalOffset <= PORTAL_BASE_SEARCH_RADIUS; horizontalOffset++) {
				BlockPos xCandidate = new BlockPos(portalPos.getX() - horizontalOffset, baseY, portalPos.getZ());
				if (isControllerForPortalBlock(world, xCandidate, portalPos)) {
					return xCandidate;
				}
				BlockPos zCandidate = new BlockPos(portalPos.getX(), baseY, portalPos.getZ() - horizontalOffset);
				if (isControllerForPortalBlock(world, zCandidate, portalPos)) {
					return zCandidate;
				}
			}
		}
		return null;
	}

	private static boolean isControllerForPortalBlock(Level world, BlockPos basePos, BlockPos portalPos) {
		if (!world.getBlockState(basePos).is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get())) {
			return false;
		}
		BlockEntity controller = world.getBlockEntity(basePos);
		return controller != null && isPortalInteriorBlock(basePos, portalPos, controller.getPersistentData());
	}

	private static boolean setLinkedPortalColor(ServerLevel sourceLevel, BlockPos sourceBasePos, DyeColor color) {
		BlockEntity sourceController = sourceLevel.getBlockEntity(sourceBasePos);
		if (sourceController == null || !sourceController.getPersistentData().getBoolean("isLinked")) {
			return false;
		}
		CompoundTag nbt = sourceController.getPersistentData();
		ResourceLocation targetDimLoc = ResourceLocation.tryParse(nbt.getString("linkedDim"));
		if (targetDimLoc == null) {
			return false;
		}
		ServerLevel targetLevel = sourceLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, targetDimLoc));
		if (targetLevel == null) {
			return false;
		}
		BlockPos targetBasePos = BlockPos.containing(nbt.getDouble("linkedX"), nbt.getDouble("linkedY"), nbt.getDouble("linkedZ"));
		return setPortalColor(targetLevel, targetBasePos, color);
	}

	private static boolean setPortalColor(Level world, BlockPos basePos, DyeColor color) {
		BlockEntity controller = world.getBlockEntity(basePos);
		if (controller == null) {
			return false;
		}
		CompoundTag nbt = controller.getPersistentData();
		boolean changed = !color.getName().equals(getStoredPortalColorName(controller));
		nbt.putString(PORTAL_COLOR_TAG, color.getName());
		controller.setChanged();
		world.sendBlockUpdated(basePos, world.getBlockState(basePos), world.getBlockState(basePos), 3);

		if (!nbt.contains("portalHeight") || !nbt.contains("portalMinExtent") || !nbt.contains("portalMaxExtent")) {
			return changed;
		}
		int portalHeight = nbt.getInt("portalHeight");
		int interiorMin = nbt.getInt("portalMinExtent") + 1;
		int interiorMax = nbt.getInt("portalMaxExtent") - 1;
		boolean eastWest = "east".equals(nbt.getString("rotation")) || "west".equals(nbt.getString("rotation"));
		for (int y = 1; y < portalHeight; y++) {
			for (int horizontal = interiorMin; horizontal <= interiorMax; horizontal++) {
				BlockPos portalPos = eastWest ? basePos.offset(0, y, horizontal) : basePos.offset(horizontal, y, 0);
				BlockState portalState = world.getBlockState(portalPos);
				if (portalState.is(CreateteleportersModBlocks.QUANTUM_PORTAL_BLOCK.get()) && portalState.getValue(COLOR) != color) {
					world.setBlock(portalPos, portalState.setValue(COLOR, color), 3);
					changed = true;
				}
			}
		}
		return changed;
	}
}
