// Made with Blockbench 5.0.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modelcustom_portal_ring<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "custom_portal_ring"), "main");
	private final ModelPart group;
	private final ModelPart RING;
	private final ModelPart hexadecagon1;

	public Modelcustom_portal_ring(ModelPart root) {
		this.group = root.getChild("group");
		this.RING = root.getChild("RING");
		this.hexadecagon1 = this.RING.getChild("hexadecagon1");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition group = partdefinition.addOrReplaceChild("group", CubeListBuilder.create().texOffs(0, 171)
				.addBox(-42.5F, -42.5F, 0.0F, 85.0F, 85.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-0.5F, -24.5F, 2.0F));

		PartDefinition RING = partdefinition.addOrReplaceChild("RING", CubeListBuilder.create(),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition hexadecagon1 = RING.addOrReplaceChild("hexadecagon1", CubeListBuilder.create().texOffs(164, 50)
				.addBox(-3.0F, -9.5478F, -48.0F, 6.0F, 19.0956F, 6.0F, new CubeDeformation(0.0F)).texOffs(64, 25)
				.addBox(-3.0F, -9.5478F, 42.0F, 6.0F, 19.0956F, 6.0F, new CubeDeformation(0.0F)).texOffs(96, 25)
				.addBox(-3.0F, 42.0F, -9.5478F, 6.0F, 6.0F, 19.0956F, new CubeDeformation(0.0F)).texOffs(96, 0)
				.addBox(-3.0F, -48.0F, -9.5478F, 6.0F, 6.0F, 19.0956F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -48.0F, 0.5F, 0.0F, 1.5708F, 0.0F));

		PartDefinition hexadecagon_r1 = hexadecagon1.addOrReplaceChild("hexadecagon_r1", CubeListBuilder.create()
				.texOffs(45, 61).addBox(-0.375F, -48.0F, -9.5478F, 5.75F, 6.0F, 19.0956F, new CubeDeformation(0.0F))
				.texOffs(96, 50).addBox(-0.375F, 42.0F, -9.5478F, 5.75F, 6.0F, 19.0956F, new CubeDeformation(0.0F))
				.texOffs(192, 0).addBox(-0.375F, -9.5478F, 42.0F, 5.75F, 19.0956F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(192, 50).addBox(-0.375F, -9.5478F, -48.0F, 5.75F, 19.0956F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.5F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F));

		PartDefinition hexadecagon_r2 = hexadecagon1.addOrReplaceChild("hexadecagon_r2", CubeListBuilder.create()
				.texOffs(96, 50).addBox(-0.375F, -48.0F, -9.5478F, 5.75F, 6.0F, 19.0956F, new CubeDeformation(0.0F))
				.texOffs(127, 6).addBox(-0.375F, 42.0F, -9.5478F, 5.75F, 6.0F, 19.0956F, new CubeDeformation(0.0F))
				.texOffs(192, 25).addBox(-0.375F, -9.5478F, 42.0F, 5.75F, 19.0956F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(192, 75).addBox(-0.375F, -9.5478F, -48.0F, 5.75F, 19.0956F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.5F, 0.0F, 0.0F, 0.3927F, 0.0F, 0.0F));

		PartDefinition hexadecagon_r3 = hexadecagon1.addOrReplaceChild("hexadecagon_r3",
				CubeListBuilder.create().texOffs(64, 0)
						.addBox(-0.5F, -9.5478F, 42.0F, 6.0F, 19.0956F, 6.0F, new CubeDeformation(0.0F)).texOffs(156, 0)
						.addBox(-0.5F, -9.5478F, -48.0F, 6.0F, 19.0956F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.5F, 0.0F, 0.0F, -0.7854F, 0.0F, 0.0F));

		PartDefinition hexadecagon_r4 = hexadecagon1.addOrReplaceChild("hexadecagon_r4", CubeListBuilder.create()
				.texOffs(146, 31).addBox(-0.5F, -9.5478F, 42.0F, 6.0F, 19.0956F, 6.0F, new CubeDeformation(0.0F))
				.texOffs(188, 130).addBox(-0.5F, -9.5478F, -48.0F, 6.0F, 19.0956F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.5F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 256, 256);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		group.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		RING.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}