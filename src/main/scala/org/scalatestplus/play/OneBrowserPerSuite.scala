/*
 * Copyright 2001-2014 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalatestplus.play

import play.api.test._
import org.scalatest._
import selenium.WebBrowser
import concurrent.Eventually
import concurrent.IntegrationPatience
import org.openqa.selenium.WebDriver
import BrowserFactory.NoDriver
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.chrome.ChromeDriver

/**
 * Trait that provides a new `FakeApplication`, running `TestServer`, and Selenium `WebBrowser` instance per ScalaTest `Suite`.
 * 
 * By default, this trait creates a new `FakeApplication` for the `Suite` using default parameter values, which
 * is made available via the `app` field defined in this trait and a new `TestServer` for the `Suite` using the port number provided by
 * its `port` field and the `FakeApplication` provided by its `app` field. If your `Suite` needs a
 * `FakeApplication` with non-default parameters, override `app`. If it needs a different port number,
 * override `port`.
 *
 * This `SuiteMixin` trait's overridden `run` method calls `start` on the `TestServer`
 * before executing the `Suite` via a call to `super.run`.
 * In addition, it places a reference to the `FakeApplication` provided by `app` into the `ConfigMap`
 * under the key `org.scalatestplus.play.app`, the port number provided by `port` under the key
 * `org.scalatestplus.play.port`, and the `WebDriver` provided by `webDriver` under the key `org.scalatestplus.play.webDriver`.
 * This allows any nested `Suite`s to access the `Suite`'s 
 * `FakeApplication`, port number, and `WebDriver` as well, most easily by having the nested `Suite`s mix in the
 * [[org.scalatestplus.play.ConfiguredServer ConfiguredServer]] trait. On the status returned by `super.run`, this
 * trait's overridden `run` method registers a call to `stop` on the `TestServer` to be executed when the `Status`
 * completes, and returns the same `Status`. This ensure the `TestServer` will continue to execute until
 * all nested suites have completed, after which the `TestServer` will be stopped.
 * This trait also overrides `Suite.withFixture` to cancel tests automatically if the related
 * `WebDriver` is not available on the host platform.
 *
 * Here's an example that shows demonstrates of the services provided by this trait:
 *
 * <pre class="stHighlight">
 * package org.scalatestplus.play.examples.onebrowserpersuite
 * 
 * import play.api.test._
 * import org.scalatest._
 * import org.scalatestplus.play._
 * import play.api.{Play, Application}
 * 
 * class ExampleSpec extends PlaySpec with OneBrowserPerSuite with FirefoxFactory {
 * 
 *   // Override app if you need a FakeApplication with other than non-default parameters.
 *   implicit override lazy val app: FakeApplication =
 *     FakeApplication(
 *       additionalConfiguration = Map("ehcacheplugin" -> "disabled"),
 *       withRoutes = TestRoute
 *     )
 * 
 *   "The OneBrowserPerSuite trait" must {
 *     "provide a FakeApplication" in {
 *       app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
 *     }
 *     "make the FakeApplication available implicitly" in {
 *       def getConfig(key: String)(implicit app: Application) = app.configuration.getString(key)
 *       getConfig("ehcacheplugin") mustBe Some("disabled")
 *     }
 *     "start the FakeApplication" in {
 *       Play.maybeApplication mustBe Some(app)
 *     }
 *     "provide the port number" in {
 *       port mustBe Helpers.testServerPort
 *     }
 *     "provide an actual running server" in {
 *       import Helpers._
 *       import java.net._
 *       val url = new URL("http://localhost:" + port + "/boum")
 *       val con = url.openConnection().asInstanceOf[HttpURLConnection]
 *       try con.getResponseCode mustBe 404
 *       finally con.disconnect()
 *     }
 *     "provide a web driver" in {
 *       go to ("http://localhost:" + port + "/testing")
 *       pageTitle mustBe "Test Page"
 *       click on find(name("b")).value
 *       eventually { pageTitle mustBe "scalatest" }
 *     }
 *   }
 * }
 * </pre>
 *
 * If you have many tests that can share the same `FakeApplication`, `TestServer`, and `WebDriver`, and you don't want to put them all into one
 * test class, you can place them into different `Suite` classes.
 * These will be your nested suites. Create a master suite that extends `OneServerPerSuite` and declares the nested 
 * `Suite`s. Annotate the nested suites with `@DoNotDiscover` and have them extend `ConfiguredBrowser`. Here's an example:
 *
 * <pre class="stHighlight">
 * package org.scalatestplus.play.examples.onebrowserpersuite
 * 
 * import play.api.test._
 * import org.scalatest._
 * import org.scalatestplus.play._
 * import play.api.{Play, Application}
 * 
 *  // This is the "master" suite
 * class NestedExampleSpec extends Suites(
 *   new OneSpec,
 *   new TwoSpec,
 *   new RedSpec,
 *   new BlueSpec
 * ) with OneBrowserPerSuite with FirefoxFactory {
 *   // Override app if you need a FakeApplication with other than non-default parameters.
 *   implicit override lazy val app: FakeApplication =
 *     FakeApplication(
 *       additionalConfiguration = Map("ehcacheplugin" -> "disabled"),
 *       withRoutes = TestRoute
 *     )
 * }
 *  
 * // These are the nested suites
 * @DoNotDiscover class OneSpec extends PlaySpec with ConfiguredBrowser
 * @DoNotDiscover class TwoSpec extends PlaySpec with ConfiguredBrowser
 * @DoNotDiscover class RedSpec extends PlaySpec with ConfiguredBrowser
 * 
 * @DoNotDiscover
 * class BlueSpec extends PlaySpec with ConfiguredBrowser {
 * 
 *   "The OneAppPerSuite trait" must {
 *     "provide a FakeApplication" in { 
 *       app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
 *     }
 *     "make the FakeApplication available implicitly" in {
 *       def getConfig(key: String)(implicit app: Application) = app.configuration.getString(key)
 *       getConfig("ehcacheplugin") mustBe Some("disabled")
 *     }
 *     "start the FakeApplication" in {
 *       Play.maybeApplication mustBe Some(app)
 *     }
 *     "provide the port number" in {
 *       port mustBe Helpers.testServerPort
 *     }
 *     "provide an actual running server" in {
 *       import Helpers._
 *       import java.net._
 *       val url = new URL("http://localhost:" + port + "/boum")
 *       val con = url.openConnection().asInstanceOf[HttpURLConnection]
 *       try con.getResponseCode mustBe 404
 *       finally con.disconnect()
 *     }
 *   }
 * }
 * </pre>
 */
