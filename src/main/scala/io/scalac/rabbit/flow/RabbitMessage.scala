package io.scalac.rabbit.flow

import com.rabbitmq.client.Channel

object RabbitMessage {
  def apply(deliveryTag: Long, body: String, channel: Channel) = new RabbitMessage(deliveryTag, body, channel)
}

class RabbitMessage(val deliveryTag: Long, val body: String, channel: Channel) {

  def ack(): Unit = channel.basicAck(deliveryTag, false)
}