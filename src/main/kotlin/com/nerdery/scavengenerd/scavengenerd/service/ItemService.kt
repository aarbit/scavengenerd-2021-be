package com.nerdery.scavengenerd.scavengenerd.service

import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum
import com.nerdery.scavengenerd.scavengenerd.model.ItemDetails
import com.nerdery.scavengenerd.scavengenerd.model.ItemEntryDetails
import com.nerdery.scavengenerd.scavengenerd.model.ItemOverview
import com.nerdery.scavengenerd.scavengenerd.repository.EntryPhotoRepository
import com.nerdery.scavengenerd.scavengenerd.repository.EntryRepository
import com.nerdery.scavengenerd.scavengenerd.repository.Item
import com.nerdery.scavengenerd.scavengenerd.repository.ItemRepository
import jdk.jshell.Snippet
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class ItemService @Autowired constructor(val itemRepository: ItemRepository,
                                         val entryService: EntryService) {
    fun getItems(): List<ItemOverview> {
        return itemRepository.findAll().map {
            val entries = entryService.getEntriesForItem(it.id!!)
            val itemStatus = calculateItemStatus(entries)
            ItemOverview(it.id, it.name, it.tier.name, itemStatus.name)
        }
    }

    fun getItem(itemId: Long): ItemDetails? {
        return itemRepository.findByIdOrNull(itemId)?.let {
            val entries = entryService.getEntriesForItem(itemId)
            val itemStatus = calculateItemStatus(entries)
            ItemDetails(it.id!!, it.name, it.tier.name, itemStatus.name, entries)
        }
    }

    private fun calculateItemStatus(entries: List<ItemEntryDetails>): StatusEnum {
        val statusSet = entries.map { it.status }.toSet()
        return when {
            statusSet.contains(StatusEnum.APPROVED.name) -> StatusEnum.APPROVED
            statusSet.contains(StatusEnum.SUBMITTED.name) -> StatusEnum.SUBMITTED
            statusSet.contains(StatusEnum.FOUND.name) -> StatusEnum.FOUND
            else -> StatusEnum.NOT_FOUND
        }
    }
}