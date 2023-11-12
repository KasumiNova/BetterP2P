package com.projecturanus.betterp2p.client.gui

import appeng.api.parts.IPartModel
import appeng.api.parts.PartItemStack
import appeng.me.GridNode
import appeng.parts.p2p.PartP2PTunnel
import appeng.parts.p2p.PartP2PTunnelME
import appeng.tile.networking.TileCableBus
import appeng.util.Platform
import com.projecturanus.betterp2p.network.P2PInfo
import com.projecturanus.betterp2p.network.hashP2P
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.renderer.ItemRenderer
import net.minecraft.client.renderer.block.model.ModelManager
import net.minecraft.client.resources.I18n
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraftforge.client.model.IModel
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.FMLCommonHandler
import java.util.*

class InfoWrapper(info: P2PInfo) {
    // Basic information
    val code: Long by lazy {
        hashP2P(pos, facing.ordinal, dim)
    }
    val frequency: Short = info.frequency
    val hasChannel = info.hasChannel
    val pos: BlockPos = info.pos
    val dim: Int = info.world
    val facing: EnumFacing = info.facing
    val output: Boolean = info.output
    var name: String = info.name
    var error: Boolean = false
    val tile: TileCableBus? by lazy {
        DimensionManager.getWorld(dim)?.getTileEntity(pos) as? TileCableBus
    }

    val bindButton = GuiButton(0, 0, 0, 34, 20, I18n.format("gui.advanced_memory_card.bind"))
    val renameButton = GuiButton(0, 0, 0, 0, 0, "")

    val icon: ResourceLocation? by lazy {
        (tile?.getPart(info.facing) as? PartP2PTunnel<*>)?.staticModels?.models?.get(2)
    }
    val overlay: ResourceLocation? by lazy {
        tile?.getPart(info.facing)?.getItemStack(PartItemStack.WRENCH)?.item?.registryName
    }

    val typeName: String by lazy {
        val item = tile?.getPart(info.facing)?.getItemStack(PartItemStack.WRENCH)
        I18n.format(item?.item?.getItemStackDisplayName(item)?.split(" - ")?.getOrNull(1)
            ?: "§c<Something broke...>")
    }

    val description: String by lazy {
        buildString {
            append("Type: ")
            append(typeName)
            append(" - ")
            if (output)
                append(I18n.format("gui.advanced_memory_card.desc.mode.output"))
            else
                append(I18n.format("gui.advanced_memory_card.desc.mode.input"))
        }
    }

    val freqDisplay: String by lazy {
        buildString {
            append(I18n.format("item.advanced_memory_card.selected"))
            append(" ")
            if (frequency != 0.toShort()) {
                val hex: String = buildString {
                    append((frequency.toUInt() shr 32).toString(16).toUpperCase())
                    append(frequency.toUInt().toString(16).toUpperCase())
                }.format4()
                append(hex)
            } else {
                append(I18n.format("gui.advanced_memory_card.desc.not_set"))
            }
        }
    }

    val hoverInfo: List<String> by lazy {
        val online = (tile?.getPart(info.facing) as? PartP2PTunnel<*>)?.proxy?.isPowered
        val list = mutableListOf(
            "§b$typeName§r",
            "§e" + I18n.format("gui.advanced_memory_card.pos", info.pos.x, info.pos.y, info.pos.z),
            "§e" + I18n.format("gui.advanced_memory_card.side", info.facing.name),
            "§e" + I18n.format("gui.advanced_memory_card.dim", info.world)
        )
        if (error || frequency == 0.toShort()) {
            list.add("§c" + I18n.format("gui.advanced_memory_card.p2p_status.unbound"))
        } else {
            list.add("§a" + I18n.format("gui.advanced_memory_card.p2p_status.bound"))
        }
        if (online != true) list.add("§c" + I18n.format("gui.advanced_memory_card.p2p_status.offline"))
        list
    }

    val channels: String? by lazy {
        val c = ((tile?.getPart(facing) as? PartP2PTunnelME)?.externalFacingNode as? GridNode)?.usedChannels()
        if (c != null) {
            I18n.format("gui.advanced_memory_card.extra.channel", c)
        } else {
            null
        }
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is InfoWrapper) {
            this.pos == other.pos &&
            this.dim == other.dim &&
            this.facing == other.facing
        } else {
            false
        }
    }
}

fun String.format4(): String {
    val format = StringBuilder()
    for (index in this.indices) {
        if (index % 4 == 0 && index != 0) {
            format.append(" ")
        }
        format.append(this[index])
    }
    return format.toString()
}
