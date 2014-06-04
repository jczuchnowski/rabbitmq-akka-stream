package io.scalac.rabbit.flow

import akka.actor._
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.net.InetSocketAddress

object RabbitConnectionActor {

  case class Connect
  
  def props(address: InetSocketAddress) = Props(new RabbitConnectionActor(address))
}

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
    case msg => log.error(s"Received unknown message $msg")
  }
  
  override def postStop() = {
    connections.foreach(_.close())
  }
}