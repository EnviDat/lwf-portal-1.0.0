import models.services.CR1000FileValidator
import org.scalatestplus.play._
import schedulers.{ParametersProject, StationKonfig}

class ProdBugSpec extends PlaySpec {

   //To Do: Write a test for testing the function validateProjectNumber of object CR1000FileValidator

    val inputFileName = "LAF_DeviceStatus"
    val statConfigs = List(StationKonfig( "ALP_DeviceStatus", 18, List(ParametersProject(1, 18, 10),ParametersProject(1, 3, 60),ParametersProject(4, 3, 10), ParametersProject(23, 3, 10))),
      StationKonfig( "LAF_DeviceStatus", 19, List(ParametersProject(1, 18, 10),ParametersProject(1, 3, 60),ParametersProject(4, 3, 10), ParametersProject(23, 3, 10)))
    )
    val lineToValidate = "2020-06-20 16:40:00" + ",20477,19,4,10,14.09,25.4,3.471"
    "project number in the file" should {
      "be correct" in {
        val mapStatKonfig = statConfigs.filter(sk => inputFileName.toUpperCase.startsWith(sk.fileName)).headOption

        val cr1000ValidatorValidateProjectNr =  CR1000FileValidator.validateProjectNumber(inputFileName, Some(4), mapStatKonfig, lineToValidate: String)
        assert(cr1000ValidatorValidateProjectNr.contains(4))
      }
    }


  }
}

