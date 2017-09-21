package models.services

import models.domain._
import models.util.{NumberParser, StringToDate}
import play.Logger
import schedulers.StationKonfig

import scala.util.{Failure, Success, Try}

object CR1000FileValidator {

  def validateLine(fileName :String, lineToValidate :String, stationKonfigs: List[StationKonfig]) :List[CR1000Exceptions] = {
    Logger.info(s"Stat Konfig loaded ${stationKonfigs.mkString("\n")}")
    val mapStatKonfig = stationKonfigs.filter(sk => fileName.toUpperCase.startsWith(sk.fileName)).headOption
    val words = lineToValidate.split(",")
    val notSufficentValues = if(words.length<6) Some(CR1000NotSufficientParameters(7, s"In sufficient values")) else None
    val date = words(0).replace("\"", "")
    val recordNumber = words(1)
    val stationNumber =  NumberParser.parseNumber(words(2))
    val projectNumber =  NumberParser.parseNumber(words(3))
    val periode = validatePeriod(NumberParser.parseNumber(words(4)))
    val elements = words.drop(5).toList
    val stationNumberError = validateStationNumber(fileName, stationNumber, mapStatKonfig)
    val projectNumberError = validateProjectNumber(fileName, projectNumber, mapStatKonfig)
    val dateError =  validateDateFormat(date)
    val parametersError = validateNumberOfParameters(elements.length, projectNumber, mapStatKonfig)
    val parameterValuesError = validateValueOfParameters(elements)
    if(stationNumberError.isEmpty && projectNumberError.isEmpty && dateError.isEmpty && parametersError.isEmpty && parameterValuesError.isEmpty && notSufficentValues.isEmpty && periode.isEmpty)
      List()
    else
      List(stationNumberError, projectNumberError, dateError, parametersError, notSufficentValues, periode).flatten ::: parameterValuesError
  }


  def validateNumberOfParameters(numberOfParameters :Int, projectNumber :Option[Int], stationKonfig: Option[StationKonfig]) :Option[CR1000Exceptions] = {
    (stationKonfig,projectNumber) match {
      case (None,_) => Some(CR1000ConfigNotFoundException(5, s"No Configuration Found for projectNumber: ${projectNumber} and parametets: ${numberOfParameters}"))
      case (Some(konfig), Some(projNr)) if konfig.projs.filter(p => (p.projNr == projNr && p.param == numberOfParameters)).nonEmpty => None
      case (_,None) => Some(CR1000InvalidIntegerFoundException(6, s"project number is not an Integer"))
      case (_,_) => Some(CR1000InvalidNumberOfParametersException(1, s"Project number and parameters doesn't corresponds accroding to configuration: projectNumber: ${projectNumber} and parametets: ${numberOfParameters}"))
    }
  }

  def validateValueOfParameters(words: List[String]): List[CR1000Exceptions] = {
    words.flatMap(w => {
      val parsedValue = NumberParser.parseBigDecimal(w)
      parsedValue match {
        case None => Some(CR1000InvalidIntegerFoundException(6, s"messart is not bigdecimal"))
        case _ => None
      }
    })
  }


  def validateStationNumber(filename: String, stationNumber: Option[Int], stationKonfig: Option[StationKonfig]) :Option[CR1000Exceptions] = {
    (stationKonfig, stationNumber) match {
      case (None,_) => Some(CR1000ConfigNotFoundException(5, s"No Configuration Found for filename: ${filename} and station Number: ${stationNumber}"))
      case (Some(konfig), Some(statNumber)) if filename.toUpperCase.startsWith(konfig.fileName) && konfig.statNr == statNumber => None
      case (_,None) => Some(CR1000InvalidIntegerFoundException(6, s"station number is not an Integer: filename: ${filename}"))
      case (_,_) => Some(CR1000InvalidStationNumberException(1, s"File name or station number is wrong: filename: ${filename} and station Number: ${stationNumber}"))
    }
  }

  def validateProjectNumber(filename: String, projectNumber: Option[Int], stationKonfig: Option[StationKonfig]) :Option[CR1000Exceptions] = {
    (stationKonfig, projectNumber) match {
      case (None,_) => Some(CR1000ConfigNotFoundException(5, s"No Configuration Found for filename: ${filename} and project Number: ${projectNumber}"))
      case (Some(konfig), Some(projNr)) if filename.toUpperCase.startsWith(konfig.fileName) && konfig.projs.map(_.projNr).contains(projNr) => None
      case (_,None) => Some(CR1000InvalidIntegerFoundException(6, s"project number is not an Integer: filename: ${filename}"))
      case (_,_) => Some(CR1000InvalidProjectNumberException(2, s"File name or project number is wrong: filename: ${filename} and station Number: ${projectNumber}"))
    }
  }

  def validateDateFormat(date: String):Option[CR1000Exceptions] = {
      StringToDate.stringToDateConvertCR1000(date)
  }

  def validatePeriod(periode: Option[Int]) = {
    periode match {
      case None => Some(CR1000InvalidIntegerFoundException(6, s"periode is not an Integer"))
      case _ => None
    }
  }


}
