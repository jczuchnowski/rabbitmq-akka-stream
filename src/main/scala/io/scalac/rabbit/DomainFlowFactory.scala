package io.scalac.rabbit

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import akka.actor.ActorRef
import akka.stream.scaladsl2.{FlowFrom, FlowMaterializer, ProcessorFlow}

import com.typesafe.scalalogging.slf4j.LazyLogging

import io.scalac.rabbit.flow.RabbitMessage
import io.scalac.rabbit.flow.RabbitPublisherActor.MessageToPublish
import io.scalac.rabbit.QueueRegistry._


/**
 * This is our flow factory.
 * 
 * Here we are only applying some simple filtering, logging and mapping, 
 * but the idea is that this part as the meat of your application.
 * 
 * Depending on your domain you could for example call some external services or actors here.
 */
object DomainFlowFactory extends LazyLogging {
    
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
  
  def domainProcessingflow()(implicit ex: ExecutionContext): ProcessorFlow[RabbitMessage, CensoredMessage] = 
    FlowFrom[RabbitMessage].
  
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
    map { CensorshipService.classify }
    
  def messageRoutingFlow(
      okPublisher: ActorRef, 
      nokPublisher: ActorRef)(implicit materializer: FlowMaterializer): ProcessorFlow[CensoredMessage, Unit] = 
    FlowFrom[CensoredMessage] groupBy { 
      case MessageSafe(msg) => OUT_OK_EXCHANGE
      case MessageThreat(msg) => OUT_NOK_EXCHANGE
    } map {
      case (exchange, producer) =>
        val flow = exchange match {
          case OUT_OK_EXCHANGE => producer map(_.message) map(okPublisher ! MessageToPublish(_))
          case OUT_NOK_EXCHANGE => producer map(_.message) map(nokPublisher ! MessageToPublish(_))
        } 
        flow consume()
    }
    
}