package com.example.autoreply

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AutoReplyTile : TileService() {

    override fun onStartListening() {
        refresh()
    }

    override fun onClick() {
        Controller.setEnabled(this, !Prefs(this).enabled)
        refresh()
    }

    private fun refresh() {
        val t = qsTile ?: return
        t.state = if (Prefs(this).enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        t.label = "AutoReply"
        t.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        t.updateTile()
    }
}
