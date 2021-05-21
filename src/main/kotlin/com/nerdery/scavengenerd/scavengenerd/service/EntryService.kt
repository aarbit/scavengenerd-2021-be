package com.nerdery.scavengenerd.scavengenerd.service

import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum
import com.nerdery.scavengenerd.scavengenerd.model.EntryPostBody
import com.nerdery.scavengenerd.scavengenerd.model.ItemEntryDetails
import com.nerdery.scavengenerd.scavengenerd.repository.Entry
import com.nerdery.scavengenerd.scavengenerd.repository.EntryPhoto
import com.nerdery.scavengenerd.scavengenerd.repository.EntryPhotoRepository
import com.nerdery.scavengenerd.scavengenerd.repository.EntryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class EntryService @Autowired constructor(val entryRepository: EntryRepository,
                                          val entryPhotoRepository: EntryPhotoRepository) {
    fun getEntriesForItem(itemId: Long): List<ItemEntryDetails> {
        val entries = entryRepository.findByItem(itemId)
        return entries.map {
            val photo = entryPhotoRepository.findByEntryId(it.id!!)
            ItemEntryDetails(it.id, it.status.name, it.userName, photo?.image?:ByteArray(0))
        }
    }

    fun addEntry(itemId: Long, entryPostBody: EntryPostBody): ItemEntryDetails {
        val entry = entryRepository.save(Entry(null, itemId, StatusEnum.FOUND, entryPostBody.userName))
        val photoEntry = entryPhotoRepository.save(EntryPhoto(null, entry.id!!, entryPostBody.photo))
        return ItemEntryDetails(entry.id, entry.status.name, entry.userName, photoEntry.image)
    }

    fun editEntry(entryId: Long, status: StatusEnum) {
        var entry = entryRepository.findByIdOrNull(entryId)
        entry?.let {
            entry.status = status
            entryRepository.save(entry)
        }
    }

    fun deleteEntry(entryId: Long) {
        entryRepository.deleteById(entryId)
    }

}