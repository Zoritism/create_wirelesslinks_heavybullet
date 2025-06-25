package com.zoritism.wirelesslinks.util;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class RaycastHelper {

	public static BlockHitResult rayTraceRange(Level world, Player player, double range) {
		Vec3 origin = getTraceOrigin(player);
		Vec3 target = getTraceTarget(player, range, origin);
		ClipContext context = new ClipContext(origin, target, Block.COLLIDER, Fluid.NONE, player);
		return world.clip(context);
	}

	public static PredicateTraceResult rayTraceUntil(Player player, double range, Predicate<BlockPos> predicate) {
		Vec3 origin = getTraceOrigin(player);
		Vec3 target = getTraceTarget(player, range, origin);
		return rayTraceUntil(origin, target, predicate);
	}

	private static Vec3 getTraceTarget(Player player, double range, Vec3 origin) {
		float pitch = player.getXRot();
		float yaw = player.getYRot();
		float x = -Mth.sin(yaw * (float) Math.PI / 180.0F) * Mth.cos(pitch * (float) Math.PI / 180.0F);
		float y = -Mth.sin(pitch * (float) Math.PI / 180.0F);
		float z = Mth.cos(yaw * (float) Math.PI / 180.0F) * Mth.cos(pitch * (float) Math.PI / 180.0F);
		return origin.add((double) x * range, (double) y * range, (double) z * range);
	}

	private static Vec3 getTraceOrigin(Player player) {
		return new Vec3(player.getX(), player.getEyeY(), player.getZ());
	}

	public static PredicateTraceResult rayTraceUntil(Vec3 start, Vec3 end, Predicate<BlockPos> predicate) {
		if (Double.isNaN(start.x) || Double.isNaN(start.y) || Double.isNaN(start.z)
				|| Double.isNaN(end.x) || Double.isNaN(end.y) || Double.isNaN(end.z))
			return null;

		int x = Mth.floor(start.x);
		int y = Mth.floor(start.y);
		int z = Mth.floor(start.z);
		int endX = Mth.floor(end.x);
		int endY = Mth.floor(end.y);
		int endZ = Mth.floor(end.z);

		MutableBlockPos currentPos = new BlockPos(x, y, z).mutable();
		if (predicate.test(currentPos))
			return new PredicateTraceResult(currentPos.immutable(), Direction.getNearest(endX - x, endY - y, endZ - z));

		int remaining = 200;
		Vec3 delta = end.subtract(start);

		while (remaining-- >= 0) {
			if (x == endX && y == endY && z == endZ)
				return new PredicateTraceResult(); // missed

			double nextX = 999.0D, nextY = 999.0D, nextZ = 999.0D;

			if (endX > x) nextX = (x + 1.0D - start.x) / delta.x;
			else if (endX < x) nextX = (x - start.x) / delta.x;

			if (endY > y) nextY = (y + 1.0D - start.y) / delta.y;
			else if (endY < y) nextY = (y - start.y) / delta.y;

			if (endZ > z) nextZ = (z + 1.0D - start.z) / delta.z;
			else if (endZ < z) nextZ = (z - start.z) / delta.z;

			double smallest = Math.min(nextX, Math.min(nextY, nextZ));
			Direction direction;

			if (smallest == nextX) {
				direction = endX > x ? Direction.EAST : Direction.WEST;
				start = start.add(delta.x * smallest, delta.y * smallest, delta.z * smallest);
				x = Mth.floor(start.x) - (direction == Direction.WEST ? 1 : 0);
			} else if (smallest == nextY) {
				direction = endY > y ? Direction.UP : Direction.DOWN;
				start = start.add(delta.x * smallest, delta.y * smallest, delta.z * smallest);
				y = Mth.floor(start.y) - (direction == Direction.DOWN ? 1 : 0);
			} else {
				direction = endZ > z ? Direction.SOUTH : Direction.NORTH;
				start = start.add(delta.x * smallest, delta.y * smallest, delta.z * smallest);
				z = Mth.floor(start.z) - (direction == Direction.NORTH ? 1 : 0);
			}

			currentPos.set(x, y, z);
			if (predicate.test(currentPos))
				return new PredicateTraceResult(currentPos.immutable(), direction);
		}

		return new PredicateTraceResult(); // Missed after max iterations
	}

	public static class PredicateTraceResult {
		private final BlockPos pos;
		private final Direction facing;

		public PredicateTraceResult(BlockPos pos, Direction facing) {
			this.pos = pos;
			this.facing = facing;
		}

		public PredicateTraceResult() {
			this.pos = null;
			this.facing = null;
		}

		public Direction getFacing() {
			return facing;
		}

		public BlockPos getPos() {
			return pos;
		}

		public boolean missed() {
			return this.pos == null;
		}
	}
}
