package models.domain.meteorology.ethlaegeren.validator

import models.domain._
import models.util.{NumberParser, StringToDate}
import play.Logger
import schedulers.{ParametersProject, StationKonfig}

object ETHLaeFileValidator {

  def validateLine(fileName: String, lineToValidate: String, stationKonfigs: List[StationKonfig]): List[CR1000Exceptions] = {
    Logger.info(s"Stat Konfig loaded ${stationKonfigs.mkString("\n")}")
    val mapStatKonfig = stationKonfigs.filter(sk => fileName.startsWith(sk.fileName))

    try {
      val words = lineToValidate.split(",")
      val notSufficentValues = if (words.length < 6) Some(CR1000NotSufficientParameters(7, s"In sufficient values")) else None
      val date = words(0).replace("\"", "")
      //val recordNumber = words(1)
      val stationNumber = Some(124)
      val projectNumber = mapStatKonfig.headOption.flatMap(_.projs.headOption.map(_.projNr))
      val periode = validatePeriod(Some(10), lineToValidate)
      val projectsParam: Option[ParametersProject] = mapStatKonfig.flatMap(_.projs).find(p => {
          projectNumber.contains(p.projNr) &&
          periode.isEmpty
      }).headOption

      val elements = words.map(_.replace("\"", "")).drop(2).toList
      val stationNumberError = validateStationNumber(fileName, stationNumber, mapStatKonfig.headOption, lineToValidate)
      val projectNumberError = validateProjectNumber(fileName, projectNumber, mapStatKonfig.headOption, lineToValidate)
      val dateError = validateDateFormat(date, lineToValidate)
      val parametersError = validateNumberOfParameters(elements.length, projectNumber, mapStatKonfig.headOption, projectsParam, lineToValidate)
      val parameterValuesError = validateValueOfParameters(elements, lineToValidate)
      if (stationNumberError.isEmpty && projectNumberError.isEmpty && dateError.isEmpty && parametersError.isEmpty && parameterValuesError.isEmpty && notSufficentValues.isEmpty && periode.isEmpty)
        List()
      else
        List(stationNumberError, projectNumberError, dateError, parametersError, notSufficentValues, periode).flatten ::: parameterValuesError
    }
  catch {
    case e: ArrayIndexOutOfBoundsException =>
      List(CR1000NotSufficientParameters(999, "File was either empty or contained lines with error values"))
    case error : Throwable =>
      List(CR1000FileError(-1, error.toString))
    }
  }


  def validateNumberOfParameters(numberOfParameters :Int, projectNumber :Option[Int], stationKonfig: Option[StationKonfig], projectsParam: Option[ParametersProject], lineToValidate: String) :Option[CR1000Exceptions] = {
    (stationKonfig,projectNumber, projectsParam) match {
      case (None,_,_) => Some(CR1000ConfigNotFoundException(5, s"No Configuration Found for projectNumber: ${projectNumber} and parametets: ${numberOfParameters}, ${lineToValidate}"))
      case (Some(konfig), Some(projNr), Some(params)) if params.projNr == projNr && params.param == numberOfParameters => None
      case (_,None,_) => Some(CR1000InvalidIntegerFoundException(6, s" ${projectNumber} : project number is not an Integer, ${lineToValidate}"))
      case (_,_,_) => Some(CR1000InvalidNumberOfParametersException(1, s"Project number and parameters doesn't corresponds accroding to configuration: projectNumber: ${projectNumber} and parametets: ${numberOfParameters}, ${lineToValidate}"))
    }
  }

  def validateValueOfParameters(words: List[String], lineToValidate: String): List[CR1000Exceptions] = {
    words.flatMap(w => {
      val parsedValue = NumberParser.parseBigDecimal(w)
      parsedValue match {
        case None => Some(CR1000InvalidIntegerFoundException(6, s"messart is not bigdecimal, ${lineToValidate}"))
        case _ => None
      }
    })
  }


  def validateStationNumber(filename: String, stationNumber: Option[Int], stationKonfig: Option[StationKonfig], lineToValidate: String) :Option[CR1000Exceptions] = {
    (stationKonfig, stationNumber) match {
      case (None,_) => Some(CR1000ConfigNotFoundException(5, s"No Configuration Found for filename: ${filename} and station Number: ${stationNumber}, ${lineToValidate}"))
      case (Some(konfig), Some(statNumber)) if filename.startsWith(konfig.fileName) && konfig.statNr == statNumber => None
      case (_,None) => Some(CR1000InvalidIntegerFoundException(6, s"station number is not an Integer: ${stationNumber} filename: ${filename}, ${lineToValidate}"))
      case (_,_) => Some(CR1000InvalidStationNumberException(1, s"File name or station number is wrong: filename: ${filename} and station Number: ${stationNumber}, ${lineToValidate}"))
    }
  }

  def validateProjectNumber(filename: String, projectNumber: Option[Int], stationKonfig: Option[StationKonfig], lineToValidate: String) :Option[CR1000Exceptions] = {
    (stationKonfig, projectNumber) match {
      case (None,_) => Some(CR1000ConfigNotFoundException(5, s"No Configuration Found for filename: ${filename} and project Number: ${projectNumber}, ${lineToValidate}"))
      case (Some(konfig), Some(projNr)) if filename.startsWith(konfig.fileName) && konfig.projs.map(_.projNr).contains(projNr) => None
      case (_,None) => Some(CR1000InvalidIntegerFoundException(6, s"project number is not an Integer: ${projectNumber} filename: ${filename}, ${lineToValidate}"))
      case (_,_) => Some(CR1000InvalidProjectNumberException(2, s"File name or project number is wrong: filename: ${filename} and station Number: ${projectNumber}, ${lineToValidate}"))
    }
  }

  def validateDateFormat(date: String, lineToValidate: String):Option[CR1000Exceptions] = {
      StringToDate.stringToDateConvertCR1000(date, lineToValidate)
  }

  def validatePeriod(periode: Option[Int], lineToValidate: String) = {
    periode match {
      case None => Some(CR1000InvalidIntegerFoundException(6, s"${periode} periode is not an Integer, ${lineToValidate}"))
      case _ => None
    }
  }


}
