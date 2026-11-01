package com.kingpaging.qwelcome.data

internal class MemoryUpdateDownloadStore : UpdateDownloadStore {
    var pending: PendingUpdateDownload? = null

    override fun read(): PendingUpdateDownload? = pending

    override fun write(download: PendingUpdateDownload) {
        pending = download
    }

    override fun clear() {
        pending = null
    }
}
