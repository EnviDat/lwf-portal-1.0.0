package models.domain.Ozone

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.ozone.OzoneSuspiciousDataLineError


case class OzonePlot(plotName: String, abbrePlot: String, clnrPlot: Integer, statnr: Integer, codePlot: Integer)

case class OzoneFileConfig(fileName: String, sammelMethode: String, anaylysenMethode: String, analysenDatum: String, blindwert: BigDecimal, farrbreagens: String, calFactor: BigDecimal, probenEingang: String, nachweisgrenze: BigDecimal, codeCountry: BigDecimal,  codeCompound: String, remarks: String, missingInfo: Boolean)

case class PassSammData(clnr: Integer,
                               startDate: String,
                               endDate: String,
                               duration: BigDecimal,
                               rowData1: BigDecimal,
                               rowData2: BigDecimal,
                               rowData3: BigDecimal,
                               rowData4: BigDecimal,
                               absorpData1: BigDecimal,
                               absorpData2: BigDecimal,
                               absorpData3: BigDecimal,
                               absorpData4: BigDecimal,
                               konzData1: BigDecimal,
                               konzData2: BigDecimal,
                               konzData3: BigDecimal,
                               konzData4: BigDecimal,
                               mittel: BigDecimal,
                               bemerk: String,
                               passval: Integer,
                               einfdat: String,
                               relSD: BigDecimal
)


object OzoneKeysConfig {

  def defaultPlotConfigs = List(
                                  OzonePlot("Jussy","JUS",334191,15,5007),
                                  OzonePlot("Othmarsingen","OTH",313136,30,5013),
                                  OzonePlot("Lausanne","LAU",335581,19,5008),
                                  OzonePlot("Bettlachstock","BET",335579,5,5003),
                                  OzonePlot("Navaggio","NOV",335582,27,5012),
                                  OzonePlot("Schänis","SCH",331934,32,5016),
                                  OzonePlot("Isone","ISO",335577,13,5006),
                                  OzonePlot("WSL, Birmensdorf", "WSL",374674,59,-14),
                                  OzonePlot("Neunkirch","NEU",336068,25,5011),
                                  OzonePlot("Vordemwald","VOR",335584,36,5015),
                                  OzonePlot("Vor dem Wald","VOR",335584,36,5015),
                                  OzonePlot("Schaenis","SCH",331934,32,5016),
                                  OzonePlot("Novaggio","NOV",335582,27,5012),
                                  OzonePlot("LAT","LAT",337588,40,5017),
                                  OzonePlot("Celerina", "CEL",333379,7,5004),
                                  OzonePlot("Nationalpark", "NAT",333373,23,5010),
                                  OzonePlot("Chironico", "CHI",335580,9,5005),
                                  OzonePlot("Beatenberg", "BEA",335578,3,5002),
                                  OzonePlot("Visp", "VIS",335583,34,5014),
                                  OzonePlot("Lens", "LEN",326208,21,5009),
                                  OzonePlot("Davos", "DAV",-1111,-6666,5018),
                                  OzonePlot("Pfynwald", "PFY",-1112,-6666,-12),
                                  OzonePlot("Lat Met", "LAT",337588,40,5017),
                                  OzonePlot("Lat OP5", "LAT",337588,40,5017),
                                  OzonePlot("Lat OP5", "LAT",337588,40,5017),
                                  OzonePlot("LAT-MET", "LAT",337588,40,5017),
                                  OzonePlot("LAT-OP5", "LAT",337588,40,5017),
                                  OzonePlot("LA-OP5", "LAT",337588,40,5017),
                                  OzonePlot("LAT-OTC", "LAT",337588,40,5017),
                                  OzonePlot("Alpthal", "ALP",-1113,-6666,5001),
                                  OzonePlot("Lattecaldo","LAT",337588,40,5017),
                                  OzonePlot("Birmensdorf", "WSL",374674,59,-14),
                                  OzonePlot("Isone ","ISO",335577,13,5006),
                                  OzonePlot("VISP", "VIS",335583,34,5014),
                                  OzonePlot("ISONE","ISO",335577,13,5006),
                                  OzonePlot("JUSSY","JUS",334191,15,5007),
                                  OzonePlot("LAUSANNE","LAU",335581,19,5008)
  )

