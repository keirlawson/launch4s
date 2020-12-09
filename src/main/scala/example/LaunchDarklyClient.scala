package example

// decide package name
// Natchez module
// Use trait for full final tagless
// Publish for 2.12, 2.13
//Client should be newed with a blocker, same with close of resource
//care about covering https://docs.launchdarkly.com/sdk/server-side/java#variation variation -> identify

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Effect, IO, Resource, Sync}
import com.launchdarkly.sdk.{LDUser, LDValue}
import com.launchdarkly.sdk.server.{LDClient, LDConfig}
import fs2.concurrent.{Signal, SignallingRef}
import cats.implicits._
import cats.effect.implicits._
import com.launchdarkly.sdk.server.interfaces.{FlagValueChangeEvent, FlagValueChangeListener}
import cats.effect.Concurrent

case class LaunchDarklyClient[F[_] : ConcurrentEffect : ContextShift](blocker: Blocker, javaClient: LDClient) {
  def boolVariation(featureKey: String, user: LDUser, defaultValue: Boolean): F[Boolean] = blocker.delay(javaClient.boolVariation(featureKey, user, defaultValue))
  def intVariation(featureKey: String, user: LDUser, defaultValue: Int): F[Int] = blocker.delay(javaClient.intVariation(featureKey, user, defaultValue))
  def doubleVariation(featureKey: String, user: LDUser, defaultValue: Double): F[Double] = blocker.delay(javaClient.doubleVariation(featureKey, user, defaultValue))
  def jsonValueVariation(featureKey: String, user: LDUser, defaultValue: LDValue): F[LDValue] = blocker.delay(javaClient.jsonValueVariation(featureKey, user, defaultValue))

  //stringvariation
  //*variationDetail
  //allFlagsState
  //flush
  //	getDataSourceStatusProvider()
  //getDataStoreStatusProvider()

  // getFlagTracker() - probably watchFlag
  //identify(LDUser user)
  //isFlagKnown(java.lang.String featureKey)
  //isInitialized()
  //isOffline()
  //secureModeHash(LDUser user)
  //track(java.lang.String eventName, LDUser user)
  //trackData(java.lang.String eventName, LDUser user, LDValue data)
  //trackMetric(java.lang.String eventName, LDUser user, LDValue data, double metricValue)

  def watchFlag(featureKey: String, user: LDUser, defaultValue: LDValue): F[Signal[F, LDValue]] = {
    jsonValueVariation(featureKey, user, defaultValue).flatMap { initial =>
      SignallingRef[F, LDValue](initial).flatTap { ref =>
        Sync[F].delay {
          val tracker = javaClient.getFlagTracker
          val listener: FlagValueChangeListener = (change: FlagValueChangeEvent) => ref.set(change.getNewValue).toIO.unsafeRunSync()
          tracker.addFlagValueChangeListener(featureKey, user, listener)
        }
      }.widen
    }
  }

}

object LaunchDarklyClient {
  def resource[F[_]: ConcurrentEffect : ContextShift](sdkKey: String, blocker: Blocker): Resource[F, LaunchDarklyClient[F]] = {
    Resource.fromAutoCloseableBlocking[F, LDClient](blocker)(Sync[F].delay(new LDClient(sdkKey))).map(client => LaunchDarklyClient(blocker, client))
  }

  def resource[F[_]: ConcurrentEffect : ContextShift](sdkKey: String, config: LDConfig, blocker: Blocker): Resource[F, LaunchDarklyClient[F]] = {
    Resource.fromAutoCloseableBlocking[F, LDClient](blocker)(Sync[F].delay(new LDClient(sdkKey, config))).map(client => LaunchDarklyClient(blocker, client))
  }
}