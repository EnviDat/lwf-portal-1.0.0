package models.ozone

sealed trait OzoneExceptions {
  def formatErrorString(s: String): String = {
        s"${s}\n"
      }
}

case class OzoneInvalidProjectNumberException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneInvalidStationNumberException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneInvalidNumberOfParametersException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneInvalidDateException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneConfigNotFoundException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneInvalidIntegerFoundException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneNotSufficientParameters(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class OzoneOracleError(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class OzoneFileError(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}


case class OzoneErrorFileInfo(fileName: String, errors : Seq[(Int, List[OzoneExceptions])], linesToSave: List[String])
