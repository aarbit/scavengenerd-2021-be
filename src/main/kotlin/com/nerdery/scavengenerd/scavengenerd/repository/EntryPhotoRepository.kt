package com.nerdery.scavengenerd.scavengenerd.repository

import org.hibernate.type.BlobType
import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Lob

@Entity
data class EntryPhoto(
    @Id
    @GeneratedValue
    val id: Long? = null,
    val entryId: Long,
    @Lob
    val image: ByteArray
)

interface EntryPhotoRepository: JpaRepository<EntryPhoto, Long> {
    fun findByEntryId(entryId: Long): EntryPhoto?
}