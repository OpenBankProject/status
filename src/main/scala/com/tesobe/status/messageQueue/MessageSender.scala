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
package com.tesobe.status.messageQueue

import net.liftweb.util._
import net.liftweb.common.Loggable
import net.liftmodules.amqp.AMQPSender
import com.rabbitmq.client.{ConnectionFactory, Channel}

import com.tesobe.status.model.GetBanksStatues

// allows the application to write messages in the message queue
object MessageSender extends Loggable{

  val factory = new ConnectionFactory {
    import ConnectionFactory._
    setHost(Props.get("connection.host", "localhost"))
    setPort(DEFAULT_AMQP_PORT)
    setUsername(Props.get("connection.user", DEFAULT_USER))
    setPassword(Props.get("connection.password", DEFAULT_PASS))
    setVirtualHost(DEFAULT_VHOST)
  }

  val amqp = new SatutesRequestSender(factory, "getStatues", "statuesRequest")

  def getStatues = {
    import net.liftmodules.amqp.AMQPMessage
    logger.info(s"sending message to get status")
    val m = GetBanksStatues()
    amqp ! AMQPMessage(m)
  }
}

class SatutesRequestSender(cf: ConnectionFactory, exchange: String, routingKey: String)
 extends AMQPSender[GetBanksStatues](cf, exchange, routingKey) {
  override def configure(channel: Channel) = {
    val conn = cf.newConnection()
    val channel = conn.createChannel()
    channel
  }
}
