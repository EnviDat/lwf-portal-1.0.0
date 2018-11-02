package models.phano

import models.domain.Ozone.OzoneKeysConfig
import models.domain.pheno.PhanoPlotKeysConfig
import models.ozone.OzoneExceptions
import models.util.{NumberParser, StringToDate}
import play.Logger

object PhanoFileValidator {

  def validateLine(fileName: String, lineToValidate: String, numberofParameters: Int): Either[(List[PhanoExceptions],String), String] = {
   /* Logger.info(s"validating the line ${lineToValidate}")

    try {
      val words = lineToValidate.split(";")
      val notSufficentValues: Option[PhanoNotSufficientParameters] = if (words.length < 14) Some(PhanoNotSufficientParameters(7, s"In sufficient values ${lineToValidate}")) else None
      val plaId = validatePlaId(words(0))
      val plaCodeError: Option[PhanoInvalidPlaIDException] = if(plaId.nonEmpty) None else Some(PhanoInvalidPlaIDException(1, s"Invalid pla code ${lineToValidate}"))
      val beginnDateError: Option[PhanoExceptions] = validateDateFormat(PhanoPlotKeysConfig.convert8DigitDateTo10Digit(words(1)) + " " + words(2) + ":00", lineToValidate)
      val endDateError: Option[OzoneExceptions] = validateDateFormat(OzoneKeysConfig.convert8DigitDateTo10Digit(words(3)) + " " + words(4) + ":00", lineToValidate)
      val remainingValues = words.drop(5).toList
      val parameterValuesError = if (remainingValues.length > 12 && numberofParameters == 12)
        validateValueOfParameters(remainingValues.slice(0,11), lineToValidate)
      else if(remainingValues.length > 15 && numberofParameters == 15)
        validateValueOfParameters(remainingValues.slice(0,14), lineToValidate)
      else
        validateValueOfParameters(remainingValues, lineToValidate)
      if (ortCodeError.isEmpty && beginnDateError.isEmpty && endDateError.isEmpty && parameterValuesError.isEmpty && notSufficentValues.isEmpty)
        Right(lineToValidate)
      else
        Left(List(ortCodeError, beginnDateError, endDateError, notSufficentValues).flatten ::: parameterValuesError, lineToValidate)
    }
  catch {
    case e: ArrayIndexOutOfBoundsException =>
      Left(List(OzoneNotSufficientParameters(999, "File was either empty or contained lines with error values")),lineToValidate)
    case error : Throwable =>
      Left(List(OzoneFileError(-1, error.toString)),lineToValidate)
    }*/
    Left(List(OzoneFileError(-1, "error.toString")),lineToValidate)
  }

  def validatePlaId(code: String) = {
    NumberParser.parseNumber(code)
  }


  def validateValueOfParameters(words: List[String], lineToValidate: String): List[OzoneInvalidNumberFoundException] = {
    words.flatMap(w => {
      val parsedValue = NumberParser.parseBigDecimalWithLessThanSign(w)
      parsedValue match {
        case None => Some(OzoneInvalidNumberFoundException(6, s"parameter is not a number, ${parsedValue} in the line ${lineToValidate}"))
        case _ => None
      }
    })
  }


  def validateDateFormat(date: String, lineToValidate: String):Option[OzoneExceptions] = {
      StringToDate.stringToDateConvertoZONE(date, lineToValidate)
  }
}
