package io.scalac.rabbit.flow

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import com.rabbitmq.client._
import scala.collection.mutable.Queue
import scala.util.Try

/**
 * Wraps a RabbitMQ consumer and exposes it as a Flow.
 * 
 * This class will first initiate a new channel and declare a simple binding between an exchange and a queue.
 */
class RabbitConsumer(binding: RabbitBinding)(implicit connection: Connection) extends ChannelInitializer {

  val autoAck = true

  val channel = initChannel(binding)
  
  val processingQueue = Queue[RabbitMessage]()
  
  val consumer = new DefaultConsumer(channel) {
    override def handleDelivery(
        consumerTag: String, 
        envelope: Envelope, 
        properties: AMQP.BasicProperties, 
        body: Array[Byte]) = {
      processingQueue += new RabbitMessage(envelope.getDeliveryTag(), new String(body, "UTF-8"), channel)
    }
  }

  val flow: Flow[RabbitMessage] = {
    register(channel, consumer)
    
    val flow = Flow(() => Try(processingQueue.dequeue))
    
    flow filter { msgTry => msgTry.isSuccess } map { msgTry => msgTry.get }
  }
  
  private def register(ch: Channel, consumer: Consumer): Unit =  {
    ch.basicQos(2)
    ch.basicConsume(binding.queue, autoAck, consumer)
  }
}