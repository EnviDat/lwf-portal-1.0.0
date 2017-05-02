package controllers

import javax.inject._

import models.domain.Station
import models.repositories.MeteoDataRepository
import models.services.MeteoService
import play.api.data.Form
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import play.api.mvc._
import play.api.routing._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(meteoService: MeteoService) extends Controller {

  import java.util.Properties

  val props: Properties = System.getProperties
  props
  .setProperty("oracle.jdbc.J2EE13Compliant", "true")

  import play.api.mvc._
  import play.api.routing._
  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.HomeController.listStations,
        routes.javascript.HomeController.index,
        routes.javascript.HomeController.listMeteoDataJson
      )
    ).as("text/javascript")
  }

  def index = Action {
    val allstations = meteoService.getAllStations
    val allMessArts = meteoService.getAllMessWerts
    val meteoDataFor14Days = meteoService.getAllMeteoData

    Ok(views.html.index("WSL", allstations, meteoDataFor14Days, allMessArts))
  }

  def bubbleChart = Action {
    Ok(views.html.dashboard("subheader.bubble", "", Seq(), Seq(), Seq()))
  }
  def liveChart = Action {
    Ok(views.html.dashboard("subheader.live", "", Seq(), Seq(), Seq()))
  }

  def createStation = TODO
  def listStations = Action {
    val allstations = meteoService.getAllStations
    Ok(views.html.dashboard.render("stations", "", allstations, Seq(), Seq()))
  }

  def listMeteoData = Action {
    val meteoDataFor14Days = meteoService.getAllMeteoData
    val allstations = meteoService.getAllStations
    Ok(views.html.dashboard.render("stations", "", allstations, meteoDataFor14Days, Seq()))

  }
  def listMeteoDataJson(id: Int) = Action {
    val meteoDataFor14Days = meteoService.getMeteoData(id)
    Ok(Json.toJson(meteoDataFor14Days))
  }

  def listLatestMeteoDataJson(stationId: Int, messartId: Int) = Action {
    val meteoLatestData = meteoService.getLatestMeteoData(stationId, messartId)
    Ok(Json.toJson(meteoLatestData))
  }
}

