package ru.rvsteal;

import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.junit.Test;
import static org.junit.Assert.*;

public class EspProjectionTest {
    private final Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(70), 16f / 9f, .05f, 512);
    @Test public void frontBehindAndNearPlane() {
        int[] visible = EspProjection.bounds(new AABB(-.5, -.5, -5, .5, .5, -4), projection, 1280, 720);
        assertNotNull(visible);
        assertTrue(visible[0] < 640 && visible[2] > 640);
        assertTrue(visible[1] < 360 && visible[3] > 360);
        assertNull(EspProjection.bounds(new AABB(-1, -1, 1, 1, 1, 2), projection, 1280, 720));
        // A close block crossing the camera plane previously vanished entirely.
        int[] near = EspProjection.bounds(new AABB(-.3, -.5, -.5, .3, .5, .2), projection, 1280, 720);
        assertNotNull(near);
        assertTrue(near[0] >= 0 && near[2] <= 1280 && near[1] >= 0 && near[3] <= 720);
        assertNull(EspProjection.bounds(new AABB(100, 0, -5, 101, 1, -4), projection, 1280, 720));
    }
}
