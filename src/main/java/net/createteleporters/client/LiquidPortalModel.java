package net.createteleporters.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

final class LiquidPortalModel extends BakedModelWrapper<BakedModel> {
	private static final ModelProperty<Boolean> REPLACED = new ModelProperty<>();
	private final Map<BlockState, List<Face>> faces = new ConcurrentHashMap<>();

	LiquidPortalModel(BakedModel original) {
		super(original);
	}

	@Override
	public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
		return super.getModelData(level, pos, state, data).derive()
			.with(REPLACED, CustomPortalSurfaceClient.owner(level, pos) != null).build();
	}

	@Override
	public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
		return Boolean.TRUE.equals(data.get(REPLACED)) ? List.of() : super.getQuads(state, side, random, data, type);
	}

	List<Face> faces(BlockState state) {
		return faces.computeIfAbsent(state, key -> {
			List<Face> result = new ArrayList<>();
			addFaces(result, key, null);
			for (Direction side : Direction.values()) addFaces(result, key, side);
			return List.copyOf(result);
		});
	}

	private void addFaces(List<Face> result, BlockState state, Direction side) {
		for (BakedQuad quad : originalModel.getQuads(state, side, RandomSource.create(42), ModelData.EMPTY, RenderType.translucent())) {
			int[] packed = quad.getVertices();
			int stride = packed.length / 4;
			float[][] vertices = new float[4][9];
			for (int i = 0; i < 4; i++) {
				int start = i * stride;
				for (int axis = 0; axis < 3; axis++) vertices[i][axis] = Float.intBitsToFloat(packed[start + axis]);
				vertices[i][3] = Float.intBitsToFloat(packed[start + 4]);
				vertices[i][4] = Float.intBitsToFloat(packed[start + 5]);
				int color = packed[start + 3];
				for (int channel = 0; channel < 4; channel++) vertices[i][5 + channel] = (color >>> (channel * 8)) & 255;
			}
			result.add(new Face(quad, side, vertices));
		}
	}

	record Face(BakedQuad quad, Direction cullFace, float[][] vertices) {
	}
}
