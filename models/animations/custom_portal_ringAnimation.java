// Save this class in your mod and generate all required imports

/**
 * Made with Blockbench 5.0.3 Exported for Minecraft version 1.19 or later with
 * Mojang mappings
 * 
 * @author Author
 */
public class custom_portal_ringAnimation {
	public static final AnimationDefinition spin = AnimationDefinition.Builder.withLength(2.0F).looping()
			.addAnimation("group",
					new AnimationChannel(AnimationChannel.Targets.ROTATION,
							new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F),
									AnimationChannel.Interpolations.LINEAR),
							new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 360.0F),
									AnimationChannel.Interpolations.LINEAR)))
			.build();
}