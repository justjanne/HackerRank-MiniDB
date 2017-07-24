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

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.StreamSupport

/**
 * Wrapper function calling the actual function to allow for easier unit testing
 */
fun main(args: Array<String>) {
    process(System.`in`.bufferedReader(StandardCharsets.US_ASCII), System.out::println)
}

/**
 *  The general concept here is simple:
 *
 *  We assign every row of input data an ID, then we process all sells in parallel,
 *  building bitmap indices of the data
 *
 *  After that we process all queries in parallel, and combine the bitmap indices to find all matching rows
 *  Additionally, we remove those rows from the result set whose row ID is higher than the row ID of the query (meaning
 *  that they are, from the perspective of the Query, in the future, and shouldn’t be included)
 *
 *  The final dataset is then printed sequentially again, to ensure proper ordering
 *
 */
fun process(input: BufferedReader, output: (Any) -> Unit) {
    // The number of row we will have to process
    val lineCount = input.readLine().toInt()
    // All data
    val data = input.readLines()

    // The idea is to have a bitset for each day marking if a specific row is on that day
    // This allows later to create a bitset of all rows that occur in a date range by just ORing them
    // Additionally, we can figure out all rows that match two conditions by ANDing the bitsets of those conditions
    // For example, to find all rows of product 1 and day 3, we do `byDay[3] AND byProduct[1]`
    val byDay = BitSetArray(100, lineCount)
    // Same for all others
    val byProduct = BitSetArray(10, lineCount)
    val byCategory = BitSetArray(4, lineCount)
    val byState = BitSetArray(7, lineCount)
    val byRegion = BitSetArray(25, lineCount)

    // This array will store for each query ID the number of results
    val results = IntArray(lineCount) {
        -1
    }

    // Iterate through all input rows for processing sells
    data.parallelIndexedStream()
            // filter for only sells
            .filter { (_, line) ->
                Sell.lineMatches(line)
            }
            // extractFromLine the sell data from the line, so that we have (rowID, sell)
            .map { (rowId, line) ->
                Pair(rowId, Sell.extractFromLine(line))
            }
            .forEach { (rowId, sell) ->
                // If the row has a day, set the lookup value for that day and rowID
                if (sell.day != -1) {
                    val bitset = byDay[sell.day - 1]
                    synchronized(bitset) {
                        bitset[rowId] = true
                    }
                }
                // Same for all others
                if (sell.product != -1) {
                    val bitset = byProduct[sell.product - 1]
                    synchronized(bitset) {
                        bitset[rowId] = true
                    }
                }
                if (sell.category != -1) {
                    val bitset = byCategory[sell.category - 1]
                    synchronized(bitset) {
                        bitset[rowId] = true
                    }
                }
                if (sell.state != -1) {
                    val bitset = byState[sell.state - 1]
                    synchronized(bitset) {
                        bitset[rowId] = true
                    }
                }
                if (sell.region != -1) {
                    val bitset = byRegion[sell.region - 1]
                    synchronized(bitset) {
                        bitset[rowId] = true
                    }
                }
            }

    // Iterate through all input rows again for processing queries
    data.parallelIndexedStream()
            // filter for only queries
            .filter { (_, line) ->
                Query.lineMatches(line)
            }
            // extractFromLine the query data from the line, so that we have (rowID, query)
            .map { (rowId, line) ->
                Pair(rowId, Query.extractFromLine(line))
            }
            .forEach { (rowId, query) ->
                // We create a new bitset for our result
                val resultSet = BitSet(lineCount)

                // We logically OR it with every day that our query is supposed to match
                query.dateRange().forEach { resultSet.or(byDay[it - 1]) }

                // If we have additionally a product constraint, apply that via a logical AND
                val anyProduct = query.product == -1
                if (!anyProduct) resultSet.and(byProduct[query.product - 1])

                // Same for all others
                val anyCategory = query.category == -1
                if (!anyCategory) resultSet.and(byCategory[query.category - 1])

                val anyState = query.state == -1
                if (!anyState) resultSet.and(byState[query.state - 1])

                val anyRegion = query.region == -1
                if (!anyRegion) resultSet.and(byRegion[query.region - 1])

                // Remove all results that were added after this query was supposed to run
                resultSet.clear(rowId, lineCount)

                // Store the number of results
                results[rowId] = resultSet.cardinality()
            }

    // Filter the results for those that actually were run, and print them in order
    // We have to do this step separately because the previous step was done in parallel, and printing from a
    // multithreaded context has undefined order
    results.filter { it != -1 }.forEach { output(it) }
}

data class Sell(val day: Int, val product: Int, val category: Int, val state: Int, val region: Int) {
    companion object {
        fun lineMatches(input: String) = input.startsWith("S")
        fun extractFromLine(input: String): Sell {
            val (_, dayStr, productGroup, stateGroup) = input.split(' ')
            val (productStr, categoryStr) = readGroup(productGroup)
            val (stateStr, regionStr) = readGroup(stateGroup)
            return Sell(
                    dayStr.toInt(),
                    productStr.toInt(),
                    categoryStr?.toIntOrNull() ?: -1,
                    stateStr.toInt(),
                    regionStr?.toIntOrNull() ?: -1
            )
        }
    }
}

data class Query(val startDay: Int, val endDay: Int, val product: Int, val category: Int, val state: Int, val region: Int) {
    /**
     * Returns a range for the date values that this query is searching for
     */
    fun dateRange() = if (endDay == -1)
        startDay..startDay
    else
        startDay..endDay

    companion object {
        fun lineMatches(input: String) = input.startsWith("Q")
        fun extractFromLine(input: String): Query {
            val (_, dayGroup, productGroup, stateGroup) = input.split(' ')

            val (startDayStr, endDayStr) = readGroup(dayGroup)
            val (productStr, categoryStr) = readGroup(productGroup)
            val (stateStr, regionStr) = readGroup(stateGroup)

            return Query(
                    startDayStr.toInt(),
                    endDayStr?.toIntOrNull() ?: -1,
                    productStr.toInt(),
                    categoryStr?.toIntOrNull() ?: -1,
                    stateStr.toInt(),
                    regionStr?.toIntOrNull() ?: -1
            )
        }
    }
}

/**
 * Creates an array containing $arraySize bitsets of $bitsetWidth width each
 */
private fun BitSetArray(arraySize: Int, bitsetWidth: Int) = Array(arraySize) {
    BitSet(bitsetWidth)
}

/**
 * Reads a group from an input string, separated by ., where the first element is mandatory and the second optional
 */
private fun readGroup(input: String): Pair<String, String?> {
    val inputSplit = input.split('.')
    return Pair(inputSplit[0], inputSplit.getOrNull(1))
}

/**
 * This helper function is used to provide a parallel, indexed stream, as Java (and Kotlin) have no way to zip streams,
 * or to create a stream over an index and the relevant value in any other way.
 */
private fun <T> List<T>.parallelIndexedStream() =
        StreamSupport.stream((0 until size).spliterator(), /* parallel = */ true).map { Pair(it, this[it]) }