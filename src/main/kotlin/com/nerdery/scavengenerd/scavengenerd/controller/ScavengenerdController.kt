package com.nerdery.scavengenerd.scavengenerd.controller

import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum
import com.nerdery.scavengenerd.scavengenerd.model.*
import com.nerdery.scavengenerd.scavengenerd.service.EntryService
import com.nerdery.scavengenerd.scavengenerd.service.ItemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
class ScavengenerdController @Autowired constructor(val itemService: ItemService,
                                                    val entryService: EntryService){

    @Value("\${versionNumber}")
    lateinit var versionNum: String

    @GetMapping("items")
    fun getItems(): List<ItemOverview> {
        return itemService.getItems()
    }

    @GetMapping("item/{itemId}")
    fun getItem(@PathVariable("itemId") itemId: Long): ItemDetails? {
        return itemService.getItem(itemId)
    }

    @PostMapping("item/{itemId}")
    fun addEntry(@PathVariable("itemId") itemId: Long, @RequestParam("photo") photo: MultipartFile, @RequestParam("userName") userName: String): ItemEntryDetails {
        val entryPostBody = EntryPostBody(userName, photo.bytes)
        return entryService.addEntry(itemId, entryPostBody)
    }

    @PatchMapping("entry/{entryId}")
    fun updateEntry(@PathVariable("entryId") entryId:Long, @RequestParam("status") status: StatusEnum) {
        return entryService.editEntry(entryId, status)
    }

    @PatchMapping("entries")
    fun submitEntries(@RequestBody body: List<Long>) {
        return entryService.submitEntries(body)
    }

    @DeleteMapping("entry/{entryId}")
    fun deleteEntry(@PathVariable("entryId") entryId: Long) {
        return entryService.deleteEntry(entryId)
    }

    @GetMapping("popstat")
    fun populateCumulativeStatuses() {
        val items = itemService.itemRepository.findAll()
        items.map { item ->
            val entries = entryService.entryRepository.findByItem(item.id!!)
            val statusSet = entries.toList().map { it.status }.toSet()
            val cumulativeStatus = when {
                statusSet.contains(StatusEnum.APPROVED) -> StatusEnum.APPROVED
                statusSet.contains(StatusEnum.SUBMITTED) -> StatusEnum.SUBMITTED
                statusSet.contains(StatusEnum.FOUND) -> StatusEnum.FOUND
                else -> StatusEnum.NOT_FOUND
            }
            val updatedItem = item.copy(cumulativeStatus = cumulativeStatus)
            itemService.itemRepository.save(updatedItem)
        }
    }
    @GetMapping()
    fun version(): String {
        return "ScavengeNerd API Version: $versionNum"
    }
}