package dev.hoyin1600p.vault_render_optimization.mixin.entitygpu;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuEntityModels;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.GpuHoleBuilder;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.HoleBatch;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.ItemMesh;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.ModelMesh;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.OculusExtendedEntity;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reserved ("hole") vertices for the GPU entity path. Holes are tied to the draw state they end up
 * in; every way the bytes can leave the builder is covered: the immediate upload hands them to the
 * GPU, any other pop fills them on the CPU, quad sorting fills them before it reads positions, and a
 * discard drops them with the vertices. Hooks use {@code require = 0}; the mixin audit verifies all of
 * them are present before the path can turn on.
 */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderGpuMixin extends DefaultedVertexConsumer implements GpuHoleBuilder {
    @Shadow private ByteBuffer buffer;
    @Shadow @Final private List<BufferBuilder.DrawState> drawStates;
    @Shadow private int lastPoppedStateIndex;
    @Shadow private int nextElementByte;
    @Shadow private int totalUploadedBytes;
    @Shadow private int vertices;
    @Shadow private int elementIndex;
    @Shadow private VertexFormat format;
    @Shadow private VertexFormat.Mode mode;
    @Shadow private boolean building;
    @Shadow private Vector3f[] sortingPoints;

    @Shadow
    private void ensureCapacity(int bytes) {
        throw new AssertionError();
    }

    @Unique
    private boolean vro$sortingHint;
    /** Whether any buffer source ever handed this builder out with a sorting hint (diagnostics). */
    @Unique
    private boolean vro$hinted;
    /** Consecutive handed-off batches too small for a dispatch; past a threshold this builder stops reserving. */
    @Unique
    private int vro$smallStreak;
    @Unique
    private int vro$smallSkips;
    @Unique
    private HoleBatch vro$current;
    @Unique
    private List<HoleBatch> vro$ended;
    @Unique
    private HoleBatch vro$popping;

    /** Oculus' own fields on this builder: whether it extends the format, and its vertex count within the quad. */
    @Unique
    private static VarHandle vro$irisExtending;
    @Unique
    private static VarHandle vro$irisVertexCount;
    @Unique
    private static boolean vro$irisResolved;

    @Override
    public boolean vro$canReserve() {
        // A batch holds one kind: model parts cannot join item runs or particles already reserved in this buffer
        // (an item frame's or armor stand's items share the buffer with model parts); they are written here instead.
        return !vro$sortingHint && (vro$current == null || vro$current.canAddModel()) && vro$canReserveItem();
    }

    @Override
    public void vro$noteBatch(boolean small) {
        vro$smallStreak = small ? vro$smallStreak + 1 : 0;
    }

    @Override
    public boolean vro$canReserveItem() {
        // A builder that keeps flushing tiny batches (per-item or per-block-entity immediate draws) writes them
        // itself: reserving would only end in a CPU fill. Every 64th draw is retried in case its use changes.
        if (vro$smallStreak >= GpuEntityModels.SMALL_STREAK && (++vro$smallSkips & 63) != 0) return false;
        if (!building || mode != VertexFormat.Mode.QUADS || elementIndex != 0 || defaultColorSet
                || sortingPoints != null || buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            return false;
        }
        if (format == DefaultVertexFormat.NEW_ENTITY) return true;
        // Oculus with a shader pack: NEW_ENTITY was switched to its extended format, and Oculus fills the
        // extended data once per four vertices, so reserve only between its quads.
        return format != null && format == GpuEntityModels.irisFormat() && vro$irisBetweenQuads();
    }

    @Unique
    private boolean vro$irisBetweenQuads() {
        if (!vro$irisResolved) {
            vro$irisResolved = true;
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                vro$irisExtending = lookup.findVarHandle(BufferBuilder.class, "extending", boolean.class);
                vro$irisVertexCount = lookup.findVarHandle(BufferBuilder.class, "vertexCount", int.class);
            } catch (ReflectiveOperationException | RuntimeException failure) {
                vro$irisExtending = null;
                GpuEntityModels.disableIris("Oculus BufferBuilder fields unreadable: " + failure);
            }
        }
        if (vro$irisExtending == null) return false;
        Object self = this;
        return (boolean) vro$irisExtending.get(self) && (int) vro$irisVertexCount.get(self) == 0;
    }

    @Override
    public void vro$setSortingHint(boolean sorting) {
        vro$sortingHint = sorting;
        vro$hinted = true;
    }

    @Override
    public void vro$reserve(ModelMesh mesh, int meshFirst, float[] pose, float[] normal, int color, int overlay,
                            int light, float[] sprite, int flips) {
        boolean iris = format != DefaultVertexFormat.NEW_ENTITY;
        int stride = format.getVertexSize();
        int bytes = mesh.vertexCount() * stride;
        // Vanilla keeps room for one more vertex after every endVertex(); keep that invariant.
        ensureCapacity(bytes + stride);
        if (vro$current == null) vro$current = GpuEntityModels.borrowBatch();
        vro$current.add(mesh, meshFirst, pose, normal, nextElementByte, color, overlay, light, sprite, flips, iris,
                iris ? OculusExtendedEntity.idsWord() : 0, iris ? OculusExtendedEntity.itemWord() : 0);
        nextElementByte += bytes;
        vertices += mesh.vertexCount();
    }

    @Override
    public VertexFormat vro$format() {
        return format;
    }

    @Override
    public boolean vro$reserveItem(ItemMesh mesh, int meshFirst, int firstVertex, int vertexCount, float[] pose,
                                   float[] normal, int tint, int overlay, int light, boolean embeddium) {
        boolean iris = format != DefaultVertexFormat.NEW_ENTITY;
        int stride = format.getVertexSize();
        int bytes = vertexCount * stride;
        boolean borrowed = vro$current == null;
        if (borrowed) vro$current = GpuEntityModels.borrowBatch();
        if (!vro$current.addItem(mesh, meshFirst, firstVertex, vertexCount, pose, normal, nextElementByte, tint,
                overlay, light, embeddium, iris, iris ? OculusExtendedEntity.idsWord() : 0,
                iris ? OculusExtendedEntity.itemWord() : 0)) {
            if (borrowed) {
                GpuEntityModels.release(vro$current);
                vro$current = null;
            }
            return false;
        }
        // Vanilla keeps room for one more vertex after every endVertex(); keep that invariant.
        ensureCapacity(bytes + stride);
        nextElementByte += bytes;
        vertices += vertexCount;
        return true;
    }

    @Override
    public boolean vro$canReserveParticle() {
        return building && format == DefaultVertexFormat.PARTICLE && mode == VertexFormat.Mode.QUADS
                && elementIndex == 0 && !defaultColorSet && sortingPoints == null
                && buffer.order() == ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    public boolean vro$reserveParticle(float x, float y, float z, float size, boolean rolled, float sin, float cos,
                                       float minU, float maxU, float minV, float maxV, int color, int light,
                                       float leftX, float leftY, float leftZ, float upX, float upY, float upZ) {
        int stride = DefaultVertexFormat.PARTICLE.getVertexSize();
        int bytes = 4 * stride;
        if (vro$current == null) vro$current = GpuEntityModels.borrowBatch();
        // Vanilla keeps room for one more vertex after every endVertex(); keep that invariant.
        ensureCapacity(bytes + stride);
        if (!vro$current.addParticle(x, y, z, size, rolled, sin, cos, minU, maxU, minV, maxV, color, light,
                nextElementByte, leftX, leftY, leftZ, upX, upY, upZ)) {
            return false;
        }
        nextElementByte += bytes;
        vertices += 4;
        return true;
    }

    /** Sorting reads the vertex positions of the buffer being built. */
    @Inject(method = "setQuadSortOrigin", at = @At("HEAD"), require = 0)
    private void vro$fillBeforeSorting(float x, float y, float z, CallbackInfo ci) {
        if (vro$current != null) {
            HoleBatch batch = vro$current;
            if (batch.items() && sortingPoints == null) {
                // Items: the sort reads only the positions of each quad's vertices 0 and 2; write those and keep the
                // holes for the GPU. Vanilla then sorts and writes the indices exactly as it would.
                batch.setBase(0);
                batch.writeSortPositions(buffer);
                GpuEntityModels.sortedItemBatch(batch, buffer);
                return;
            }
            vro$current = null;
            batch.setBase(0);
            GpuEntityModels.sortFill(vro$hinted);
            GpuEntityModels.fillOnCpu(batch, buffer);
        }
    }

    @Inject(method = "end", at = @At("RETURN"), require = 0)
    private void vro$endHoles(CallbackInfo ci) {
        if (vro$current != null) {
            vro$current.setStateIndex(drawStates.size() - 1);
            if (vro$ended == null) vro$ended = new ArrayList<>(2);
            vro$ended.add(vro$current);
            vro$current = null;
        }
    }

    @Inject(method = "popNextBuffer", at = @At("HEAD"), require = 0)
    private void vro$takeHoles(CallbackInfoReturnable<Pair<BufferBuilder.DrawState, ByteBuffer>> cir) {
        vro$popping = null;
        if (vro$ended == null || vro$ended.isEmpty()) return;
        for (int i = 0; i < vro$ended.size(); i++) {
            HoleBatch batch = vro$ended.get(i);
            if (batch.stateIndex() == lastPoppedStateIndex) {
                vro$ended.remove(i);
                batch.setBase(totalUploadedBytes);
                vro$popping = batch;
                return;
            }
        }
    }

    @Inject(method = "popNextBuffer", at = @At("RETURN"), require = 0)
    private void vro$handOffHoles(CallbackInfoReturnable<Pair<BufferBuilder.DrawState, ByteBuffer>> cir) {
        HoleBatch batch = vro$popping;
        Pair<BufferBuilder.DrawState, ByteBuffer> popped = cir.getReturnValue();
        if (batch == null) {
            GpuEntityModels.poppedWithoutHoles(popped.getFirst(), popped.getSecond());
            return;
        }
        vro$popping = null;
        GpuEntityModels.popped(batch, popped.getFirst(), popped.getSecond(), this);
    }

    /** Memory this builder handed out (possibly parked for Oculus) is about to be reused. */
    @Inject(method = "begin", at = @At("HEAD"), require = 0)
    private void vro$beginReusesMemory(VertexFormat.Mode mode, VertexFormat format, CallbackInfo ci) {
        GpuEntityModels.builderBegins(this);
    }

    @Inject(method = "discard", at = @At("HEAD"), require = 0)
    private void vro$discardHoles(CallbackInfo ci) {
        if (vro$current != null) {
            GpuEntityModels.release(vro$current);
            vro$current = null;
        }
        if (vro$ended != null) {
            for (HoleBatch batch : vro$ended) GpuEntityModels.release(batch);
            vro$ended.clear();
        }
    }
}
