package models.domain.pheno

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.domain.CR1000Exceptions
import models.ozone.{OzoneExceptions, OzoneSuspiciousDataLineError, WSOzoneFileParser}
import models.util.NumberParser


case class PhanoPlot(plotName: String, abbrePlot: String, clnrPlot: BigDecimal, statnr: Integer, codePlot: String, icpPlotCode: Int)

case class PhanoFileLevelInfo(fileName: String, stationName: String, beobachterName: String, comments: String, besuchDatums: List[String], missingInfo: Boolean, speciesName: String)

case class BesuchInfo(statnr: Integer, invnr: Integer, persnr: Integer, besuchDatum: String, comments: String)

case class PhanoGrowthData(invnr: Integer,
                               stationNr: Integer,
                               pflaId: Integer,
                               typeCode: Integer,
                               messdat: String,
                               social: Integer,
                               umfang: BigDecimal,
                               height: BigDecimal,
                               bemerkung: String
)

case class PhanoPFLA(stationNr: Integer,
                    typCode: Int,
                    statusCode: Int,
                    species: Int,
                    sprache: String,
                    invnr: Int,
                    pflaId: Int,
                    einfdat: String
                    )

case class AufnEreig(stationNr: Integer,
                    typCode: Int,
                    invnr: Int,
                    ereignisCode: Int,
                    ereignisDate: String,
                    persNr: Int,
                    intensityCode: Int,
                    pflaId: Int
                    )


object PhanoPlotKeysConfig {

  def defaultPlotConfigs = ???

  def defaultValidKeywords = List("Stationsname", "Beobachtungsjahr", "Beobachter Name","Beobachter Vorname","Meereshöhe m ü. M.:", "Exposition","Hangneigung:","Art","Daten der Beobachtungsgange","Bemerkungen")
  def defaultInvalidLinesPrefix = List( "Phänologische Beobachtungen", "Nr.;"
                                        )

  def preparePhanoFileLevelInfo(validLines: Seq[String], fileName : String) = {

    val stationNameLine: Option[String] = validLines.filter(_.startsWith("Stationsname")).headOption
    val beobachterNachNameLine: Option[String] = validLines.filter(_.contains("Beobachter Name")).headOption
    val beobachterVorNameLine: Option[String] = validLines.filter(_.contains("Beobachter Vorname")).headOption

    val bemerkungLine = validLines.filter(_.contains("Bemerkungen")).headOption
    val besuchDatumLine = validLines.filter(_.contains("Daten der Beobachtungsgange")).headOption
    val speciesNameLine: Option[String] = validLines.filter(_.startsWith("Art")).headOption

    val stationName = getStationNameValue(stationNameLine)
    val beobachterNachName = getBeobachterValue(beobachterNachNameLine)
    val beobachterVorname = getBeobachterVornameValue(beobachterVorNameLine)
    val beobachterName: String =  (beobachterNachName,beobachterVorname) match {
      case (Some(x), Some(y)) => (x + y).toUpperCase.sorted
      case (Some(x), None) => (x).toUpperCase.sorted
      case (None, Some(x)) => (x).toUpperCase.sorted
      case (None, None) => "undefined"
      case (_,_) => "undefined"
    }
    val besuchDatums = getBesuchDatums(besuchDatumLine)
    val comments = bemerkungLine.mkString(",") replaceAll(";", "")
    val speciesName = getSpeciesNameValue(speciesNameLine)

    val validComments = if(comments.size > 6) comments else ""

    val missingValue = stationName.isEmpty || beobachterName.isEmpty || speciesName.isEmpty

    PhanoFileLevelInfo(fileName,
      stationName.getOrElse("undefined"),
      beobachterName,
      validComments,
      besuchDatums.getOrElse(List()),
      missingValue,
      speciesName.getOrElse("undefined")
    )

  }

    def getStationNameValue(sammelMethodeLine: Option[String]): Option[String] = {
      sammelMethodeLine.map( line => {
        val index = line.indexOfSlice("Stationsname")
        val stationName = line.stripPrefix("Stationsname").trim.replaceAll("\\;","")
        if (stationName.nonEmpty) stationName else "unknown"
    })
  }

  def getSpeciesNameValue(sammelMethodeLine: Option[String]): Option[String] = {
    sammelMethodeLine.map( line => {
      val index = line.indexOfSlice("Art")
      val speciesName = line.stripPrefix("Art").trim.replaceAll("\\;","")
      if (speciesName.nonEmpty) speciesName else "unknown"
    })
  }


  def getBeobachterValue(sammelMethodeLine: Option[String]): Option[String] = {
    sammelMethodeLine.map( line => {
     val beobachterName = line.stripPrefix("Beobachter Name;").trim.replaceAll("\\;","")
      if (beobachterName.nonEmpty) beobachterName else "unknown"
    })
  }

