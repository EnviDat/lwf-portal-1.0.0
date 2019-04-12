package models.phano

import models.domain.Ozone.{OzoneKeysConfig, PassSammData}
import models.domain._
import models.domain.pheno.{AufnEreig, PhanoGrowthData, PhanoPFLA}
import models.services.PhanoService
import models.util.StringToDate.formatOzoneDate
import models.util.{CurrentSysDateInSimpleFormat, NumberParser, StringToDate}
import org.joda.time.DateTimeZone
import play.Logger


object PhanoFileParser {

  def parseAndSaveData(phanoData: String, phanoService: PhanoService, valid: Boolean, stationNr: Integer, personNr: Integer, invnr: Integer, typeCode: Integer, species: Integer): List[CR1000OracleError] = {
    Logger.info(s"validating the line ${phanoData}")

    try {
        val words = phanoData.split(";", -1)

        val notSufficentValues: Option[PhanoNotSufficientParameters] = if (words.length == 1) Some(PhanoNotSufficientParameters(7, s"Only one value there in this line${phanoData}")) else None
        val plaId = validatePlaId(words(0))
        val bhdUmfangcm = getActualOrDummyValue3digitsFromString(words(1)).getOrElse(BigDecimal(-999))
        val baumhohen = getActualOrDummyValue3digitsFromString(words(2)).getOrElse(BigDecimal(-999))
        val socialStellung = getActualOrDummyValue1digitFromString(words(3)).getOrElse(BigDecimal(-9)).toInt
        val dateOfMeasurement = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(4)))
       val exceptionsWhileInsertingData = plaId match {
          case Some(pid) => {
            val pflaParameter = PhanoGrowthData(invnr,
              stationNr,
              pid,
              typeCode,
              dateOfMeasurement,
              socialStellung,
              bhdUmfangcm,
              baumhohen,
              "automatic insert")
            val phanoPfla = PhanoPFLA(stationNr,
              typeCode,
              socialStellung,
              species: Int,
              "D",
              invnr: Int,
              pid: Int,
              CurrentSysDateInSimpleFormat.sysdateDateInOracleformat
            )

            val intensityOrBluhstarke = getActualOrDummyValue1digitFromString(words(8)).getOrElse(BigDecimal(-9)).toInt

            val blattentfaltungBegin = dateOfMeasurement
            val aufnEreigBlattentfaltungBegin = AufnEreig(stationNr,
              typeCode,
              invnr,
              1,
              blattentfaltungBegin,
              personNr,
              intensityOrBluhstarke,
              pid)

            val blattentfaltungFiftyPercent = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(5)))
            val aufnEreigblattentfaltungFiftyPercent = AufnEreig(stationNr,
              typeCode,
              invnr,
              2,
              blattentfaltungFiftyPercent,
              personNr,
              intensityOrBluhstarke,
              pid)

