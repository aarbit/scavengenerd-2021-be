package com.nerdery.scavengenerd.scavengenerd.model

import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum

data class ItemOverview(
    val id: Long,
    val name: String,
    val tier: String,
    val status: String
)

data class ItemDetails(
    val id: Long,
    val name: String,
    val tier: String,
    val status: String,
    val entries: List<ItemEntryDetails>
)

data class ItemEntryDetails(
    val id: Long,
    val status: String,
    val userName: String,
    val photo: ByteArray
)

data class EntryPostBody(
    val userName: String,
    val photo: ByteArray
)

data class EntryPatchBody(
    val status: StatusEnum
)