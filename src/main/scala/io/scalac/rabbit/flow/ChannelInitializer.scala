package io.scalac.rabbit.flow

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import scala.collection.JavaConversions._

/**
 * Utility trait exposing the logic to initiate new channel and bindings.
 */
trait ChannelInitializer {
  
  def initChannel(binding: RabbitBinding)(implicit connection: Connection): Channel = {
    val ch = connection.createChannel()
    ch.exchangeDeclare(binding.exchange, "direct", true)
    ch.queueDeclare(binding.queue, true, false, false, Map[String, java.lang.Object]())
    ch.queueBind(binding.queue, binding.exchange, "")
    ch
  }
}