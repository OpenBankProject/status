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

package bootstrap.liftweb


import java.io.{ File, FileInputStream}
import net.liftweb._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.http.js.jquery.JqJsCmds
import net.liftweb.json.JsonDSL._
import net.liftweb.mapper._
import net.liftweb.util._
import net.liftweb.util.Helpers._

import com.tesobe.status.api.Statues



/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable{
  def boot {

    val contextPath = LiftRules.context.path
    val propsPath = tryo{Box.legacyNullTest(System.getProperty("props.resource.dir"))}.toIterable.flatten

    logger.info("external props folder: " + propsPath)

    /**
     * Where this application looks for props files:
     *
     * All properties files follow the standard lift naming scheme for order of preference (see https://www.assembla.com/wiki/show/liftweb/Properties)
     * within a directory.
     *
     * The first choice of directory is $props.resource.dir/CONTEXT_PATH where $props.resource.dir is the java option set via -Dprops.resource.dir=...
     * The second choice of directory is $props.resource.dir
     *
     * For example, on a production system:
     *
     * api1.example.com with context path /api1
     *
     * Looks first in (outside of war file): $props.resource.dir/api1, following the normal lift naming rules (e.g. production.default.props)
     * Looks second in (outside of war file): $props.resource.dir, following the normal lift naming rules (e.g. production.default.props)
     * Looks third in the war file
     *
     * and
     *
     * api2.example.com with context path /api2
     *
     * Looks first in (outside of war file): $props.resource.dir/api2 , following the normal lift naming rules (e.g. production.default.props)
     * Looks second in (outside of war file): $props.resource.dir, following the normal lift naming rules (e.g. production.default.props)
     * Looks third in the war file, following the normal lift naming rules
     *
     */

    val firstChoicePropsDir = for {
      propsPath <- propsPath
    } yield {
      Props.toTry.map {
        f => {
          val name = propsPath + contextPath + f() + "props"
          name -> { () => tryo{new FileInputStream(new File(name))} }
        }
      }
    }

    val secondChoicePropsDir = for {
      propsPath <- propsPath
    } yield {
      Props.toTry.map {
        f => {
          val name = propsPath +  f() + "props"
          name -> { () => tryo{new FileInputStream(new File(name))} }
        }
      }
    }

    Props.whereToLook = () => {
      firstChoicePropsDir.flatten.toList ::: secondChoicePropsDir.flatten.toList
    }


    if (!DB.jndiJdbcConnAvailable_?) {
      val driver =
        Props.mode match {
          case Props.RunModes.Production | Props.RunModes.Staging | Props.RunModes.Development =>  Props.get("db.driver") openOr "org.h2.Driver"
          case _ => "org.h2.Driver"
        }
      val vendor =
        Props.mode match {
          case Props.RunModes.Production | Props.RunModes.Staging | Props.RunModes.Development =>
            new StandardDBVendor(driver,
               Props.get("db.url") openOr "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
               Props.get("db.user"), Props.get("db.password"))
          case _ =>
            new StandardDBVendor(
              driver,
              "jdbc:h2:mem:OBPTest;DB_CLOSE_DELAY=-1",
              Empty, Empty)
        }

      logger.debug("Using database driver: " + driver)
      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)

      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    val runningMode = Props.mode match {
      case Props.RunModes.Production => "Production mode"
      case Props.RunModes.Staging => "Staging mode"
      case Props.RunModes.Development => "Development mode"
      case Props.RunModes.Test => "test mode"
      case _ => "other mode"
    }

    logger.info("running mode: " + runningMode)

    // where to search snippet
    LiftRules.addToPackages("com.tesobe.status")


    LiftRules.statelessDispatchTable.append(Statues)
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))

    LiftRules.explicitlyParsedSuffixes = Helpers.knownSuffixes &~ (Set("com"))
    import Mailer._

    object MyExceptionLogger extends Loggable{

      def unapply(in: (Props.RunModes.Value, Req, Throwable)): Option[(Props.RunModes.Value, Req, Throwable)] = {
        val outputStream = new java.io.ByteArrayOutputStream
        val printStream = new java.io.PrintStream(outputStream)
        in._3.printStackTrace(printStream)
        val currentTime = now.toString
        val stackTrace = new String(outputStream.toByteArray)
        val error = currentTime + ": " + stackTrace
        val host = Props.get("hostname", "unknown host")

        val mailSent = for {
          from <- Props.get("mail.exception.sender.address") ?~ "Could not send mail: Missing props param for 'from'"
          // no spaces, comma separated e.g. mail.api.consumer.registered.notification.addresses=notify@example.com,notify2@example.com,notify3@example.com
          toAddressesString <- Props.get("mail.exception.registered.notification.addresses") ?~ "Could not send mail: Missing props param for 'to'"
        } yield {

          //technically doesn't work for all valid email addresses so this will mess up if someone tries to send emails to "foo,bar"@example.com
          val to = toAddressesString.split(",").toList
          val toParams = to.map(To(_))
          val params = PlainMailBodyType(error) :: toParams

          //this is an async call
          Mailer.sendMail(
            From(from),
            Subject(s"you got an exception on $host"),
            params :_*
          )
        }

        //if Mailer.sendMail wasn't called (note: this actually isn't checking if the mail failed to send as that is being done asynchronously)
        if(mailSent.isEmpty)
          logger.warn(s"Exception notification failed: $mailSent")

         None
      }
    }

    LiftRules.exceptionHandler.prepend{
      case MyExceptionLogger(_, _, t) => throw t // this will never happen
    }
  }
}
