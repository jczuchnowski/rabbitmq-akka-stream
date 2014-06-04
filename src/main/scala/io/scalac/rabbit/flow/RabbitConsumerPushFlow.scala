package io.scalac.rabbit.flow

import akka.actor._
import akka.stream._
import akka.stream.scaladsl._
import com.rabbitmq.client._
import scala.util._
import RabbitConsumerFlow._
import scala.collection.mutable.Queue

object RabbitConsumerPushFlow {
  def apply[T](queue: String, transform: RabbitProcessing[T])(implicit connection: Connection, 
    actorSystem: ActorSystem) = new RabbitConsumerPushFlow(queue, transform)
}

class RabbitConsumerPushFlow[T](
    queue: String, 
    processingFunc: RabbitProcessing[T])(implicit connection: Connection, actorSystem: ActorSystem) extends RabbitConsumerFlow {

  val autoAck = false

  val channel = initChannel()
  
  val materializer = FlowMaterializer(MaterializerSettings())
  
  val processingQueue = Queue[RabbitMessage]()
  
  val consumer = new DefaultConsumer(channel) {
    override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
      val msg = RabbitMessage(envelope.getDeliveryTag(), new String(body), channel)
      processingQueue += msg
    }
  }
  
  def startProcessing() = {
    channel.basicConsume(queue, autoAck, consumer)
    processingFunc(
      Flow(() => Try(processingQueue.dequeue).getOrElse(null)).
      filter(_ != null)
    ).consume(materializer)
  }
  
  private def initChannel(): Channel =  {
    val ch = connection.createChannel()
    ch.queueDeclare(queue, true, false, false, null)
    ch.basicQos(2)
    ch
  }
}