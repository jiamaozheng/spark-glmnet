package org.apache.spark.ml.regression

import org.apache.spark.ml.param.{ ParamMap, Params, DoubleParam, IntParam, BooleanParam, ParamValidators }

// Coded into and generated by org.apache.spark.ml.param.shared.SharedParamsCodeGen.

///**
// * (private[ml]) Trait for shared param maxIter.
// */
private[ml] trait HasLambdaIndex extends Params {

  /**
   * Param for lambda index (>= 0).
   * @group param
   */
  final val lambdaIndex: IntParam = new IntParam(this, "lambdaIndex", "lambda index (>= 0)", ParamValidators.gtEq(0))

  /** @group getParam */
  final def getLambdaIndex: Int = $(lambdaIndex)
}

//TODO - Temporary param to allow testing multiple versions of CoordinateDescent with minimum code duplication
private[ml] trait HasOptimizerVersion extends Params {

  /**
   * Param for optimizerVersion (>= 0).
   * @group param
   */
  final val optimizerVersion: IntParam = new IntParam(this, "optimizerVersion", "optimizerVersion (>= 0)", ParamValidators.gtEq(0))

  /** @group getParam */
  final def getOptimizerVersion: Int = $(optimizerVersion)
}

private[ml] trait HasLogSaveAll extends Params {
  /**
   * Param for some param (>= 0).
   * @group param
   */
  final val logSaveAll: BooleanParam = new BooleanParam(this, "logSaveAll", "some param (>= 0)")

  /** @group getParam */
  final def getLogSaveAll: Boolean = $(logSaveAll)
}

private[ml] trait HasNumLambdas extends Params {
  /**
   * Param for lambda index (>= 0).
   * @group param
   */
  final val numLambdas: IntParam = new IntParam(this, "numLambdas", "number of lambdas (>= 0)", ParamValidators.gtEq(0))

  /** @group getParam */
  final def getNumLambdas: Int = $(numLambdas)
}

private[ml] trait HasLambdaShrink extends Params {
  /**
   * Param for lambda index (>= 0).
   * @group param
   */
  final val lambdaShrink: DoubleParam = new DoubleParam(this, "lambdaShrink ", "lambda shrink (>= 0.0)", ParamValidators.gtEq(0))

  /** @group getParam */
  final def getLambdaShrink: Double = $(lambdaShrink)
}

private[ml] trait HasNumFolds extends Params {
  /**
   * Param for lambda index (>= 0).
   * @group param
   */
  final val numFolds: IntParam = new IntParam(this, "numFolds", "numFolds (>= 0)", ParamValidators.gtEq(0))

  /** @group getParam */
  final def getNumFolds: Int = $(numFolds)
}