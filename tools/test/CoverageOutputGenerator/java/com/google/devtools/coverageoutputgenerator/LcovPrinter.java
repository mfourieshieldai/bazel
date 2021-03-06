// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.coverageoutputgenerator;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Prints coverage data stored in a collection of {@link SourceFileCoverage} in a <a
 * href="http://ltp.sourceforge.net/coverage/lcov/geninfo.1.php">lcov tracefile format</a>
 */
class LcovPrinter {
  private static final Logger logger = Logger.getLogger(LcovPrinter.class.getName());
  private final BufferedWriter bufferedWriter;

  private LcovPrinter(BufferedWriter bufferedWriter) {
    this.bufferedWriter = bufferedWriter;
  }

  static boolean print(OutputStream outputStream, Coverage coverage) {
    BufferedWriter bufferedWriter;
    try (Writer fileWriter = new OutputStreamWriter(outputStream, UTF_8)) {
      bufferedWriter = new BufferedWriter(fileWriter);
      LcovPrinter lcovPrinter = new LcovPrinter(bufferedWriter);
      lcovPrinter.print(coverage);
      bufferedWriter.close();
    } catch (IOException exception) {
      logger.log(Level.SEVERE, "Could not write to output file.");
      return false;
    }
    return true;
  }

  private boolean print(Coverage coverage) {
    try {
      for (SourceFileCoverage sourceFile : coverage.getAllSourceFiles()) {
        print(sourceFile);
      }
    } catch (IOException exception) {
      logger.log(Level.SEVERE, "Could not write to output file.");
      return false;
    }
    return true;
  }

  /**
   * Prints the given source data in an lcov tracefile format.
   *
   * <p>Assumes the file is opened and closed outside of this method.
   */
  @VisibleForTesting
  void print(SourceFileCoverage sourceFile) throws IOException {
    printSFLine(sourceFile);
    printFNLines(sourceFile);
    printFNDALines(sourceFile);
    printFNFLine(sourceFile);
    printFNHLine(sourceFile);
    printBRDALines(sourceFile);
    printBALines(sourceFile);
    printBRFLine(sourceFile);
    printBRHLine(sourceFile);
    printDALines(sourceFile);
    printLHLine(sourceFile);
    printLFLine(sourceFile);
    printEndOfRecordLine();
  }

  // SF:<absolute path to the source file>
  private void printSFLine(SourceFileCoverage sourceFile) throws IOException {
    bufferedWriter.write(Constants.SF_MARKER);
    bufferedWriter.write(sourceFile.sourceFileName());
    bufferedWriter.newLine();
  }

  // FN:<line number of function start>,<function name>
  private void printFNLines(SourceFileCoverage sourceFile) throws IOException {
    for (Entry<String, Integer> entry : sourceFile.getAllLineNumbers()) {
      bufferedWriter.write(Constants.FN_MARKER);
      bufferedWriter.write(Integer.toString(entry.getValue())); // line number of function start
      bufferedWriter.write(Constants.DELIMITER);
      bufferedWriter.write(entry.getKey()); // function name
      bufferedWriter.newLine();
    }
  }

  // FNDA:<execution count>,<function name>
  private void printFNDALines(SourceFileCoverage sourceFile) throws IOException {
    for (Entry<String, Integer> entry : sourceFile.getAllExecutionCount()) {
      bufferedWriter.write(Constants.FNDA_MARKER);
      bufferedWriter.write(Integer.toString(entry.getValue())); // execution count
      bufferedWriter.write(Constants.DELIMITER);
      bufferedWriter.write(entry.getKey()); // function name
      bufferedWriter.newLine();
    }
  }

  // FNF:<number of functions found>
  private void printFNFLine(SourceFileCoverage sourceFile) throws IOException {
    bufferedWriter.write(Constants.FNF_MARKER);
    bufferedWriter.write(Integer.toString(sourceFile.nrFunctionsFound()));
    bufferedWriter.newLine();
  }

  // FNH:<number of functions hit>
  private void printFNHLine(SourceFileCoverage sourceFile) throws IOException {
    bufferedWriter.write(Constants.FNH_MARKER);
    bufferedWriter.write(Integer.toString(sourceFile.nrFunctionsHit()));
    bufferedWriter.newLine();
  }

