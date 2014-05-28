package io.scalac.rabbit

import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.Connection
import akka.stream.scaladsl.Flow
import akka.stream.FlowMaterializer
import akka.stream.MaterializerSettings
import akka.actor.ActorSystem
import java.net.InetSocketAddress
import akka.util.Timeout
import scala.concurrent.duration._
import akka.io.IO
import akka.stream.io.StreamTcp
import akka.pattern.ask
import akka.util.ByteString
import scala.util.Success
import scala.util.Failure
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.GetResponse
import com.rabbitmq.client.Channel

class RabbitClient {
  
  def consume(exchange: String, queue: String, channel: Channel)(implicit system: ActorSystem): Flow[GetResponse] = {
	    
    channel.exchangeDeclare(exchange, "direct")
    channel.queueDeclare(queue, true, false, false, null)
    channel.queueBind(queue, exchange, "")
    
    val autoAck = false
    val func = () => channel.basicGet(queue, autoAck) 
    
    Flow( func ).filter(_ != null)
  }

}