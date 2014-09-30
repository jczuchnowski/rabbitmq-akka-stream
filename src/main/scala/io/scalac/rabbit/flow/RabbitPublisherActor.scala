package io.scalac.rabbit.flow

import akka.actor.{Actor, Props}

import com.rabbitmq.client.{Channel, Connection}


object RabbitPublisherActor {
  
  case class MessageToPublish(msg: String)
  
  def props(binding: RabbitBinding)(implicit connection: Connection) = Props(new RabbitPublisherActor(binding))
}
/**
 * Wraps the action of publishing to a RabbitMQ channel and exposes it as a Flow processing.
 * 
 * This class will first initiate a new channel and declare a simple binding between an exchange and a queue.
 */
class RabbitPublisherActor(binding: RabbitBinding)(
    implicit connection: Connection) extends Actor with ChannelInitializer {

  import RabbitPublisherActor._
  
  val channel = initChannel(binding)
  
  override def receive = {
    case MessageToPublish(msg: String) =>
      channel.basicPublish(binding.exchange, "", null, msg.getBytes())
  }  
}