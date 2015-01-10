package actor.salt

import actor.ActorUtils
import akka.actor.{Cancellable, Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask
import scala.language.postfixOps

/**
 * Created by mind on 7/31/14.
 */

case class RefreshHosts(areaId: Int) // refresh host include refresh files
case class RefreshFiles(areaId: Int) // refresh file include refresh file server and returner

class RefreshSpiritsActor extends Actor with ActorLogging {
  import context._
  implicit val timeout = Timeout(60 seconds)

  var refreshSchedule: Cancellable = _

  override def preStart(): Unit = {
    refreshSchedule = context.system.scheduler.schedule(10 seconds, 3600 * 24 seconds, self, RefreshSpiritsHosts)
  }

  override def postStop(): Unit = {
    if (refreshSchedule != null) {
      refreshSchedule.cancel()
    }
  }

  override def receive = LoggingReceive {
    case RefreshSpiritsHosts => {
      val future = ActorUtils.spirits ? ConnectedSpirits
      val spiritIds = Await.result(future, timeout.duration).asInstanceOf[Seq[Int]]

      log.debug(s"Auto refresh host info, spiritIds: ${spiritIds}")
      spiritIds.foreach { spiritId =>
        self ! RefreshHosts(spiritId)
      }
    }

    case RefreshHosts(spiritId) => {
      log.debug(s"Refresh Hosts, begin refresh hosts: ${spiritId}")
      val rha = context.actorOf(Props(classOf[RefreshHostsActor], spiritId, sender))
      rha ! Run
    }

    case RefreshFiles(spiritId) => {
      val rfa = context.actorOf(Props(classOf[RefreshFilesActor], spiritId, sender))
      rfa ! Run
    }

    case x => log.debug(s"RefreshSpiritsActor receive unknown message ${x}")
  }
}

//case class Run()
case class Finish()
case class Error()

private case class RefreshSpiritsHosts()