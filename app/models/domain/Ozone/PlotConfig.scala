package models.domain.Ozone

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.ozone.{OzoneSuspiciousDataLineError, WSOzoneFileParser}
import models.util.NumberParser


case class OzonePlot(plotName: String, abbrePlot: String, clnrPlot: BigDecimal, statnr: Integer, codePlot: String, icpPlotCode: Int)

case class OzoneFileConfig(fileName: String, sammelMethode: String, anaylysenMethode: String, analysenDatum: String, blindwert: BigDecimal, farrbreagens: String, calFactor: BigDecimal, probenEingang: String, nachweisgrenze: BigDecimal, codeCountry: BigDecimal,  codeCompound: String, remarks: String, missingInfo: Boolean)

case class PassSammData(clnr: BigDecimal,
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
                               relSD: BigDecimal,
                               blindSampler: Int
)


object OzoneKeysConfig {

  def defaultPlotConfigs = List(
                                  OzonePlot("Jussy","JUS",334191,15,"5007",7),
                                  OzonePlot("Othmarsingen","OTH",313136,30,"5013",13),
                                  OzonePlot("Lausanne","LAU",335581,19,"5008",8),
                                  OzonePlot("Bettlachstock","BET",335579,5,"5003",3),
                                  OzonePlot("Betlachstock","BET",335579,5,"5003",3),
                                  OzonePlot("Navaggio","NOV",335582,27,"5012",12),
                                  OzonePlot("Schänis","SCH",331934,32,"5016",16),
                                  OzonePlot("Isone","ISO",335577,13,"5006",6),
                                  OzonePlot("WSL, Birmensdorf", "WSL",374674,59,"-14",-14),
                                  OzonePlot("Neunkirch","NEU",336068,25,"5011",11),
                                  OzonePlot("Vordemwald","VOR",335584,36,"5015",15),
                                  OzonePlot("Vor dem Wald","VOR",335584,36,"5015",15),
                                  OzonePlot("Schaenis","SCH",331934,32,"5016",16),
                                  OzonePlot("Novaggio","NOV",335582,27,"5012",12),
                                  OzonePlot("LAT","LAT",337588.1,40,"5017_1",17),//remove these plots
                                  OzonePlot("Celerina", "CEL",333379,7,"5004",4),
                                  OzonePlot("Nationalpark", "NAT",333373,23,"5010",10),//remove these plots
                                  OzonePlot("Chironico", "CHI",335580,9,"5005",5),
                                  OzonePlot("Beatenberg", "BEA",335578,3,"5002",2),
                                  OzonePlot("Visp", "VIS",335583,34,"5014",14),
                                  OzonePlot("Lens", "LEN",326208,21,"5009",9),
                                  OzonePlot("Davos", "DAV",-1111,-6666,"5018",18),
                                  OzonePlot("Pfynwald", "PFY",-1112,-6666,"-12",-12),
                                  OzonePlot("Lat Met", "LAT-MET",337588.2,40,"5017_2",17),
                                  OzonePlot("Lat OP5", "LAT-OP5",337588.3,40,"5017_3",17),
                                  OzonePlot("Lat OP5", "LAT-OP5",337588.3,40,"5017_3",17),
                                  OzonePlot("LAT-MET", "LAT-MET",337588.2,40,"5017_2",17),
                                  OzonePlot("LAT -MET", "LAT-MET",337588.2,40,"5017_2",17),
                                  OzonePlot("LAT- MET", "LAT-MET",337588.2,40,"5017_2",17),
                                  OzonePlot("LAT - MET", "LAT-MET",337588.2,40,"5017_2",17),
                                  OzonePlot("LAT-OP5", "LAT-OP5",337588.3,40,"5017_3",17),
                                  OzonePlot("LAT- OP5", "LAT-OP5",337588.3,40,"5017_3",17),
                                  OzonePlot("LAT -OP5", "LAT-OP5",337588.3,40,"5017_3",17),
                                  OzonePlot("LAT - OP5", "LAT-OP5",337588.3,40,"5017_3",17),
                                  OzonePlot("LA-OP5", "LAT-OP5",337588.3,40,"5017_3",17),
                                  OzonePlot("LAT-OTC", "LAT-OTC",337588.4,40,"5017_4",17),
                                  OzonePlot("LAT - OTC", "LAT-OTC",337588.4,40,"5017_4",17),
                                  OzonePlot("LAT- OTC", "LAT-OTC",337588.4,40,"5017_4",17),
                                  OzonePlot("LAT -OTC", "LAT-OTC",337588.4,40,"5017_4",17),
                                  OzonePlot("Alpthal", "ALP",335110,-6666,"5001",1),//alpthal bestand meteo station is used now instead of -1113
                                  OzonePlot("Lattecaldo","LAT",337588.1,40,"5017_1",17),
                                  OzonePlot("Birmensdorf", "WSL",374674,59,"-14",-14),
                                  OzonePlot("Isone ","ISO",335577,13,"5006",6),
                                  OzonePlot("VISP", "VIS",335583,34,"5014",14),
                                  OzonePlot("ISONE","ISO",335577,13,"5006",6),//remove these plots
                                  OzonePlot("JUSSY","JUS",334191,15,"5007",7),
                                  OzonePlot("LAUSANNE","LAU",335581,19,"5008",8)
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
  def dbGeneratedFileHeader = "CODE_COUNTRY;	CODE_PLOT;	NAME_PLOT;	DATE_START;	time_start;	DATE_END;	time_end;	exp_time;	CODE_COMPOUND;	SAMPLER_NUMBER; absorb_code;	absorb_value;	VALUE_AQ (ug/m3);	mean; rel SD; Blind value;	Date  of color reagent;	Cal. Factor.;	Date of analysis;	Method of analysis;	Sampler methode; Detection Limit; File_Name; Comments For File; Comments For Line"

  def icpForestAQPandAQBFileHeader = "!Sequence; country; plot; sampler; date_start; date_end; compound; value; other_observations"

  def icpForestPPSFileHeader = "!Sequence;country;plot;latitude;longitude;altitude;compound;sampler;manufacturer;date_monitoring_first;date_monitoring_last;measurements;col;altitude_m;elevation_lowest2500;elevation_lowest5000; other_observations"


  def forNumberOfParametersInFile = List("Wert 4")

  def listOfBlindSamplerTextPermutations = List("Blind sampler", "blind sampler", "Blind Sampler", "Blind_sampler", "Blindsampler", "blind Sampler")


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
    val validComments = if(comments.size > 6) comments else ""

    val missingValue = sammelMethodeValue.isEmpty || analysenMethodeValue.isEmpty || analysenDatumValue.isEmpty || blindwertValue.isEmpty || farbreagensDatumValue.isEmpty || calibrationValue.isEmpty || probenEingangDatumValue.isEmpty || nachweisgrenzeValue.isEmpty ||
      (blindwertValue.nonEmpty && blindwertValue.contains(BigDecimal(-9999))) ||   (calibrationValue.nonEmpty && calibrationValue.contains(BigDecimal(-9999))) ||
      (nachweisgrenzeValue.nonEmpty && nachweisgrenzeValue.contains(BigDecimal(-9999))) ||
      (analysenDatumValue.nonEmpty && analysenDatumValue.contains("01.01.1900")) ||
      (farbreagensDatumValue.nonEmpty && farbreagensDatumValue.contains("01.01.1900")) ||
      (probenEingangDatumValue.nonEmpty && probenEingangDatumValue.contains("01.01.1900"))

    OzoneFileConfig(fileName,
      sammelMethodeValue.getOrElse("undefined"),
      analysenMethodeValue.getOrElse("undefined"),
      analysenDatumValue.getOrElse("01.01.1900"),
      blindwertValue.getOrElse(BigDecimal("-9999")),
      farbreagensDatumValue.getOrElse("01.01.1900"),
      BigDecimal(calibrationValue.getOrElse("-9999")),
      probenEingangDatumValue.getOrElse("01.01.1900"),
      BigDecimal(nachweisgrenzeValue.getOrElse("-9999")),
        BigDecimal(50),
        "O3",
      validComments,
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
  val parser: RowParser[PassSammDataRow] = {
           get[BigDecimal]("clnr")~
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
  }

}

case class OzoneFileInfo(fileName: String, header: String, ozoneData: List[String])

