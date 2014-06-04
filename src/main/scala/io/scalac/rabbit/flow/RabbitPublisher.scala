package io.scalac.rabbit.flow

import akka.stream.scaladsl.Flow
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel

case class RabbitPublisher(exchange: String)(implicit connection: Connection) {

  lazy val channel = initChannel()
  
  lazy val flow: Flow[String] => Flow[Unit] = in => in.foreach(msg => channel.basicPublish(exchange, "", null, msg.getBytes()))
  
  private def initChannel(): Channel =  {
    val ch = connection.createChannel()
    ch.exchangeDeclare(exchange, "direct")
    ch
  }
}