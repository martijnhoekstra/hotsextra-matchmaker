package org.hotsextra.matchmaking
package webinterface
package servestrings

import org.http4s.headers.`Content-Type`
import org.http4s.MediaType
import org.http4s.EntityEncoder._

object Css {
  val site = """
  |body {
  |  color: #666;
  |  font-family: "Open Sans", sans-serif;
  |  font-size: 14px;
  |  line-height: 1.75em;
  |}
  |
  |table {
  |  text-align: right;
  |}
  |
  |th {
  |  text-align: center;  
  |}
  |
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