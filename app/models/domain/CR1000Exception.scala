package models.domain

import models.ozone._

sealed trait CR1000Exceptions {
  def formatErrorString(s: String): String = {
        s"${s}\n"
      }
}

case class CR1000InvalidProjectNumberException(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class CR1000InvalidStationNumberException(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class CR1000InvalidNumberOfParametersException(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class CR1000InvalidDateException(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class CR1000ConfigNotFoundException(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class CR1000InvalidIntegerFoundException(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class CR1000NotSufficientParameters(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class CR1000OracleError(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class CR1000FileError(errorCode: Int,errorMessage: String) extends CR1000Exceptions {
  override def toString() = formatErrorString(errorMessage)
}


case class CR1000ErrorFileInfo(fileName: String, errors : Seq[(Int, List[CR1000Exceptions])], linesToSave: List[String])

object FormatMessage {

  def formatCR1000ErrorMessage(errors: Seq[(Int, List[CR1000Exceptions])]) : String = {
    val groupByLine = errors.sortBy(_._1).groupBy(_._1)
     groupByLine.map(l => {
      s"line number: ${l._1}  errors: ${l._2.map(_._2.toString()).mkString("\n")}"
    }).mkString("\n")
  }

  def formatOzoneErrorMessage(errors: List[OzoneExceptions]) : String = {
    errors.map(er => {
      er match {
        case OzoneSuspiciousDataLineError(_,message) => "Suspicious lines:" + message
        case OzoneFileLevelInfoMissingError(_,_) => "File level parameter values are missing or malformed. Look at the file."
        case OzoneOracleError(_,_) => "Some exception from Oracle database. Consult DB responsible person"
        case OzoneNotSufficientParameters(_,errorMessage) => errorMessage
        case OzoneInvalidPlotException(_,_) => "Invalid Plot/inexisting plot/missing information in file."
        case _ => ""
      }

    }).mkString("\n")
   }
}