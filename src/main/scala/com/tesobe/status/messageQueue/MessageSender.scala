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

import com.tesobe.status.model.{GetBanksStatues, GetSupportedBanks}

// allows the application to write messages in the message queue
object MessageSender extends Loggable{
  import net.liftmodules.amqp.AMQPMessage

  val amqp = new MessageSender[GetBanksStatues](MQConnection.factory, "getStatues", "statuesRequest")
  val amqp2 = new MessageSender[GetSupportedBanks](MQConnection.factory, "getBanks", "banksRequest")

  def getStatues = {

    logger.info(s"sending message to get status")
    val m = GetBanksStatues()
    amqp ! AMQPMessage(m)
  }

  def getBankList = {
    logger.info(s"sending message to banks list")
    val m = GetSupportedBanks()
    amqp2 ! AMQPMessage(m)
  }
}

class MessageSender[T](cf: ConnectionFactory, exchange: String, routingKey: String)
 extends AMQPSender[T](cf, exchange, routingKey) {
  override def configure(channel: Channel) = {
    val conn = cf.newConnection()
    val channel = conn.createChannel()
    channel
  }
}
