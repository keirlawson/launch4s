package example

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
import com.launchdarkly.sdk.EvaluationDetail
import cats.Functor
import cats.Eval
import Instances._

trait LaunchDarklyClient[F[_]] {
  def boolVariation(featureKey: String, user: LDUser, defaultValue: Boolean): F[Boolean]
  def boolVariationDetail(featureKey: String, user: LDUser, defaultValue: Boolean): F[EvaluationDetail[Boolean]]
  def intVariation(featureKey: String, user: LDUser, defaultValue: Int): F[Int]
  def intVariationDetail(featureKey: String, user: LDUser, defaultValue: Int): F[EvaluationDetail[Int]]
  def doubleVariation(featureKey: String, user: LDUser, defaultValue: Double): F[Double]
  def doubleVariationDetail(featureKey: String, user: LDUser, defaultValue: Double): F[EvaluationDetail[Double]]
  def jsonValueVariation(featureKey: String, user: LDUser, defaultValue: LDValue): F[LDValue]
  def jsonValueVariationDetail(featureKey: String, user: LDUser, defaultValue: LDValue): F[EvaluationDetail[LDValue]]
  def stringVariation(featureKey: String, user: LDUser, defaultValue: String): F[String]
  def stringVariationDetail(featureKey: String, user: LDUser, defaultValue: String): F[EvaluationDetail[String]]

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
    def boolVariationDetail(featureKey: String, user: LDUser, defaultValue: Boolean): F[EvaluationDetail[Boolean]] = blocker.delay(javaClient.boolVariationDetail(featureKey, user, defaultValue).map(_.booleanValue()))
    
    def intVariation(featureKey: String, user: LDUser, defaultValue: Int): F[Int] = blocker.delay(javaClient.intVariation(featureKey, user, defaultValue))
    def intVariationDetail(featureKey: String, user: LDUser, defaultValue: Int): F[EvaluationDetail[Int]] = blocker.delay(javaClient.intVariationDetail(featureKey, user, defaultValue).map(_.intValue()))
    
    def doubleVariation(featureKey: String, user: LDUser, defaultValue: Double): F[Double] = blocker.delay(javaClient.doubleVariation(featureKey, user, defaultValue))
    def doubleVariationDetail(featureKey: String, user: LDUser, defaultValue: Double): F[EvaluationDetail[Double]] = blocker.delay(javaClient.doubleVariationDetail(featureKey, user, defaultValue).map(_.doubleValue()))
    
    def jsonValueVariation(featureKey: String, user: LDUser, defaultValue: LDValue): F[LDValue] = blocker.delay(javaClient.jsonValueVariation(featureKey, user, defaultValue))
    def jsonValueVariationDetail(featureKey: String, user: LDUser, defaultValue: LDValue): F[EvaluationDetail[LDValue]] = blocker.delay(javaClient.jsonValueVariationDetail(featureKey, user, defaultValue))
    
    def stringVariation(featureKey: String, user: LDUser, defaultValue: String): F[String] = blocker.delay(javaClient.stringVariation(featureKey, user, defaultValue))
    def stringVariationDetail(featureKey: String, user: LDUser, defaultValue: String): F[EvaluationDetail[String]] = blocker.delay(javaClient.stringVariationDetail(featureKey, user, defaultValue))
    

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