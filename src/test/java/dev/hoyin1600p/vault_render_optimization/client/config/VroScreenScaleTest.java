package dev.hoyin1600p.vault_render_optimization.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class VroScreenScaleTest {
    private static final double EPSILON = 0.001;

    @Test
    void commonResolutionsShareOneVirtualSize() {
        assertEquals(2.0, VroScreenScale.scaleFor(1920, 1080), EPSILON);
        assertEquals(2.667, VroScreenScale.scaleFor(2560, 1440), EPSILON);
        assertEquals(4.0, VroScreenScale.scaleFor(3840, 2160), EPSILON);
        assertEquals(1.333, VroScreenScale.scaleFor(1280, 720), EPSILON);
    }

    @Test
    void ultrawideIsLimitedByHeight() {
        assertEquals(2.667, VroScreenScale.scaleFor(3440, 1440), EPSILON);
    }

    @Test
    void smallWindowsClampToOne() {
        assertEquals(1.0, VroScreenScale.scaleFor(800, 600), EPSILON);
        assertEquals(1.0, VroScreenScale.scaleFor(320, 240), EPSILON);
    }
}
