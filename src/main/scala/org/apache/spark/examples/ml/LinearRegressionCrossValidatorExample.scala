/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package org.apache.spark.examples.ml

import org.apache.spark.{ SparkConf, SparkContext }
import org.apache.spark.sql.SQLContext
import org.apache.spark.mllib.util.LinearDataGenerator
import org.apache.spark.examples.mllib.AbstractParams
import org.apache.spark.ml.tuning.{ CrossValidator, AutoGeneratedParamGridBuilder }
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.{ LinearRegressionWithCD, LinearRegressionWithCDModel, ModelLogger }
import org.apache.spark.Logging
import org.apache.spark.sql.Row
import org.apache.spark.sql.DataFrame
import nonsubmit.utils.Timer
import scopt.OptionParser

/**
 * A simple example demonstrating linear regression model selection using CrossValidator and auto-generated regParam values.
 */
// From spark/examples/src/main/scala/org/apache/spark/examples/ml/CrossValidatorExample.scala
// http://spark.apache.org/docs/latest/ml-guide.html
object LinearRegressionCrossValidatorExample extends Logging {

  case class Params(
    elasticNetParam: Seq[Double] = Seq(0.01), // value of alpha
    numLambdas: Int = 100, // number of lambdas
    maxIter: Int = 100, //  max iterations for beta loop, or until tol
    tol: Double = 1E-3, //  tolerance for beta loop, or until maxIter
    lambdaShrink: Double = 1E-3,
    fitIntercept: Boolean = true,
    numFolds: Int = 1,
    logSaveAll: Boolean = false) extends AbstractParams[Params]

  object ScoptV330 {
    // To use csv read functional from scopt 3.3.0 in Spark scopt version 3.2.0
    import scopt.Read
    import scopt.Read.reads
    val sep = ","
    implicit def seqRead[A: Read]: Read[Seq[A]] = reads { (s: String) =>
      s.split(sep).map(implicitly[Read[A]].reads)
    }
  }

  def main(args: Array[String]) {
    import ScoptV330._

    val defaultParams = Params()

    val parser = new OptionParser[Params]("LinearRegression") {
      head("LinearRegression: an example app for linear regression.")
      opt[Seq[Double]]("elasticNetParam")
        .text("value of alpha")
        .action((x, c) => c.copy(elasticNetParam = x))
      opt[Int]("numLambdas")
        .text("number of lambdas")
        .action((x, c) => c.copy(numLambdas = x))
      opt[Int]("maxIter")
        .text("max number of iterations in beta loop")
        .action((x, c) => c.copy(maxIter = x))
      opt[Double]("tol")
        .text("tolerance for beta loop")
        .action((x, c) => c.copy(tol = x))
      opt[Double]("lambdaShrink")
        .text("value of lambda shrink parameter")
        .action((x, c) => c.copy(lambdaShrink = x))
      opt[Boolean]("fitIntercept")
        .text("fit intercept if True")
        .action((x, c) => c.copy(fitIntercept = x))
      opt[Int]("numFolds")
        .text("number of folds for cross validation")
        .action((x, c) => c.copy(numFolds = x))
      opt[Boolean]("logSaveAll")
        .text("save intermediate values to log if True")
        .action((x, c) => c.copy(logSaveAll = x))

      note(
        """
          |For example, the following command runs this app on a synthetic dataset:
          |
          | bin/spark-submit --class org.apache.spark.examples.mllib.LinearRegression \
          |  examples/target/scala-*/spark-examples-*.jar \
          |  data/mllib/sample_linear_regression_data.txt
        """.stripMargin)
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    } getOrElse {
      sys.exit(1)
    }
  }

