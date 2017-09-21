package models.domain

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



case class CR1000ErrorFileInfo(fileName: String, errors : Seq[(Int, List[CR1000Exceptions])], linesToSave: List[String])

object FormatMessage {

  def formatErrorMessage(errors: Seq[(Int, List[CR1000Exceptions])]) : String = {
    val groupByLine = errors.sortBy(_._1).groupBy(_._1)
     groupByLine.map(l => {
      s"line number: ${l._1}  errors: ${l._2.map(_._2.toString()).mkString("\n")}"
    }).mkString("\n")
  }
}