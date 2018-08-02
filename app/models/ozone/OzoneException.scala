package models.ozone

sealed trait OzoneExceptions {
  def formatErrorString(s: String): String = {
        s"${s}\n"
      }
}

case class OzoneInvalidProjectNumberException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneInvalidPlotException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
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
case class OzoneInvalidNumberFoundException(errorCode: Int,errorMessage: String) extends OzoneExceptions {
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

case class OzoneFileLevelInfoMissingError(errorCode: Int,errorMessage: String) extends OzoneExceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class OzoneErrorFileInfo(fileName: String, errors : List[OzoneExceptions])



