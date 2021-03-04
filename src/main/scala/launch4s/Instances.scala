package launch4s

import cats.Functor
import com.launchdarkly.sdk.EvaluationDetail

object Instances {
  implicit val evaluationDetailFunctorInstance: Functor[EvaluationDetail] = new Functor[EvaluationDetail] {
    def map[A, B](fa: EvaluationDetail[A])(f: A => B): EvaluationDetail[B] = {
        EvaluationDetail.fromValue(f(fa.getValue()), fa.getVariationIndex(), fa.getReason())
    }
  }
}