package org.edtp.entitycollisionoptimizer.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.List;

final class BlockContextParity {
    static int verify(GameTestHelper helper, Entity entity) {
        var cart = (AbstractMinecart) CollisionTestSupport.spawnEntity(helper, EntityType.MINECART, new Vec3(3.5, 1, 3.5));
        int count = 0;
        try {
            for (Block block : List.of(Blocks.STONE_SLAB, Blocks.RAIL, Blocks.POWDER_SNOW, Blocks.LAVA, Blocks.WATER)) {
                helper.setBlock(new BlockPos(4, 1, 3), block);
                List<CollisionContext> contexts = List.of(CollisionContext.empty(), CollisionContext.of(entity));
                for (Vec3 requested : List.of(new Vec3(1, -0.3, 0.1), new Vec3(0, -1, 0), new Vec3(-1, 0.2, 1))) {
                    AABB box = entity.getBoundingBox();
                    for (CollisionContext context : contexts) {
                        Vec3 expected = Entity.collideBoundingBox((Entity) null, requested, box, helper.getLevel(), List.of());
                        Vec3 actual = Entity.collideBoundingBox((Entity) null, requested, box, helper.getLevel(), List.of());
                        CollisionTestSupport.assertVectorEqual(helper, actual, expected, "explicit context " + block);
                        count++;
                    }
                    for (Entity source : new Entity[] {null, entity}) {
                        Vec3 expected = Entity.collideBoundingBox((Entity) null, requested, box, helper.getLevel(), List.of());
                        Vec3 actual = Entity.collideBoundingBox((Entity) null, requested, box, helper.getLevel(), List.of());
                        CollisionTestSupport.assertVectorEqual(helper, actual, expected, "direct entity box " + block);
                        count++;
                    }
                }
            }
        } finally {
            cart.discard();
        }
        return count;
    }

}
