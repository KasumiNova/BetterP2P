package com.projecturanus.betterp2p.network.data

import appeng.parts.p2p.PartP2PTunnel
import io.netty.buffer.ByteBuf
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

data class P2PLocation(var pos: BlockPos, var facing: EnumFacing, var dim: Int) {
  override fun hashCode(): Int {
    return hashP2P(pos, facing.ordinal, dim).hashCode()
  }

  /** Autogenerated equals by IntelliJ */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as P2PLocation

    if (pos != other.pos) return false
    if (dim != other.dim) return false
    if (facing != other.facing) return false

    return true
  }
}

fun writeP2PLocation(buf: ByteBuf, loc: P2PLocation) {
  buf.writeLong(loc.pos.toLong())
  buf.writeByte(loc.facing.ordinal)
  buf.writeInt(loc.dim)
}

fun readP2PLocation(buf: ByteBuf): P2PLocation? {
  return try {
    P2PLocation(
        pos = BlockPos.fromLong(buf.readLong()),
        facing = EnumFacing.values()[buf.readByte().toInt()],
        dim = buf.readInt())
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

fun writeP2PLocation(loc: P2PLocation?): NBTTagCompound {
  val nbt = NBTTagCompound()

  if (loc != null) {
    nbt.setLong("pos", loc.pos.toLong())
    nbt.setByte("facing", loc.facing.ordinal.toByte())
    nbt.setInteger("dim", loc.dim)
  }

  return nbt
}

fun readP2PLocation(tag: NBTTagCompound): P2PLocation? {
  return try {
    P2PLocation(
        pos = BlockPos.fromLong(tag.getLong("pos")),
        facing = EnumFacing.values()[tag.getByte("facing").toInt()],
        dim = tag.getInteger("dim"))
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

fun PartP2PTunnel<*>.toLoc() =
    P2PLocation(location.pos, side.facing, location.world.provider.dimension)