  def defaultValidKeywords = List("Sammelmethode:", "Analysendatum:", "Blindwert","Farbreagens:", "Cal. Fact.","Probeneingang:","Nachweisgrenze")
  def defaultInvalidLinesPrefix = List( "Ozon (O3)-Messung mit Passivsammlern",
                                        "M-Periode","Ort","Code:",
                                        "Messunsicherheit www",
                                        "Die Messwerte  sind repräsentativ nur für den unmittelbaren",
                                        "Diese Daten sind Teil einer längeren Messreihe",
                                        "Geprüft",
                                        "passam ag"
                                        )
  def dbGeneratedFileHeader = "CODE_COUNTRY;	CODE_PLOT;	NAME_PLOT;	DATE_START;	time_start;	DATE_END;	time_end;	exp_time;	CODE_COMPOUND;	SAMPLER_NUMBER; absorb_code;	absorb_value;	VALUE_AQ (ug/m3);	mittel; rel SD; Blind value;	Date  of Farbreagenz;	Cal. Factor.;	Date of analysis;	Method of analysis;	Sammeler Method; Nachweisgrenze; File_Name; Bemerkung For File; Bemerkung For Line"

  def forNumberOfParametersInFile = List("Wert 4")


  def prepareOzoneFileLevelInfo(validLines: Seq[String], commentLines: Seq[String], fileName : String): OzoneFileConfig = {

    val sammelMethodeLine: Option[String] = validLines.filter(_.startsWith("Sammelmethode")).headOption
    val analysendatumLine: Option[String] = validLines.filter(_.contains("Analysendatum:")).headOption
    val probeEingangLine = validLines.filter(_.contains("Probeneingang:")).headOption

    val sammelMethodeValue = getSammelmethodValue(sammelMethodeLine)
    val analysenMethodeValue = getAnalysenMethodValue(sammelMethodeLine)
    val analysenDatumValue = getAnalysenDatumValue(analysendatumLine)
    val blindwertValue = getBlindwertValue(analysendatumLine)
    val farbreagensDatumValue = getFarbreagensDatumValue(analysendatumLine)
    val calibrationValue = getCalibrationFactorValue(analysendatumLine)
    val probenEingangDatumValue = getProbenEingangDatumValue(probeEingangLine)
    val nachweisgrenzeValue = getNachweisgrenzeValue(probeEingangLine)
    val comments = commentLines.mkString(",") replaceAll(";", "")

    val missingValue = sammelMethodeValue.isEmpty || analysenMethodeValue.isEmpty || analysenDatumValue.isEmpty || blindwertValue.isEmpty || farbreagensDatumValue.isEmpty || calibrationValue.isEmpty || probenEingangDatumValue.isEmpty || nachweisgrenzeValue.isEmpty

    OzoneFileConfig(fileName,
      sammelMethodeValue.getOrElse("undefined"),
      analysenMethodeValue.getOrElse("undefined"),
      analysenDatumValue.getOrElse("01.01.1900"),
      BigDecimal(blindwertValue.getOrElse("-9999")),
      farbreagensDatumValue.getOrElse("01.01.1900"),
      BigDecimal(calibrationValue.getOrElse("-9999")),
      probenEingangDatumValue.getOrElse("01.01.1900"),
      BigDecimal(nachweisgrenzeValue.getOrElse("-9999")),
        BigDecimal(50),
        "O3",
      comments,
      missingValue
    )

  }

    def getSammelmethodValue(sammelMethodeLine: Option[String]): Option[String] = {
      sammelMethodeLine.map( line => {
        val index = line.indexOfSlice("Analysenmethode")
        val stringToParse = line.slice(0,index)
        val samMthod = stringToParse.stripPrefix("Sammelmethode:").trim
        if (samMthod.nonEmpty) samMthod else "unknown"
    })
  }

  def getAnalysenMethodValue(sammelMethodeLine: Option[String]): Option[String] = {
    sammelMethodeLine.map( line => {
      val index = line.indexOfSlice("Analysenmethode")
      val indexOfFisrtSemicolon = line.indexOfSlice(";")
      val stringToParse = line.slice(index, indexOfFisrtSemicolon)
      val analysMethod = stringToParse.stripPrefix("Analysenmethode:").trim
      if(analysMethod.nonEmpty) analysMethod else "unknown"
    })
  }