  // BRDA:<line number>,<block number>,<branch number>,<taken>
  private void printBRDALines(SourceFileCoverage sourceFile) throws IOException {
    for (BranchCoverage branch : sourceFile.getAllBranches()) {
      if (branch.blockNumber().isEmpty() || branch.branchNumber().isEmpty()) {
        // We skip printing this as a BRDA line and print it later as a BA line.
        continue;
      }
      bufferedWriter.write(Constants.BRDA_MARKER);
      bufferedWriter.write(Integer.toString(branch.lineNumber()));
      bufferedWriter.write(Constants.DELIMITER);
      bufferedWriter.write(branch.blockNumber());
      bufferedWriter.write(Constants.DELIMITER);
      bufferedWriter.write(branch.branchNumber());
      bufferedWriter.write(Constants.DELIMITER);
      if (branch.wasExecuted()) {
        bufferedWriter.write(Integer.toString(branch.nrOfExecutions()));
      } else {
        bufferedWriter.write(Constants.TAKEN);
      }
      bufferedWriter.newLine();
    }
  }

  // BA:<line number>,<taken>
  private void printBALines(SourceFileCoverage sourceFile) throws IOException {
    for (BranchCoverage branch : sourceFile.getAllBranches()) {
      if (!branch.blockNumber().isEmpty() && !branch.branchNumber().isEmpty()) {
        // This branch was already printed with more information as a BRDA line.
        continue;
      }
      bufferedWriter.write(Constants.BA_MARKER);
      bufferedWriter.write(Integer.toString(branch.lineNumber()));
      bufferedWriter.write(Constants.DELIMITER);
      // 0 = branch was not executed
      // 1 = branch was executed but not taken
      // 2 = branch was executed and taken
      bufferedWriter.write(branch.wasExecuted() ? "2" : "0");
      bufferedWriter.newLine();
    }
  }

  // BRF:<number of branches found>
  private void printBRFLine(SourceFileCoverage sourceFile) throws IOException {
    if (sourceFile.nrBranchesFound() > 0) {
      bufferedWriter.write(Constants.BRF_MARKER);
      bufferedWriter.write(Integer.toString(sourceFile.nrBranchesFound()));
      bufferedWriter.newLine();
    }
  }

  // BRH:<number of branches hit>
  private void printBRHLine(SourceFileCoverage sourceFile) throws IOException {
    // Only print if there were any branches found.
    if (sourceFile.nrBranchesFound() > 0) {
      bufferedWriter.write(Constants.BRH_MARKER);
      bufferedWriter.write(Integer.toString(sourceFile.nrBranchesHit()));
      bufferedWriter.newLine();
    }
  }

  // DA:<line number>,<execution count>[,<checksum>]
  private void printDALines(SourceFileCoverage sourceFile) throws IOException {
    for (LineCoverage lineExecution : sourceFile.getAllLineExecution()) {
      bufferedWriter.write(Constants.DA_MARKER);
      bufferedWriter.write(Integer.toString(lineExecution.lineNumber()));
      bufferedWriter.write(Constants.DELIMITER);
      bufferedWriter.write(Integer.toString(lineExecution.executionCount()));
      if (lineExecution.checksum() != null) {
        bufferedWriter.write(Constants.DELIMITER);
        bufferedWriter.write(lineExecution.checksum());
      }
      bufferedWriter.newLine();
    }
  }

  // LH:<number of lines with a non-zero execution count>
  private void printLHLine(SourceFileCoverage sourceFile) throws IOException {
    bufferedWriter.write(Constants.LH_MARKER);
    bufferedWriter.write(Integer.toString(sourceFile.nrOfLinesWithNonZeroExecution()));
    bufferedWriter.newLine();
  }

  // LF:<number of instrumented lines>
  private void printLFLine(SourceFileCoverage sourceFile) throws IOException {
    bufferedWriter.write(Constants.LF_MARKER);
    bufferedWriter.write(Integer.toString(sourceFile.nrOfInstrumentedLines()));
    bufferedWriter.newLine();
  }

  // end_of_record
  private void printEndOfRecordLine() throws IOException {
    bufferedWriter.write(Constants.END_OF_RECORD_MARKER);
    bufferedWriter.newLine();
  }
}
