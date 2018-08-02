package models.domain.Ozone


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
                               einfdat: String
)


object OzoneKeysConfig {

  def defaultPlotConfigs = List(
                                  OzonePlot("Jussy","JUS",334191,15,5007),
                                  OzonePlot("Othmarsingen","OTH",313136,30,5013),
                                  OzonePlot("Lausanne","LAU",335581,19,5008),
                                  OzonePlot("Bettlachstock","BET",335579,5,5003),
                                  OzonePlot("Novaggio","NOV",335582,27,5012),
                                  OzonePlot("Schänis","SCH",331934,32,5016)
                                )
  def defaultValidKeywords = List("Sammelmethode:", "Analysendatum:", "Blindwert","Farbreagens:", "Cal. Fact.","Probeneingang:","Nachweisgrenze")
  def defaultInvalidLinesPrefix = List( "Ozon (O3)-Messung mit Passivsammlern",
                                        "WSL, Birmensdorf","Ort",
                                        "Messunsicherheit www",
                                        "Die Messwerte  sind repräsentativ nur für den unmittelbaren",
                                        "Diese Daten sind Teil einer längeren Messreihe",
                                        "Geprüft",
                                        "passam ag"
                                        )
  def intermediateFileHeader = "CODE_COUNTRY;	CODE_PLOT;	NAME_PLOT;	SAMPLER_NUMBER;	DATE_START;	time_start;	DATE_END;	time_end;	exp_time;	CODE_COMPOUND;	absorb_code;	absorb_value;	VALUE_AQ (ug/m3);	Blind value;	Date  of Farbreagenz;	Cal. Factor.;	Date of analysis;	Method of analysis;	File_Name"

  def forNumberOfParametersInFile = List("Wert 4")


  def prepareOzoneFileLevelInfo(validLines: Seq[String], commentLines: Seq[String], fileName : String): OzoneFileConfig = {
    val sammelMethodeLine: Option[String] = validLines.filter(_.startsWith("Sammelmethode")).headOption
    val analysendatumLine: Option[String] = validLines.filter(_.startsWith(";Analysendatum:")).headOption
    val probeEingangLine = validLines.filter(_.startsWith(";Probeneingang:")).headOption

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
      analysenDatumValue.getOrElse("01.01.1900 00:00:00"),
      BigDecimal(blindwertValue.getOrElse("0")),
      farbreagensDatumValue.getOrElse("01.01.1900 00:00:00"),
      BigDecimal(calibrationValue.getOrElse("0")),
      probenEingangDatumValue.getOrElse("01.01.1900 00:00:00"),
      BigDecimal(nachweisgrenzeValue.getOrElse("0")),
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
       stringToParse.stripPrefix("Sammelmethode:").trim
    })
  }

  def getAnalysenMethodValue(sammelMethodeLine: Option[String]): Option[String] = {
    sammelMethodeLine.map( line => {
      val index = line.indexOfSlice("Analysenmethode")
      val indexOfFisrtSemicolon = line.indexOfSlice(";")
      val stringToParse = line.slice(index, indexOfFisrtSemicolon)
      stringToParse.stripPrefix("Analysenmethode:").trim
    })
  }

  def getAnalysenDatumValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Blindwert")
      val stringToParse = line.slice(0, index)
      val datumWithDelimiters = stringToParse.stripPrefix(";Analysendatum:").trim
      val indexOfFisrtSemicolon = datumWithDelimiters.indexOfSlice(";")
      datumWithDelimiters.slice(0,indexOfFisrtSemicolon)

    })
  }

  def getBlindwertValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Farbreagens")
      val indexOfBlindwert = line.indexOfSlice("Blindwert")
      val stringToParse = line.slice(indexOfBlindwert, index)
      val valueWithDelimiters = stringToParse.stripPrefix("Blindwert;;").trim
      val indexOfFisrtSemicolon = valueWithDelimiters.indexOfSlice(";")
      valueWithDelimiters.slice(0,indexOfFisrtSemicolon)

    })
  }

  def getCalibrationFactorValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Cal. Fact.")
      val stringToParse = line.slice(index,line.length)
      val valueWithDelimiters = stringToParse.stripPrefix("Cal. Fact.").trim
      val indexOfFisrtSemicolon = valueWithDelimiters.indexOfSlice(";")
      valueWithDelimiters.slice(0,indexOfFisrtSemicolon)
    })
  }

  def getFarbreagensDatumValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Cal.")

      val indexOfFarbreagens = line.indexOfSlice("Farbreagens")
      val stringToParse = line.slice(indexOfFarbreagens, index)
      val datumWithDelimiters = stringToParse.stripPrefix("Farbreagens:").trim
      val indexOfFisrtSemicolon = datumWithDelimiters.indexOfSlice(";")
      datumWithDelimiters.slice(0,indexOfFisrtSemicolon)

    })
  }

  def getProbenEingangDatumValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Nachweisgrenze")
      val stringToParse = line.slice(0, index)
      val datumWithDelimiters = stringToParse.stripPrefix(";Probeneingang:").trim
      val indexOfFisrtSemicolon = datumWithDelimiters.indexOfSlice(";")
      datumWithDelimiters.slice(0,indexOfFisrtSemicolon)

    })
  }

  def getNachweisgrenzeValue(line: Option[String]): Option[String] = {
    line.map( line => {
      val index = line.indexOfSlice("Nachweisgrenze")
      val indexOfFisrtSemicolon = line.indexOfSlice(";ug")
      val stringToParse = line.slice(index, indexOfFisrtSemicolon)
      stringToParse.stripPrefix("Nachweisgrenze;;").trim
    })
  }
}