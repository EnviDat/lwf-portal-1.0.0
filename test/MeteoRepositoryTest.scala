import javax.inject.Inject

import models.repositories.MeteoDataRepository
import java.sql.Connection

import models.services.{FileGenerator, MeteoService}
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.scalatest.junit.JUnitRunner
import org.scalatestplus.play.PlaySpec
import org.specs2.mock._
import play.api.db.Database
import play.api.mvc.RequestHeader

@RunWith(classOf[JUnitRunner])
class MeteoServiceSpec extends PlaySpec with Mockito {

  "Application" should {

    "index page" in {
      val connection = mock[Connection]
      val database = mock[Database]
      val repository = mock[MeteoDataRepository]
      val service = mock[FileGenerator]
      service.generateFiles()

    }
  }
}

