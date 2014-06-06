package io.scalac.rabbit.flow

import akka.stream.scaladsl.Flow
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection

/**
 * Wraps the action of publishing to a RabbitMQ channel and exposes it as a Flow processing.
 * 
 * This class will first initiate a new channel and declare a simple binding between an exchange and a queue.
 */
class RabbitPublisher(binding: RabbitBinding)(implicit connection: Connection) extends ChannelInitializer {

  val channel = initChannel(binding)
  
  val flow: Flow[String] => Flow[Unit] = 
    input => input foreach { 
      msg => channel.basicPublish(binding.exchange, "", null, msg.getBytes()) 
    }
  
}