package net.coderbot.iris.vertices;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * Test-only mapped Minecraft boundary: 52-byte Oculus 1.6.7 terrain layout.
 * The production Oculus JAR uses SRG names; standalone tests use Mojmap.
 * Layout research: Oculus-xhfp-backport bb9e122; see THIRD_PARTY_NOTICES.md.
 * Never bundled. Does not simulate shader pipeline or Mixin transformation.
 * Generic attribute indices use zero: Oculus's constructor-relaxation mixin is
 * not applied in JUnit. Element order, types, widths and byte offsets are exact;
 * OpenGL attribute bindings are deliberately not modeled by this fixture.
 */
public final class IrisVertexFormats {
    public static final VertexFormat TERRAIN = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                    .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                    .put("Color", DefaultVertexFormat.ELEMENT_COLOR)
                    .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
                    .put("UV2", DefaultVertexFormat.ELEMENT_UV2)
                    .put("Normal", DefaultVertexFormat.ELEMENT_NORMAL)
                    .put("Padding", DefaultVertexFormat.ELEMENT_PADDING)
                    .put("mc_Entity", new VertexFormatElement(0, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.GENERIC, 2))
                    .put("mc_midTexCoord", new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.GENERIC, 2))
                    .put("at_tangent", new VertexFormatElement(0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, 4))
                    .put("at_midBlock", new VertexFormatElement(0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.GENERIC, 3))
                    .put("Padding2", DefaultVertexFormat.ELEMENT_PADDING).build());
}
