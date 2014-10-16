package io.scalac.rabbit

import scala.concurrent.duration._

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.actor.ActorPublisher
import akka.stream.MaterializerSettings
import akka.stream.scaladsl2._
import akka.util.Timeout

import com.rabbitmq.client.Connection

import com.typesafe.scalalogging.slf4j.LazyLogging

import io.scalac.rabbit.flow._
import io.scalac.rabbit.flow.RabbitConnectionActor.Connect
import io.scalac.rabbit.flow.RabbitPublisherActor.MessageToPublish
import io.scalac.rabbit.QueueRegistry._

import java.net.InetSocketAddress


object ConsumerApp extends App with LazyLogging {

  implicit val timeout = Timeout(2 seconds)
  
  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
  
  implicit val executionContext = actorSystem.dispatcher
  
  implicit val materializer = FlowMaterializer(MaterializerSettings(actorSystem))
  
  val connectionActor = actorSystem.actorOf(
    RabbitConnectionActor.props(new InetSocketAddress("127.0.0.1", 5672))
  )
  
  /*
   * Ask for a connection and start processing.
   */
  (connectionActor ? Connect).mapTo[Connection] map { implicit conn =>
    
    logger.info("connected to RabbitMQ")
    
    val rabbitConsumer = ActorPublisher[RabbitMessage](actorSystem.actorOf(RabbitConsumerActor.props(IN_BINDING)))
    val okPublisherActor = actorSystem.actorOf(RabbitPublisherActor.props(OUT_OK_BINDING))
    val nokPublisherActor = actorSystem.actorOf(RabbitPublisherActor.props(OUT_NOK_BINDING))
    
    val domainProcessing = DomainFlowFactory.domainProcessingflow()
    val messageRouter = DomainFlowFactory.messageRoutingFlow(okPublisherActor, nokPublisherActor)
    
    val flow = FlowFrom(rabbitConsumer) append domainProcessing append messageRouter
    
    flow consume()
  }

}