  def getAnalysenDatumValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Blindwert")
      val stringToParse = line.slice(0, index)
      val datumWithDelimiters = stringToParse.stripPrefix(";Analysendatum:").trim
      val indexOfFisrtSemicolon = datumWithDelimiters.indexOfSlice(";")
      val analysDatum = datumWithDelimiters.slice(0,indexOfFisrtSemicolon)
      convert8DigitDateTo10Digit(analysDatum)
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

  def getBlindwertValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Farbreagens")
      val indexOfBlindwert = line.indexOfSlice("Blindwert")
      val stringToParse = line.slice(indexOfBlindwert, index)
      //To Do: Strip 'Blindwert ;;'
      val valueWithDelimiters = stringToParse.trim.stripPrefix("Blindwert").replaceAll("^\\s+", "").stripPrefix(";;")
      val indexOfFisrtSemicolon = valueWithDelimiters.indexOfSlice(";")
      val blindWert = valueWithDelimiters.slice(0,indexOfFisrtSemicolon)
      if (blindWert.nonEmpty) blindWert else "-9999"

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

case class PassSammDataRow(clnr: Integer,
                           startDate: String,
                           startTime: String,
                           endDate: String,
                           endTime: String,
                           analysid: Int,
                           duration: BigDecimal,
                           rowData1: BigDecimal,
                           rowData2 : BigDecimal,
                           rowData3 : BigDecimal,
                           bw_rowData : BigDecimal,
                           absorpData1 : BigDecimal,
                           absorpData2 : BigDecimal,
                           absorpData3 : BigDecimal,
                           bw_absorpData1 : BigDecimal,
                           konzData1 : BigDecimal,
                           konzData2 : BigDecimal,
                           konzData3 : BigDecimal,
                           bw_konzData1 : BigDecimal,
                           mittel : BigDecimal,
                           relsd : BigDecimal,
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
                           lineBem : String)

object OzoneDataRow {
  val parser: RowParser[PassSammDataRow] = {
           get[Int]("clnr")~
           get[String]("startDate")~
           get[String]("startTime")~
           get[String]("endDate")~
           get[String]("endTime")~
           get[Int]("analysid")~
           get[BigDecimal]("expduration") ~
           get[BigDecimal]("rawDat1")~
           get[BigDecimal]("rawDat2")~
           get[BigDecimal]("rawDat3")~
           get[BigDecimal]("bw_rawDat")~
           get[BigDecimal]("absorpDat1")~
           get[BigDecimal]("absorpDat2")~
           get[BigDecimal]("absorpDat3")~
           get[BigDecimal]("bw_absorpDat")~
           get[BigDecimal]("konzDat1")~
           get[BigDecimal]("konzDat2")~
           get[BigDecimal]("konzDat3")~
           get[BigDecimal]("bw_konzDat")~
           get[BigDecimal]("mittel")~
           get[BigDecimal]("relsd")~
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
             get[Option[String]]("lineBem") map {
             case clnr ~ startDate ~ startTime ~ endDate ~ endTime ~ analysid ~ duration ~ rowData1 ~ rowData2 ~ rowData3 ~ bw_rowData ~ absorpData1 ~ absorpData2 ~ absorpData3 ~ bw_absorpData1 ~ konzData1 ~ konzData2 ~ konzData3 ~ bw_konzData1 ~ mittel ~ relsd ~ probeeingdat ~  analysedatum ~ analysemethode ~ sammel ~ blindwert ~ farbreagens ~ calfactor ~ filename ~ nachweisgrenze ~ fileBem ~lineBem =>
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
                 bw_rowData,
                 absorpData1,
                 absorpData2,
                 absorpData3,
                 bw_absorpData1,
                 konzData1,
                 konzData2,
                 konzData3,
                 bw_konzData1,
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
                 lineBem.getOrElse(""))
           }
  }

}

case class OzoneFileInfo(fileName: String, header: String, ozoneData: List[String])

