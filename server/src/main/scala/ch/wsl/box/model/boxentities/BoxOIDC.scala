package ch.wsl.box.model.boxentities


import ch.wsl.box.jdbc.PostgresProfile.api._
import ch.wsl.box.rest.runtime.Registry

object BoxOIDC {

  val profile = ch.wsl.box.jdbc.PostgresProfile
  private val schema = Some(Registry.box().schema)

  case class BoxOIDC_row(
                          state: String,
                          challenge: String,
                          verifier: String
                        )


  class BoxOIDC(_tableTag: Tag) extends profile.api.Table[BoxOIDC_row](_tableTag,schema, "oidc_codes") {
    def * = (state, challenge,verifier) <> (BoxOIDC_row.tupled, BoxOIDC_row.unapply)
    /** Maps whole row to an option. Useful for outer joins. */

    def state = column[String]("state", O.PrimaryKey)
    def challenge = column[String]("challenge")
    def verifier = column[String]("verifier")


  }
  /** Collection-like TableQuery object for table Conf  */
  lazy val BoxOIDCTable = new TableQuery(tag => new BoxOIDC(tag))
}
