package io.scalac.rabbit.flow

import akka.actor._

import com.rabbitmq.client.{Connection, ConnectionFactory}

import java.net.InetSocketAddress


object RabbitConnectionActor {

  case object Connect
  
  def props(address: InetSocketAddress) = Props(new RabbitConnectionActor(address))
}

/**
 * Manager of RabbitMQ connections.
 * 
 * Initiates a new connection on a Connect message and returns it to the sender.
 * Takes care of closing the connections on system close. 
 */
class RabbitConnectionActor(address: InetSocketAddress) extends Actor with ActorLogging {

  import RabbitConnectionActor._
  
  val factory = new ConnectionFactory()
  factory.setHost(address.getHostName())
  factory.setPort(address.getPort())
  
  var connections: List[Connection] = Nil
  
  def receive = {
    case Connect => 
      val client = sender()
      val conn = factory.newConnection()
      connections = conn :: connections
      client ! conn
      log.info(s"Connected to RabbitMQ server on $address")
    case msg => log.error(s"Received unknown message $msg")
  }
  
  override def postStop() =
    connections foreach { conn => 
      log.info("Closing connection")
      conn.close() 
    }
}