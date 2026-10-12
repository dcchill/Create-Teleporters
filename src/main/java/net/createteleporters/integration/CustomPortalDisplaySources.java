package net.createteleporters.integration;

import java.math.BigDecimal;
import java.util.function.Function;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.FillLevelDisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.createteleporters.CreateteleportersMod;
import net.createteleporters.block.CustomPortalBaseBlock;
import net.createteleporters.block.entity.CustomPortalBaseBlockEntity;
import net.createteleporters.init.CreateteleportersModBlocks;
import net.createteleporters.util.CustomPortalSetup;
import net.createteleporters.util.CustomPortalTeleportMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CustomPortalDisplaySources {
    private static final DeferredRegister<DisplaySource> REGISTRY = DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, CreateteleportersMod.MODID);
    public static final DeferredHolder<DisplaySource, DisplaySource> STATUS = REGISTRY.register("portal_status",
        () -> new PortalTextSource(base -> CustomPortalSetup.status(base).message().copy()));
    public static final DeferredHolder<DisplaySource, DisplaySource> FUEL = REGISTRY.register("portal_fuel",
        () -> new PortalTextSource(base -> Component.translatable("createteleporters.display_source.telejuice_amount", base.getFluidTank().getFluidAmount())));
    public static final DeferredHolder<DisplaySource, DisplaySource> FILL = REGISTRY.register("portal_fuel_fill", PortalFillSource::new);
    public static final DeferredHolder<DisplaySource, DisplaySource> DESTINATION = REGISTRY.register("portal_destination",
        () -> new PortalTextSource(CustomPortalDisplaySources::destination));

    private CustomPortalDisplaySources() {}

    public static void register(IEventBus bus) {
        REGISTRY.register(bus);
        bus.addListener(CustomPortalDisplaySources::setup);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (var source : REGISTRY.getEntries()) {
                DisplaySource.BY_BLOCK.add(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE.get(), source.get());
                DisplaySource.BY_BLOCK.add(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get(), source.get());
            }
        });
    }

    private static BlockEntity loadedBlockEntity(ServerLevel level, BlockPos pos) {
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockEntity(pos);
    }

    private static CustomPortalBaseBlockEntity controller(DisplayLinkContext context) {
        if (!(context.level() instanceof ServerLevel level)) return null;
        BlockPos pos = context.getSourcePos();
        BlockEntity source = loadedBlockEntity(level, pos);
        if (source instanceof CustomPortalBaseBlockEntity base) return base;
        if (source == null || !source.getBlockState().is(CreateteleportersModBlocks.CUSTOM_PORTAL_BASE_DUMMY_BLOCK.get())) return null;
        CompoundTag data = source.getPersistentData();
        if (!data.contains("main_x", Tag.TAG_ANY_NUMERIC) || !data.contains("main_y", Tag.TAG_ANY_NUMERIC) || !data.contains("main_z", Tag.TAG_ANY_NUMERIC)) return null;
        double x = data.getDouble("main_x"), y = data.getDouble("main_y"), z = data.getDouble("main_z");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return null;
        BlockPos main = BlockPos.containing(x, y, z);
        if (!(loadedBlockEntity(level, main) instanceof CustomPortalBaseBlockEntity base)) return null;
        var facing = base.getBlockState().getValue(CustomPortalBaseBlock.FACING);
        return pos.equals(main.relative(facing.getClockWise())) || pos.equals(main.relative(facing.getCounterClockWise())) ? base : null;
    }

    private static MutableComponent destination(CustomPortalBaseBlockEntity base) {
        CompoundTag data = base.getPersistentData();
        String dim, xKey, yKey, zKey;
        if (CustomPortalTeleportMode.COORDINATE.equals(CustomPortalTeleportMode.getOrMigrate(base))) {
            if (!CustomPortalSetup.hasLink(base)) return noDestination();
            data = base.getItem(0).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            dim = data.getString("dimension").trim();
            xKey = "xpo"; yKey = "ypo"; zKey = "zpo";
        } else {
            if (!data.getBoolean("isLinked")) return noDestination();
            dim = data.getString("linkedDim").trim();
            xKey = "linkedX"; yKey = "linkedY"; zKey = "linkedZ";
        }
        if (dim.isEmpty() || ResourceLocation.tryParse(dim) == null
            || !data.contains(xKey, Tag.TAG_ANY_NUMERIC) || !data.contains(yKey, Tag.TAG_ANY_NUMERIC) || !data.contains(zKey, Tag.TAG_ANY_NUMERIC)) return noDestination();
        double x = data.getDouble(xKey), y = data.getDouble(yKey), z = data.getDouble(zKey);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return noDestination();
        return Component.literal(dim + " " + coordinate(x) + ", " + coordinate(y) + ", " + coordinate(z));
    }

    private static String coordinate(double value) { return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
    private static MutableComponent noDestination() { return Component.translatable("createteleporters.display_source.no_destination"); }

    private static final class PortalTextSource extends SingleLineDisplaySource {
        private final Function<CustomPortalBaseBlockEntity, MutableComponent> read;
        private PortalTextSource(Function<CustomPortalBaseBlockEntity, MutableComponent> read) { this.read = read; }
        @Override protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
            var base = controller(context);
            return base == null ? EMPTY_LINE : read.apply(base);
        }
        @Override protected boolean allowsLabeling(DisplayLinkContext context) { return true; }
        @Override public int getPassiveRefreshTicks() { return 20; }
    }

    private static final class PortalFillSource extends FillLevelDisplaySource {
        @Override protected Float getProgress(DisplayLinkContext context) {
            var base = controller(context);
            return base == null ? null : base.getFluidTank().getFluidAmount() / (float) base.getFluidTank().getCapacity();
        }
        @Override protected String getTranslationKey() { return "portal_fuel_fill"; }
        @Override public int getPassiveRefreshTicks() { return 20; }
    }
}
