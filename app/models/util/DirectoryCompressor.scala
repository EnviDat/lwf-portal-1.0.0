package models.util

import java.io.{BufferedInputStream, File, FileInputStream, FileOutputStream}
import java.util

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.io.FileUtils
import play.api.Logger


object DirectoryCompressor {
  import java.io.IOException

  @throws[IOException]
  def getEntryName(source: File, file: File) = {
    val index = source.getAbsolutePath.length + 1
    val path = file.getCanonicalPath
    path.substring(index)
  }

  def compressAllFiles(source: File, destination: File): Unit = {

    val archiveStream = new FileOutputStream(destination)
    val archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream)

    val fileList = FileUtils.listFiles(source, null, true)

    import scala.collection.JavaConversions._
      for (file <- fileList) {
        val entryName = getEntryName(source, file)
        val entry = new ZipArchiveEntry(entryName)
        archive.putArchiveEntry(entry)
        val input = new BufferedInputStream(new FileInputStream(file))
        Logger.info(s" source: ${source.getAbsolutePath.toString} ${destination.getAbsolutePath}: created zip entry :${entryName}")
        IOUtils.copy(input, archive)
        input.close()
        archive.closeArchiveEntry()
      }


    archive.finish()
    archive.close()
    archiveStream.close()

  }

}
