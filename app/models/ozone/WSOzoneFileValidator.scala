package models.ozone

import models.domain.Ozone.OzoneKeysConfig
import models.util.{NumberParser, StringToDate}
import play.Logger

object WSOzoneFileValidator {

  def validateLine(fileName: String, lineToValidate: String): List[OzoneExceptions] = {
    Logger.info(s"validating the line ${lineToValidate}")

    try {
      val words = lineToValidate.split(";")
      val notSufficentValues: Option[OzoneNotSufficientParameters] = if (words.length < 17) Some(OzoneNotSufficientParameters(7, s"In sufficient values ${lineToValidate}")) else None
      val ortCode = validatePlotCode(words(0))
      val ortCodeError: Option[OzoneInvalidPlotException] = if(ortCode.nonEmpty) None else Some(OzoneInvalidPlotException(1, s"Invalid plot code ${lineToValidate}"))
      val beginnDateError: Option[OzoneExceptions] = validateDateFormat(words(1) + " " + words(2) + ":00", lineToValidate)
      val endDateError: Option[OzoneExceptions] = validateDateFormat(words(3) + " " + words(4) + ":00", lineToValidate)
      val remainingValues = words.drop(5).toList
      val parameterValuesError: Seq[OzoneExceptions] = validateValueOfParameters(remainingValues, lineToValidate)
      if (ortCodeError.isEmpty && beginnDateError.isEmpty && endDateError.isEmpty && parameterValuesError.isEmpty && notSufficentValues.isEmpty)
        List()
      else
        List(ortCodeError, beginnDateError, endDateError, notSufficentValues).flatten ::: parameterValuesError
    }
  catch {
    case e: ArrayIndexOutOfBoundsException =>
      List(OzoneNotSufficientParameters(999, "File was either empty or contained lines with error values"))
    case error : Throwable =>
      List(OzoneFileError(-1, error.toString))
    }
  }

  def validatePlotCode(code: String) = {
    OzoneKeysConfig.defaultPlotConfigs.find(plot => {
      (plot.plotName == code.trim) || plot.abbrePlot == code.trim
    })
  }


  def validateValueOfParameters(words: List[String], lineToValidate: String): List[OzoneExceptions] = {
    words.flatMap(w => {
      val parsedValue = NumberParser.parseBigDecimal(w)
      parsedValue match {
        case None => Some(OzoneInvalidNumberFoundException(6, s"parameter is not a number, ${lineToValidate}"))
        case _ => None
      }
    })
  }


  def validateDateFormat(date: String, lineToValidate: String):Option[OzoneExceptions] = {
      StringToDate.stringToDateConvertoZONE(date, lineToValidate)
  }
}
