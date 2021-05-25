package com.nerdery.scavengenerd.scavengenerd.service

import com.amazonaws.regions.Regions
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder
import com.amazonaws.services.simpleemail.model.RawMessage
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest
import com.nerdery.scavengenerd.scavengenerd.enum.StatusEnum
import com.nerdery.scavengenerd.scavengenerd.model.EntryPostBody
import com.nerdery.scavengenerd.scavengenerd.model.ItemEntryDetails
import com.nerdery.scavengenerd.scavengenerd.repository.*
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
    fun getEntriesForItem(itemId: Long): List<ItemEntryDetails> {
        val entries = entryRepository.findByItem(itemId)
        return entries.map {
            val photo = entryPhotoRepository.findByEntryId(it.id!!)
            ItemEntryDetails(it.id, it.status.name, it.userName, photo?.smallImage?:photo?.image?:ByteArray(0))
        }
    }

    fun addEntry(itemId: Long, entryPostBody: EntryPostBody): ItemEntryDetails {
        val entry = entryRepository.save(Entry(null, itemId, StatusEnum.FOUND, entryPostBody.userName))
        val smallPhoto = scaleDownImage(entryPostBody.photo)
        val photoEntry = entryPhotoRepository.save(EntryPhoto(null, entry.id!!, entryPostBody.photo, smallPhoto))
        return ItemEntryDetails(entry.id, entry.status.name, entry.userName, photoEntry.smallImage)
    }

    fun submitEntries(entries: List<Long>) {
        val entriesToUpdate = entries.mapNotNull { entryRepository.findByIdOrNull(it) }
        val itemId = entriesToUpdate[0].item
        val item = itemRepository.findByIdOrNull(itemId)
        for (entry in entriesToUpdate) {
            updateEntryStatus(entry, StatusEnum.SUBMITTED)
        }
        val entryPhotos = entriesToUpdate.mapNotNull { it.id?.let { id -> entryPhotoRepository.findByEntryId(id) }}
        item?.let {
            emailEntryPhotos(it.name, entryPhotos)
        }

    }


    fun editEntry(entryId: Long, status: StatusEnum) {
        val entry = entryRepository.findByIdOrNull(entryId)
        entry?.let {
            updateEntryStatus(it, status)
        }
    }

    fun deleteEntry(entryId: Long) {
        entryRepository.deleteById(entryId)
    }

    private fun updateEntryStatus(entry: Entry, status: StatusEnum) {
        entry.status = status
        entryRepository.save(entry)
    }

    private fun emailEntryPhotos(itemName: String, photos: List<EntryPhoto>) {
        val session = Session.getDefaultInstance(Properties())
        val message = MimeMessage(session)
        message.setSubject("The Ineffables '$itemName' submission", "UTF-8")
        message.setFrom("aarbit@nerdery.com")
        message.setRecipients(Message.RecipientType.TO, "aarbit@gmail.com")
        val body = MimeMultipart("alternative")
        val wrapper = MimeBodyPart()
        val textPart = MimeBodyPart()
        textPart.setContent("Our plaintext submission", "text/plain")
        val htmlPart = MimeBodyPart()
        htmlPart.setContent("<html><body>Our HTML submission</body></html>", "text/html")
        body.addBodyPart(textPart)
        body.addBodyPart(htmlPart)
        wrapper.setContent(body)
        val multiPartMsg = MimeMultipart("mixed")
        message.setContent(multiPartMsg)
        multiPartMsg.addBodyPart(wrapper)
        val attachment = MimeBodyPart()
        for(photo in photos) {
            val dataSource = ByteArrayDataSource(photo.image, "image/jpeg")
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

    private fun scaleDownImage(inputImage: ByteArray): ByteArray {
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

    fun resizeImage(originalImage: BufferedImage, targetWidth: Int, targetHeight: Int, scaleType: Int): BufferedImage {
        val resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, scaleType);
        val outputImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        outputImage.getGraphics().drawImage(resultingImage, 0, 0, null);
        return outputImage;
    }

}