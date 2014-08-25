// Copyright (c) 2012 Coverity, Inc. All rights reserved worldwide.

package sharath;

import java.io.File;
import java.io.PrintStream;

import org.junit.experimental.max.MaxCore;
import org.junit.internal.JUnitSystem;
import org.junit.internal.RealSystem;
import org.junit.internal.TextListener;
import org.junit.internal.requests.ClassRequest;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import static java.lang.String.format;

/**
 * Provides a main method which can invoke a single test.  Uses JUnit's MaxCore test runner
 * as a base, so we get a useful ordering of tests (most recently failed tests first).
 * <p>
 * I created this because JUnit 4 doesn't come with a command-line test runner that can
 * run a single method; you have to run a whole class at once.  I want to be able to run
 * a single method repeatedly; hance this test runner.  Can be used in conjunction with
 * <p>
 * Syntax for specifying a test: com.coverity.package.MyTestClass#myTestMethod, where the
 * #myTestMethod part is optional.  In Intellij, you can click on a test method and invoke
 * "copy reference" (cmd-opt-shift-C) to get a reference in this format.
 *
 * @author jbyler
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class RunTest {
    public static void main(final String[] args) throws ClassNotFoundException {
        if (args.length < 1 || args[0].contains("-help") || args.length > 1) {
            System.out.println("Usage: testClass[#methodName]");
            return;
        }
        final File resultsFile = new File(System.getProperty("java.io.tmpdir"), "coverity_junit_MaxCore_results");
        final MaxCore maxCore = MaxCore.storedLocally(resultsFile);
        final Request request = getRequest(args);
        final JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener(new CovRunListener(new RealSystem()));
        maxCore.run(request, jUnitCore);
    }

    private static Request getRequest(String[] args) throws ClassNotFoundException {
        // currently can only run one spec at a time.  Would be nice to be able to pass
        // a number of classes or class#method arguments and run them all.
        if (args.length != 1) {
            throw new IllegalArgumentException("Wrong number of args to getRequest");
        }

        final Request request;
        final String spec = args[0];
        if (spec.contains("#")) {
            String[] split = spec.split("#", 2);
            final Class testClass = Class.forName(split[0]);
            final String methodName = split[1];
            request = Request.method(testClass, methodName);
        } else {
            final Class testClass = Class.forName(args[0]);
            request = new ClassRequest(testClass);
        }
        return request;
    }

    private static class CovRunListener extends TextListener {
        private PrintStream writer;
        private int assumptionFailures = 0;

        public CovRunListener(JUnitSystem system) {
            this(system.out());
        }

        public CovRunListener(PrintStream writer) {
            super(writer);
            this.writer = writer;
        }

        @Override
        public void testFailure(Failure failure) {
            writer.append('\u0008').append('E');
        }

        @Override
        public void testIgnored(Description description) {
            writer.append('\u0008').append('I');
        }

        @Override
        public void testAssumptionFailure(Failure failure) {
            writer.append('\u0008').append('V');
            assumptionFailures++;
        }

        @Override
        protected void printFooter(Result result) {
            writer.println();
            if (result.wasSuccessful()) {
                writer.print("OK (");
            } else {
                writer.print("FAILURES!!! (");
            }

            if (result.getFailureCount() > 0 || result.getIgnoreCount() > 0 || assumptionFailures > 0) {
                writer.print("Of ");
            }

            writer.print(format("%d %s", result.getRunCount(), pluralize(result.getRunCount(), "test", "tests")));

            if (result.getFailureCount() > 0) {
                writer.print(format(", %d failed", result.getFailureCount()));
            }

            if (result.getIgnoreCount() > 0) {
                writer.print(format(", %d %s ignored", result.getIgnoreCount(), pluralize(result.getIgnoreCount(), "test was", "tests were")));
            }

            if (assumptionFailures > 0) {
                writer.print(format(", %d %s skipped due to %s",
                        assumptionFailures,
                        pluralize(assumptionFailures, "test was", "tests were"),
                        pluralize(assumptionFailures, "an assumption violation", "assumption violations")));
            }

            writer.println(")");
            writer.println();
        }

        private static String pluralize(int count, String singular, String plural) {
            return count == 1 ? singular : plural;
        }
    }

}
