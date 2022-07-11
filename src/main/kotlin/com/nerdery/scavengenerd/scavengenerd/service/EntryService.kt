package com.nerdery.scavengenerd.scavengenerd.service

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.RawMessage
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest
import com.nerdery.scavengenerd.scavengenerd.*
import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum
import com.nerdery.scavengenerd.scavengenerd.model.EntryPostBody
import com.nerdery.scavengenerd.scavengenerd.model.ItemEntryDetails
import com.nerdery.scavengenerd.scavengenerd.repository.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.awt.Dimension
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*
import javax.activation.DataHandler
import javax.imageio.ImageIO
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource


@Service
class EntryService @Autowired constructor(val entryRepository: EntryRepository,
                                          val entryPhotoRepository: EntryPhotoRepository,
                                          val itemRepository: ItemRepository) {

    val log = LoggerFactory.getLogger(EntryService::class.java)
    fun getEntriesForItem(itemId: Long): List<ItemEntryDetails> {
        val entries = entryRepository.findByItem(itemId)
        return entries.map {
            val photo = entryPhotoRepository.findByEntryId(it.id!!)
            ItemEntryDetails(it.id, it.status.name, it.userName, photo?.smallImage?:photo?.image?:ByteArray(0))
        }
    }

    fun addEntry(itemId: Long, entryPostBody: EntryPostBody): ItemEntryDetails {
        val entry = entryRepository.save(Entry(null, itemId, StatusEnum.FOUND, entryPostBody.userName))
        val smallPhoto = scaleImageForThumbnail(entryPostBody.photo)
        val photoEntry = entryPhotoRepository.save(EntryPhoto(null, entry.id!!, entryPostBody.photo, smallPhoto))
        updateCumulativeStatus(itemId)
        return ItemEntryDetails(entry.id, entry.status.name, entry.userName, photoEntry.smallImage)
    }

    fun submitEntries(entries: List<Long>) {
        val entriesToUpdate = entries.mapNotNull { entryRepository.findByIdOrNull(it) }
        val itemId = entriesToUpdate[0].item
        val item = itemRepository.findByIdOrNull(itemId)
        val entryPhotos = entriesToUpdate.mapNotNull { it.id?.let { id -> entryPhotoRepository.findByEntryId(id) }}
        item?.let {
            try {
                emailEntryPhotos(it.name, entryPhotos)
                log.info("Email sent for item ${item.name}")
            } catch (e: Exception) {
                log.error("Email failed to send for item ${item.name} with id $itemId for entries ${entriesToUpdate.map { it.id }.joinToString(",")}", e)
                return
            }
        }
        for (entry in entriesToUpdate) {
            updateEntryStatus(entry, StatusEnum.SUBMITTED)
        }
        updateCumulativeStatus(itemId)
    }


    fun editEntry(entryId: Long, status: StatusEnum) {
        val entry = entryRepository.findByIdOrNull(entryId)
        entry?.let {
            updateEntryStatus(it, status)
            updateCumulativeStatus(it.item)
        }
    }

    fun deleteEntry(entryId: Long) {
        val itemId = entryRepository.findByIdOrNull(entryId)?.let {
            it.item
        }
        entryRepository.deleteById(entryId)
        itemId?.let {updateCumulativeStatus(it) }
    }

    private fun updateEntryStatus(entry: Entry, status: StatusEnum) {
        entry.status = status
        entryRepository.save(entry)
    }

    private fun emailEntryPhotos(itemName: String, photos: List<EntryPhoto>) {
        val session = Session.getDefaultInstance(Properties())
        val message = MimeMessage(session)
        val titleMessage = "Scavengenerd: Jurassic Bark '$itemName' submission"
        message.setSubject(titleMessage, "UTF-8")
        message.setFrom("aarbit@nerdery.com")
        message.setRecipients(Message.RecipientType.TO, "scavengenerdhunt@nerdery.com")
        //message.setRecipients(Message.RecipientType.TO, "aarbit@gmail.com")
        val body = MimeMultipart("alternative")
        val wrapper = MimeBodyPart()
        val textPart = MimeBodyPart()
        textPart.setContent("$titleMessage is attached", "text/plain")
        val htmlPart = MimeBodyPart()
        htmlPart.setContent("<html><body>$titleMessage is attached</body></html>", "text/html")
        body.addBodyPart(textPart)
        body.addBodyPart(htmlPart)
        wrapper.setContent(body)
        val multiPartMsg = MimeMultipart("mixed")
        message.setContent(multiPartMsg)
        multiPartMsg.addBodyPart(wrapper)
        val attachment = MimeBodyPart()
        for(photo in photos) {
            val dataSource = ByteArrayDataSource(scaleImageForEmail(photo.image), "image/jpeg")
            attachment.dataHandler = DataHandler(dataSource)
            attachment.fileName = "image${photo.id}.jpg"
            multiPartMsg.addBodyPart(attachment)
        }

        val client = AmazonSimpleEmailServiceClientBuilder.standard()
            .withRegion(Regions.US_EAST_1)
            .build()
        val outputStream = ByteArrayOutputStream()
        message.writeTo(outputStream)
        val rawMessage = RawMessage(ByteBuffer.wrap(outputStream.toByteArray()))
        val rawEmailRequest = SendRawEmailRequest(rawMessage)
        client.sendRawEmail(rawEmailRequest)
    }

    private fun scaleImageForEmail(inputImage: ByteArray): ByteArray {
        if (inputImage.size > 10485000) {
            val inStream = ByteArrayInputStream(inputImage)
            val buffIn = ImageIO.read(inStream)
            val newDimensions = Dimension(buffIn.width/2, buffIn.height/2)
            val outBuff = resizeImage(buffIn, newDimensions.width, newDimensions.height, Image.SCALE_AREA_AVERAGING)
            val outStream = ByteArrayOutputStream()
            ImageIO.write(outBuff, "jpg", outStream)
            return outStream.toByteArray()
        }
        return inputImage
    }

    private fun scaleImageForThumbnail(inputImage: ByteArray): ByteArray {
        val inStream = ByteArrayInputStream(inputImage)
        val buffIn = ImageIO.read(inStream)
        val newDimensions = if(buffIn.width < 1000) {
            Dimension(buffIn.width, buffIn.height)
        } else {
            val orgWidth = buffIn.width
            val orgHeight = buffIn.height
            val newWidth = 1000
            val scale = orgWidth.div(newWidth)
            val newHeight = orgHeight.div(scale)
            Dimension(newWidth, newHeight)
        }

        val outBuff = resizeImage(buffIn, newDimensions.width, newDimensions.height, Image.SCALE_AREA_AVERAGING)
        val outStream = ByteArrayOutputStream()
        ImageIO.write(outBuff, "jpg", outStream)
        return outStream.toByteArray()
    }

    private fun resizeImage(originalImage: BufferedImage, targetWidth: Int, targetHeight: Int, scaleType: Int): BufferedImage {
        val resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, scaleType)
        val outputImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        outputImage.graphics.drawImage(resultingImage, 0, 0, null)
        return outputImage
    }

    private fun updateCumulativeStatus(itemId: Long) {
        val entries = entryRepository.findByItem(itemId)
        val cumulativeStatus = calculateItemStatus(entries)
        itemRepository.findByIdOrNull(itemId)?.let { item ->
            val updatedItem = item.copy(cumulativeStatus = cumulativeStatus)
            itemRepository.save(updatedItem)
        }
    }

    private fun calculateItemStatus(entries: List<Entry>): StatusEnum {
        val statusSet = entries.map { it.status }.toSet()
        return when {
            statusSet.contains(StatusEnum.APPROVED) -> StatusEnum.APPROVED
            statusSet.contains(StatusEnum.SUBMITTED) -> StatusEnum.SUBMITTED
            statusSet.contains(StatusEnum.FOUND) -> StatusEnum.FOUND
            else -> StatusEnum.NOT_FOUND
        }
    }

}