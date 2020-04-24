import java.io._

import models.domain.meteorology.ethlaegeren.domain.StationConfiguration
import models.services.CR1000FileValidator
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.utils.IOUtils
import org.apache.commons.io.FileUtils
import org.scalatestplus.play._
import schedulers.{ParametersProject, StationKonfig}

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



    //To Do: Write a test for testing the function validateProjectNumber of object CR1000FileValidator

    val inputFileName = "LAF_DeviceStatus"
    val statConfigs = List(StationKonfig( "ALP_DeviceStatus", 18, List(ParametersProject(1, 18, 10),ParametersProject(1, 3, 60),ParametersProject(4, 3, 10), ParametersProject(23, 3, 10))),
      StationKonfig( "LAF_DeviceStatus", 19, List(ParametersProject(1, 18, 10),ParametersProject(1, 3, 60),ParametersProject(4, 3, 10), ParametersProject(23, 3, 10)))
    )
    val lineToValidate = "2020-06-20 16:40:00" + ",20477,19,4,10,14.09,25.4,3.471"
    "project number in the file" should {
      "be correct" in {
        val mapStatKonfig = statConfigs.filter(sk => inputFileName.toUpperCase.startsWith(sk.fileName)).headOption

        val cr1000ValidatorValidateProjectNr =  CR1000FileValidator.validateProjectNumber(inputFileName, Some(4), mapStatKonfig, lineToValidate: String)
        assert(cr1000ValidatorValidateProjectNr.contains(4))
      }
    }


  }
}
