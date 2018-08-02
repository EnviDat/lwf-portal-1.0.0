package models.ozone

import models.domain.Ozone.{OzoneKeysConfig, PassSammData}
import models.domain._
import models.ozone.WSOzoneFileValidator.validateDateFormat
import models.services.MeteoService
import models.util.StringToDate.formatOzoneDate
import models.util.{CurrentSysDateInSimpleFormat, NumberParser, StringToDate}
import org.joda.time.DateTimeZone
import play.Logger


object WSOzoneFileParser {

  def parseAndSaveData(ozoneData: String, meteoService: MeteoService, valid: Boolean, analyseId: Int, numberofParameters: Int): Option[OzoneExceptions] = {
    Logger.info(s"validating the line ${ozoneData}")

    try {
        val words = ozoneData.split(";")

        val notSufficentValues: Option[OzoneNotSufficientParameters] = if (words.length < 17) Some(OzoneNotSufficientParameters(7, s"In sufficient values ${ozoneData}")) else None
        val ortCode = validatePlotCode(words(0))
        val beginnDateError: Option[OzoneExceptions] = validateDateFormat(words(1) + " " + words(2) + ":00", ozoneData)
        val endDateError: Option[OzoneExceptions] = validateDateFormat(words(3) + " " + words(4) + ":00", ozoneData)
        val remainingValues: Seq[String] = words.drop(5).toList

        val clnr: Int = ortCode.map(_.clnrPlot.intValue()).getOrElse(Integer.parseInt("-1"))
        val beginDate: String = if (beginnDateError.isEmpty) getActualOrDummyDate(words(1) + " " + words(2) + ":00") else getActualOrDummyDate("01.01.1900 00:00:00")
        val endDate = if (endDateError.isEmpty) getActualOrDummyDate(words(3) + " " + words(4) + ":00") else getActualOrDummyDate("01.01.1900 00:00:00")
        val parameterValues = if (remainingValues.length > 12 && numberofParameters == 12)
          getActualOrDummyValues(remainingValues.slice(0, 11))
        else if(remainingValues.length > 15 && numberofParameters == 15)
          getActualOrDummyValues(remainingValues.slice(0, 14))
        else
          getActualOrDummyValues(remainingValues)

        if (numberofParameters == 12) {

          val mapFolgNr: Seq[(BigDecimal, Int)] = parameterValues.zipWithIndex
          val duration: BigDecimal = mapFolgNr.find(_._2 == 0).map(_._1).getOrElse(BigDecimal("-9999"))
          val rowdata1 = mapFolgNr.find(_._2 == 2).map(_._1).getOrElse(BigDecimal("-9999"))
          val rowdata2 = mapFolgNr.find(_._2 == 4).map(_._1).getOrElse(BigDecimal("-9999"))
          val rowdata3 = mapFolgNr.find(_._2 == 6).map(_._1).getOrElse(BigDecimal("-9999"))

          val absorpData1 = mapFolgNr.find(_._2 == 1).map(_._1).getOrElse(BigDecimal("-9999"))
          val absorpData2 = mapFolgNr.find(_._2 == 3).map(_._1).getOrElse(BigDecimal("-9999"))
          val absorpData3 = mapFolgNr.find(_._2 == 5).map(_._1).getOrElse(BigDecimal("-9999"))
          //val absorpData4 =  mapFolgNr.find(_._1 == 5).map(_._2).getOrElse(BigDecimal("-9999"))
          val konzData1 = mapFolgNr.find(_._2 == 7).map(_._1).getOrElse(BigDecimal("-9999"))
          val konzData2 = mapFolgNr.find(_._2 == 8).map(_._1).getOrElse(BigDecimal("-9999"))
          val konzData3 = mapFolgNr.find(_._2 == 9).map(_._1).getOrElse(BigDecimal("-9999"))
          //val konzData4 =  mapFolgNr.find(_._1 == 5).map(_._2).getOrElse(BigDecimal("-9999"))
          val mittel = mapFolgNr.find(_._2 == 10).map(_._1).getOrElse(BigDecimal("-9999"))
          val manualValidation = if (valid == false) 0 else 1
          val bemerk = if (valid == false) "Look at the remarks stored with File information" else ""
          val einfdat = CurrentSysDateInSimpleFormat.sysdateDateInOracleformat
          val dataToInsert = PassSammData(clnr, beginDate, endDate, duration, rowdata1, rowdata2, rowdata3, BigDecimal("-9999"), absorpData1, absorpData2, absorpData3, BigDecimal("-9999"), konzData1, konzData2, konzData3, BigDecimal("-9999"), mittel, bemerk, manualValidation, einfdat)
          meteoService.insertOzoneData(dataToInsert, analyseId)
        } else {
          val mapFolgNr: Seq[(BigDecimal, Int)] = parameterValues.zipWithIndex
          val duration: BigDecimal = mapFolgNr.find(_._2 == 0).map(_._1).getOrElse(BigDecimal("-9999"))
          val rowdata1 = mapFolgNr.find(_._2 == 2).map(_._1).getOrElse(BigDecimal("-9999"))
          val rowdata2 = mapFolgNr.find(_._2 == 4).map(_._1).getOrElse(BigDecimal("-9999"))
          val rowdata3 = mapFolgNr.find(_._2 == 6).map(_._1).getOrElse(BigDecimal("-9999"))
          val rowdata4 =  mapFolgNr.find(_._2 == 8).map(_._1).getOrElse(BigDecimal("-9999"))

          val absorpData1 = mapFolgNr.find(_._2 == 1).map(_._1).getOrElse(BigDecimal("-9999"))
          val absorpData2 = mapFolgNr.find(_._2 == 3).map(_._1).getOrElse(BigDecimal("-9999"))
          val absorpData3 = mapFolgNr.find(_._2 == 5).map(_._1).getOrElse(BigDecimal("-9999"))
          val absorpData4 =  mapFolgNr.find(_._2 == 7).map(_._1).getOrElse(BigDecimal("-9999"))

          val konzData1 = mapFolgNr.find(_._2 == 9).map(_._1).getOrElse(BigDecimal("-9999"))
          val konzData2 = mapFolgNr.find(_._2 == 10).map(_._1).getOrElse(BigDecimal("-9999"))
          val konzData3 = mapFolgNr.find(_._2 == 11).map(_._1).getOrElse(BigDecimal("-9999"))
          val konzData4 =  mapFolgNr.find(_._2 == 12).map(_._1).getOrElse(BigDecimal("-9999"))

          val mittel = mapFolgNr.find(_._2 == 13).map(_._1).getOrElse(BigDecimal("-9999"))
          val manualValidation = if (valid == false) 0 else 1
          val bemerk = if (valid == false) "Look at the remarks stored with File information" else ""
          val einfdat = CurrentSysDateInSimpleFormat.sysdateDateInOracleformat
          val dataToInsert = PassSammData(clnr, beginDate, endDate, duration, rowdata1, rowdata2, rowdata3, rowdata4, absorpData1, absorpData2, absorpData3, absorpData4, konzData1, konzData2, konzData3, konzData4, mittel, bemerk, manualValidation, einfdat)
          meteoService.insertOzoneData(dataToInsert, analyseId)
        }

    }
    catch {
      case e: ArrayIndexOutOfBoundsException =>
        Some(OzoneNotSufficientParameters(999, "File was either empty or contained lines with error values"))
      case error : Throwable =>
       Some(OzoneFileError(-1, error.toString))
    }

  }

  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), cf.messArt)).sortBy(_._1)
  }

  def validatePlotCode(code: String) = {
    OzoneKeysConfig.defaultPlotConfigs.find(plot => {
      (plot.plotName == code.trim) || plot.abbrePlot == code.trim
    })
  }

  def getActualOrDummyDate(dateInString: String) = {
    s"to_date('${StringToDate.oracleDateFormat.print(formatOzoneDate.withZone(DateTimeZone.UTC).parseDateTime(dateInString))}', 'DD.MM.YYYY HH24:MI:SS')"
  }



  def getActualOrDummyValues(values : Seq[String]): Seq[BigDecimal] = {
    values.flatMap(element => {
      NumberParser.parseBigDecimal(element) match {
        case Some(x) => Some(x)
        case _ => Some(BigDecimal(-9999))
      }
    })
  }

}
