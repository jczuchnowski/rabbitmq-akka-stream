package io.scalac.rabbit.flow

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.rabbitmq.client._
import scala.util._
import RabbitConsumerFlow._

object RabbitConsumerPullFlow {
  
  def apply[T](queue: String, transform: RabbitProcessing[T])(implicit connection: Connection, 
    actorSystem: ActorSystem) = new RabbitConsumerPullFlow(queue, transform)
}

class RabbitConsumerPullFlow[T](
    queue: String, 
    processingFunc: RabbitProcessing[T])(implicit connection: Connection, actorSystem: ActorSystem) extends RabbitConsumerFlow {

  val channel = initChannel()
  
  val autoAck = false
  val basicGetFunc: () => GetResponse = () => channel.basicGet(queue, autoAck) 
  
  val materializer = FlowMaterializer(MaterializerSettings())
  
  def startProcessing() =
    processingFunc(
      Flow(basicGetFunc).
      filter(_ != null).
      map(msg => toRabbitMessage(msg, channel))
    ).consume(materializer)
  
  private def initChannel(): Channel =  {
    val ch = connection.createChannel()
    ch.queueDeclare(queue, true, false, false, null)
    ch
  }
  
  private def toRabbitMessage(msg: GetResponse, channel: Channel): RabbitMessage = 
    RabbitMessage(msg.getEnvelope().getDeliveryTag(), new String(msg.getBody(), "UTF-8"), channel)
}