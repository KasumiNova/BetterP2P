package com.projecturanus.betterp2p.network.packet

import appeng.api.networking.IGridHost
import appeng.api.parts.IPartHost
import appeng.api.util.AEPartLocation
import appeng.parts.p2p.PartP2PTunnel
import com.projecturanus.betterp2p.network.ModNetwork
import com.projecturanus.betterp2p.network.data.P2PLocation
import com.projecturanus.betterp2p.network.data.readP2PLocation
import com.projecturanus.betterp2p.network.data.toLoc
import com.projecturanus.betterp2p.network.data.writeP2PLocation
import io.netty.buffer.ByteBuf
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class C2SRenameP2P(var p2p: P2PLocation? = null, var name: String = ""): IMessage {

    override fun fromBytes(buf: ByteBuf) {
        p2p = readP2PLocation(buf)
        val length = buf.readInt()
        val nameB = StringBuilder()
        for (i in 0 until length) {
            nameB.append(buf.readChar())
        }
        name = nameB.toString()
    }

    override fun toBytes(buf: ByteBuf) {
        writeP2PLocation(buf, p2p!!)
        buf.writeInt(name.length)
        for (c in name) {
            buf.writeChar(c.toInt())
        }
    }
}

class ServerRenameP2PTunnel : IMessageHandler<C2SRenameP2P, IMessage?> {
    override fun onMessage(message: C2SRenameP2P, ctx: MessageContext): IMessage? {
        if (message.p2p == null) {
            return null
        }

        val world: World = DimensionManager.getWorld(message.p2p!!.dim)
        val te: TileEntity? = world.getTileEntity(message.p2p!!.pos)
        val state = ModNetwork.playerState[ctx.serverHandler.player.uniqueID] ?: return null
        val facing = message.p2p!!.facing

        if (te is IGridHost && te.getGridNode(AEPartLocation.fromFacing(facing)) != null) {
            val partTunnel = (te as IPartHost).getPart(AEPartLocation.fromFacing(facing))

            if (partTunnel is PartP2PTunnel<*>) {
                partTunnel.setCustomName(message.name)
                val input: PartP2PTunnel<*> = if (partTunnel.isOutput) {
                    partTunnel.inputs.first()
                } else {
                    partTunnel
                }
                // Mark all dirty
                input.outputs.forEach {
                    state.gridCache.markDirty(it.toLoc(), it)
                }
                state.gridCache.markDirty(input.toLoc(), input)

                ModNetwork.requestP2PUpdate(ctx.serverHandler.player)
            }
        }

        return null
    }
}
