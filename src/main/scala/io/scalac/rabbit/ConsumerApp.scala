package io.scalac.rabbit

import akka.actor.ActorSystem
import java.net.InetSocketAddress
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import akka.stream.FlowMaterializer
import akka.stream.MaterializerSettings
import scala.util.Success
import scala.util.Failure

object ConsumerApp extends App {
  
  val factory = new ConnectionFactory
  val connection = factory.newConnection()
  
  implicit val system = ActorSystem("RabbitConsumer")
  val serverAddress = new InetSocketAddress("127.0.0.1", 5672)
    
  val client = new RabbitClient
  
  val channel = connection.createChannel()
  val materializer = FlowMaterializer(MaterializerSettings())
  val flow = client.consume("reactiveStreamTestExchange", "reactiveStreamTestQueue", channel)
  
  flow.foreach(msg => {
    channel.basicAck(msg.getEnvelope().getDeliveryTag(), false)
    println("from flow: " + new String(msg.getBody()))
    
    //simulate slow operation
    Thread.sleep(5000)
  }).onComplete(materializer) {
    case Success(_) => 
      println("Success")
      channel.close()
    case Failure(e) =>
      println("Failure: " + e.getMessage)
      channel.close()
  }
  
}