package io.scalac.rabbit.flow

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import com.rabbitmq.client._
import RabbitConsumer._
import scala.collection.mutable.Queue
import scala.util.Try

object RabbitConsumer {
  
  def apply(queue: String)(implicit connection: Connection) = new RabbitConsumer(queue)
}

class RabbitConsumer(queue: String)(implicit connection: Connection) {

  val autoAck = true

  lazy val channel = initChannel()
  
  val processingQueue = Queue[RabbitMessage]()
  
  val consumer = new DefaultConsumer(channel) {
    override def handleDelivery(consumerTag: String, envelope: Envelope, properties: AMQP.BasicProperties, body: Array[Byte]) = {
      val msg = RabbitMessage(envelope.getDeliveryTag(), new String(body), channel)
      processingQueue += msg
    }
  }

  //TODO do something on failure
  lazy val flow: Flow[RabbitMessage] = Flow(() => Try(processingQueue.dequeue).getOrElse(null)).filter(_ != null)
 
  private def initChannel(): Channel =  {
    val ch = connection.createChannel()
    ch.queueDeclare(queue, true, false, false, null)
    ch.basicQos(2)
    ch.basicConsume(queue, autoAck, consumer)
    ch
  }
}