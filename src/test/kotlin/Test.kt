/*
 *  Copyright (C) 2017 Janne Koschinski
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Test
import java.io.BufferedReader
import java.nio.charset.StandardCharsets

class MainTest {
    @Test
    fun test() {
        preWarm()

        runTest("/testData1")
        runTest("/testData2")
        runTest("/testData3")
    }

    companion object {
        private fun preWarm() {
            for (i in 0..50) {
                runSingleTest(getReader("/testData1"))
                runSingleTest(getReader("/testData2"))
                runSingleTest(getReader("/testData3"))
            }
        }

        private fun runTest(name: String) {
            val results = (0..20).map {
                this.runSingleTest(MainTest.getReader(name))
            }
            println(String.format("%s: %.2fms±%.2f%%", name, results.average() * 0.000_001, results.stddev() * 100))
        }

        private fun runSingleTest(reader: BufferedReader, output: (Any) -> Unit = {}): Long {
            val before = System.nanoTime()
            process(reader, output)
            val after = System.nanoTime()
            return after - before
        }

        private fun getReader(name: String) =
                MainTest::class.java.getResourceAsStream(name).bufferedReader(StandardCharsets.US_ASCII)
    }
}