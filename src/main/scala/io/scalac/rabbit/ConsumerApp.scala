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
import akka.stream.scaladsl.Duct
import com.typesafe.scalalogging.slf4j.LazyLogging

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
object MyDomainProcessing extends LazyLogging {
  
  def apply() = Duct.apply[RabbitMessage]
  
    // extract message body
    .map(_.body)
    
    // filter out dangerous messages
    .filter(!_.contains("terror"))
    
    // do something time consuming - like go to sleep
    // then log the message text
    .map( msg => {
      Thread.sleep(2000)
      logger.info(msg)
      msg })
      
    // add the censorship mark
    .map(_ + "\nmessage processed")
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

    val domainProcessingDuct = MyDomainProcessing()
    
    val publisherDuct = new RabbitPublisher(RabbitBinding(OUTBOUND_EXCHANGE, OUTBOUND_QUEUE)).flow
    
    /*
     * the actual flow initialization
     */
    consumerFlow append domainProcessingDuct append publisherDuct consume(materializer)
  }

}