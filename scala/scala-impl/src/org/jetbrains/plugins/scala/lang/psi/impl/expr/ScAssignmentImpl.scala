package org.jetbrains.plugins.scala
package lang
package psi
package impl
package expr

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiField
import org.jetbrains.plugins.scala.caches.BlockModificationTracker
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScClassParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.types.api.Unit
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil
import org.jetbrains.plugins.scala.lang.resolve.processor.MethodResolveProcessor
import org.jetbrains.plugins.scala.lang.resolve.{ScalaResolveResult, ScalaResolveState, StdKinds}
import org.jetbrains.plugins.scala.macroAnnotations.Cached

/**
  * @author Alexander Podkhalyuzin
  */
class ScAssignmentImpl(node: ASTNode) extends ScExpressionImplBase(node) with ScAssignment {

  protected override def innerType: TypeResult = {
    leftExpression match {
      case call: ScMethodCall => call.`type`()
      case _ =>
        resolveAssignment match {
          case Some(_) =>
            mirrorMethodCall match {
              case Some(call) => call.`type`()
              case None => Right(Unit)
            }
          case _ => Right(Unit)
        }
    }
  }

  @Cached(BlockModificationTracker(this), this)
  override def resolveAssignment: Option[ScalaResolveResult] = resolveAssignmentInner(shapeResolve = false)

  @Cached(BlockModificationTracker(this), this)
  override def shapeResolveAssignment: Option[ScalaResolveResult] = resolveAssignmentInner(shapeResolve = true)

  @Cached(BlockModificationTracker(this), this)
  override def mirrorMethodCall: Option[ScMethodCall] = {
    leftExpression match {
      case ref: ScReferenceExpression =>
        val text = s"${ref.refName}_=(${rightExpression.map(_.getText).getOrElse("")})"
        val mirrorExpr = ScalaPsiElementFactory.createExpressionWithContextFromText(text, getContext, this)
        mirrorExpr match {
          case call@ScMethodCall(referenceExpression: ScReferenceExpression, _) =>
            referenceExpression.assignment = this
            Some(call)
          case _ => None
        }
      case methodCall: ScMethodCall =>
        val invokedExpr = methodCall.getInvokedExpr
        val text = s"${invokedExpr.getText}.update(${methodCall.args.exprs.map(_.getText).mkString(",")}," +
          s" ${rightExpression.map(_.getText).getOrElse("")}"
        val mirrorExpr = ScalaPsiElementFactory.createExpressionWithContextFromText(text, getContext, this)
        //todo: improve performance: do not re-evaluate resolve to "update" method
        mirrorExpr match {
          case call: ScMethodCall => Some(call)
          case _ => None
        }
      case _ => None
    }
  }

  private def resolveAssignmentInner(shapeResolve: Boolean): Option[ScalaResolveResult] = {
    leftExpression match {
      case ref: ScReferenceExpression =>
        ref.bind() match {
          case Some(r) =>
            ScalaPsiUtil.nameContext(r.element) match {
              case _: ScVariable => None
              case c: ScClassParameter if c.isVar => None
              case _: PsiField => None
              case fun: ScFunction if ScalaPsiUtil.isViableForAssignmentFunction(fun) =>
                val processor = new MethodResolveProcessor(ref, ScalaNamesUtil.clean(fun.name) + "_=",
                  rightExpression.map(expr => List(Seq(expr))).getOrElse(Nil), Nil, ref.getPrevTypeInfoParams,
                  isShapeResolve = shapeResolve, kinds = StdKinds.methodsOnly)
                r.fromType match {
                  case Some(tp) => processor.processType(tp, ref)
                  case None =>
                    fun.getContext match {
                      case d: ScDeclarationSequenceHolder =>
                        d.processDeclarations(processor, ScalaResolveState.empty, fun, ref)
                      case _ =>
                    }
                }
                val candidates = processor.candidatesS
                if (candidates.size == 1) Some(candidates.toArray.apply(0))
                else None
              case _ => None
            }
          case _ => None
        }
      case _ => None
    }
  }

  override def toString: String = "AssignStatement"
}
