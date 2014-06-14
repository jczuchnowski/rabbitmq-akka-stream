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

  val INBOUND_EXCHANGE = "censorship.inbound.exchange"
  val INBOUND_QUEUE = "censorship.inbound.queue"
    
  val OUT_OK_EXCHANGE = "censorship.ok.exchange"
  val OUT_OK_QUEUE = "censorship.ok.queue"
  
  val OUT_NOK_EXCHANGE = "censorship.nok.exchange"
  val OUT_NOK_QUEUE = "censorship.nok.queue"
  
}

/**
 * This is the message processing specific for a domain. Here we are only applying some 
 * simple filtering, logging and mapping, but the idea is that this part as the meat of your application.
 * 
 * Depending on your domain you could for example call some external services or actors here.
 */
object MyDomainProcessing extends LazyLogging {
  
  def apply() = Duct[RabbitMessage].
  
    // extract message body
    map(_.body).
    
    // do something time consuming - like go to sleep
    // then log the message text
    map( msg => {
      Thread.sleep(2000)
      logger.info(msg)
      msg }).

    // classify
    map(CensorshipService.classify(_)).
    
    // split by classification and assign an outbound exchange
    groupBy { 
      case MessageSafe(msg) => OUT_OK_EXCHANGE
      case MessageThreat(msg) => OUT_NOK_EXCHANGE
    }
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
    
    val okPublisherDuct = new RabbitPublisher(RabbitBinding(OUT_OK_EXCHANGE, OUT_OK_QUEUE)).flow
    val nokPublisherDuct = new RabbitPublisher(RabbitBinding(OUT_NOK_EXCHANGE, OUT_NOK_QUEUE)).flow
    
    val publisherDuct: String => Duct[String, Unit] = ex => ex match {
      case OUT_OK_EXCHANGE => okPublisherDuct
      case OUT_NOK_EXCHANGE => nokPublisherDuct
    }
    
    /*
     * connect the flows with ducts and consume
     */
    consumerFlow append domainProcessingDuct map { 
      case (exchange, producer) => 
        
        // start a new flow for each message type
        Flow(producer)
        
          // extract the message
          .map(_.message) 
          
          // add the outbound publishing duct
          .append(publisherDuct(exchange))
          
          // and start the flow
          .consume(materializer)
        
    } consume(materializer)
  }

}