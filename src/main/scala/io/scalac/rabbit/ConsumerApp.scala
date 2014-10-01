package io.scalac.rabbit

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

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.Random

object QueueRegistry {

  val INBOUND_EXCHANGE = "censorship.inbound.exchange"
  val INBOUND_QUEUE = "censorship.inbound.queue"
    
  val OUT_OK_EXCHANGE = "censorship.ok.exchange"
  val OUT_OK_QUEUE = "censorship.ok.queue"
  
  val OUT_NOK_EXCHANGE = "censorship.nok.exchange"
  val OUT_NOK_QUEUE = "censorship.nok.queue"
  
  val IN_BINDING = RabbitBinding(INBOUND_EXCHANGE, INBOUND_QUEUE)
  val OUT_OK_BINDING = RabbitBinding(OUT_OK_EXCHANGE, OUT_OK_QUEUE)
  val OUT_NOK_BINDING = RabbitBinding(OUT_NOK_EXCHANGE, OUT_NOK_QUEUE)
}

/**
 * This is the message processing specific for a domain. Here we are only applying some 
 * simple filtering, logging and mapping, but the idea is that this part as the meat of your application.
 * 
 * Depending on your domain you could for example call some external services or actors here.
 */
object MyDomainProcessing extends LazyLogging {
  
  /**
   * Tuple assigning a RabbitMQ exchange name to a stream Producer.
   */
  type ExchangeMapping = (String, FlowWithSource[CensoredMessage, CensoredMessage])
  
  /*
   *  do something time consuming - like go to sleep
   *  then log the message text
   */
  def expensiveCall(msg: String)(implicit ec: ExecutionContext): Future[String] = Future {
    val millis = Random.nextInt(2000) + 1000
 		logger.info(s"message: '$msg' \n will be held for $millis ms")
    Thread.sleep(millis)
    msg
  }
  
  def apply()(implicit ex: ExecutionContext): ProcessorFlow[RabbitMessage, ExchangeMapping] = FlowFrom[RabbitMessage].
  
    // acknowledge and pass on
    map { msg =>
      msg.ack()
      msg
    }.
    
    // extract message body
    map { _.body.utf8String }.
    
    // do something time consuming
    mapFuture { expensiveCall }.

    // call domain service
    map { CensorshipService.classify }.
    
    // split by classification and assign an outbound exchange
    groupBy { 
      case MessageSafe(msg) => OUT_OK_EXCHANGE
      case MessageThreat(msg) => OUT_NOK_EXCHANGE
    }
}

object ConsumerApp extends App {

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
    
    println("connected to RabbitMQ")
    
    val rabbitConsumer = ActorPublisher[RabbitMessage](actorSystem.actorOf(RabbitConsumerActor.props(IN_BINDING)))
    
    val domainProcessingDuct = MyDomainProcessing()
    
    val okPublisherActor = actorSystem.actorOf(RabbitPublisherActor.props(OUT_OK_BINDING))
    val nokPublisherActor = actorSystem.actorOf(RabbitPublisherActor.props(OUT_NOK_BINDING))
    
    val publisherDuct: String => ProcessorFlow[String, Unit] = ex => ex match {
      case OUT_OK_EXCHANGE => FlowFrom[String].map(okPublisherActor ! MessageToPublish(_))
      case OUT_NOK_EXCHANGE => FlowFrom[String].map(nokPublisherActor ! MessageToPublish(_))
    }
    
    val messageSerializer = FlowFrom[CensoredMessage].map(_.message)

    val flow = FlowFrom(rabbitConsumer).append(domainProcessingDuct).map {
      case (exchange, producer) =>
        producer.map(_.message).append(publisherDuct(exchange)).consume()
    }
    
    flow.consume()
  }

}