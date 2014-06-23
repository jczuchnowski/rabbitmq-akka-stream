package io.scalac.rabbit.flow

import com.rabbitmq.client.Channel
import com.typesafe.scalalogging.Logger
import com.typesafe.scalalogging.slf4j.LazyLogging
import akka.util.ByteString

/**
 * Simple representation of RabbitMQ message. 
 * 
 * Encloses a channel to allow acknowledging or rejecting the message later during processing.
 */
class RabbitMessage(val deliveryTag: Long, val body: ByteString, channel: Channel) extends LazyLogging {

  /**
   * Ackowledge the message.
   */
  def ack(): Unit = {
    logger.debug(s"ack $deliveryTag")
    channel.basicAck(deliveryTag, false)
  }
  
  /**
   * Reject and requeue the message.
   */
  def nack(): Unit = {
    logger.debug(s"nack $deliveryTag")
    channel.basicNack(deliveryTag, false, true)
  }
}

/**
 * Exchange and queue names.
 */
case class RabbitBinding(exchange: String, queue: String)

