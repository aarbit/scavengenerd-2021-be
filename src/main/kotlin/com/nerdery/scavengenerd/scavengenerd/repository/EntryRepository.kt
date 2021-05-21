package com.nerdery.scavengenerd.scavengenerd.repository

import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum
import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Entry(
    @Id
    @GeneratedValue
    val id: Long? = null,
    val item: Long,
    var status: StatusEnum = StatusEnum.NOT_FOUND,
    val userName: String
)

interface EntryRepository: JpaRepository<Entry, Long> {
    fun findByItem(item: Long): List<Entry>
}