  def getBeobachterVornameValue(sammelMethodeLine: Option[String]): Option[String] = {
    sammelMethodeLine.map( line => {
      val beobachterName = line.stripPrefix("Beobachter Vorname;").trim.replaceAll("\\;","")
      if (beobachterName.nonEmpty) beobachterName else "unknown"
    })
  }


  def getBesuchDatums(line: Option[String]) = {
    line.map( line => {
      val datumWithDelimiters = line.stripPrefix("Daten der Beobachtungsgange;").trim
      val datums = datumWithDelimiters.split(";")
      datums.map(convert8DigitDateTo10Digit(_)).toList
    })
  }

  def convert8DigitDateTo10Digit(datum: String) = {
    if(datum.nonEmpty) {
      val days = datum.split("\\.")
      if (days.size == 3) {
        val day = days(0).trim
        val month = days(1).trim
        val year = days(2).trim
        if (year.size == 4 && (day.size == 1 || day.size == 2) && (month.size == 1 || month.size == 2)) day + "." + month + "." + year else if (year.size == 2 && (day.size == 1 || day.size == 2) && (month.size == 1 || month.size == 2)) day + "." + month + "." + "20" + year else "01.01.1900"
      } else "01.01.1900"
    } else "01.01.1900"
  }

  def getBlindwertValue(line: Option[String]): Option[BigDecimal] = {
    line.map( line => {
      val index = line.indexOfSlice("Farbreagens")
      val indexOfBlindwert = line.indexOfSlice("Blindwert")
      val stringToParse = line.slice(indexOfBlindwert, index)
      //To Do: Strip 'Blindwert ;;'
      val valueWithDelimiters = stringToParse.trim.stripPrefix("Blindwert").replaceAll("^\\s+", "").stripPrefix(";;")
      val indexOfFisrtSemicolon = valueWithDelimiters.indexOfSlice(";")
      val blindWert = valueWithDelimiters.replaceAll("\\;", "")
      if (blindWert.nonEmpty && blindWert != "") NumberParser.parseBigDecimalWithLessThanSign(blindWert).getOrElse(BigDecimal("-9999")) else BigDecimal("-9999")

    })
  }

  def getCalibrationFactorValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Cal. Fact.")
      val stringToParse = line.slice(index,line.length)
      val valueWithDelimiters = stringToParse.stripPrefix("Cal. Fact.").trim
      val indexOfFisrtSemicolon = valueWithDelimiters.indexOfSlice(";")
      val calFactor = valueWithDelimiters.slice(0,indexOfFisrtSemicolon)
      if (calFactor.nonEmpty) calFactor else "-9999"
    })
  }

  def getFarbreagensDatumValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Cal.")
      val indexOfFarbreagens = line.indexOfSlice("Farbreagens")
      val indexOfCalFactor = if(index == -1) line.size-1 else index
      val stringToParse = line.slice(indexOfFarbreagens, indexOfCalFactor)
      val datumWithDelimiters = stringToParse.stripPrefix("Farbreagens:").trim
      val indexOfFisrtSemicolon = datumWithDelimiters.indexOfSlice(";")
      val farbDatum = datumWithDelimiters.slice(0,indexOfFisrtSemicolon)
      convert8DigitDateTo10Digit(farbDatum)

    })
  }

  def getProbenEingangDatumValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Nachweisgrenze")
      val indexOfNachweisgrenze = if(index == -1) line.size-1 else index
      val stringToParse = line.slice(0, indexOfNachweisgrenze)
      val stringStartWithProbeingang = stringToParse.substring(stringToParse.indexOf("Probeneingang")).trim
      val datumWithDelimiters = stringStartWithProbeingang.replaceAll("\\:", "").trim.stripPrefix("Probeneingang")
      val indexOfFisrtSemicolon = datumWithDelimiters.indexOfSlice(";")
      val probDatum = datumWithDelimiters.slice(0,indexOfFisrtSemicolon)
      convert8DigitDateTo10Digit(probDatum)

    })
  }

  def getNachweisgrenzeValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Nachweisgrenze")
      val indexOfFisrtSemicolon = line.indexOfSlice(";ug")
      val stringToParse = line.slice(index, indexOfFisrtSemicolon)
      val nachGrenze = stringToParse.stripPrefix("Nachweisgrenze;;").trim
      if (nachGrenze.nonEmpty) nachGrenze else "-9999"

    })
  }

  def findSuspiciousKommentLines(validKommentLines: List[String]): List[OzoneSuspiciousDataLineError] = {
    validKommentLines.flatMap(line => {
      val words = line.replaceAll("\\s", "").split(";")
      if (words.filter(_ != "").size > 6) {
        Some(OzoneSuspiciousDataLineError(100, s"Line was not parsed the possible reason could be Plot name was misspelled. Line is ${line}"))
      } else {
        if (isAllDigits(line.replaceAll("\\;", "").trim)) {
          Some(OzoneSuspiciousDataLineError(100, s"Line was not parsed, contains only numbers the possible reason could be shift in line. Line is ${line}"))
        }
        else
          None
      }})
  }

  def isAllDigits(x: String) = x forall Character.isDigit

}

