package com.projecturanus.betterp2p.client.gui.widget

import com.projecturanus.betterp2p.BetterP2P
import com.projecturanus.betterp2p.client.gui.GuiAdvancedMemoryCard
import com.projecturanus.betterp2p.client.gui.InfoList
import com.projecturanus.betterp2p.client.gui.InfoWrapper
import com.projecturanus.betterp2p.item.BetterMemoryCardModes
import com.projecturanus.betterp2p.network.ModNetwork
import com.projecturanus.betterp2p.network.packet.C2SLinkP2P
import com.projecturanus.betterp2p.network.packet.C2SRenameP2P
import com.projecturanus.betterp2p.network.packet.C2SUnlinkP2P
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import kotlin.reflect.KProperty0

/**
 * WidgetP2PColumn
 *
 * A widget that contains a list of P2P entries.
 */
class WidgetP2PColumn(private val fontRenderer: FontRenderer,
                      private val gui: GuiAdvancedMemoryCard,
                      private val infos: InfoList,
                      private var x: Int, private var y: Int,
                      private val selectedInfo: KProperty0<InfoWrapper?>,
                      val mode: () -> BetterMemoryCardModes,
                      private var scrollBar: WidgetScrollBar) {

    val entries: MutableList<WidgetP2PDevice> = mutableListOf()
    private val renameBar: IGuiTextField = IGuiTextField(fontRenderer, 160,12)

    init {
        renameBar.setMaxStringLength(50)
    }

    /**
     * Resize the column
     */
    fun resize(scale: GuiScale, availableHeight: Int) {
        entries.clear()
        for (i in 0 until scale.size(availableHeight)) {
            val widget = WidgetP2PDevice(selectedInfo, mode,
                { infos.filtered.getOrNull(i + scrollBar.currentScroll) },
                gui, x, y + i * (P2PEntryConstants.HEIGHT + 1))
            entries.add(widget)
        }
    }

    fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
        for ((i, entry) in entries.withIndex()) {
            entry.x = x
            entry.y = y + i * (P2PEntryConstants.HEIGHT + 1)
        }
    }

    fun finishRename(){
        for (widget in entries){
            widget.renderNameTextfield = true
        }
        if(renameBar.info != null && renameBar.text.isNotEmpty() && renameBar.info.name != renameBar.text){
            val info: InfoWrapper = renameBar.info

            renameBar.text = renameBar.text.trim()
            ModNetwork.channel.sendToServer(C2SRenameP2P(info.loc, renameBar.text))
        }
        renameBar.visible = false
        renameBar.text = ""
        renameBar.isFocused = false
        renameBar.info = null
    }

    /**
     * Called when rename button "area" is clicked. Rename text bar must be
     * visible after this is called
     */
    private fun onRenameButtonClicked(info:InfoWrapper, index: Int) {
//        if (GuiScreen.isShiftKeyDown()) {
//            transportPlayer(info)
//        }
        renameBar.visible = true
        renameBar.y = (this.y) + index * (P2PEntryConstants.HEIGHT + 1) + 1
        renameBar.x = this.x + 50
        renameBar.text = info.name
        renameBar.isFocused = true
        renameBar.cursorPosition = 0
        renameBar.info = info
    }

    private fun onSelectButtonClicked(widget: WidgetP2PDevice, info: InfoWrapper, mouseButton: Int) {
        if (mouseButton == 1) {
            gui.openTypeSelector(widget, false)
        } else if (selectedInfo.get() != info) {
            gui.selectInfo(info.loc)
        }

        info.bindButton.playPressSound(gui.mc.soundHandler)
    }

    private fun onBindButtonClicked(info: InfoWrapper) {
        if (infos.selectedEntry == null) return
        when (mode()) {
            BetterMemoryCardModes.INPUT -> {
                BetterP2P.logger.debug("Bind ${info.loc} as input")
                ModNetwork.channel.sendToServer(C2SLinkP2P(info.loc, infos.selectedEntry))
            }
            BetterMemoryCardModes.OUTPUT -> {
                BetterP2P.logger.debug("Bind ${info.loc} as output")
                ModNetwork.channel.sendToServer(C2SLinkP2P(infos.selectedEntry, info.loc))
            }
            BetterMemoryCardModes.COPY -> {
                val input = findInput(infos.selectedInfo?.frequency)
                if (input != null)
                    ModNetwork.channel.sendToServer(C2SLinkP2P(input.loc, info.loc))
            } else -> {
                BetterP2P.logger.debug("Somehow bind button was pressed while in UNBIND mode.")
            }
        }
        info.bindButton.playPressSound(gui.mc.soundHandler)
    }

    private fun onUnbindButtonClicked(info: InfoWrapper) {
        if (info.frequency != 0.toShort()) {
            ModNetwork.channel.sendToServer(C2SUnlinkP2P(info.loc, gui.getTypeID()))
            info.frequency = 0.toShort()
        }
    }

    fun findInput(frequency: Short?) =
        infos.filtered.find { it.frequency == frequency && !it.output }

    fun findOutput(frequency: Short?) =
        infos.filtered.find { it.frequency == frequency && it.output }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        var clickRenameButton = false
        for ((index, widget) in entries.withIndex()) {
            val info = widget.infoSupplier()
            if (info?.bindButton?.mousePressed(gui.mc, mouseX, mouseY) == true) {
                onBindButtonClicked(widget.infoSupplier()!!)
            } else if (info?.unbindButton?.mousePressed(gui.mc, mouseX, mouseY) == true) {
                onUnbindButtonClicked(widget.infoSupplier()!!)
            } else if (mouseX > widget.x.toDouble() + 50 && mouseX < widget.x.toDouble() + 50 + 160 &&
                       mouseY > widget.y.toDouble() +  1 && mouseY < widget.y.toDouble() +  1 +  13 &&
                       widget.infoSupplier() != null) {
                if(renameBar.info != widget.infoSupplier()) {
                    finishRename()
                }
                widget.renderNameTextfield = false
                onRenameButtonClicked(widget.infoSupplier()!!, index)
                clickRenameButton = true
            } else if (mouseX > widget.x && mouseX < widget.x + P2PEntryConstants.WIDTH &&
                       mouseY > widget.y && mouseY < widget.y + P2PEntryConstants.HEIGHT &&
                       info != null
            ) {
                onSelectButtonClicked(widget, info, mouseButton)
            }
        }
        renameBar.mouseClicked(mouseX,mouseY,mouseButton)
        if(!clickRenameButton && renameBar.visible) {
            finishRename()
        }
    }

    fun mouseHovered(mouseX: Int, mouseY: Int) {
        entries.forEach {
            val info = it.infoSupplier()
            if (mouseX > it.x.toDouble() && mouseX < it.x + 20.0 &&
                mouseY > it.y.toDouble() && mouseY < it.y + P2PEntryConstants.HEIGHT && info != null) {
                gui.drawHoveringText(info.hoverInfo, it.x + 168, it.y + 15, gui.mc.fontRenderer)
            } else if (mouseX > it.x.toDouble() + 50 && mouseX < it.x.toDouble() + 50 + 160 &&
                       mouseY > it.y.toDouble() +  1 && mouseY < it.y.toDouble() +  1 +  13 &&
                       !renameBar.isFocused && info != null
            ) {
                GuiTextField.drawRect(
                    it.x + 50,
                    it.y +  1,
                    it.x + 50 + 160,
                    it.y + 1 + 12,
                    0x6E000000 // ARGB xd
                )
            }

        }
    }

    fun keyTyped(char: Char, key: Int): Boolean {
        if (renameBar.isFocused){
            if (key == Keyboard.KEY_RETURN) {
                finishRename()
            } else {
                renameBar.textboxKeyTyped(char, key)
            }
            return true
        }
        return false
    }

    fun render(mouseX: Int, mouseY: Int, partialTicks: Float) {
        for (widget in entries) {
            widget.render(mouseX, mouseY, partialTicks)
        }
        renameBar.drawTextBox()
    }

    fun onGuiClosed() {
        finishRename()
    }
}

