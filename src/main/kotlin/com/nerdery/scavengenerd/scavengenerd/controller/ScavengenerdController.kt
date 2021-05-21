package com.nerdery.scavengenerd.scavengenerd.controller

import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum
import com.nerdery.scavengenerd.scavengenerd.model.*
import com.nerdery.scavengenerd.scavengenerd.service.EntryService
import com.nerdery.scavengenerd.scavengenerd.service.ItemService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
class ScavengenerdController @Autowired constructor(val itemService: ItemService,
                                                    val entryService: EntryService){
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

    @DeleteMapping("entry/{entryId}")
    fun deleteEntry(@PathVariable("entryId") entryId: Long) {
        return entryService.deleteEntry(entryId)
    }
}