            val bluteBegin = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(6)))
            val aufnEreigBluteBegin = AufnEreig(stationNr,
              typeCode,
              invnr,
              3,
              bluteBegin,
              personNr,
              intensityOrBluhstarke,
              pid)

            val bluteFiftyPercent = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(7)))
            val aufnEreigBluteFiftyPercent = AufnEreig(stationNr,
              typeCode,
              invnr,
              4,
              bluteFiftyPercent,
              personNr,
              intensityOrBluhstarke,
              pid)

            val fruchtreifeBegin = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(9)))
            val aufnEreigFruchtreifeBegin = AufnEreig(stationNr,
              typeCode,
              invnr,
              5,
              fruchtreifeBegin,
              personNr,
              intensityOrBluhstarke,
              pid)

            val fruchtreifeFiftyPercent = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(10)))
            val aufnEreigFruchtreifeFiftyPercent = AufnEreig(stationNr,
              typeCode,
              invnr,
              6,
              fruchtreifeFiftyPercent,
              personNr,
              intensityOrBluhstarke,
              pid)

            val blattfallFiftyPercent = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(13)))
            val aufnEreigBlattfallFiftyPercent = AufnEreig(stationNr,
              typeCode,
              invnr,
              9,
              blattfallFiftyPercent,
              personNr,
              intensityOrBluhstarke,
              pid)

            val blattverfärbungTenPercent = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(11)))
            val aufnEreigBlattverfärbungTenPercent = AufnEreig(stationNr,
              typeCode,
              invnr,
              7,
              blattverfärbungTenPercent,
              personNr,
              intensityOrBluhstarke,
              pid)

            val blattverfärbungFiftyPercent = getActualOrDummyDateOnly(OzoneKeysConfig.convert8DigitDateTo10Digit(words(12)))
            val aufnEreigBlattverfärbungFiftyPercent = AufnEreig(stationNr,
              typeCode,
              invnr,
              8,
              blattverfärbungFiftyPercent,
              personNr,
              intensityOrBluhstarke,
              pid)

            val exceptionInsertingPhanoPFLA = phanoService.insertPhanoPFLA(phanoPfla)
            val exceptionInsertingPhanoPFLAParam = phanoService.insertPhanoPFLAParameter(pflaParameter)
            val exceptionBlattentfaltungBegin = phanoService.insertPhanoAufnEreignis(aufnEreigBlattentfaltungBegin)
            val exceptionBlattentfaltungFiftyPercent = phanoService.insertPhanoAufnEreignis(aufnEreigblattentfaltungFiftyPercent)
            val exceptionBluteBegin = phanoService.insertPhanoAufnEreignis(aufnEreigBluteBegin)
            val exceptionBluteFiftyPercent = phanoService.insertPhanoAufnEreignis(aufnEreigBluteFiftyPercent)
            val exceptionFruchtreifeBegin = phanoService.insertPhanoAufnEreignis(aufnEreigFruchtreifeBegin)
            val exceptionFruchtreifeFiftyPercent = phanoService.insertPhanoAufnEreignis(aufnEreigFruchtreifeFiftyPercent)
            val exceptionBlattfallFiftyPercent = phanoService.insertPhanoAufnEreignis(aufnEreigBlattfallFiftyPercent)
            val exceptionBlattverfärbungTenPercent = phanoService.insertPhanoAufnEreignis(aufnEreigBlattverfärbungTenPercent)
            val exceptionBlattverfärbungFiftyPercent = phanoService.insertPhanoAufnEreignis(aufnEreigBlattverfärbungFiftyPercent)

            List(
              exceptionInsertingPhanoPFLA,
              exceptionInsertingPhanoPFLAParam,
              exceptionBlattentfaltungBegin,
              exceptionBlattentfaltungFiftyPercent,
              exceptionBluteBegin,
              exceptionBluteFiftyPercent,
              exceptionFruchtreifeBegin,
              exceptionFruchtreifeFiftyPercent,
              exceptionBlattfallFiftyPercent,
              exceptionBlattverfärbungTenPercent,
              exceptionBlattverfärbungFiftyPercent
            )
          }
          case None => List()
        }
        exceptionsWhileInsertingData.flatten.toList
    }
    catch {
      case e: ArrayIndexOutOfBoundsException =>
        List(CR1000OracleError(999, "File was either empty or contained lines with error values"))
      case error : Throwable =>
       List(CR1000OracleError(-1, error.toString))
    }

  }

  private def ifItIsBlindWertObservation(passmData: PassSammData) = {
      passmData.rowData1 != BigDecimal(-9999) &&
      passmData.absorpData1 != BigDecimal(-9999) &&
      passmData.konzData1 != BigDecimal(-9999) &&
      passmData.rowData2 == BigDecimal(-9999) &&
      passmData.absorpData2 == BigDecimal(-9999) &&
      passmData.konzData2 == BigDecimal(-9999) &&
      passmData.rowData3 == BigDecimal(-9999) &&
      passmData.absorpData3 == BigDecimal(-9999) &&
      passmData.konzData3 == BigDecimal(-9999) &&
      (passmData.blindSampler != 1)

  }

  def validatePlaId(code: String) = {
    NumberParser.parseNumber(code)
  }

  private def ifValuesBelowNachweisGrenze(passmData: PassSammData, nachweisgrenze: BigDecimal) = {
    ((passmData.konzData1 != BigDecimal(-9999) && passmData.konzData1 != BigDecimal(-8888) && passmData.konzData1 < nachweisgrenze) &&
    (passmData.konzData2 != BigDecimal(-9999) && passmData.konzData2 != BigDecimal(-8888) && passmData.konzData2 < nachweisgrenze) &&
    (passmData.konzData3 != BigDecimal(-9999) &&  passmData.konzData3 != BigDecimal(-8888) && passmData.konzData3 < nachweisgrenze)) ||
      (passmData.konzData3 == BigDecimal(-8888) &&
      passmData.konzData1 == BigDecimal(-8888) &&
      passmData.konzData2 == BigDecimal(-8888))
  }

  private def ifAnyValuesBelowNachweisGrenze(passmData: PassSammData, nachweisgrenze: BigDecimal) = {
    (passmData.konzData1 != BigDecimal(-9999) && passmData.konzData1 != BigDecimal(-8888) && passmData.konzData1 < nachweisgrenze) ||
      (passmData.konzData2 != BigDecimal(-9999) && passmData.konzData2 != BigDecimal(-8888) && passmData.konzData2 < nachweisgrenze) ||
      (passmData.konzData3 != BigDecimal(-9999) &&  passmData.konzData3 != BigDecimal(-8888) && passmData.konzData3 < nachweisgrenze) ||
      passmData.konzData3 == BigDecimal(-8888) ||
      passmData.konzData1 == BigDecimal(-8888) ||
      passmData.konzData2 == BigDecimal(-8888)
  }

  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), cf.messArt)).sortBy(_._1)
  }

  def validatePlotCode(code: String) = {
    OzoneKeysConfig.defaultPlotConfigs.find(plot => {
      (plot.plotName == code.trim) || plot.abbrePlot == code.trim
    })
  }

  def getActualOrDummyDateTime(dateInString: String) = {
    s"to_date('${StringToDate.oracleDateFormat.print(formatOzoneDate.withZone(DateTimeZone.UTC).parseDateTime(dateInString))}', 'DD.MM.YYYY HH24:MI:SS')"
  }

  def getActualOrDummyDateOnly(dateInString: String) = {
    s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(dateInString))}', 'DD.MM.YYYY')"
  }

  def getActualOrDummyValues(values : Seq[String]): Seq[BigDecimal] = {
    values.flatMap(element => {
      getActualOrDummyValueFromString(element)
    })
  }

  def getActualOrDummyValueFromString(element: String) = {
    NumberParser.parseBigDecimalWithLessThanSign(element) match {
      case Some(x) => Some(x)
      case _ => Some(BigDecimal(-9999))
    }
  }

    def getActualOrDummyValue3digitsFromString(element: String) = {
      NumberParser.parseBigDecimalWithLessThanSign(element) match {
        case Some(x) => Some(x)
        case _ => Some(BigDecimal(-999))
      }
  }

  def getActualOrDummyValue1digitFromString(element: String) = {
    NumberParser.parseBigDecimalWithLessThanSign(element) match {
      case Some(x) => Some(x)
      case _ => Some(BigDecimal(-9))
    }
  }

}
