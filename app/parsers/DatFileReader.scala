package parsers
import java.io.{FileNotFoundException, IOException, PrintWriter}

import models.domain.MeteoDataRow

import scala.io.Source

object DatFileReader {
 def readFilesFromFile(datFileName: String) {
   val src = Source.fromFile(datFileName)

   try {
     for (line <- src.getLines()) {
       println(line)
     }
   } catch {
     case ex: FileNotFoundException => println("Couldn't find that file.")
     case ex: IOException => println("Had an IOException trying to read that file")
   }
 }
}

object DatFileWriter {
  def writeDataIntoFile(datFileName: String, data: Seq[MeteoDataRow]) {
    try {
      new PrintWriter(datFileName + "sample" + System.currentTimeMillis()) { write(data.toList.mkString("\n")); close }
    } catch {
      case ex: FileNotFoundException => println("Couldn't find that file.")
      case ex: IOException => println("Had an IOException trying to read that file")
    }
  }
}