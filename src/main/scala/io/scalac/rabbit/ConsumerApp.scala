package io.scalac.rabbit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.scaladsl.Flow
import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.util.Timeout
import com.rabbitmq.client.Connection
import io.scalac.rabbit.flow._
import io.scalac.rabbit.flow.RabbitConnectionActor.Connect
import java.net.InetSocketAddress
import scala.concurrent.duration._
import scala.language.postfixOps

class ConsumerApp2 extends App {

  val INBOUND_EXCHANGE = "rabitAkkaStreamInboundExchange"
  val INBOUND_QUEUE = "rabbitAkkaStreamInboundQueue"
    
  val OUTBOUND_EXCHANGE = "rabitAkkaStreamOutboundExchange"
  val OUTBOUND_QUEUE = "rabbitAkkaStreamOutboundQueue"
    
  implicit val timeout = Timeout(2 seconds)
  
  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
  
  implicit val executor = actorSystem.dispatcher
  
  val serverAddress = new InetSocketAddress("127.0.0.1", 5672)

  val connectionActor = actorSystem.actorOf(RabbitConnectionActor.props(serverAddress))
  
  val censorshipDomainProcessing = (in: Flow[RabbitMessage]) => 
    in.map(_.body).filter(!_.contains("terror")).map( msg => {
      Thread.sleep(2000)
      println(msg)
      msg}).map(_ + "\nmessage processed") 
  
  (connectionActor ? Connect).mapTo[Connection] map { conn =>
    
    implicit val connection = conn
    
    val consumerFlow = RabbitConsumer(INBOUND_QUEUE).flow
    
    val publisherFlow = RabbitPublisher(OUTBOUND_EXCHANGE).flow
    
    publisherFlow(censorshipDomainProcessing(consumerFlow)).consume(FlowMaterializer(MaterializerSettings()))
  }

  //TODO use this
  def configureRabbit(conn: Connection): Unit = {
    val channel = conn.createChannel()
    channel.exchangeDeclare(INBOUND_EXCHANGE, "direct")
    channel.queueDeclare(INBOUND_QUEUE, true, false, false, null)
    channel.queueBind(INBOUND_QUEUE, INBOUND_EXCHANGE, "")

    channel.exchangeDeclare(OUTBOUND_EXCHANGE, "direct")
    channel.queueDeclare(OUTBOUND_QUEUE, true, false, false, null)
    channel.queueBind(OUTBOUND_QUEUE, OUTBOUND_EXCHANGE, "")

    channel.close()
  }
}