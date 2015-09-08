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
import org.apache.spark.ml.regression.LinearRegression
import org.apache.spark.annotation.Experimental
import org.apache.spark.mllib.util.LinearDataGenerator
import org.apache.spark.ml.tuning.{ CrossValidator, ParamGridBuilder }
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.regression.LinearRegressionWithCD
import org.apache.spark.mllib.util.MLUtils

/**
 * A simple example demonstrating model selection using CrossValidator.
 * This example also demonstrates how Pipelines are Estimators.
 *
 * This example uses the [[LabeledDocument]] and [[Document]] case classes from
 * [[SimpleTextClassificationPipeline]].
 *
 * Run with
 * {{{
 * bin/run-example ml.CrossValidatorExample
 * }}}
 */
//From spark/examples/src/main/scala/org/apache/spark/examples/ml/CrossValidatorExample.scala
//http://spark.apache.org/docs/latest/ml-guide.html
object CrossValidatorExample {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("CrossValidatorExample").setMaster("local")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._

    // Prepare training documents, which are labeled.
    //LinearDataGenerator.generateLinearRDD(sc, numGenExamples, numGenFeatures, eps, numGenPartitions)
    //val training = LinearDataGenerator.generateLinearRDD(sc, 12, 10, 10)
    val training = LinearDataGenerator.generateLinearRDD(sc, 10000, 10, 10)

    
        //val path = "data/sample_linear_regression_data.txt"
    //val path = "data/sample_linear_regression_data_fold2.txt"
    //val training = MLUtils.loadLibSVMFile(sc, path)

    // Configure an ML pipeline, which consists of three stages: tokenizer, hashingTF, and lr.
    //    val tokenizer = new Tokenizer()
    //      .setInputCol("text")
    //      .setOutputCol("words")
    //    val hashingTF = new HashingTF()
    //      .setInputCol(tokenizer.getOutputCol)
    //      .setOutputCol("features")
    //    val lr = new LogisticRegression()
    //      .setMaxIter(10)
    //    val pipeline = new Pipeline()
    //      .setStages(Array(tokenizer, hashingTF, lr))
    val lr = new LinearRegressionWithCD("")
      .setMaxIter(100)

    // We use a ParamGridBuilder to construct a grid of parameters to search over.
    // With 3 values for hashingTF.numFeatures and 2 values for lr.regParam,
    // this grid will have 3 x 2 = 6 parameter settings for CrossValidator to choose from.
    //val paramGrid = new   MultiModelParamGridBuilder(lr.getMaxIter)
    val paramGridBuilder = new ParamGridBuilder()
      //.addGrid(lr.regParam, Array(0.1, 0.01))
     // .addGrid(lr.tol, Array(1E-6, 1E-9))
    .addGrid(lr.elasticNetParam, Array(0.2, 0.3))
      //.build()

     //val paramGrid = paramGridBuilder.build
     val paramGrid = paramGridBuilder.build.flatMap(pm => Array.fill(3)(pm.copy))
     //val paramGrid1 = paramGridBuilder.build.flatMap(pm => Array.fill(lr.numIterations)(pm.copy))
//     val paramGrid = paramGrid1.zipWithIndex.map({case (pm, i) => {
//       val rp = lr.regParam
//       pm.put(rp.w(i%3))
//       }})
     println(s"paramGrid: ${paramGrid.mkString("\n")}")

    // We now treat the Pipeline as an Estimator, wrapping it in a CrossValidator instance.
    // This will allow us to jointly choose parameters for all Pipeline stages.
    // A CrossValidator requires an Estimator, a set of Estimator ParamMaps, and an Evaluator.
    val crossval = new CrossValidator("")
      //.setEstimator(pipeline)
      .setEstimator(lr)
      //.setEvaluator(new BinaryClassificationEvaluator)
      .setEvaluator(new RegressionEvaluator)
      .setEstimatorParamMaps(paramGrid)
      .setNumFolds(2) // Use 3+ in practice

    // Run cross-validation, and choose the best set of parameters.
    val cvModel = crossval.fit(training.toDF())
    println(s"\nBest Model: ${cvModel.bestModel.explainParams}")
    //    // Prepare test documents, which are unlabeled.
    // LinearDataGenerator.generateLinearRDD(sc, numGenExamples, numGenFeatures, eps, numGenPartitions)
    //val test = LinearDataGenerator.generateLinearRDD(sc, 4, 10, 10)

    //    // Make predictions on test documents. cvModel uses the best model found (lrModel).
    //    cvModel.transform(test.toDF())
    //      .select("id", "text", "probability", "prediction")
    //      .collect()
    //      .foreach {
    //        case Row(id: Long, text: String, prob: Vector, prediction: Double) =>
    //          println(s"($id, $text) --> prob=$prob, prediction=$prediction")
    //      }

    sc.stop()
  }
}
// scalastyle:on println