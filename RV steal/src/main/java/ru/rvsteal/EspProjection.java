package ru.rvsteal;

import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector4f;

final class EspProjection {
    private static final float NEAR = .05f;
    /** Clips box edges against the near plane instead of discarding close targets. */
    static int[] bounds(AABB box, Matrix4f matrix, int width, int height) {
        Vector4f[] points = new Vector4f[8];
        for (int i = 0; i < 8; i++) points[i] = matrix.transform(new Vector4f(
                (float)((i & 1) == 0 ? box.minX : box.maxX),
                (float)((i & 2) == 0 ? box.minY : box.maxY),
                (float)((i & 4) == 0 ? box.minZ : box.maxZ), 1));
        float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (int i = 0; i < 8; i++) {
            if (points[i].w >= NEAR) include(points[i], bounds, width, height);
            for (int bit = 1; bit <= 4; bit *= 2) {
                int j = i ^ bit;
                if (j <= i || (points[i].w >= NEAR) == (points[j].w >= NEAR)) continue;
                float t = (NEAR - points[i].w) / (points[j].w - points[i].w);
                include(new Vector4f(points[i]).lerp(points[j], t), bounds, width, height);
            }
        }
        if (!Float.isFinite(bounds[0]) || bounds[2] < 0 || bounds[3] < 0 || bounds[0] > width || bounds[1] > height) return null;
        return new int[]{Math.clamp((int)Math.floor(bounds[0]), 0, width), Math.clamp((int)Math.floor(bounds[1]), 0, height),
                Math.clamp((int)Math.ceil(bounds[2]), 0, width), Math.clamp((int)Math.ceil(bounds[3]), 0, height)};
    }
    private static void include(Vector4f v, float[] b, int width, int height) {
        float x = (v.x / v.w + 1) * width / 2f, y = (1 - v.y / v.w) * height / 2f;
        b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.max(b[2], x); b[3] = Math.max(b[3], y);
    }
}
