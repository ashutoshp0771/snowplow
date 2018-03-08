/*
 * Copyright (c) 2012-2018 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.snowplowanalytics.snowplow.enrich.common
package adapters
package registry

// Joda-Time
import org.joda.time.DateTime

// Scalaz
import scalaz._
import Scalaz._

// json4s
import org.json4s._

// Snowplow
import loaders.{CollectorApi, CollectorContext, CollectorPayload, CollectorSource}

// Specs2
import org.specs2.{ScalaCheck, Specification}
import org.specs2.matcher.DataTables
import org.specs2.scalaz.ValidationMatchers

class MandrillAdapterSpec extends Specification with DataTables with ValidationMatchers with ScalaCheck {
  def is = s2"""
  This is a specification to test the MandrillAdapter functionality
  payloadBodyToEvents must return a Success List[JValue] for a valid events string                      $e1
  payloadBodyToEvents must return a Failure String if the mapped events string is not in a valid format $e2
  payloadBodyToEvents must return a Failure String if the event string could not be parsed into JSON    $e3
  toRawEvents must return a Success Nel if every event in the payload is successful                     $e4
  toRawEvents must return a Failure Nel if any of the events in the payload do not pass full validation $e5
  toRawEvents must return a Failure Nel if the payload body is empty                                    $e6
  toRawEvents must return a Failure Nel if the payload content type is empty                            $e7
  toRawEvents must return a Failure Nel if the payload content type does not match expectation          $e8
  """

  implicit val resolver = SpecHelpers.IgluResolver

  object Shared {
    val api       = CollectorApi("com.mandrill", "v1")
    val cljSource = CollectorSource("clj-tomcat", "UTF-8", None)
    val context = CollectorContext(DateTime.parse("2013-08-29T00:18:48.000+00:00").some,
                                   "37.157.33.123".some,
                                   None,
                                   None,
                                   Nil,
                                   None)
  }

  val ContentType = "application/x-www-form-urlencoded"

  def e1 = {
    val bodyStr  = "mandrill_events=%5B%7B%22event%22%3A%20%22subscribe%22%7D%5D"
    val expected = List(JObject(List(("event", JString("subscribe")))))
    MandrillAdapter.payloadBodyToEvents(bodyStr) must beSuccessful(expected)
  }

  def e2 =
    "SPEC NAME"                           || "STRING TO PROCESS"                        | "EXPECTED OUTPUT" |
      "Failure, empty events string"      !! "mandrill_events="                         ! "Mandrill events string is empty: nothing to process" |
      "Failure, too many key-value pairs" !! "mandrill_events=some&mandrill_extra=some" ! "Mapped Mandrill body has invalid count of keys: 2" |
      "Failure, incorrect key"            !! "events_mandrill=something"                ! "Mapped Mandrill body does not have 'mandrill_events' as a key" |> {
      (_, str, expected) =>
        MandrillAdapter.payloadBodyToEvents(str) must beFailing(expected)
    }

  def e3 = {
    val bodyStr = "mandrill_events=%5B%7B%22event%22%3A%22click%7D%5D"
    val expected =
      "Mandrill events string failed to parse into JSON: [com.fasterxml.jackson.core.JsonParseException: Unexpected end-of-input: was expecting closing quote for a string value at [Source: [{\"event\":\"click}]; line: 1, column: 37]]"
    MandrillAdapter.payloadBodyToEvents(bodyStr) must beFailing(expected)
  }

  def e4 = { // Spec for nine seperate events being passed and returned.
    val bodyStr =
      "mandrill_events=%5B%7B%22event%22%3A%22send%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%5D%2C%22clicks%22%3A%5B%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22deferral%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%5D%2C%22clicks%22%3A%5B%5D%2C%22state%22%3A%22deferred%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa1%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%2C%22smtp_events%22%3A%5B%7B%22destination_ip%22%3A%22127.0.0.1%22%2C%22diag%22%3A%22451+4.3.5+Temporarily+unavailable%2C+try+again+later.%22%2C%22source_ip%22%3A%22127.0.0.1%22%2C%22ts%22%3A1365111111%2C%22type%22%3A%22deferred%22%2C%22size%22%3A0%7D%5D%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa1%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22hard_bounce%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22state%22%3A%22bounced%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa2%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%2C%22bounce_description%22%3A%22bad_mailbox%22%2C%22bgtools_code%22%3A10%2C%22diag%22%3A%22smtp%3B550+5.1.1+The+email+account+that+you+tried+to+reach+does+not+exist.+Please+try+double-checking+the+recipient%27s+email+address+for+typos+or+unnecessary+spaces.%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa2%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22soft_bounce%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22state%22%3A%22soft-bounced%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa3%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%2C%22bounce_description%22%3A%22mailbox_full%22%2C%22bgtools_code%22%3A22%2C%22diag%22%3A%22smtp%3B552+5.2.2+Over+Quota%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa3%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22open%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%7B%22ts%22%3A1365111111%7D%5D%2C%22clicks%22%3A%5B%7B%22ts%22%3A1365111111%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%7D%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa4%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa4%22%2C%22ip%22%3A%22127.0.0.1%22%2C%22location%22%3A%7B%22country_short%22%3A%22US%22%2C%22country%22%3A%22United+States%22%2C%22region%22%3A%22Oklahoma%22%2C%22city%22%3A%22Oklahoma+City%22%2C%22latitude%22%3A35.4675598145%2C%22longitude%22%3A-97.5164337158%2C%22postal_code%22%3A%2273101%22%2C%22timezone%22%3A%22-05%3A00%22%7D%2C%22user_agent%22%3A%22Mozilla%5C%2F5.0+%28Macintosh%3B+U%3B+Intel+Mac+OS+X+10.6%3B+en-US%3B+rv%3A1.9.1.8%29+Gecko%5C%2F20100317+Postbox%5C%2F1.1.3%22%2C%22user_agent_parsed%22%3A%7B%22type%22%3A%22Email+Client%22%2C%22ua_family%22%3A%22Postbox%22%2C%22ua_name%22%3A%22Postbox+1.1.3%22%2C%22ua_version%22%3A%221.1.3%22%2C%22ua_url%22%3A%22http%3A%5C%2F%5C%2Fwww.postbox-inc.com%5C%2F%22%2C%22ua_company%22%3A%22Postbox%2C+Inc.%22%2C%22ua_company_url%22%3A%22http%3A%5C%2F%5C%2Fwww.postbox-inc.com%5C%2F%22%2C%22ua_icon%22%3A%22http%3A%5C%2F%5C%2Fcdn.mandrill.com%5C%2Fimg%5C%2Femail-client-icons%5C%2Fpostbox.png%22%2C%22os_family%22%3A%22OS+X%22%2C%22os_name%22%3A%22OS+X+10.6+Snow+Leopard%22%2C%22os_url%22%3A%22http%3A%5C%2F%5C%2Fwww.apple.com%5C%2Fosx%5C%2F%22%2C%22os_company%22%3A%22Apple+Computer%2C+Inc.%22%2C%22os_company_url%22%3A%22http%3A%5C%2F%5C%2Fwww.apple.com%5C%2F%22%2C%22os_icon%22%3A%22http%3A%5C%2F%5C%2Fcdn.mandrill.com%5C%2Fimg%5C%2Femail-client-icons%5C%2Fmacosx.png%22%2C%22mobile%22%3Afalse%7D%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22click%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%7B%22ts%22%3A1365111111%7D%5D%2C%22clicks%22%3A%5B%7B%22ts%22%3A1365111111%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%7D%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa5%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa5%22%2C%22ip%22%3A%22127.0.0.1%22%2C%22location%22%3A%7B%22country_short%22%3A%22US%22%2C%22country%22%3A%22United+States%22%2C%22region%22%3A%22Oklahoma%22%2C%22city%22%3A%22Oklahoma+City%22%2C%22latitude%22%3A35.4675598145%2C%22longitude%22%3A-97.5164337158%2C%22postal_code%22%3A%2273101%22%2C%22timezone%22%3A%22-05%3A00%22%7D%2C%22user_agent%22%3A%22Mozilla%5C%2F5.0+%28Macintosh%3B+U%3B+Intel+Mac+OS+X+10.6%3B+en-US%3B+rv%3A1.9.1.8%29+Gecko%5C%2F20100317+Postbox%5C%2F1.1.3%22%2C%22user_agent_parsed%22%3A%7B%22type%22%3A%22Email+Client%22%2C%22ua_family%22%3A%22Postbox%22%2C%22ua_name%22%3A%22Postbox+1.1.3%22%2C%22ua_version%22%3A%221.1.3%22%2C%22ua_url%22%3A%22http%3A%5C%2F%5C%2Fwww.postbox-inc.com%5C%2F%22%2C%22ua_company%22%3A%22Postbox%2C+Inc.%22%2C%22ua_company_url%22%3A%22http%3A%5C%2F%5C%2Fwww.postbox-inc.com%5C%2F%22%2C%22ua_icon%22%3A%22http%3A%5C%2F%5C%2Fcdn.mandrill.com%5C%2Fimg%5C%2Femail-client-icons%5C%2Fpostbox.png%22%2C%22os_family%22%3A%22OS+X%22%2C%22os_name%22%3A%22OS+X+10.6+Snow+Leopard%22%2C%22os_url%22%3A%22http%3A%5C%2F%5C%2Fwww.apple.com%5C%2Fosx%5C%2F%22%2C%22os_company%22%3A%22Apple+Computer%2C+Inc.%22%2C%22os_company_url%22%3A%22http%3A%5C%2F%5C%2Fwww.apple.com%5C%2F%22%2C%22os_icon%22%3A%22http%3A%5C%2F%5C%2Fcdn.mandrill.com%5C%2Fimg%5C%2Femail-client-icons%5C%2Fmacosx.png%22%2C%22mobile%22%3Afalse%7D%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22spam%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%7B%22ts%22%3A1365111111%7D%5D%2C%22clicks%22%3A%5B%7B%22ts%22%3A1365111111%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%7D%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa6%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa6%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22unsub%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%7B%22ts%22%3A1365111111%7D%5D%2C%22clicks%22%3A%5B%7B%22ts%22%3A1365111111%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%7D%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa7%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa7%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22reject%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%5D%2C%22clicks%22%3A%5B%5D%2C%22state%22%3A%22rejected%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa8%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa8%22%2C%22ts%22%3A1415267366%7D%5D"
    val payload = CollectorPayload(Shared.api, Nil, ContentType.some, bodyStr.some, Shared.cljSource, Shared.context)
    val expected = NonEmptyList(
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/message_sent/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"opens":[],"clicks":[],"state":"sent","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa","_version":"exampleaaaaaaaaaaaaaaa"},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa","ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      ),
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/message_delayed/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"opens":[],"clicks":[],"state":"deferred","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa1","_version":"exampleaaaaaaaaaaaaaaa","smtp_events":[{"destination_ip":"127.0.0.1","diag":"451 4.3.5 Temporarily unavailable, try again later.","source_ip":"127.0.0.1","ts":"2013-04-04T21:31:51.000Z","type":"deferred","size":0}]},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa1","ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      ),
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/message_bounced/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"state":"bounced","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa2","_version":"exampleaaaaaaaaaaaaaaa","bounce_description":"bad_mailbox","bgtools_code":10,"diag":"smtp;550 5.1.1 The email account that you tried to reach does not exist. Please try double-checking the recipient's email address for typos or unnecessary spaces."},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa2","ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      ),
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/message_soft_bounced/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"state":"soft-bounced","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa3","_version":"exampleaaaaaaaaaaaaaaa","bounce_description":"mailbox_full","bgtools_code":22,"diag":"smtp;552 5.2.2 Over Quota"},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa3","ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      ),
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/message_opened/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"opens":[{"ts":"2013-04-04T21:31:51.000Z"}],"clicks":[{"ts":"2013-04-04T21:31:51.000Z","url":"http://mandrill.com"}],"state":"sent","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa4","_version":"exampleaaaaaaaaaaaaaaa"},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa4","ip":"127.0.0.1","location":{"country_short":"US","country":"United States","region":"Oklahoma","city":"Oklahoma City","latitude":35.4675598145,"longitude":-97.5164337158,"postal_code":"73101","timezone":"-05:00"},"user_agent":"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.1.8) Gecko/20100317 Postbox/1.1.3","user_agent_parsed":{"type":"Email Client","ua_family":"Postbox","ua_name":"Postbox 1.1.3","ua_version":"1.1.3","ua_url":"http://www.postbox-inc.com/","ua_company":"Postbox, Inc.","ua_company_url":"http://www.postbox-inc.com/","ua_icon":"http://cdn.mandrill.com/img/email-client-icons/postbox.png","os_family":"OS X","os_name":"OS X 10.6 Snow Leopard","os_url":"http://www.apple.com/osx/","os_company":"Apple Computer, Inc.","os_company_url":"http://www.apple.com/","os_icon":"http://cdn.mandrill.com/img/email-client-icons/macosx.png","mobile":false},"ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      ),
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/message_clicked/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"opens":[{"ts":"2013-04-04T21:31:51.000Z"}],"clicks":[{"ts":"2013-04-04T21:31:51.000Z","url":"http://mandrill.com"}],"state":"sent","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa5","_version":"exampleaaaaaaaaaaaaaaa"},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa5","ip":"127.0.0.1","location":{"country_short":"US","country":"United States","region":"Oklahoma","city":"Oklahoma City","latitude":35.4675598145,"longitude":-97.5164337158,"postal_code":"73101","timezone":"-05:00"},"user_agent":"Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.1.8) Gecko/20100317 Postbox/1.1.3","user_agent_parsed":{"type":"Email Client","ua_family":"Postbox","ua_name":"Postbox 1.1.3","ua_version":"1.1.3","ua_url":"http://www.postbox-inc.com/","ua_company":"Postbox, Inc.","ua_company_url":"http://www.postbox-inc.com/","ua_icon":"http://cdn.mandrill.com/img/email-client-icons/postbox.png","os_family":"OS X","os_name":"OS X 10.6 Snow Leopard","os_url":"http://www.apple.com/osx/","os_company":"Apple Computer, Inc.","os_company_url":"http://www.apple.com/","os_icon":"http://cdn.mandrill.com/img/email-client-icons/macosx.png","mobile":false},"url":"http://mandrill.com","ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      ),
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/message_marked_as_spam/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"opens":[{"ts":"2013-04-04T21:31:51.000Z"}],"clicks":[{"ts":"2013-04-04T21:31:51.000Z","url":"http://mandrill.com"}],"state":"sent","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa6","_version":"exampleaaaaaaaaaaaaaaa"},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa6","ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      ),
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/recipient_unsubscribed/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"opens":[{"ts":"2013-04-04T21:31:51.000Z"}],"clicks":[{"ts":"2013-04-04T21:31:51.000Z","url":"http://mandrill.com"}],"state":"sent","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa7","_version":"exampleaaaaaaaaaaaaaaa"},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa7","ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      ),
      RawEvent(
        Shared.api,
        Map(
          "tv"    -> "com.mandrill-v1",
          "e"     -> "ue",
          "p"     -> "srv",
          "ue_pr" -> """{"schema":"iglu:com.snowplowanalytics.snowplow/unstruct_event/jsonschema/1-0-0","data":{"schema":"iglu:com.mandrill/message_rejected/jsonschema/1-0-0","data":{"msg":{"ts":"2013-04-04T21:13:19.000Z","subject":"This an example webhook message","email":"example.webhook@mandrillapp.com","sender":"example.sender@mandrillapp.com","tags":["webhook-example"],"opens":[],"clicks":[],"state":"rejected","metadata":{"user_id":111},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa8","_version":"exampleaaaaaaaaaaaaaaa"},"_id":"exampleaaaaaaaaaaaaaaaaaaaaaaaaa8","ts":"2014-11-06T09:49:26.000Z"}}}"""
        ),
        ContentType.some,
        Shared.cljSource,
        Shared.context
      )
    )
    MandrillAdapter.toRawEvents(payload) must beSuccessful(expected)
  }

  def e5 = { // Spec for nine seperate events where two have incorrect event names and one does not have event as a parameter
    val bodyStr =
      "mandrill_events=%5B%7B%22event%22%3A%22sending%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%5D%2C%22clicks%22%3A%5B%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22deferred%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%5D%2C%22clicks%22%3A%5B%5D%2C%22state%22%3A%22deferred%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa1%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%2C%22smtp_events%22%3A%5B%7B%22destination_ip%22%3A%22127.0.0.1%22%2C%22diag%22%3A%22451+4.3.5+Temporarily+unavailable%2C+try+again+later.%22%2C%22source_ip%22%3A%22127.0.0.1%22%2C%22ts%22%3A1365111111%2C%22type%22%3A%22deferred%22%2C%22size%22%3A0%7D%5D%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa1%22%2C%22ts%22%3A1415267366%7D%2C%7B%22eventsss%22%3A%22hard_bounce%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22state%22%3A%22bounced%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa2%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%2C%22bounce_description%22%3A%22bad_mailbox%22%2C%22bgtools_code%22%3A10%2C%22diag%22%3A%22smtp%3B550+5.1.1+The+email+account+that+you+tried+to+reach+does+not+exist.+Please+try+double-checking+the+recipient%27s+email+address+for+typos+or+unnecessary+spaces.%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa2%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22soft_bounce%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22state%22%3A%22soft-bounced%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa3%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%2C%22bounce_description%22%3A%22mailbox_full%22%2C%22bgtools_code%22%3A22%2C%22diag%22%3A%22smtp%3B552+5.2.2+Over+Quota%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa3%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22open%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%7B%22ts%22%3A1365111111%7D%5D%2C%22clicks%22%3A%5B%7B%22ts%22%3A1365111111%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%7D%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa4%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa4%22%2C%22ip%22%3A%22127.0.0.1%22%2C%22location%22%3A%7B%22country_short%22%3A%22US%22%2C%22country%22%3A%22United+States%22%2C%22region%22%3A%22Oklahoma%22%2C%22city%22%3A%22Oklahoma+City%22%2C%22latitude%22%3A35.4675598145%2C%22longitude%22%3A-97.5164337158%2C%22postal_code%22%3A%2273101%22%2C%22timezone%22%3A%22-05%3A00%22%7D%2C%22user_agent%22%3A%22Mozilla%5C%2F5.0+%28Macintosh%3B+U%3B+Intel+Mac+OS+X+10.6%3B+en-US%3B+rv%3A1.9.1.8%29+Gecko%5C%2F20100317+Postbox%5C%2F1.1.3%22%2C%22user_agent_parsed%22%3A%7B%22type%22%3A%22Email+Client%22%2C%22ua_family%22%3A%22Postbox%22%2C%22ua_name%22%3A%22Postbox+1.1.3%22%2C%22ua_version%22%3A%221.1.3%22%2C%22ua_url%22%3A%22http%3A%5C%2F%5C%2Fwww.postbox-inc.com%5C%2F%22%2C%22ua_company%22%3A%22Postbox%2C+Inc.%22%2C%22ua_company_url%22%3A%22http%3A%5C%2F%5C%2Fwww.postbox-inc.com%5C%2F%22%2C%22ua_icon%22%3A%22http%3A%5C%2F%5C%2Fcdn.mandrill.com%5C%2Fimg%5C%2Femail-client-icons%5C%2Fpostbox.png%22%2C%22os_family%22%3A%22OS+X%22%2C%22os_name%22%3A%22OS+X+10.6+Snow+Leopard%22%2C%22os_url%22%3A%22http%3A%5C%2F%5C%2Fwww.apple.com%5C%2Fosx%5C%2F%22%2C%22os_company%22%3A%22Apple+Computer%2C+Inc.%22%2C%22os_company_url%22%3A%22http%3A%5C%2F%5C%2Fwww.apple.com%5C%2F%22%2C%22os_icon%22%3A%22http%3A%5C%2F%5C%2Fcdn.mandrill.com%5C%2Fimg%5C%2Femail-client-icons%5C%2Fmacosx.png%22%2C%22mobile%22%3Afalse%7D%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22click%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%7B%22ts%22%3A1365111111%7D%5D%2C%22clicks%22%3A%5B%7B%22ts%22%3A1365111111%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%7D%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa5%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa5%22%2C%22ip%22%3A%22127.0.0.1%22%2C%22location%22%3A%7B%22country_short%22%3A%22US%22%2C%22country%22%3A%22United+States%22%2C%22region%22%3A%22Oklahoma%22%2C%22city%22%3A%22Oklahoma+City%22%2C%22latitude%22%3A35.4675598145%2C%22longitude%22%3A-97.5164337158%2C%22postal_code%22%3A%2273101%22%2C%22timezone%22%3A%22-05%3A00%22%7D%2C%22user_agent%22%3A%22Mozilla%5C%2F5.0+%28Macintosh%3B+U%3B+Intel+Mac+OS+X+10.6%3B+en-US%3B+rv%3A1.9.1.8%29+Gecko%5C%2F20100317+Postbox%5C%2F1.1.3%22%2C%22user_agent_parsed%22%3A%7B%22type%22%3A%22Email+Client%22%2C%22ua_family%22%3A%22Postbox%22%2C%22ua_name%22%3A%22Postbox+1.1.3%22%2C%22ua_version%22%3A%221.1.3%22%2C%22ua_url%22%3A%22http%3A%5C%2F%5C%2Fwww.postbox-inc.com%5C%2F%22%2C%22ua_company%22%3A%22Postbox%2C+Inc.%22%2C%22ua_company_url%22%3A%22http%3A%5C%2F%5C%2Fwww.postbox-inc.com%5C%2F%22%2C%22ua_icon%22%3A%22http%3A%5C%2F%5C%2Fcdn.mandrill.com%5C%2Fimg%5C%2Femail-client-icons%5C%2Fpostbox.png%22%2C%22os_family%22%3A%22OS+X%22%2C%22os_name%22%3A%22OS+X+10.6+Snow+Leopard%22%2C%22os_url%22%3A%22http%3A%5C%2F%5C%2Fwww.apple.com%5C%2Fosx%5C%2F%22%2C%22os_company%22%3A%22Apple+Computer%2C+Inc.%22%2C%22os_company_url%22%3A%22http%3A%5C%2F%5C%2Fwww.apple.com%5C%2F%22%2C%22os_icon%22%3A%22http%3A%5C%2F%5C%2Fcdn.mandrill.com%5C%2Fimg%5C%2Femail-client-icons%5C%2Fmacosx.png%22%2C%22mobile%22%3Afalse%7D%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22spam%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%7B%22ts%22%3A1365111111%7D%5D%2C%22clicks%22%3A%5B%7B%22ts%22%3A1365111111%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%7D%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa6%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa6%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22unsub%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%7B%22ts%22%3A1365111111%7D%5D%2C%22clicks%22%3A%5B%7B%22ts%22%3A1365111111%2C%22url%22%3A%22http%3A%5C%2F%5C%2Fmandrill.com%22%7D%5D%2C%22state%22%3A%22sent%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa7%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa7%22%2C%22ts%22%3A1415267366%7D%2C%7B%22event%22%3A%22reject%22%2C%22msg%22%3A%7B%22ts%22%3A1365109999%2C%22subject%22%3A%22This+an+example+webhook+message%22%2C%22email%22%3A%22example.webhook%40mandrillapp.com%22%2C%22sender%22%3A%22example.sender%40mandrillapp.com%22%2C%22tags%22%3A%5B%22webhook-example%22%5D%2C%22opens%22%3A%5B%5D%2C%22clicks%22%3A%5B%5D%2C%22state%22%3A%22rejected%22%2C%22metadata%22%3A%7B%22user_id%22%3A111%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa8%22%2C%22_version%22%3A%22exampleaaaaaaaaaaaaaaa%22%7D%2C%22_id%22%3A%22exampleaaaaaaaaaaaaaaaaaaaaaaaaa8%22%2C%22ts%22%3A1415267366%7D%5D"
    val payload = CollectorPayload(Shared.api, Nil, ContentType.some, bodyStr.some, Shared.cljSource, Shared.context)
    val expected = NonEmptyList(
      "Mandrill event at index [0] failed: type parameter [sending] not recognized",
      "Mandrill event at index [1] failed: type parameter [deferred] not recognized",
      "Mandrill event at index [2] failed: type parameter not provided - cannot determine event type"
    )
    MandrillAdapter.toRawEvents(payload) must beFailing(expected)
  }

  def e6 = {
    val payload = CollectorPayload(Shared.api, Nil, ContentType.some, None, Shared.cljSource, Shared.context)
    MandrillAdapter.toRawEvents(payload) must beFailing(
      NonEmptyList("Request body is empty: no Mandrill events to process"))
  }

  def e7 = {
    val body    = "mandrill_events=%5B%7B%22event%22%3A%20%22subscribe%22%7D%5D"
    val payload = CollectorPayload(Shared.api, Nil, None, body.some, Shared.cljSource, Shared.context)
    MandrillAdapter.toRawEvents(payload) must beFailing(
      NonEmptyList(
        "Request body provided but content type empty, expected application/x-www-form-urlencoded for Mandrill"))
  }

  def e8 = {
    val body    = "mandrill_events=%5B%7B%22event%22%3A%20%22subscribe%22%7D%5D"
    val ct      = "application/x-www-form-urlencoded; charset=utf-8"
    val payload = CollectorPayload(Shared.api, Nil, ct.some, body.some, Shared.cljSource, Shared.context)
    MandrillAdapter.toRawEvents(payload) must beFailing(NonEmptyList(
      "Content type of application/x-www-form-urlencoded; charset=utf-8 provided, expected application/x-www-form-urlencoded for Mandrill"))
  }
}
