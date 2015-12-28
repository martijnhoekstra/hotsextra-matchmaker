package org.hotsextra.matchmaking
package webinterface
package servestrings

import org.http4s.headers.`Content-Type`
import org.http4s.MediaType
import org.http4s.EntityEncoder._

object Css {
  val site = """
  |#found {
  |  float:left;
  |  width:40%;
  |}
  |
  |#found ul {
  |  list-style-type: none;
  |}
  |
  |#waiting {
  |  float:right;
  |  width:40%;
  |}""".stripMargin

  def cssEncoder = stringEncoder.withContentType(`Content-Type`(MediaType.`text/css`))

}