trait OneBrowserPerSuite extends SuiteMixin with WebBrowser with Eventually with IntegrationPatience with BrowserFactory { this: Suite =>

  /**
   * An implicit instance of `FakeApplication`.
   *
   * This trait's implementation initializes this `lazy` `val` with a new instance of `FakeApplication` with
   * parameters set to their defaults. Override this `lazy` `val` if you need a `FakeApplication` created with non-default parameter values.
   */
  implicit lazy val app: FakeApplication = new FakeApplication()

  /**
   * The port used by the `TestServer`.  By default this will be set to the result returned from
   * `Helpers.testServerPort`. You can override this to provide a different port number.
   */
  lazy val port: Int = Helpers.testServerPort

  /**
   * Implicit `PortNumber` instance that wraps `port`. The value returned from `portNumber.value`
   * will be same as the value of `port`.
   */
  implicit final lazy val portNumber: PortNumber = PortNumber(port)

  /**
   * An implicit instance of `WebDriver`, created by calling `createWebDriver`.  
   * If there is an error when creating the `WebDriver`, `NoDriver` will be assigned 
   * instead.
   */
  implicit val webDriver: WebDriver = createWebDriver()

  /**
   * Automatically cancels tests with an appropriate error message when the `webDriver` field is a `NoDriver`,
   * else calls `super.withFixture(test)`
   */
  abstract override def withFixture(test: NoArgTest): Outcome = {
    webDriver match {
      case NoDriver(ex, errorMessage) =>
          ex match {
            case Some(e) => cancel(errorMessage, e)
            case None => cancel(errorMessage)
          }
      case _ => super.withFixture(test)
    }
  }

  /**
   * Invokes `start` on a new `TestServer` created with the `FakeApplication` provided by `app` and the
   * port number defined by `port`, places the `FakeApplication`, port number, and `WebDriver` into the `ConfigMap` under the keys
   *  `org.scalatestplus.play.app`, `org.scalatestplus.play.port`, and `org.scalatestplus.play.webDriver` respectively, to make
   * them available to nested suites; calls `super.run`; and lastly ensures the `FakeApplication`, test server, and `WebDriver` are stopped after
   * all tests and nested suites have completed.
   *
   * @param testName an optional name of one test to run. If `None`, all relevant tests should be run.
   *                 I.e., `None` acts like a wildcard that means run all relevant tests in this `Suite`.
   * @param args the `Args` for this run
   * @return a `Status` object that indicates when all tests and nested suites started by this method have completed, and whether or not a failure occurred.
   */
  abstract override def run(testName: Option[String], args: Args): Status = {
    val testServer = TestServer(port, app)
    val cleanup: Boolean => Unit = { _ =>
      testServer.stop()
      webDriver match {
        case _: NoDriver => // do nothing for NoDriver
        case safariDriver: SafariDriver => safariDriver.quit()
        case chromeDriver: ChromeDriver => chromeDriver.quit()
        case _ => webDriver.close()
      }
    }
    try {
      testServer.start()
      val newConfigMap = args.configMap + ("org.scalatestplus.play.app" -> app) + ("org.scalatestplus.play.port" -> port) + ("org.scalatestplus.play.webDriver" -> webDriver)
      val newArgs = args.copy(configMap = newConfigMap)
      val status = super.run(testName, newArgs)
      status.whenCompleted(cleanup)
      status
    } catch {
      case ex: Throwable =>
        cleanup(false)
        throw ex
    }
  }
}

