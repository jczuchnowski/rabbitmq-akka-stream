package io.scalac.rabbit

import akka.actor.ActorSystem
import com.rabbitmq.client._
import io.scalac.rabbit.flow._
import RabbitConsumerFlow._
import java.net.InetSocketAddress
import scala.util._

  
object ConsumerApp extends App {
  
  val QUEUE = "reactiveStreamTestQueue"
  val EXCHANGE = "reactiveStreamTestExchange"
    
  val factory = new ConnectionFactory
  implicit val connection = factory.newConnection()
  
  implicit val system = ActorSystem("RabbitConsumer")
  val serverAddress = new InetSocketAddress("127.0.0.1", 5672)
    
  //initialize exchange, queues and bindings
  configureRabbit(connection)
  
  val flow1 = RabbitConsumerPullFlow(
    QUEUE, 
    _.foreach(msg => {
      msg.ack()
      println("flow1: " + msg.body)

      //slow down a little
      Thread.sleep(5000)
    })
  )

  val flow2 = RabbitConsumerPushFlow(
    QUEUE, 
    _.foreach(msg => {
      msg.ack()
      println("flow2: " + msg.body)

      //slow down a little
      Thread.sleep(2000)
    })
  )
  
  //run the flow
  flow1.startProcessing()

  flow2.startProcessing()

  def configureRabbit(conn: Connection): Unit = {
    val channel = conn.createChannel()
    channel.exchangeDeclare(EXCHANGE, "direct")
    channel.queueDeclare(QUEUE, true, false, false, null)
    channel.queueBind(QUEUE, EXCHANGE, "")
    channel.close()
  }
}