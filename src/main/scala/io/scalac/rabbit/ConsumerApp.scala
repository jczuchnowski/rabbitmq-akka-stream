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
import QueueRegistry._
import scala.concurrent.duration._
import scala.language.postfixOps

object QueueRegistry {

  val INBOUND_EXCHANGE = "rabitAkkaStreamInboundExchange"
  val INBOUND_QUEUE = "rabbitAkkaStreamInboundQueue"
    
  val OUTBOUND_EXCHANGE = "rabitAkkaStreamOutboundExchange"
  val OUTBOUND_QUEUE = "rabbitAkkaStreamOutboundQueue"
  
}

/**
 * This is the message processing specific for a domain. Here we are only applying some 
 * simple filtering, logging and mapping, but the idea is that this part as the meat of your application.
 * 
 * Depending on your domain you could for example call some external services or actors here.
 */
object MyDomainProcessing extends Function1[Flow[RabbitMessage], Flow[String]] {
  
  def apply(input: Flow[RabbitMessage]) = input
    .map(_.body)                     // extract message body
    .filter(!_.contains("terror"))   // filter out dangerous messages 
    .map( msg => {
      Thread.sleep(2000)             // do something time consuming - like go to sleep
      println(msg)                   // then echo the message text
      msg })
    .map(_ + "\nmessage processed")  // add the censorship mark
}

object ConsumerApp extends App {

  implicit val timeout = Timeout(2 seconds)
  
  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
  
  implicit val executor = actorSystem.dispatcher
  
  val materializer = FlowMaterializer(MaterializerSettings())
  
  val connectionActor = actorSystem.actorOf(
    RabbitConnectionActor.props(new InetSocketAddress("127.0.0.1", 5672))
  )
  
  /*
   * Ask for a connection and start processing.
   */
  (connectionActor ? Connect).mapTo[Connection] map { implicit conn =>
    
    val consumerFlow = new RabbitConsumer(RabbitBinding(INBOUND_EXCHANGE, INBOUND_QUEUE)).flow

    val publisherFlow = new RabbitPublisher(RabbitBinding(OUTBOUND_EXCHANGE, OUTBOUND_QUEUE)).flow
    
    /*
     * the actual flow initialization
     */
    (MyDomainProcessing andThen publisherFlow)(consumerFlow).consume(materializer)
  }

}