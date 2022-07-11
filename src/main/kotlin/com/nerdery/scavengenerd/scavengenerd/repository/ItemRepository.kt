package com.nerdery.scavengenerd.scavengenerd.repository

import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum
import com.nerdery.scavengenerd.scavengenerd.enum.TierEnum
import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Item(
    @Id
    val id: Long? = null,
    val name: String,
    val tier: TierEnum,
    val cumulativeStatus: StatusEnum? = StatusEnum.NOT_FOUND
)

interface ItemRepository:JpaRepository<Item, Long>