import java.io._

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.io.FileUtils
import org.scalatestplus.play._

class ArchiveSpec extends PlaySpec {

  import java.io.IOException

  @throws[IOException]
  def getEntryName(source: File, file: File) = {
    val index = source.getAbsolutePath.length + 1
    val path = file.getCanonicalPath
    path.substring(index)
  }

  "files from generatedFiles" should {
    "be zipped and moved to archived\\generatedFiles.zip" in {
      val source = new File("D:\\projects\\lwf-portal\\generatedFiles\\")
      val destination = new File("D:\\projects\\lwf-portal\\archived\\generatedFiles.zip")
      //destination.delete()
      val archiveStream = new FileOutputStream(destination)
      val archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream)

      val fileList = FileUtils.listFiles(source, null, true)

      import scala.collection.JavaConversions._
      for (file <- fileList) {
        val entryName = getEntryName(source, file)
        val entry = new ZipArchiveEntry(entryName)
        archive.putArchiveEntry(entry)
        val input = new BufferedInputStream(new FileInputStream(file))
        IOUtils.copy(input, archive)
        input.close()
        archive.closeArchiveEntry()
      }

      archive.finish()
    }


  }
}
