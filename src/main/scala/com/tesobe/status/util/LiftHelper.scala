/**
Open Bank Project - API
Copyright (C) 2011, 2014, TESOBE / Music Pictures Ltd

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE / Music Pictures Ltd
Osloerstrasse 16/17
Berlin 13359, Germany

  This product includes software developed at
  TESOBE (http://www.tesobe.com/)
  by
  Ayoub Benali: ayoub AT tesobe DOT com

 */

// imported from: https://github.com/fmpwizard/lift_starter_2.4/blob/la-futures-2/src/main/scala/com/fmpwizard/lib/FutureHelper.scala

package com.tesobe.status.util

import net.liftweb.actor._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js._
import net.liftweb.util.CanBind
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._


import xml.{Node, Elem, NodeSeq}

/**
 * This could be part of Lift
 * @param la the LAFuture holding the NodeSeq to update the UI
 */

case class FutureIsHere(la: LAFuture[NodeSeq], id: String) extends JsCmd with Loggable {

  logger.debug(id)

  val updatePage: JsCmd = if (la.isSatisfied) {
    Replace(id , la.get)
  } else {
    tryAgain()
  }

  private def tryAgain(): JsCmd = {
    val funcName: String = S.request.flatMap(_._params.toList.headOption.map(_._1)).openOr("")
    val retry = "setTimeout(function(){liftAjax.lift_ajaxHandler('%s=true', null, null, null)}, 3000)".format(funcName)
    JE.JsRaw(retry).cmd
  }

  override val toJsCmd = updatePage.toJsCmd
}

object LiftHelper extends Loggable {
  implicit def laFutureNSTransform: CanBind[LAFuture[NodeSeq]] = new CanBind[LAFuture[NodeSeq]] {
    def apply(future: => LAFuture[NodeSeq])(ns: NodeSeq): Seq[NodeSeq] = {
      val elem: Option[Elem] = ns match {
        case e: Elem => Some(e)
        case nodeSeq if nodeSeq.length == 1 && nodeSeq(0).isInstanceOf[Elem] => Box.asA[Elem](nodeSeq(0))
        case nodeSeq => None
      }

      val id: String = elem.map(_.attributes.filter(att => att.key == "id")).map{ meta =>
        tryo(meta.value.text).getOrElse( nextFuncName )
      } getOrElse{
        ""
      }

      val ret: Option[NodeSeq] = ns.toList match {
        case head :: tail => {
          elem.map{ e =>
            e % ("id" -> id) ++ tail ++ Script(OnLoad( SHtml.ajaxInvoke( () => FutureIsHere( future, id ) ).exp.cmd ))
          }
        }

        case empty => None
      }

      ret getOrElse NodeSeq.Empty
    }
  }
}