case class OzoneFileData(analysid : Int, firmenid: Int, stsnr: Int, probeeingdat: String, analysedatum: String, einfdat: String, analysemethode: String, sammel: String, blindwert: Int, farbreagens: String, calfactor: BigDecimal, filename: String, nachweisgrenze: BigDecimal, bemerk: String)

object OzoneFileDataRow {
  val parser: RowParser[OzoneFileData] = {
    get[Int]("analysid")~
      get[Int]("firmenid")~
      get[Int]("stsnr")~
      get[String]("probeeingdat")~
      get[String]("analysedatum")~
      get[String]("einfdat")~
      get[String]("analysemethode")~
      get[String]("sammel")~
      get[Int]("blindwert")~
      get[String]("farbreagens")~
      get[BigDecimal]("calfactor")~
      get[String]("filename")~
      get[BigDecimal]("nachweisgrenze")~
      get[String]("bemerk") map {
      case analysid~firmenid~stsnr~probeeingdat~analysedatum~einfdat~analysemethode~sammel~blindwert~farbreagens~calfactor~filename~nachweisgrenze~bemerk => OzoneFileData(analysid , firmenid, stsnr, probeeingdat, analysedatum, einfdat, analysemethode, sammel, blindwert, farbreagens, calfactor, filename, nachweisgrenze, bemerk)
    }
  }
}

case class PassSammDataRow(clnr: BigDecimal,
                           startDate: String,
                           startTime: String,
                           endDate: String,
                           endTime: String,
                           analysid: Int,
                           duration: String,
                           rowData1: String,
                           rowData2 : String,
                           rowData3 : String,
                           absorpData1 : BigDecimal,
                           absorpData2 : BigDecimal,
                           absorpData3 : BigDecimal,
                           konzData1 : String,
                           konzData2 : String,
                           konzData3 : String,
                           mittel : String,
                           relsd : String,
                           probeeingdat : String,
                           analysedatum : String,
                           analysemethode : String,
                           sammel : String,
                           blindwert :Int,
                           farbreagens : String,
                           calfactor : BigDecimal,
                           filename : String,
                           nachweisgrenze : BigDecimal,
                           fileBem : String,
                           lineBem : String,
                           blindSampler: Int
                          )

object OzoneDataRow {
  /*val parser: RowParser[PassSammDataRow] = {
           get[BigDecial]("clnr")~
           get[String]("startDate")~
           get[String]("startTime")~
           get[String]("endDate")~
           get[String]("endTime")~
           get[Int]("analysid")~
           get[String]("expduration") ~
           get[String]("rawDat1")~
           get[String]("rawDat2")~
           get[String]("rawDat3")~
           get[BigDecimal]("absorpDat1")~
           get[BigDecimal]("absorpDat2")~
           get[BigDecimal]("absorpDat3")~
           get[String]("konzDat1")~
           get[String]("konzDat2")~
           get[String]("konzDat3")~
           get[String]("mittel")~
           get[String]("relsd")~
             get[String]("probeeingdat")~
             get[String]("analysedatum")~
             get[String]("analysemethode")~
             get[String]("sammel")~
             get[Int]("blindwert")~
             get[String]("farbreagens")~
             get[BigDecimal]("calfactor")~
             get[String]("filename")~
             get[BigDecimal]("nachweisgrenze")~
             get[Option[String]]("fileBem")~
             get[Option[String]]("lineBem") ~
             get[Int]("blindsampler") map {
             case clnr ~ startDate ~ startTime ~ endDate ~ endTime ~ analysid ~ duration ~ rowData1 ~ rowData2 ~ rowData3 ~ absorpData1 ~ absorpData2 ~ absorpData3 ~ konzData1 ~ konzData2 ~ konzData3 ~ mittel ~ relsd ~ probeeingdat ~  analysedatum ~ analysemethode ~ sammel ~ blindwert ~ farbreagens ~ calfactor ~ filename ~ nachweisgrenze ~ fileBem ~lineBem ~ blindsampler=>
               PassSammDataRow(clnr,
                 startDate,
                 startTime,
                 endDate,
                 endTime,
                 analysid,
                 duration,
                 rowData1,
                 rowData2,
                 rowData3,
                 absorpData1,
                 absorpData2,
                 absorpData3,
                 konzData1,
                 konzData2,
                 konzData3,
                 mittel,
                 relsd,
                 probeeingdat,
                 analysedatum,
                 analysemethode,
                 sammel,
                 blindwert,
                 farbreagens,
                 calfactor,
                 filename,
                 nachweisgrenze,
                 fileBem.getOrElse(""),
                 lineBem.getOrElse(""),
               blindsampler)
           }
  }*/

}

case class OzoneFileInfo(fileName: String, header: String, ozoneData: List[String])
case class PhanoErrorFileInfo(fileName: String, errors : List[CR1000Exceptions])
