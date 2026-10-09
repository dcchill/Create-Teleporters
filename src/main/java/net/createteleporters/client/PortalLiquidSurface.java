package net.createteleporters.client;

final class PortalLiquidSurface {
	static final float RIPPLE_TICKS = 32;
	static final float OPENING_TICKS = 48;
	static final float MAX_DISPLACEMENT = 1.128f;

	private PortalLiquidSurface() {
	}

	static float depth(float baseDepth, float wave, float ripple, float edge) {
		float displacement = (wave * 0.008f + Math.max(0, ripple) * 0.22f) * edge;
		return baseDepth + Math.max(-1, Math.min(1, (baseDepth - 0.5f) * 16)) * displacement;
	}

	static float surfaceDepth(float baseDepth, float h, float y, float min, float max, float top,
			float time, float rippleH, float rippleY, float age, float openingAge, float[][] trainRipples) {
		float edge = Math.max(0, Math.min(1, Math.min(Math.min(h - min, max - h), Math.min(y - 1, top - y)) * 3));
		float opening = 0;
		if (openingAge >= 0 && openingAge < OPENING_TICKS || trainRipples.length != 0) {
			float radius = (float) Math.hypot(max - min, top - 1) * 0.5f;
			float distance = (float) Math.hypot(h - (min + max) * 0.5f, y - (top + 1) * 0.5f);
			opening = openingRipple(distance, radius, openingAge);
			for (float[] ripple : trainRipples) {
				if (ripple[2] < 0 || ripple[2] >= OPENING_TICKS) continue;
				distance = (float) Math.hypot(h - ripple[0], y - ripple[1]);
				opening = Math.max(opening, openingRipple(distance, radius, ripple[2]));
			}
		}
		return depth(baseDepth, wave(h, y, time), ripple((float) Math.hypot(h - rippleH, y - rippleY), age), edge)
			+ Math.max(-1, Math.min(1, (baseDepth - 0.5f) * 16)) * opening * edge;
	}

	static float openingRipple(float distance, float radius, float age) {
		if (age < 0 || age >= OPENING_TICKS) return 0;
		float front = distance - (radius + 1.5f) * age / 40;
		float width = 0.6f + radius * 0.12f;
		float fade = Math.min(1, age / 4) * (1 - age / OPENING_TICKS);
		return (float) (0.9f * Math.max(0, Math.cos(front * 3 / width)) * Math.exp(-front * front / (width * width)) * fade);
	}

	static float interpolate(float a, float b, float c, float d, float s, float t) {
		return (a + (b - a) * s) * (1 - t) + (d + (c - d) * s) * t;
	}

	static float[] cuts(float start, float end, int density) {
		if (Math.abs(end - start) < 0.00001f) return new float[] {0, 1};
		int first = (int) Math.floor(Math.min(start, end) * density) + 1;
		int last = (int) Math.ceil(Math.max(start, end) * density) - 1;
		int count = Math.max(0, last - first + 1);
		float[] cuts = new float[count + 2];
		for (int i = 0; i < count; i++) {
			int grid = end > start ? first + i : last - i;
			cuts[i + 1] = (grid / (float) density - start) / (end - start);
		}
		cuts[cuts.length - 1] = 1;
		return cuts;
	}

	static float wave(float horizontal, float y, float time) {
		return (float) (Math.sin(horizontal * 2.7 + y * 1.8 - time * 1.5) * 0.55
			+ Math.sin(y * 4.1 - horizontal * 1.3 + time * 2.1) * 0.3
			+ Math.sin(horizontal * 5.2 + y * 3.6 - time * 0.8) * 0.15);
	}

	static float ripple(float distance, float age) {
		if (age < 0 || age >= RIPPLE_TICKS) return 0;
		float front = distance - age * 0.23f;
		float fade = (1 - age / RIPPLE_TICKS) * Math.min(1, age / 3);
		return (float) (Math.cos(front * 8) * Math.exp(-front * front * 2) * fade);
	}
}
