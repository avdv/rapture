/******************************************************************************************************************\
* Rapture CSV, version 2.0.0. Copyright 2010-2015 Jon Pretty, Propensive Ltd.                                      *
*                                                                                                                  *
* The primary distribution site is http://rapture.io/                                                              *
*                                                                                                                  *
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in complance    *
* with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.            *
*                                                                                                                  *
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed *
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License    *
* for the specific language governing permissions and limitations under the License.                               *
\******************************************************************************************************************/
package rapture.csv

import rapture.core._
import rapture.base._

object Macros {

  def extractorMacro[T: c.WeakTypeTag](c: BlackboxContext): c.Expr[CsvRowExtractor[T]] = {
    import c.universe._
    import compatibility._

    val cellExtractor = typeOf[CsvCellExtractor[_]].typeSymbol.asType.toTypeConstructor

    require(weakTypeOf[T].typeSymbol.asClass.isCaseClass)

    val params = declarations(c)(weakTypeOf[T]).collect {
      case m: MethodSymbol if m.isCaseAccessor => m.asMethod
    }.zipWithIndex.map { case (p, i) =>

      Apply(
        Select(
          Ident(termName(c, "mode")),
          termName(c, "unwrap")
        ),
        List(
          Apply(
            Select(
              c.inferImplicitValue(appliedType(cellExtractor, List(p.returnType)), false, false),
              termName(c, "extract")
            ),
            List(
              Apply(
                Select(
                  Ident(termName(c, "values")),
                  termName(c, "apply")
                ),
                List(Literal(Constant(i)))
              ),
              Literal(Constant(i)),
              Ident(termName(c, "mode"))
            )
          )
        )
      )
    }
    
    val construction = c.Expr[T](
      Apply(
        Select(
          New(
            TypeTree(weakTypeOf[T])
          ),
          constructor(c)
        ),
        params.to[List]
      )
    )

    reify {
      new CsvRowExtractor[T] {
        def extract(values: Seq[String], mode: Mode[_]): mode.Wrap[T, CsvGetException] =
          mode.wrap(construction.splice)
      }
    }
  }
}

