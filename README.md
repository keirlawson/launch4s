# Launch4s

A Cats-friendly Scala wrapper around the LaunchDarkly [Java SDK](https://github.com/launchdarkly/java-server-sdk)

## Usage

The API largely mirrors the [Java SDK's API](http://launchdarkly.github.io/java-server-sdk/com/launchdarkly/sdk/server/package-summary.html), however there are small deviations in places.  For full details consult the [Scaladoc](#).

### Example

```scala
import launch4s.LaunchDarklyClient
import com.launchdarkly.sdk.LDUser

LaunchDarklyClient.resource[F]("sdkkey", blocker).use { ldClient =>
    val flagValue: F[Boolean] = ldClient.boolVariation("myflagkey", new LDUser(), false)
}
```