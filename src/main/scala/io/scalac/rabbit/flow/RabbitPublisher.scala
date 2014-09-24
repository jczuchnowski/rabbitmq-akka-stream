package io.scalac.rabbit.flow

import akka.stream.scaladsl2.{FlowFrom, ProcessorFlow}

import com.rabbitmq.client.{Channel, Connection}


/**
 * Wraps the action of publishing to a RabbitMQ channel and exposes it as a Flow processing.
 * 
 * This class will first initiate a new channel and declare a simple binding between an exchange and a queue.
 */
class RabbitPublisher(binding: RabbitBinding)(
    implicit connection: Connection) extends ChannelInitializer {

  val channel = initChannel(binding)
  
  val flow: ProcessorFlow[String, Unit] = 
    FlowFrom[String] map { 
      msg => channel.basicPublish(binding.exchange, "", null, msg.getBytes()) 
    }  
}