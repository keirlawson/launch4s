package example

// decide package name
// Natchez module
// Publish for 2.12, 2.13
// scaladoc
// support getDataSourceStatusProvider(), getDataStoreStatusProvider()
// Circe JSON stuff to make LDValue s? - http://launchdarkly.github.io/java-server-sdk/com/launchdarkly/sdk/json/package-summary.html
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
import com.launchdarkly.sdk.server.FeatureFlagsState
import com.launchdarkly.sdk.server.FlagsStateOption
import com.launchdarkly.sdk.server.interfaces.DataSource
import com.launchdarkly.sdk.server.interfaces.DataSourceStatusProvider

trait LaunchDarklyClient[F[_]] {
  def boolVariation(featureKey: String, user: LDUser, defaultValue: Boolean): F[Boolean]
  def intVariation(featureKey: String, user: LDUser, defaultValue: Int): F[Int]
  def doubleVariation(featureKey: String, user: LDUser, defaultValue: Double): F[Double]
  def jsonValueVariation(featureKey: String, user: LDUser, defaultValue: LDValue): F[LDValue]
  def stringVariation(featureKey: String, user: LDUser, defaultValue: String): F[String]

  def identify(user: LDUser): F[Unit]

  def isFlagKnown(featureKey: String): F[Boolean]
  def isInitialized: F[Boolean]
  def isOffline: F[Boolean]

  def flush: F[Unit]

  def track(eventName: String, user: LDUser): F[Unit]
  def trackData(eventName: String, user: LDUser, data: LDValue): F[Unit]
  def trackMetric(eventName: String, user: LDUser, data: LDValue, metricValue: Double): F[Unit]

  def allFlagsState(user: LDUser, options: FlagsStateOption*): F[FeatureFlagsState]

  def secureModeHash(user: LDUser): F[String]

  def watchFlag(featureKey: String, user: LDUser, defaultValue: LDValue): F[Signal[F, LDValue]]
}

object LaunchDarklyClient {

  def make[F[_] : ConcurrentEffect : ContextShift](blocker: Blocker, javaClient: LDClient): LaunchDarklyClient[F] = new LaunchDarklyClient[F] {
    def boolVariation(featureKey: String, user: LDUser, defaultValue: Boolean): F[Boolean] = blocker.delay(javaClient.boolVariation(featureKey, user, defaultValue))
    def intVariation(featureKey: String, user: LDUser, defaultValue: Int): F[Int] = blocker.delay(javaClient.intVariation(featureKey, user, defaultValue))
    def doubleVariation(featureKey: String, user: LDUser, defaultValue: Double): F[Double] = blocker.delay(javaClient.doubleVariation(featureKey, user, defaultValue))
    def jsonValueVariation(featureKey: String, user: LDUser, defaultValue: LDValue): F[LDValue] = blocker.delay(javaClient.jsonValueVariation(featureKey, user, defaultValue))
    def stringVariation(featureKey: String, user: LDUser, defaultValue: String): F[String] = blocker.delay(javaClient.stringVariation(featureKey, user, defaultValue))
    

    def identify(user: LDUser): F[Unit] = blocker.delay(javaClient.identify(user))

    def isFlagKnown(featureKey: String): F[Boolean] = blocker.delay(javaClient.isFlagKnown(featureKey))
    def isInitialized: F[Boolean] = blocker.delay(javaClient.isInitialized())
    def isOffline: F[Boolean] = blocker.delay(javaClient.isOffline())

    def flush: F[Unit] = blocker.delay(javaClient.flush())

    def track(eventName: String, user: LDUser): F[Unit] = blocker.delay(javaClient.track(eventName, user))
    def trackData(eventName: String, user: LDUser, data: LDValue): F[Unit] = blocker.delay(javaClient.trackData(eventName, user, data))
    def trackMetric(eventName: String, user: LDUser, data: LDValue, metricValue: Double): F[Unit] = blocker.delay(javaClient.trackMetric(eventName, user, data, metricValue))
    
    def allFlagsState(user: LDUser, options: FlagsStateOption*): F[FeatureFlagsState] = blocker.delay(javaClient.allFlagsState(user, options:_*))

    def secureModeHash(user: LDUser): F[String] = blocker.delay(javaClient.secureModeHash(user))

    //*variationDetail

    def getDataSourceStatusProvider: F[DataSourceStatusProvider] = blocker.delay(javaClient.getDataSourceStatusProvider())

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

  def resource[F[_]: ConcurrentEffect : ContextShift](sdkKey: String, blocker: Blocker): Resource[F, LaunchDarklyClient[F]] = {
    Resource.fromAutoCloseableBlocking[F, LDClient](blocker)(Sync[F].delay(new LDClient(sdkKey))).map(client => make(blocker, client))
  }

  def resource[F[_]: ConcurrentEffect : ContextShift](sdkKey: String, config: LDConfig, blocker: Blocker): Resource[F, LaunchDarklyClient[F]] = {
    Resource.fromAutoCloseableBlocking[F, LDClient](blocker)(Sync[F].delay(new LDClient(sdkKey, config))).map(client => make(blocker, client))
  }
}