package org.jetbrains.plugins.scala.worksheet.integration.plain

import org.jetbrains.plugins.scala.WorksheetEvaluationTests
import org.jetbrains.plugins.scala.util.runners.{ RunWithScalaVersionsFilter, TestScalaVersion}
import org.junit.experimental.categories.Category

//TODO: probably interactive test doesnt work, CHECK
@Category(Array(classOf[WorksheetEvaluationTests]))
@RunWithScalaVersionsFilter(Array(
  TestScalaVersion.Scala_3_0,
))
class WorksheetPlainCompileLocallyRunLocallyIntegrationTest extends WorksheetPlainIntegrationBaseTest {

  override def useCompileServer = false

  // the value doesn't actually matter, cause compile server isn't used anyway
  override def runInCompileServerProcess = false
}
