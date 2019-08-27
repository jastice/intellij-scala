package org.jetbrains.plugins.scala
package lang
package parser
package parsing
package expressions

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder
import org.jetbrains.plugins.scala.lang.parser.parsing.patterns.Guard

/*
 * Enumerators ::= Generator {semi ForBinding}
 */
object Enumerators {

  def parse(builder: ScalaPsiBuilder): Boolean = {
    val enumsMarker = builder.mark

    // eat all semicolons (which is not correct), show error in ScForAnnotator
    CommonUtils.eatAllSemicolons(builder)

    if (!Generator.parse(builder)) {
      enumsMarker.drop()
      return false
    }
    var exit = true
    while (exit) {
      val guard = builder.getTokenType match {
        case ScalaTokenTypes.tSEMICOLON =>
          builder.advanceLexer()
          // eat all semicolons (which is not correct), show error in ScForAnnotator
          CommonUtils.eatAllSemicolons(builder)
          false
        case _ if builder.newlineBeforeCurrentToken => false
        case _ if Guard.parse(builder) => true
        case _ => exit = false; true
      }
      if (!guard && !Enumerator.parse(builder)) exit = false
    }
    enumsMarker.done(ScalaElementType.ENUMERATORS)
    true
  }
}