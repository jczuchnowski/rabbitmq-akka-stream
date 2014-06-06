package io.scalac.rabbit.flow

import com.rabbitmq.client.Channel

/**
 * Simple representation of RabbitMQ message. 
 * 
 * Encloses a channel to allow acknowledging the message later during processing.
 */
class RabbitMessage(val deliveryTag: Long, val body: String, channel: Channel) {

  def ack(): Unit = channel.basicAck(deliveryTag, false)
}

/**
 * Exchange and queue names.
 */
case class RabbitBinding(exchange: String, queue: String)

