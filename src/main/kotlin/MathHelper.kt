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

@JvmName("stddevOfFloat")
fun Iterable<Float>.stddev(): Double = Math.sqrt(this
        .map { it - this.average() }
        .map(Math::abs)
        .map { Math.pow(it, 2.0) }
        .average()) / this.average()

@JvmName("stddevOfDouble")
fun Iterable<Double>.stddev(): Double = Math.sqrt(this
        .map { it - this.average() }
        .map(Math::abs)
        .map { Math.pow(it, 2.0) }
        .average()) / this.average()

@JvmName("stddevOfShort")
fun Iterable<Short>.stddev(): Double = Math.sqrt(this
        .map { it - this.average() }
        .map(Math::abs)
        .map { Math.pow(it, 2.0) }
        .average()) / this.average()

@JvmName("stddevOfInt")
fun Iterable<Int>.stddev(): Double = Math.sqrt(this
        .map { it - this.average() }
        .map(Math::abs)
        .map { Math.pow(it, 2.0) }
        .average()) / this.average()

@JvmName("stddevOfLong")
fun Iterable<Long>.stddev(): Double = Math.sqrt(this
        .map { it - this.average() }
        .map(Math::abs)
        .map { Math.pow(it, 2.0) }
        .average()) / this.average()