  def run(params: Params) {

    val conf = new SparkConf().setAppName("LinearRegressionCrossValidatorExample").setMaster("local")
    val sc = new SparkContext(conf)

    // nexamples, nfeatures, eps, intercept, fracTest
    val (training, test) = generateData(sc, 1000, 10, 0.1, 6.2, 0.2)

    val lr = new LinearRegressionWithCD("")
      .setElasticNetParam(params.elasticNetParam(0))
      .setNumLambdas(params.numLambdas)
      .setMaxIter(params.maxIter)
      .setTol(params.tol)
      .setLambdaShrink(params.lambdaShrink)
      .setFitIntercept(params.fitIntercept)
      .setLogSaveAll(params.logSaveAll)

    val paramGrid = new AutoGeneratedParamGridBuilder()
      .addGrid(lr.elasticNetParam, params.elasticNetParam)
      .buildWithAutoGeneratedGrid("lambdaIndex", params.numLambdas)

    val crossval = (new CrossValidator(""))
      .setEstimator(lr)
      .setEvaluator(new RegressionEvaluator)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(2) // Use 3+ in practice

    // Run cross-validation, and choose the best set of parameters.
    val cvModel = crossval.fit(training)
    ModelLogger.logInfo(s"Best Model:\n${cvModel.bestModel.explainParams}")
    val bestModel = cvModel.bestModel.asInstanceOf[LinearRegressionWithCDModel]
    ModelLogger.logInfo(s"Order of model weights: [${bestModel.orderOfWeights.mkString(",")}]")
    ModelLogger.logInfo(s"Model weights: ${bestModel.weights}")

    // Make predictions on test data. cvModel uses the best model found (lrModel).
    val predictions = cvModel.transform(test)
    val eval = new RegressionEvaluator()
    //"mse", "rmse", "r2", "mae"
    EvalLogger.logInfo(s"Test MSE: ${eval.setMetricName("mse").evaluate(predictions)}")
    EvalLogger.logInfo(s"Test R2: ${eval.setMetricName("r2").evaluate(predictions)}")

    //logPredictions(predictions)

    logDebug(s"${Timer.timers.mkString("\n")}")

    sc.stop()
  }

  private def logPredictions(predictions: DataFrame) = {
    predictions.select("label", "prediction")
      .collect()
      .foreach {
        case Row(label: Double, prediction: Double) =>
          logInfo(s"label - prediction = ${label - prediction}")
      }
  }

  /**
   * Generate a tuple of DataFrames (training, test) consisting of RDD[LabeledPoint] containing sample data for Linear Regression models.
   *
   * @param sc SparkContext to be used for generating the RDD.
   * @param nexamples Number of examples that will be contained in the RDD.
   * @param nfeatures Number of features to generate for each example.
   * @param eps Epsilon factor by which examples are scaled.
   * @param intercept Intercept.
   * @param fracTest Fraction of data to hold out for testing. Default value is 0.2.
   * @param nparts Number of partitions in the RDD. Default value is 2.
   *
   * @return Tuple of DataFrames (training, training) consisting of RDD[LabeledPoint] containing sample data.
   */
  private def generateData(
    sc: SparkContext,
    nexamples: Int,
    nfeatures: Int,
    eps: Double = 0.1,
    intercept: Double = 0.0,
    fracTest: Double = 0.2,
    nparts: Int = 2): (DataFrame, DataFrame) = {
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    val data = LinearDataGenerator.generateLinearRDD(sc, nexamples, nfeatures, eps, nparts, intercept)
      .randomSplit(Array(1.0 - fracTest, fracTest), seed = 12345)

    val dataFrames = (data(0).toDF(), data(1).toDF())
    logInfo(s"generated data; nexamples: ${nexamples}, nfeatures: ${nfeatures}, eps: ${eps}, intercept: ${intercept}, fracTest: ${fracTest}, training nexamples: ${dataFrames._1.count}, test nexamples: ${dataFrames._2.count}")
    dataFrames
  }
}

object EvalLogger extends Logging {
  override def logInfo(msg: => String) = { super.logInfo(msg) }
}
// scalastyle:on println
